package pl.skidam.automodpack_core.decade;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.skidam.automodpack_core.config.Jsons;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class PackVerifierTest {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String SHA_A = "da39a3ee5e6b4b0d3255bfef95601890afd80709";
    private static final String SHA_B = "356a192b7913b04c54574d18c28d46e6395428ab";

    /** A key pair and its raw public key, as the signing side makes them. */
    record Key(KeyPair pair, byte[] raw) {
        static Key generate() throws Exception {
            KeyPair pair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
            byte[] encoded = pair.getPublic().getEncoded();
            return new Key(pair, Arrays.copyOfRange(encoded, encoded.length - 32, encoded.length));
        }

        String id() {
            return Ed25519.keyId(raw);
        }

        String base64() {
            return Base64.getEncoder().encodeToString(raw);
        }

        byte[] sign(byte[] body) throws Exception {
            Signature s = Signature.getInstance("Ed25519");
            s.initSign(pair.getPrivate());
            s.update(body);
            return (Base64.getEncoder().encodeToString(s.sign()) + "\n").getBytes(StandardCharsets.US_ASCII);
        }
    }

    private static byte[] manifest(Key key, long version, Map<String, String> files, Map<String, String> delete) {
        Map<String, Object> m = new TreeMap<>();
        m.put("format", 1);
        m.put("packName", "Decade");
        m.put("packVersion", version);
        m.put("keyId", key.id());
        m.put("files", files);
        m.put("delete", delete);
        return (GSON.toJson(m) + "\n").getBytes(StandardCharsets.UTF_8);
    }

    private static Map<String, String> files() {
        Map<String, String> f = new LinkedHashMap<>();
        f.put("/mods/a.jar", SHA_A);
        f.put("/tacz/p/b.json", SHA_B);
        return f;
    }

    private static Jsons.ModpackContentFields content(Map<String, String> offered) {
        Set<Jsons.ModpackContentFields.ModpackContentItem> items = new HashSet<>();
        offered.forEach((path, sha) -> items.add(item(path, sha)));
        // the signed files themselves are always in the server's list
        items.add(item(PackVerifier.MANIFEST, "0000000000000000000000000000000000000001"));
        items.add(item(PackVerifier.MANIFEST_SIG, "0000000000000000000000000000000000000002"));
        return new Jsons.ModpackContentFields(items);
    }

    private static Jsons.ModpackContentFields.ModpackContentItem item(String path, String sha) {
        return new Jsons.ModpackContentFields.ModpackContentItem(path, "1", "mod", false, false, sha, null);
    }

    private static PackVerifier.Anchors anchors(Key primary, Key emergency) {
        return new PackVerifier.Anchors(List.of(primary.raw), emergency == null ? List.of() : List.of(emergency.raw), List.of());
    }

    @Test
    void acceptsTheSignedListAndRaisesTheHighestVersion() throws Exception {
        Key k = Key.generate();
        byte[] body = manifest(k, 3, files(), Map.of());
        var out = PackVerifier.verify(body, k.sign(body), null, null, content(files()), new TrustState(), anchors(k, null));
        assertTrue(out.trusted(), () -> out.problems().toString());
        assertEquals(3, out.state().highestPackVersion);
        assertEquals(k.id(), out.keyId());
        assertFalse(out.testKey());
    }

    @Test
    void rejectsAListChangedAfterSigning() throws Exception {
        Key k = Key.generate();
        byte[] body = manifest(k, 3, files(), Map.of());
        byte[] sig = k.sign(body);
        byte[] changed = new String(body, StandardCharsets.UTF_8).replace(SHA_A, SHA_B).getBytes(StandardCharsets.UTF_8);
        var out = PackVerifier.verify(changed, sig, null, null, content(files()), new TrustState(), anchors(k, null));
        assertFalse(out.trusted());
    }

    @Test
    void rejectsAListSignedByAnUntrustedKey() throws Exception {
        Key trusted = Key.generate(), other = Key.generate();
        byte[] body = manifest(other, 3, files(), Map.of());
        var out = PackVerifier.verify(body, other.sign(body), null, null, content(files()), new TrustState(), anchors(trusted, null));
        assertFalse(out.trusted());
    }

    @Test
    void rejectsASignatureOfTheRightKeyOverOtherBytes() throws Exception {
        Key k = Key.generate();
        byte[] body = manifest(k, 3, files(), Map.of());
        byte[] other = manifest(k, 4, files(), Map.of());
        var out = PackVerifier.verify(body, k.sign(other), null, null, content(files()), new TrustState(), anchors(k, null));
        assertFalse(out.trusted());
    }

    @Test
    void rejectsAFileOfferedButNotSigned() throws Exception {
        Key k = Key.generate();
        byte[] body = manifest(k, 3, files(), Map.of());
        Map<String, String> offered = files();
        offered.put("/mods/evil.jar", SHA_A);
        var out = PackVerifier.verify(body, k.sign(body), null, null, content(offered), new TrustState(), anchors(k, null));
        assertFalse(out.trusted());
        assertTrue(out.problems().stream().anyMatch(p -> p.contains("/mods/evil.jar")));
    }

    @Test
    void rejectsAFileOfferedWithAnotherHash() throws Exception {
        Key k = Key.generate();
        byte[] body = manifest(k, 3, files(), Map.of());
        Map<String, String> offered = files();
        offered.put("/mods/a.jar", SHA_B);
        assertFalse(PackVerifier.verify(body, k.sign(body), null, null, content(offered), new TrustState(), anchors(k, null)).trusted());
    }

    @Test
    void rejectsASignedFileTheServerLeavesOut() throws Exception {
        Key k = Key.generate();
        byte[] body = manifest(k, 3, files(), Map.of());
        Map<String, String> offered = files();
        offered.remove("/tacz/p/b.json");
        assertFalse(PackVerifier.verify(body, k.sign(body), null, null, content(offered), new TrustState(), anchors(k, null)).trusted());
    }

    @Test
    void rejectsAnOlderVersionButAcceptsTheSameOne() throws Exception {
        Key k = Key.generate();
        TrustState seen = new TrustState();
        seen.highestPackVersion = 5;
        byte[] old = manifest(k, 4, files(), Map.of());
        assertFalse(PackVerifier.verify(old, k.sign(old), null, null, content(files()), seen, anchors(k, null)).trusted());
        byte[] same = manifest(k, 5, files(), Map.of());
        assertTrue(PackVerifier.verify(same, k.sign(same), null, null, content(files()), seen, anchors(k, null)).trusted());
    }

    @Test
    void acceptsOnlySignedDeletions() throws Exception {
        Key k = Key.generate();
        Jsons.ModpackContentFields offered = content(files());
        offered.nonModpackFilesToDelete = Set.of(new Jsons.ModpackContentFields.FileToDelete("/mods/old.jar", SHA_B, "0"));
        byte[] unsigned = manifest(k, 3, files(), Map.of());
        assertFalse(PackVerifier.verify(unsigned, k.sign(unsigned), null, null, offered, new TrustState(), anchors(k, null)).trusted());
        byte[] signed = manifest(k, 3, files(), Map.of("/mods/old.jar", SHA_B));
        assertTrue(PackVerifier.verify(signed, k.sign(signed), null, null, offered, new TrustState(), anchors(k, null)).trusted());
    }

    @Test
    void refusesToWriteIntoAutoModpackEvenWhenSigned() throws Exception {
        Key k = Key.generate();
        Map<String, String> f = files();
        f.put("/automodpack/.private/decade-trust.json", SHA_A);
        byte[] body = manifest(k, 3, f, Map.of());
        assertFalse(PackVerifier.verify(body, k.sign(body), null, null, content(f), new TrustState(), anchors(k, null)).trusted());
        assertFalse(PackVerifier.safePath("/mods/../automodpack/x"));
        assertFalse(PackVerifier.safePath("/AutoModpack/x"));
        assertFalse(PackVerifier.safePath("mods/a.jar"));
        assertTrue(PackVerifier.safePath("/mods/a.jar"));
    }

    @Test
    void rejectsMissingOrMalformedLists() throws Exception {
        Key k = Key.generate();
        assertFalse(PackVerifier.verify(null, null, null, null, content(files()), new TrustState(), anchors(k, null)).trusted());
        byte[] junk = "{\"keyId\": [1, 2]}".getBytes(StandardCharsets.UTF_8);
        assertFalse(PackVerifier.verify(junk, k.sign(junk), null, null, content(files()), new TrustState(), anchors(k, null)).trusted());
        byte[] notJson = "not json".getBytes(StandardCharsets.UTF_8);
        assertFalse(PackVerifier.verify(notJson, k.sign(notJson), null, null, content(files()), new TrustState(), anchors(k, null)).trusted());
    }

    private static byte[] rotation(Key emergency, long sequence, Key revoke, Key next) {
        Map<String, Object> r = new TreeMap<>();
        r.put("format", 1);
        r.put("keyId", emergency.id());
        r.put("sequence", sequence);
        r.put("revoke", List.of(revoke.id()));
        r.put("primary", List.of(next.base64()));
        return (GSON.toJson(r) + "\n").getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void aRotationRevokesTheOldPrimaryAndTrustsTheNewOne() throws Exception {
        Key old = Key.generate(), next = Key.generate(), emergency = Key.generate();
        byte[] rot = rotation(emergency, 1, old, next);
        byte[] rotSig = emergency.sign(rot);

        byte[] byOld = manifest(old, 3, files(), Map.of());
        assertFalse(PackVerifier.verify(byOld, old.sign(byOld), rot, rotSig, content(files()), new TrustState(), anchors(old, emergency)).trusted());

        byte[] byNext = manifest(next, 3, files(), Map.of());
        var out = PackVerifier.verify(byNext, next.sign(byNext), rot, rotSig, content(files()), new TrustState(), anchors(old, emergency));
        assertTrue(out.trusted(), () -> out.problems().toString());
        assertEquals(1, out.state().rotationSequence);
        assertTrue(out.state().revoked.contains(old.id()));

        // once saved, the rotation holds even when the server stops offering it
        var later = PackVerifier.verify(byOld, old.sign(byOld), null, null, content(files()), out.state(), anchors(old, emergency));
        assertFalse(later.trusted());
        var laterNext = PackVerifier.verify(byNext, next.sign(byNext), null, null, content(files()), out.state(), anchors(old, emergency));
        assertTrue(laterNext.trusted());
    }

    @Test
    void aRotationNotSignedByAnEmergencyKeyRejectsTheUpdate() throws Exception {
        Key primary = Key.generate(), emergency = Key.generate(), attacker = Key.generate();
        byte[] rot = rotation(primary, 1, primary, attacker);   // the primary key may not rotate itself
        byte[] body = manifest(attacker, 3, files(), Map.of());
        assertFalse(PackVerifier.verify(body, attacker.sign(body), rot, primary.sign(rot), content(files()), new TrustState(), anchors(primary, emergency)).trusted());
    }

    @Test
    void anOlderRotationIsIgnored() throws Exception {
        Key a = Key.generate(), b = Key.generate(), c = Key.generate(), emergency = Key.generate();
        TrustState s = new TrustState();
        s.rotationSequence = 2;
        s.revoked.add(a.id());
        s.adoptedPrimary = List.of(c.base64());
        byte[] stale = rotation(emergency, 1, c, b);   // would undo the later rotation
        byte[] byC = manifest(c, 3, files(), Map.of());
        var out = PackVerifier.verify(byC, c.sign(byC), stale, emergency.sign(stale), content(files()), s, anchors(a, emergency));
        assertTrue(out.trusted(), () -> out.problems().toString());
        assertEquals(List.of(c.base64()), out.state().adoptedPrimary);
    }

    @Test
    void aFailedCheckLeavesTheStateAsItWas() throws Exception {
        Key k = Key.generate();
        TrustState s = new TrustState();
        s.highestPackVersion = 2;
        byte[] body = manifest(k, 9, files(), Map.of());
        Map<String, String> offered = files();
        offered.put("/mods/evil.jar", SHA_A);
        var out = PackVerifier.verify(body, k.sign(body), null, null, content(offered), s, anchors(k, null));
        assertFalse(out.trusted());
        assertEquals(2, out.state().highestPackVersion);
    }

    /** Signed by tools/packsign.py (its serialisation, a fixed seed): Python and Java agree on the bytes. */
    @Test
    void verifiesAListSignedByTheSigningTool() {
        byte[] raw = Ed25519.decodeKey("A6EHv/POEL4dcN0Y50vAmWfk1jCbpQ1fHdyGZBJVMbg=");
        assertEquals("56475aa75463474c", Ed25519.keyId(raw));
        byte[] body = Base64.getDecoder().decode("ewogImRlbGV0ZSI6IHt9LAogImZpbGVzIjogewogICIvbW9kcy9hLmphciI6ICJkYTM5YTNlZTVlNmI0YjBkMzI1NWJmZWY5NTYwMTg5MGFmZDgwNzA5IiwKICAiL3RhY3ovcC/phY3nva4uanNvbiI6ICIzNTZhMTkyYjc5MTNiMDRjNTQ1NzRkMThjMjhkNDZlNjM5NTQyOGFiIgogfSwKICJmb3JtYXQiOiAxLAogImtleUlkIjogIjU2NDc1YWE3NTQ2MzQ3NGMiLAogInBhY2tOYW1lIjogIkRlY2FkZSIsCiAicGFja1ZlcnNpb24iOiA3Cn0K");
        byte[] sig = "nw63tj3AKqZv2KOyh5YLe/3raJ1Med1VnKvWrP2CUftra3f0vtrqrevnR9mNkfZ0dQ0fC3qXgF4GzBpto1p1Cg==\n".getBytes(StandardCharsets.US_ASCII);
        Map<String, String> offered = new LinkedHashMap<>();
        offered.put("/mods/a.jar", SHA_A);
        offered.put("/tacz/p/配置.json", SHA_B);
        var out = PackVerifier.verify(body, sig, null, null, content(offered), new TrustState(),
                new PackVerifier.Anchors(List.of(raw), List.of(), List.of()));
        assertTrue(out.trusted(), () -> out.problems().toString());
        assertEquals(7, out.packVersion());
    }

    /** The jar players get trusts exactly the registered primary and emergency keys, and no test key. */
    @Test
    void theBuiltInKeysAreTheRegisteredOnesAndNoTestKey() {
        var anchors = PackVerifier.Anchors.builtIn();
        assertEquals(List.of("c9226e7155a7e1af"), anchors.primary().stream().map(Ed25519::keyId).toList());
        assertEquals(List.of("f2e571382b234725"), anchors.emergency().stream().map(Ed25519::keyId).toList());
        assertTrue(anchors.test().isEmpty(), "a build given to players must not trust a test key");
    }

    @Test
    void theStateSurvivesASaveAndLoad(@TempDir Path dir) throws Exception {
        TrustState s = new TrustState();
        s.highestPackVersion = 12;
        s.rotationSequence = 1;
        s.revoked.add("0123456789abcdef");
        s.adoptedPrimary = List.of("A6EHv/POEL4dcN0Y50vAmWfk1jCbpQ1fHdyGZBJVMbg=");
        Path file = dir.resolve("sub").resolve("decade-trust.json");
        s.save(file);
        TrustState back = TrustState.load(file);
        assertEquals(12, back.highestPackVersion);
        assertEquals(1, back.rotationSequence);
        assertEquals(s.revoked, back.revoked);
        assertEquals(s.adoptedPrimary, back.adoptedPrimary);
        assertEquals(0, TrustState.load(dir.resolve("missing.json")).highestPackVersion);
    }
}
