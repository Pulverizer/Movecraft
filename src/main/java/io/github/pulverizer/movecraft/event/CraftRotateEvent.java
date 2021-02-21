package io.github.pulverizer.movecraft.event;

import io.github.pulverizer.movecraft.craft.Craft;
import io.github.pulverizer.movecraft.utils.HitBox;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.cause.Cause;

/**
 * Called whenever a craft is rotated
 * This event is called before the craft is physically moved, but after collision is checked.
 * @see Craft
 */
@SuppressWarnings("unused")
public class CraftRotateEvent extends CraftEvent implements Cancellable {

    private final HitBox oldHitBox;
    private final HitBox newHitBox;
    private String failMessage = "";
    private boolean isCancelled = false;

    public CraftRotateEvent(Craft craft, HitBox oldHitBox, HitBox newHitBox) {
        super(craft, true);
        this.oldHitBox = oldHitBox;
        this.newHitBox = newHitBox;
    }

    public HitBox getNewHitBox() {
        return newHitBox;
    }

    public HitBox getOldHitBox() {
        return oldHitBox;
    }

    public String getFailMessage() {
        return failMessage;
    }

    public void setFailMessage(String failMessage) {
        this.failMessage = failMessage;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        isCancelled = cancel;
    }

    @Override
    public Cause getCause() {
        return null;
    }
}