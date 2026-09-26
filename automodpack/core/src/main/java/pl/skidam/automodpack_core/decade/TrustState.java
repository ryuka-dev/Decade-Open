package pl.skidam.automodpack_core.decade;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static pl.skidam.automodpack_core.GlobalVariables.LOGGER;

/**
 * What this client has learned from signed lists, kept between launches in automodpack/.private/.
 *
 * <p>{@link #highestPackVersion} blocks replays: a server that is broken into still holds every list we ever
 * signed and could serve an old one to put players back on a version with a known hole. A rotation adds
 * adopted primary keys and revoked key ids. Nothing the server sends may write into automodpack/, so only a
 * valid signature changes this. A missing or unreadable file starts over from the keys in the jar, which
 * only the player can cause.
 */
public final class TrustState {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public long highestPackVersion = 0;
    public long rotationSequence = 0;
    public Set<String> revoked = new LinkedHashSet<>();
    public List<String> adoptedPrimary = new ArrayList<>();

    public TrustState copy() {
        TrustState c = new TrustState();
        c.highestPackVersion = highestPackVersion;
        c.rotationSequence = rotationSequence;
        c.revoked = new LinkedHashSet<>(revoked);
        c.adoptedPrimary = new ArrayList<>(adoptedPrimary);
        return c;
    }

    public static TrustState load(Path file) {
        if (!Files.isRegularFile(file)) {
            return new TrustState();
        }
        try {
            TrustState s = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), TrustState.class);
            if (s == null) {
                return new TrustState();
            }
            if (s.revoked == null) s.revoked = new LinkedHashSet<>();
            if (s.adoptedPrimary == null) s.adoptedPrimary = new ArrayList<>();
            return s;
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Decade trust state {} is unreadable, starting from the built-in keys", file, e);
            return new TrustState();
        }
    }

    /** Written to a temporary file and moved into place, so a crash never leaves half a file. */
    public void save(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, GSON.toJson(this), StandardCharsets.UTF_8);
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
