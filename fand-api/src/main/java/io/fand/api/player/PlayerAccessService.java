package io.fand.api.player;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

/**
 * Player identity lookup and server access-list controls.
 */
public interface PlayerAccessService {

    CompletableFuture<Optional<PlayerProfile>> profile(String name);

    CompletableFuture<Optional<PlayerProfile>> profile(UUID uniqueId);

    PlayerProfile offlineProfile(String name);

    default CompletableFuture<Optional<OfflinePlayer>> offlinePlayer(UUID uniqueId) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    default CompletableFuture<Optional<OfflinePlayer>> offlinePlayer(String name) {
        return profile(name).thenCompose(profile -> profile
                .map(value -> offlinePlayer(value.uniqueId()))
                .orElseGet(() -> CompletableFuture.completedFuture(Optional.empty())));
    }

    Collection<BanEntry> bans();

    Optional<BanEntry> ban(PlayerProfile profile);

    boolean banned(PlayerProfile profile);

    default boolean ban(PlayerProfile profile, String source, String reason) {
        return ban(profile, source, reason, null);
    }

    boolean ban(PlayerProfile profile, String source, String reason, @Nullable Instant expires);

    boolean pardon(PlayerProfile profile);

    Collection<PlayerProfile> whitelist();

    boolean whitelisted(PlayerProfile profile);

    boolean addWhitelist(PlayerProfile profile);

    boolean removeWhitelist(PlayerProfile profile);

    boolean whitelistEnabled();

    void setWhitelistEnabled(boolean enabled);

    Collection<OperatorEntry> operators();

    Optional<OperatorEntry> operator(PlayerProfile profile);

    boolean isOperator(PlayerProfile profile);

    default boolean op(PlayerProfile profile) {
        return op(profile, OperatorLevel.OWNERS, false);
    }

    boolean op(PlayerProfile profile, OperatorLevel level, boolean bypassesPlayerLimit);

    boolean deop(PlayerProfile profile);
}
