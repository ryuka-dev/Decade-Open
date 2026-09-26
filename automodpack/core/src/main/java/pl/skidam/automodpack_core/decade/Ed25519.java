package pl.skidam.automodpack_core.decade;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;

/** Ed25519 through the JDK (Java 15 and later), on raw 32-byte public keys. */
public final class Ed25519 {
    // DER prefix of an X.509 SubjectPublicKeyInfo holding an Ed25519 key; the 32 raw bytes follow it
    private static final byte[] X509_PREFIX = HexFormat.of().parseHex("302a300506032b6570032100");

    private Ed25519() {
    }

    public static byte[] decodeKey(String base64) {
        byte[] raw = Base64.getDecoder().decode(base64.trim());
        if (raw.length != 32) {
            throw new IllegalArgumentException("an Ed25519 public key is 32 bytes, got " + raw.length);
        }
        return raw;
    }

    /** First 16 hex characters of the key's SHA-256, as the signing side names keys. */
    public static String keyId(byte[] raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw);
            return HexFormat.of().formatHex(digest).substring(0, 16);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Whether signatureBase64 is raw's signature over exactly these bytes. False on any malformed input. */
    public static boolean verify(byte[] raw, byte[] body, byte[] signatureBase64) {
        try {
            byte[] signature = Base64.getDecoder().decode(new String(signatureBase64, StandardCharsets.US_ASCII).trim());
            byte[] encoded = new byte[X509_PREFIX.length + raw.length];
            System.arraycopy(X509_PREFIX, 0, encoded, 0, X509_PREFIX.length);
            System.arraycopy(raw, 0, encoded, X509_PREFIX.length, raw.length);
            PublicKey key = KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(encoded));
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(key);
            verifier.update(body);
            return verifier.verify(signature);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }
}
