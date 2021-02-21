package io.github.pulverizer.movecraft.event;

import io.github.pulverizer.movecraft.craft.Craft;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.cause.Cause;

public class CraftSinkEvent extends CraftEvent implements Cancellable {

    private boolean cancelled = false;

    public CraftSinkEvent(Craft craft) {
        super(craft);
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public Cause getCause() {
        return null;
    }
}