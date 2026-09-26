package pl.skidam.automodpack_core.auth;

import pl.skidam.automodpack_core.GlobalVariables;
import pl.skidam.automodpack_core.config.ConfigTools;
import pl.skidam.automodpack_core.config.Jsons;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SecretsStore {
    private static class SecretsCache {
        private final ConcurrentMap<String, Secrets.Secret> cache;
        private Jsons.SecretsFields db;
        private final Path configFile;

        public SecretsCache(Path configFile) {
            this.configFile = configFile;
            this.cache = new ConcurrentHashMap<>();
        }

        public synchronized void load() {
            if (db != null)
                return;
            db = ConfigTools.load(configFile, Jsons.SecretsFields.class);
            if (db != null && db.secrets != null && !db.secrets.isEmpty()) {
                cache.putAll(db.secrets);
            }
        }

        public synchronized void save() {
            ConfigTools.save(configFile, db);
        }

        public void remove(String key) {
            load();
            cache.remove(key);
            if (db != null && db.secrets != null) {
                db.secrets.remove(key);
            }
        }

        public Secrets.Secret get(String key) {
            load();
            return cache.get(key);
        }

        public void save(String key, Secrets.Secret secret) throws IllegalArgumentException {
            if (key == null || key.isBlank() || secret == null || secret.secret().isBlank())
                throw new IllegalArgumentException("Key or secret cannot be null or blank");
            load();
            cache.put(key, secret);
            if (db == null) {
                db = new Jsons.SecretsFields();
            }
            db.secrets.put(key, secret);
            save();
        }
    }

    private static final SecretsCache hostSecrets = new SecretsCache(GlobalVariables.serverSecretsFile);
    private static final SecretsCache clientSecrets = new SecretsCache(GlobalVariables.clientSecretsFile);

    // Decade: upstream keeps one secret per player and replaces it on every login, so a player who joins from a
    // second installation (another computer, another launcher profile) invalidates the first one's secret, and
    // that installation can no longer update at launch; a download already running is cut off too (both seen on
    // the test server). Here each login adds a secret under "<uuid>#<n>" and a player keeps the newest
    // MAX_SECRETS_PER_PLAYER that have not expired. Keys without "#" are upstream's and are read the same way.
    static final int MAX_SECRETS_PER_PLAYER = 4;

    private static long issued(Secrets.Secret secret) {
        return secret.timestamp() == null ? 0 : secret.timestamp();
    }

    /** The player a stored key belongs to: the key itself, or the part before "#". */
    static String playerOf(String key) {
        int hash = key.indexOf('#');
        return hash < 0 ? key : key.substring(0, hash);
    }

    public static Map.Entry<String, Secrets.Secret>  getHostSecret(String secret) {
        hostSecrets.load();
        for (var entry : hostSecrets.cache.entrySet()) {
            var thisSecret = entry.getValue().secret();
            if (Objects.equals(thisSecret, secret)) {
                // callers take the key for the player's UUID (whitelist and ban checks)
                return Map.entry(playerOf(entry.getKey()), entry.getValue());
            }
        }

        return null;
    }

    public static void saveHostSecret(String uuid, Secrets.Secret secret) {
        hostSecrets.load();
        long now = System.currentTimeMillis() / 1000;
        long lifetime = GlobalVariables.serverConfig == null ? Long.MAX_VALUE : GlobalVariables.serverConfig.secretLifetime * 3600;
        for (String key : keysToDrop(hostSecrets.cache, uuid, now, lifetime)) {
            hostSecrets.remove(key);
        }
        hostSecrets.save(uuid + "#" + Long.toHexString(System.nanoTime()), secret);
    }

    /**
     * Which of this player's stored secrets to drop before a new one is added: the expired ones, and the oldest
     * beyond MAX_SECRETS_PER_PLAYER - 1, so that with the new one the player holds at most MAX_SECRETS_PER_PLAYER.
     */
    static java.util.List<String> keysToDrop(Map<String, Secrets.Secret> stored, String uuid, long now, long lifetime) {
        var mine = new java.util.ArrayList<Map.Entry<String, Secrets.Secret>>();
        for (var entry : stored.entrySet()) {
            if (playerOf(entry.getKey()).equals(uuid)) mine.add(entry);
        }
        mine.sort((a, b) -> Long.compare(issued(b.getValue()), issued(a.getValue())));   // newest first
        var drop = new java.util.ArrayList<String>();
        int kept = 0;
        for (var entry : mine) {
            boolean expired = issued(entry.getValue()) + lifetime <= now;
            if (expired || kept >= MAX_SECRETS_PER_PLAYER - 1) {
                drop.add(entry.getKey());
            } else {
                kept++;
            }
        }
        return drop;
    }

    public static Secrets.Secret getClientSecret(String modpack) {
        return clientSecrets.get(modpack);
    }

    public static void saveClientSecret(String modpack, Secrets.Secret secret) throws IllegalArgumentException {
        clientSecrets.save(modpack, secret);
    }
}
