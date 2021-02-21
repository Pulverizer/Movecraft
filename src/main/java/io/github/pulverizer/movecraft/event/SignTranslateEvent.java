package io.github.pulverizer.movecraft.event;

import com.flowpowered.math.vector.Vector3i;
import io.github.pulverizer.movecraft.craft.Craft;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.world.World;


public class SignTranslateEvent extends CraftEvent {

    private final Vector3i location;
    private final World world;

    public SignTranslateEvent(Vector3i location, Craft craft) throws IndexOutOfBoundsException {
        super(craft);
        this.location = location;
        this.world = craft.getWorld();
    }

    public Vector3i getBlockPosition() {
        return location;
    }

    public World getWorld() {
        return world;
    }

    @Override
    public Cause getCause() {
        return null;
    }

    @Override
    public Object getSource() {
        return null;
    }

    @Override
    public EventContext getContext() {
        return null;
    }
}