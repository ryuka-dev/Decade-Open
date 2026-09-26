package pl.skidam.automodpack_loader_core.client;

import pl.skidam.automodpack_core.auth.Secrets;
import pl.skidam.automodpack_core.config.Jsons;
import pl.skidam.automodpack_core.decade.PackVerifier;
import pl.skidam.automodpack_core.decade.TrustState;
import pl.skidam.automodpack_core.protocol.DownloadClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static pl.skidam.automodpack_core.GlobalVariables.LOGGER;
import static pl.skidam.automodpack_core.GlobalVariables.privateDir;

/**
 * Fetches Decade's signed list from the modpack host and checks the server's offer against it
 * ({@link PackVerifier}), before any file of the offer is downloaded. The result is kept for the rest of
 * the update, so a list the server sends later (a refresh after failed downloads) is checked against the
 * same signed bytes.
 */
public final class DecadeGate {
    public static final Path STATE_FILE = privateDir.resolve("decade-trust.json");
    // the signed files are small; anything bigger is not ours
    private static final long MAX_SIGNED_FILE = 4L * 1024 * 1024;

    private final byte[] manifest, manifestSig, rotation, rotationSig;
    private final PackVerifier.Outcome outcome;

    private DecadeGate(byte[] manifest, byte[] manifestSig, byte[] rotation, byte[] rotationSig,
                       PackVerifier.Outcome outcome) {
        this.manifest = manifest;
        this.manifestSig = manifestSig;
        this.rotation = rotation;
        this.rotationSig = rotationSig;
        this.outcome = outcome;
    }

    public boolean trusted() {
        return outcome.trusted();
    }

    public List<String> problems() {
        return outcome.problems();
    }

    /** Downloads the signed files the offer names and checks the offer. Never throws: a failure is untrusted. */
    public static DecadeGate check(Jsons.ModpackContentFields content, Jsons.ModpackAddresses addresses,
                                   Secrets.Secret secret) {
        byte[][] got = new byte[4][];
        String problem = null;
        if (secret == null) {
            problem = "no download secret, cannot fetch the signed list";
        } else {
            try (DownloadClient client = DownloadClient.tryCreate(addresses, secret.secretBytes(), 1,
                    ModpackUtils.userValidationCallback(addresses.hostAddress, false))) {
                if (client == null) {
                    problem = "could not connect to the modpack host for the signed list";
                } else {
                    got[0] = fetch(client, content, PackVerifier.MANIFEST);
                    got[1] = fetch(client, content, PackVerifier.MANIFEST_SIG);
                    got[2] = fetch(client, content, PackVerifier.ROTATION);
                    got[3] = fetch(client, content, PackVerifier.ROTATION_SIG);
                }
            } catch (Exception e) {
                problem = "fetching the signed list failed: " + e;
            }
        }
        PackVerifier.Outcome outcome = problem != null
                ? new PackVerifier.Outcome(false, -1, null, false, List.of(problem), null)
                : PackVerifier.verify(got[0], got[1], got[2], got[3], content, TrustState.load(STATE_FILE),
                PackVerifier.Anchors.builtIn());
        DecadeGate gate = new DecadeGate(got[0], got[1], got[2], got[3], outcome);
        gate.report("offered update");
        gate.save();
        return gate;
    }

    /** Checks another offer from the same server against the signed list already fetched. */
    public DecadeGate recheck(Jsons.ModpackContentFields content) {
        PackVerifier.Outcome again = manifest == null
                ? outcome
                : PackVerifier.verify(manifest, manifestSig, rotation, rotationSig, content,
                TrustState.load(STATE_FILE), PackVerifier.Anchors.builtIn());
        DecadeGate gate = new DecadeGate(manifest, manifestSig, rotation, rotationSig, again);
        gate.report("refreshed offer");
        return gate;
    }

    private void report(String what) {
        if (outcome.trusted()) {
            LOGGER.info("Decade: {} matches signed pack version {} (key {})", what, outcome.packVersion(), outcome.keyId());
            if (outcome.testKey()) {
                LOGGER.warn("Decade: signed by a TEST key. This client must never be given to players.");
            }
        } else {
            LOGGER.error("Decade: {} rejected, nothing of it will be installed:", what);
            outcome.problems().forEach(p -> LOGGER.error("Decade:   {}", p));
        }
    }

    private void save() {
        if (!outcome.trusted()) {
            return;
        }
        try {
            outcome.state().save(STATE_FILE);
        } catch (IOException e) {
            // the check itself stands; only the replay protection is not raised this time
            LOGGER.error("Decade: could not save {}", STATE_FILE, e);
        }
    }

    private static byte[] fetch(DownloadClient client, Jsons.ModpackContentFields content, String path) throws Exception {
        Jsons.ModpackContentFields.ModpackContentItem item = null;
        for (Jsons.ModpackContentFields.ModpackContentItem i : content.list) {
            if (path.equals(i.file)) item = i;
        }
        if (item == null) {
            return null;
        }
        if (Long.parseLong(item.size) > MAX_SIGNED_FILE) {
            throw new IOException(path + " is too big to be a signed list");
        }
        Path tmp = Files.createTempFile("decade-signed", ".tmp");
        try {
            client.downloadFile(item.sha1.getBytes(StandardCharsets.UTF_8), tmp, bytes -> { }).get(60, TimeUnit.SECONDS);
            if (Files.size(tmp) > MAX_SIGNED_FILE) {
                throw new IOException(path + " is too big to be a signed list");
            }
            return Files.readAllBytes(tmp);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
