package io.fand.api.structure;

import io.fand.api.world.Location;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.key.Key;

public interface StructureService {

    Optional<StructureTemplate> template(Key key);

    CompletableFuture<Boolean> save(Key key, StructureVolume volume);

    CompletableFuture<Boolean> place(Key key, Location origin, StructurePlacement placement);

    CompletableFuture<Optional<Location>> locate(Key structure, Location origin, int radius);

    static StructureService empty() {
        return new StructureService() {
            @Override
            public Optional<StructureTemplate> template(Key key) {
                return Optional.empty();
            }

            @Override
            public CompletableFuture<Boolean> save(Key key, StructureVolume volume) {
                return CompletableFuture.completedFuture(false);
            }

            @Override
            public CompletableFuture<Boolean> place(Key key, Location origin, StructurePlacement placement) {
                return CompletableFuture.completedFuture(false);
            }

            @Override
            public CompletableFuture<Optional<Location>> locate(Key structure, Location origin, int radius) {
                return CompletableFuture.completedFuture(Optional.empty());
            }
        };
    }
}
