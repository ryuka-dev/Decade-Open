package me.xjqsh.lrtactical.client.resource;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.handler.FirstPersonRenderHandler;

/**
 * Reloading resources builds every display anew, each with a fresh state machine that is not
 * initialized yet. The held item's first-person instance only initializes its machine when it is
 * drawn, and it was drawn long ago, so it would keep showing no animation until the player switched
 * away and back. Resetting simplebedrockmodel's first-person state makes the next tick treat the
 * held item as just picked up: a new instance, an initialized machine, and a draw animation.
 * TaCZ's own fallback for this (FirstPersonRenderEvent) is never registered, so nothing else does it.
 */
public final class HeldItemRedraw {
    private HeldItemRedraw() {
    }

    /** Call once the state machines have been replaced; on the client thread, as reload listeners apply. */
    public static void afterStateMachinesReplaced() {
        FirstPersonRenderHandler.reset();
    }
}
