package io.github.pulverizer.movecraft.event;

import io.github.pulverizer.movecraft.craft.Craft;
import org.spongepowered.api.event.cause.Cause;

/**
 * Called whenever a craft is piloted
 * @see Craft
 */
@SuppressWarnings("unused")
public class CraftPilotEvent extends CraftEvent {

    private final Reason reason;

    public CraftPilotEvent(Craft craft, Reason reason) {
        super(craft);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    @Override
    public Cause getCause() {
        return null;
    }

    public enum Reason {
        SUB_CRAFT, PLAYER, FORCE
    }
}