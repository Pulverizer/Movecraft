package io.github.pulverizer.movecraft.event;

import io.github.pulverizer.movecraft.craft.Craft;
import io.github.pulverizer.movecraft.utils.HashHitBox;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.cause.Cause;

public class CraftCollisionEvent extends CraftEvent implements Cancellable {

    private final HashHitBox hitBox;

    public CraftCollisionEvent(Craft craft, HashHitBox hitBox) {
        super(craft);
        this.hitBox = hitBox;
    }

    public HashHitBox getHitBox() {
        return hitBox;
    }

    @Override
    public Cause getCause() {
        return null;
    }
}