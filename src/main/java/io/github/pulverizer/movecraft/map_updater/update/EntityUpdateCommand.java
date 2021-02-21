package io.github.pulverizer.movecraft.map_updater.update;

import com.flowpowered.math.vector.Vector3d;
import io.github.pulverizer.movecraft.utils.WorldUtils;
import org.spongepowered.api.entity.Entity;

import java.util.Objects;

/**
 * Class that stores the data about a single blocks changes to the map in an unspecified world. The world is retrieved contextually from the
 * submitting craft.
 */
public class EntityUpdateCommand extends UpdateCommand {

    private final Entity entity;
    private final Vector3d newLocation;
    private final float yaw;

    public EntityUpdateCommand(Entity entity, Vector3d newLocation, float yaw) {
        this.entity = entity;
        this.newLocation = newLocation;
        this.yaw = yaw;
    }

    public Entity getEntity() {
        return entity;
    }

    @Override
    public void doUpdate() {
        WorldUtils.moveEntity(entity, newLocation, yaw);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entity.getUniqueId(), newLocation, yaw);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EntityUpdateCommand)) {
            return false;
        }
        EntityUpdateCommand other = (EntityUpdateCommand) obj;
        return this.newLocation == other.newLocation &&
                this.yaw == other.yaw &&
                this.entity.equals(other.entity);
    }
}