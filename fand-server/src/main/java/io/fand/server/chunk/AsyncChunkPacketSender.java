package io.fand.server.chunk;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.SharedConstants;
import net.minecraft.network.protocol.game.ClientboundChunkBatchFinishedPacket;
import net.minecraft.network.protocol.game.ClientboundChunkBatchStartPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacketData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prepares full chunk packets away from the tick thread while keeping the
 * vanilla batch framing and packet send order on the connection thread.
 */
public final class AsyncChunkPacketSender implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncChunkPacketSender.class);
    private static final int MAX_AUTO_THREADS = 4;

    private final AtomicBoolean closed = new AtomicBoolean(false);
    private volatile boolean enabled;
    private volatile ExecutorService executor;

    public AsyncChunkPacketSender(boolean enabled) {
        this.enabled = enabled;
        this.executor = createExecutor();
    }

    public boolean enabled() {
        return this.enabled && !this.closed.get();
    }

    public void reconfigure(boolean enabled) {
        this.enabled = enabled;
    }

    public @Nullable PendingBatch submit(ServerPlayer player, List<LevelChunk> chunks) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(chunks, "chunks");
        if (!this.enabled() || chunks.isEmpty()) {
            return null;
        }

        ServerLevel level = player.level();
        var snapshots = new ArrayList<ChunkPacketSnapshot>(chunks.size());
        var chunkKeys = new LongOpenHashSet(chunks.size());
        for (LevelChunk chunk : chunks) {
            chunkKeys.add(chunk.getPos().pack());
            snapshots.add(new ChunkPacketSnapshot(
                    chunk,
                    ClientboundLevelChunkPacketData.fand$snapshot(chunk),
                    new ClientboundLightUpdatePacketData(chunk.getPos(), level.getLightEngine(), null, null)));
        }

        try {
            var future = CompletableFuture.supplyAsync(() -> prepare(snapshots), this.executor);
            return new PendingBatch(chunkKeys, future);
        } catch (RejectedExecutionException rejected) {
            return null;
        }
    }

    public int sendIfReady(ServerGamePacketListenerImpl connection, PendingBatch batch) {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(batch, "batch");
        if (!batch.ready()) {
            return 0;
        }

        PreparedBatch prepared = batch.join();
        if (prepared.packets().isEmpty()) {
            return 0;
        }

        ServerLevel level = connection.player.level();
        int sent = 0;
        for (ClientboundLevelChunkWithLightPacket packet : prepared.packets()) {
            long key = ChunkPos.pack(packet.getX(), packet.getZ());
            if (!batch.contains(key)) {
                continue;
            }
            if (sent == 0) {
                connection.send(ClientboundChunkBatchStartPacket.INSTANCE);
            }
            connection.send(packet);
            sent++;
            if (SharedConstants.DEBUG_VERBOSE_SERVER_EVENTS) {
                LOGGER.debug("SEN {}", new ChunkPos(packet.getX(), packet.getZ()));
            }
            level.debugSynchronizers().startTrackingChunk(connection.player, new ChunkPos(packet.getX(), packet.getZ()));
        }
        if (sent > 0) {
            connection.send(new ClientboundChunkBatchFinishedPacket(sent));
        }
        return sent;
    }

    @Override
    public void close() {
        if (!this.closed.compareAndSet(false, true)) {
            return;
        }
        this.executor.shutdown();
        try {
            if (!this.executor.awaitTermination(3L, TimeUnit.SECONDS)) {
                this.executor.shutdownNow();
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            this.executor.shutdownNow();
        }
    }

    private static PreparedBatch prepare(List<ChunkPacketSnapshot> snapshots) {
        var packets = new ArrayList<ClientboundLevelChunkWithLightPacket>(snapshots.size());
        for (ChunkPacketSnapshot snapshot : snapshots) {
            packets.add(new ClientboundLevelChunkWithLightPacket(snapshot.chunk, snapshot.chunkSnapshot, snapshot.lightData));
        }
        return new PreparedBatch(List.copyOf(packets));
    }

    private static ExecutorService createExecutor() {
        int threads = Math.max(1, Math.min(MAX_AUTO_THREADS, Runtime.getRuntime().availableProcessors() / 4));
        AtomicInteger sequence = new AtomicInteger(1);
        return Executors.newFixedThreadPool(threads, task -> {
            Thread thread = new Thread(task, "Fand Async Chunk Packet #" + sequence.getAndIncrement());
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            thread.setUncaughtExceptionHandler((runningThread, throwable) ->
                    LOGGER.error("Uncaught exception in thread {}", runningThread.getName(), throwable));
            return thread;
        });
    }

    public static final class PendingBatch {

        private final LongSet chunkKeys;
        private final CompletableFuture<PreparedBatch> future;

        private PendingBatch(LongSet chunkKeys, CompletableFuture<PreparedBatch> future) {
            this.chunkKeys = chunkKeys;
            this.future = future;
        }

        public boolean ready() {
            return this.future.isDone();
        }

        public boolean failed() {
            return this.future.isCompletedExceptionally() || this.future.isCancelled();
        }

        public boolean contains(long chunkKey) {
            return this.chunkKeys.contains(chunkKey);
        }

        public void drop(long chunkKey) {
            this.chunkKeys.remove(chunkKey);
        }

        public boolean empty() {
            return this.chunkKeys.isEmpty();
        }

        public void restoreTo(LongSet target) {
            target.addAll(this.chunkKeys);
        }

        private PreparedBatch join() {
            return this.future.join();
        }
    }

    private record PreparedBatch(List<ClientboundLevelChunkWithLightPacket> packets) {
    }

    private record ChunkPacketSnapshot(
            LevelChunk chunk,
            ClientboundLevelChunkPacketData.FandSnapshot chunkSnapshot,
            ClientboundLightUpdatePacketData lightData
    ) {
    }
}
