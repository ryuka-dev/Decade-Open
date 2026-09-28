package me.xjqsh.lrtactical.config;

/**
 * Which mouse buttons (the attack and use keys) start and cancel the use of a consumable, throwable
 * or detonator. A button that does neither does nothing while the item is in hand: it neither
 * attacks nor digs, nor interacts with blocks and entities.
 * <p>
 * For hold uses, letting go of every button that starts the use ends it as before (a throwable is
 * thrown); only a cancel button that does not also start the use cancels outright.
 */
public enum UseButtons {
    BOTH(true, true, true, true),
    RIGHT_ONLY(false, true, false, true),
    LEFT_ONLY(true, false, true, false),
    LEFT_USE_RIGHT_CANCEL(true, false, false, true),
    RIGHT_USE_LEFT_CANCEL(false, true, true, false);

    private final boolean leftUses;
    private final boolean rightUses;
    private final boolean leftCancels;
    private final boolean rightCancels;

    UseButtons(boolean leftUses, boolean rightUses, boolean leftCancels, boolean rightCancels) {
        this.leftUses = leftUses;
        this.rightUses = rightUses;
        this.leftCancels = leftCancels;
        this.rightCancels = rightCancels;
    }

    public boolean uses(boolean left) {
        return left ? leftUses : rightUses;
    }

    public boolean cancels(boolean left) {
        return left ? leftCancels : rightCancels;
    }
}
