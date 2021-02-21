package io.github.pulverizer.movecraft.event;

import io.github.pulverizer.movecraft.craft.Craft;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.impl.AbstractEvent;

/**
 * A base event for all craft-related event
 * @see Craft
 */

public abstract class CraftEvent extends AbstractEvent implements Cancellable {

    protected final Craft craft;
    protected final boolean isAsync;
    private boolean isCancelled = false;

    public CraftEvent(Craft craft) {
        this.isAsync = false;
        this.craft = craft;
    }

    public CraftEvent(Craft craft, boolean isAsync) {
        this.isAsync = isAsync;
        this.craft = craft;
    }

    public final Craft getCraft() {
        return craft;
    }

    public final boolean isAsync() {
        return isAsync;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public void setCancelled(boolean cancelled) {
        isCancelled = cancelled;
    }


}