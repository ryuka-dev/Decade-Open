package pl.skidam.automodpack_core.decade;

import java.util.List;

/**
 * The public keys this client trusts to sign Decade's update lists: Ed25519, base64 of the 32 raw bytes.
 *
 * <p>They live in the jar, not in a config file, because the updater syncs config files: a key there could
 * be replaced by the very server it is meant to check. Changing them means shipping a new client.
 *
 * <p>{@link #PRIMARY} signs every update list. {@link #EMERGENCY} signs one thing only: a statement that
 * revokes a primary key and names its successor, so a lost or leaked primary key does not strand players.
 * {@link #TEST} keys are for development and the test server; a build that still carries one must never
 * be given to players, and every list it accepts from one is logged as such.
 */
public final class DecadeKeys {
    public static final List<String> PRIMARY = List.of(
            "vT3D6W2N/zOjVqE1g0yMGpXexv+saugyI16oJOdyX1Q="   // c9226e7155a7e1af
    );
    public static final List<String> EMERGENCY = List.of(
            "Syx6jurAdMJrWa6TJYdOIzNJpohd9cJX9IRCinS1Nh8="   // f2e571382b234725
    );
    // The test key 793e534344f11a65 was trusted up to 4.0.6-decade.3 and is no longer
    public static final List<String> TEST = List.of();

    private DecadeKeys() {
    }
}
