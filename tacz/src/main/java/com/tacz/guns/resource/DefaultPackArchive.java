package com.tacz.guns.resource;

import com.tacz.guns.GunMod;
import com.tacz.guns.util.GetJarResources;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Decade: the default gun pack ships as a zip beside the jar instead of inside it, so a code update is
 * small (README-DECADE.md). It is still exported to {@code tacz/tacz_default_gun/} the way upstream
 * exports it from the jar, and at the same point, because other mods edit that directory in place:
 * GunDB merges its unjam animations into it right after this, before the packs are scanned. The zip
 * lives in its own directory beside {@code tacz/}, where TaCZ does not load it as a second pack.
 */
public final class DefaultPackArchive {
    /** Under the game directory. */
    public static final String DIR = "tacz_default";
    private static final String PREFIX = GunMod.DEFAULT_GUN_PACK_NAME + "-";

    private DefaultPackArchive() {
    }

    /** Exports the pack into {@code packsDir} if the zip is there and changed since the last export. */
    static void export(Path packsDir) {
        Path dir = FMLPaths.GAMEDIR.get().resolve(DIR);
        if (!Files.isDirectory(dir)) {
            return;
        }
        List<Path> zips;
        try (Stream<Path> files = Files.list(dir)) {
            zips = files.filter(p -> {
                String name = p.getFileName().toString();
                return name.startsWith(PREFIX) && name.endsWith(".zip") && Files.isRegularFile(p);
            }).sorted().toList();
        } catch (IOException e) {
            GunMod.LOGGER.error("Failed to look for the default gun pack in {}", dir, e);
            return;
        }
        if (zips.isEmpty()) {
            GunMod.LOGGER.warn("No {}*.zip in {}: the default guns will be missing", PREFIX, dir);
            return;
        }
        Path zip = zips.get(zips.size() - 1);
        if (zips.size() > 1) {
            GunMod.LOGGER.warn("More than one default gun pack in {}, using {}", dir, zip.getFileName());
        }
        GetJarResources.copyZipDirectory(zip, packsDir, GunMod.DEFAULT_GUN_PACK_NAME);
    }
}
