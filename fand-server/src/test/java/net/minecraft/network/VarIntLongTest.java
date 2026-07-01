package net.minecraft.network;

import static org.assertj.core.api.Assertions.assertThat;

import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

final class VarIntLongTest {

    @Test
    void varIntWriteAndReadRoundTripsKnownValues() {
        for (int value : List.of(0, 1, 127, 128, 255, 16_383, 16_384, 2_097_151, 2_097_152, Integer.MAX_VALUE, -1, Integer.MIN_VALUE)) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeVarInt(value);

            assertThat(buffer.writerIndex()).isEqualTo(VarInt.getByteSize(value));
            assertThat(buffer.readVarInt()).isEqualTo(value);
        }
    }

    @Test
    void varLongWriteAndReadRoundTripsKnownValues() {
        for (long value : List.of(0L, 1L, 127L, 128L, 255L, 16_383L, 16_384L, 2_097_151L, 2_097_152L, 268_435_455L, 268_435_456L, Long.MAX_VALUE, -1L, Long.MIN_VALUE)) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeVarLong(value);

            assertThat(buffer.writerIndex()).isEqualTo(VarLong.getByteSize(value));
            assertThat(buffer.readVarLong()).isEqualTo(value);
        }
    }

    @Test
    void varIntAndVarLongSizeMatchExpectedBoundaries() {
        assertThat(VarInt.getByteSize(0)).isEqualTo(1);
        assertThat(VarInt.getByteSize(127)).isEqualTo(1);
        assertThat(VarInt.getByteSize(128)).isEqualTo(2);
        assertThat(VarInt.getByteSize(16_384)).isEqualTo(3);
        assertThat(VarInt.getByteSize(Integer.MAX_VALUE)).isEqualTo(5);
        assertThat(VarInt.getByteSize(-1)).isEqualTo(5);

        assertThat(VarLong.getByteSize(0L)).isEqualTo(1);
        assertThat(VarLong.getByteSize(127L)).isEqualTo(1);
        assertThat(VarLong.getByteSize(128L)).isEqualTo(2);
        assertThat(VarLong.getByteSize(16_384L)).isEqualTo(3);
        assertThat(VarLong.getByteSize(2_097_152L)).isEqualTo(4);
        assertThat(VarLong.getByteSize(268_435_456L)).isEqualTo(5);
        assertThat(VarLong.getByteSize(Long.MAX_VALUE)).isEqualTo(9);
        assertThat(VarLong.getByteSize(-1L)).isEqualTo(10);
    }
}
