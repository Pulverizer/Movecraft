package io.github.pulverizer.movecraft.event;

import io.github.pulverizer.movecraft.craft.Craft;
import io.github.pulverizer.movecraft.utils.HashHitBox;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.cause.Cause;

/**
 * Called whenever a craft is translated.
 * This event is called before the craft is physically moved, but after collision is checked.
 * @see Craft
 */
@SuppressWarnings("unused")
public class CraftTranslateEvent extends AbstractCraftEvent implements Cancellable {

    private final HashHitBox oldHitBox;
    private final HashHitBox newHitBox;
    private String failMessage = "";
    private boolean isCancelled = false;

    public CraftTranslateEvent(Craft craft, HashHitBox oldHitBox, HashHitBox newHitBox) {
        super(craft, true);
        this.oldHitBox = oldHitBox;
        this.newHitBox = newHitBox;
    }

    public HashHitBox getNewHitBox() {
        return newHitBox;
    }

    public HashHitBox getOldHitBox() {
        return oldHitBox;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        isCancelled = cancel;
    }

    public String getFailMessage() {
        return failMessage;
    }

    public void setFailMessage(String failMessage) {
        this.failMessage = failMessage;
    }

    @Override
    public Cause getCause() {
        return null;
    }
}