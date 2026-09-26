package pl.skidam.automodpack_core.utils;

/** Compatibility rules for the unchanged AutoModpack login handshake. */
public final class AutoModpackProtocol {

    private static final int LEGACY_COMPATIBLE_MAJOR = 4;
    private static final int LEGACY_COMPATIBLE_MINOR = 0;

    private AutoModpackProtocol() {
    }

    // Decade: our builds are 4.0.x-decade.N. Semantic versioning reads them as pre-releases, so the upstream rule
    // below would demand an exact match, and every client would be turned away the day the server moves to the
    // next build; yet the handshake is unchanged, so they can all talk. A Decade server takes Decade clients of
    // any build and no others: an upstream client does not check the signed list and must not install our pack.
    private static final java.util.regex.Pattern DECADE = java.util.regex.Pattern.compile("4\\.0\\.\\d+-decade\\.\\d+");

    /** Whether a version is one of Decade's builds of AutoModpack. */
    public static boolean isDecadeVersion(String version) {
        return version != null && DECADE.matcher(version).matches();
    }

    /** Returns whether the client version is valid for this server version. */
    public static boolean acceptsClient(String serverVersion, String clientVersion) {
        if (isDecadeVersion(serverVersion)) {
            return isDecadeVersion(clientVersion);
        }
        return (serverVersion != null && serverVersion.equals(clientVersion))
                || (isLegacyCompatibleVersion(serverVersion)
                && isLegacyCompatibleVersion(clientVersion));
    }

    /**
     * Returns the version to put in {@code amVersion}. Stable 4.0.x clients
     * advertise the server's version so old servers accept newer clients.
     */
    public static String getHandshakeVersion(String serverVersion, String clientVersion) {
        if (isLegacyCompatibleVersion(serverVersion)
                && isLegacyCompatibleVersion(clientVersion)) {
            return serverVersion;
        }

        return clientVersion;
    }

    /**
     * Returns whether a version belongs to the unchanged legacy 4.0.x family.
     */
    public static boolean isLegacyCompatibleVersion(String version) {
        try {
            SemanticVersion parsed = SemanticVersion.parse(version);
            return parsed.isStable()
                    && parsed.major() == LEGACY_COMPATIBLE_MAJOR
                    && parsed.minor() == LEGACY_COMPATIBLE_MINOR;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
