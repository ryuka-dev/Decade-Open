package pl.skidam.automodpack_core.decade;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import pl.skidam.automodpack_core.config.Jsons;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Decides whether an update the server offers was signed by Decade, before anything is downloaded from it.
 *
 * <p>Upstream AutoModpack trusts whatever the server lists; a jar it cannot find on Modrinth or CurseForge
 * only earns a warning screen. Here the server's list is checked against a list signed offline: every file
 * the server offers must be in the signed list with the same SHA-1, every signed file must be offered, every
 * deletion the server asks for must be signed too, and the pack version must not go below the highest one
 * seen. Anything else rejects the whole update; there is no "continue anyway".
 *
 * <p>Pure: bytes in, an {@link Outcome} out. Downloading the signed files and saving the new state belong to
 * the caller, and the new state must be saved only when {@link Outcome#trusted()} is true.
 */
public final class PackVerifier {
    public static final String MANIFEST = "/decade/pack-manifest.json";
    public static final String MANIFEST_SIG = "/decade/pack-manifest.sig";
    public static final String ROTATION = "/decade/key-rotation.json";
    public static final String ROTATION_SIG = "/decade/key-rotation.sig";
    /** The signed files themselves travel in the server's list but cannot be in the list they sign. */
    public static final Set<String> SIGNATURE_FILES = Set.of(MANIFEST, MANIFEST_SIG, ROTATION, ROTATION_SIG);

    static final int FORMAT = 1;
    static final String PACK_NAME = "Decade";

    private PackVerifier() {
    }

    /** Keys the jar ships with, decoded. */
    public record Anchors(List<byte[]> primary, List<byte[]> emergency, List<byte[]> test) {
        public static Anchors builtIn() {
            return new Anchors(decode(DecadeKeys.PRIMARY), decode(DecadeKeys.EMERGENCY), decode(DecadeKeys.TEST));
        }

        private static List<byte[]> decode(List<String> keys) {
            List<byte[]> out = new ArrayList<>();
            for (String k : keys) out.add(Ed25519.decodeKey(k));
            return out;
        }
    }

    /**
     * @param trusted     true only when every check passed
     * @param packVersion the signed pack version, or -1 when the list could not be read
     * @param keyId       the key that signed the list, or null
     * @param testKey     the list was signed by a test key: fine on the test server, never for players
     * @param problems    why it was rejected, one line each; empty when trusted
     * @param state       the state to save when trusted (rotation applied, highest version raised)
     */
    public record Outcome(boolean trusted, long packVersion, String keyId, boolean testKey, List<String> problems,
                          TrustState state) {
    }

    /**
     * @param manifest    bytes of {@link #MANIFEST} as downloaded, or null when the server offered none
     * @param manifestSig bytes of {@link #MANIFEST_SIG}, or null
     * @param rotation    bytes of {@link #ROTATION}, or null when the server offers no rotation
     * @param rotationSig bytes of {@link #ROTATION_SIG}, or null
     * @param content     the list the server offers
     */
    public static Outcome verify(byte[] manifest, byte[] manifestSig, byte[] rotation, byte[] rotationSig,
                                 Jsons.ModpackContentFields content, TrustState current, Anchors anchors) {
        // everything here comes from outside: a malformed input rejects the update, it never throws
        try {
            return check(manifest, manifestSig, rotation, rotationSig, content, current, anchors);
        } catch (RuntimeException e) {
            return rejected(new ArrayList<>(List.of("malformed input: " + e)), current);
        }
    }

    private static Outcome check(byte[] manifest, byte[] manifestSig, byte[] rotation, byte[] rotationSig,
                                 Jsons.ModpackContentFields content, TrustState current, Anchors anchors) {
        List<String> problems = new ArrayList<>();
        if (content == null || content.list == null) {
            problems.add("the server offers no list");
            return rejected(problems, current);
        }
        TrustState state = current.copy();

        if (rotation != null || rotationSig != null) {
            applyRotation(rotation, rotationSig, state, anchors, problems);
            if (!problems.isEmpty()) {
                return rejected(problems, state);
            }
        }

        if (manifest == null || manifestSig == null) {
            problems.add("the server offers no signed list");
            return rejected(problems, state);
        }

        // key id -> key, from the jar plus adopted keys, less revoked ones
        Map<String, byte[]> primary = new HashMap<>();
        Set<String> testIds = new HashSet<>();
        for (byte[] k : anchors.primary()) primary.put(Ed25519.keyId(k), k);
        for (String k : state.adoptedPrimary) {
            try {
                byte[] raw = Ed25519.decodeKey(k);
                primary.put(Ed25519.keyId(raw), raw);
            } catch (IllegalArgumentException e) {
                problems.add("an adopted key in the saved state is malformed");
            }
        }
        for (byte[] k : anchors.test()) {
            primary.put(Ed25519.keyId(k), k);
            testIds.add(Ed25519.keyId(k));
        }
        primary.keySet().removeAll(state.revoked);

        JsonObject m;
        try {
            m = JsonParser.parseString(new String(manifest, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException e) {
            problems.add("the signed list is not valid JSON");
            return rejected(problems, state);
        }
        String keyId = string(m, "keyId");
        byte[] key = keyId == null ? null : primary.get(keyId);
        if (key == null) {
            problems.add("the list names key " + keyId + ", which this client does not trust (unknown or revoked)");
            return rejected(problems, state);
        }
        if (!Ed25519.verify(key, manifest, manifestSig)) {
            problems.add("the list's signature does not match key " + keyId + ": it was changed or not signed by it");
            return rejected(problems, state);
        }

        // from here on the list is authentic; what remains is whether the server offers exactly what it says
        long packVersion = number(m, "packVersion");
        if (number(m, "format") != FORMAT) {
            problems.add("unknown list format " + m.get("format"));
        }
        if (!PACK_NAME.equals(string(m, "packName"))) {
            problems.add("the list is for pack " + string(m, "packName") + ", not " + PACK_NAME);
        }
        if (packVersion < state.highestPackVersion) {
            problems.add("pack version " + packVersion + " is older than version " + state.highestPackVersion
                    + " already seen here: an old list replayed");
        }

        Map<String, String> signedFiles = map(m, "files", problems);
        Map<String, String> signedDeletes = map(m, "delete", problems);
        Set<String> offered = new HashSet<>();
        for (Jsons.ModpackContentFields.ModpackContentItem item : content.list) {
            String path = item.file;
            if (SIGNATURE_FILES.contains(path)) {
                continue;
            }
            offered.add(path);
            if (!safePath(path)) {
                problems.add("refusing path " + path);
            }
            String signed = signedFiles.get(path);
            if (signed == null) {
                problems.add("offered but not signed: " + path);
            } else if (!signed.equalsIgnoreCase(item.sha1)) {
                problems.add("offered with another hash than signed: " + path);
            }
        }
        for (String path : signedFiles.keySet()) {
            if (!offered.contains(path)) {
                problems.add("signed but not offered: " + path);
            }
        }
        if (content.nonModpackFilesToDelete != null) {
            for (Jsons.ModpackContentFields.FileToDelete d : content.nonModpackFilesToDelete) {
                String signed = signedDeletes.get(d.file);
                if (signed == null || !signed.equalsIgnoreCase(d.sha1)) {
                    problems.add("deletion not signed: " + d.file);
                }
            }
        }

        if (!problems.isEmpty()) {
            return new Outcome(false, packVersion, keyId, testIds.contains(keyId), problems, current);
        }
        state.highestPackVersion = Math.max(state.highestPackVersion, packVersion);
        return new Outcome(true, packVersion, keyId, testIds.contains(keyId), List.of(), state);
    }

    private static void applyRotation(byte[] rotation, byte[] rotationSig, TrustState state, Anchors anchors,
                                      List<String> problems) {
        if (rotation == null || rotationSig == null) {
            problems.add("a key rotation is offered without its signature or without its statement");
            return;
        }
        JsonObject r;
        try {
            r = JsonParser.parseString(new String(rotation, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException e) {
            problems.add("the key rotation is not valid JSON");
            return;
        }
        String keyId = string(r, "keyId");
        byte[] key = null;
        for (byte[] k : anchors.emergency()) {
            if (Ed25519.keyId(k).equals(keyId)) key = k;
        }
        if (key == null || !Ed25519.verify(key, rotation, rotationSig)) {
            problems.add("the key rotation is not signed by an emergency key this client trusts");
            return;
        }
        if (number(r, "format") != FORMAT) {
            problems.add("unknown key rotation format " + r.get("format"));
            return;
        }
        long sequence = number(r, "sequence");
        if (sequence <= state.rotationSequence) {
            return;   // already applied, or older than one that was
        }
        List<String> newPrimary = new ArrayList<>();
        for (JsonElement e : array(r, "primary", problems)) {
            try {
                Ed25519.decodeKey(e.getAsString());
                newPrimary.add(e.getAsString());
            } catch (RuntimeException ex) {
                problems.add("the key rotation names a malformed key");
            }
        }
        if (!problems.isEmpty()) {
            return;
        }
        for (JsonElement e : array(r, "revoke", problems)) {
            state.revoked.add(e.getAsString());
        }
        state.adoptedPrimary = newPrimary;
        state.rotationSequence = sequence;
    }

    /** A path the updater may write: absolute within the game directory, no way up, nothing in automodpack/. */
    static boolean safePath(String path) {
        if (path == null || !path.startsWith("/") || path.contains("\\") || path.contains("\0")) {
            return false;
        }
        for (String part : path.substring(1).split("/", -1)) {
            if (part.isEmpty() || part.equals(".") || part.equals("..")) return false;
        }
        String lower = path.toLowerCase(Locale.ROOT);
        return !lower.startsWith("/automodpack/");
    }

    private static Outcome rejected(List<String> problems, TrustState state) {
        return new Outcome(false, -1, null, false, problems, state);
    }

    private static String string(JsonObject o, String name) {
        JsonElement e = o.get(name);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
    }

    private static long number(JsonObject o, String name) {
        JsonElement e = o.get(name);
        try {
            return e != null && e.isJsonPrimitive() ? e.getAsLong() : -1;
        } catch (RuntimeException ex) {
            return -1;
        }
    }

    private static Map<String, String> map(JsonObject o, String name, List<String> problems) {
        Map<String, String> out = new LinkedHashMap<>();
        JsonElement e = o.get(name);
        if (e == null || !e.isJsonObject()) {
            problems.add("the signed list has no " + name);
            return out;
        }
        for (Map.Entry<String, JsonElement> entry : e.getAsJsonObject().entrySet()) {
            out.put(entry.getKey(), entry.getValue().getAsString());
        }
        return out;
    }

    private static List<JsonElement> array(JsonObject o, String name, List<String> problems) {
        JsonElement e = o.get(name);
        if (e == null || !e.isJsonArray()) {
            problems.add("the key rotation has no " + name);
            return List.of();
        }
        List<JsonElement> out = new ArrayList<>();
        e.getAsJsonArray().forEach(out::add);
        return out;
    }
}
