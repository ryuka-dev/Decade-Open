package pl.skidam.automodpack_core.auth;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SecretsStoreTest {
    private static final String A = "11111111-1111-1111-1111-111111111111";
    private static final String B = "22222222-2222-2222-2222-222222222222";
    private static final long LIFETIME = 336 * 3600;
    private static final long NOW = 1_800_000_000L;

    private static Secrets.Secret issued(long secondsAgo) {
        return new Secrets.Secret("s" + secondsAgo, NOW - secondsAgo);
    }

    @Test
    void theStoredKeyStillNamesThePlayer() {
        assertEquals(A, SecretsStore.playerOf(A));                // upstream's key: the UUID alone
        assertEquals(A, SecretsStore.playerOf(A + "#1a2b3c"));
    }

    @Test
    void aSecondLoginKeepsTheFirstSecret() {
        Map<String, Secrets.Secret> stored = new HashMap<>();
        stored.put(A + "#1", issued(60));
        assertEquals(List.of(), SecretsStore.keysToDrop(stored, A, NOW, LIFETIME));
    }

    @Test
    void keepsTheNewestThreeSoTheNewOneMakesFour() {
        Map<String, Secrets.Secret> stored = new HashMap<>();
        stored.put(A + "#1", issued(400));
        stored.put(A + "#2", issued(300));
        stored.put(A + "#3", issued(200));
        stored.put(A, issued(100));                                // an upstream key counts like the others
        assertEquals(List.of(A + "#1"), SecretsStore.keysToDrop(stored, A, NOW, LIFETIME));
    }

    @Test
    void dropsExpiredSecretsAndLeavesOtherPlayersAlone() {
        Map<String, Secrets.Secret> stored = new HashMap<>();
        stored.put(A + "#old", issued(LIFETIME + 1));
        stored.put(A + "#new", issued(10));
        stored.put(B + "#old", issued(LIFETIME + 1));
        assertEquals(List.of(A + "#old"), SecretsStore.keysToDrop(stored, A, NOW, LIFETIME));
    }

    @Test
    void aSecretWithoutATimeCountsAsTheOldest() {
        Map<String, Secrets.Secret> stored = new HashMap<>();
        stored.put(A + "#none", new Secrets.Secret("x", null));
        stored.put(A + "#1", issued(30));
        stored.put(A + "#2", issued(20));
        stored.put(A + "#3", issued(10));
        assertEquals(List.of(A + "#none"), SecretsStore.keysToDrop(stored, A, NOW, LIFETIME));
    }
}
