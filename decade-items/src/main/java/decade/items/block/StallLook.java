package decade.items.block;

import java.util.regex.Pattern;

import net.minecraft.nbt.CompoundTag;

/**
 * What one stall looks like: one roof, one set of furniture, one carpet, the note board's colour and the lamp.
 * Only names: which of them a stall may wear is decided on the server (decade_economy), and the parts themselves are
 * models shipped in the client jar, found by these names. A name this jar does not know is
 * drawn as the default part, never an error.
 *
 * <p>Stored in the stall's block entity under {@link #TAG}, the same tag the server writes; vanilla's {@code /data merge
 * block} writes it too, which is how a look is tried by hand.
 *
 * @param roof   roof shape: blue_white, white, triangle
 * @param cloth  the roof's colour or pattern, or {@link #BARE} for poles only (an unclaimed stall)
 * @param set    furniture set, 1..6
 * @param board  colour of the note board, for the sets that have one
 * @param carpet carpet kind: plain, check, or {@link #NONE}
 * @param rug    the carpet's colour
 * @param lamp   whether the stall hangs a lamp (lit at night by the server)
 */
public record StallLook(String roof, String cloth, String set, String board, String carpet, String rug, boolean lamp) {
    public static final String TAG = "look";
    public static final String BARE = "bare";
    public static final String NONE = "none";

    /** An unclaimed stall: the frame and an empty shelf, no cloth, no carpet. */
    public static final StallLook DEFAULT = new StallLook("white", BARE, "4", "purple", NONE, "light_gray", false);

    private static final Pattern NAME = Pattern.compile("[a-z0-9_]{1,32}");

    public static StallLook read(CompoundTag tag) {
        if (!tag.contains(TAG, CompoundTag.TAG_COMPOUND)) {
            return DEFAULT;
        }
        CompoundTag t = tag.getCompound(TAG);
        return new StallLook(name(t, "roof", DEFAULT.roof), name(t, "cloth", DEFAULT.cloth), name(t, "set", DEFAULT.set),
                name(t, "board", DEFAULT.board), name(t, "carpet", DEFAULT.carpet), name(t, "rug", DEFAULT.rug),
                t.getBoolean("lamp"));
    }

    public void write(CompoundTag tag) {
        CompoundTag t = new CompoundTag();
        t.putString("roof", roof);
        t.putString("cloth", cloth);
        t.putString("set", set);
        t.putString("board", board);
        t.putString("carpet", carpet);
        t.putString("rug", rug);
        t.putBoolean("lamp", lamp);
        tag.put(TAG, t);
    }

    /** A name that is safe in a resource path; anything else falls back, so a bad tag can't break model lookup. */
    private static String name(CompoundTag t, String key, String fallback) {
        String v = t.getString(key);
        return NAME.matcher(v).matches() ? v : fallback;
    }
}
