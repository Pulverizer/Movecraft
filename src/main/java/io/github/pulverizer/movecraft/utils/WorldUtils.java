package io.github.pulverizer.movecraft.utils;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import io.github.pulverizer.movecraft.Movecraft;
import io.github.pulverizer.movecraft.config.Settings;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.world.Location;

public class WorldUtils {

    public WorldUtils() {
    }

    /**
     * Moves the Entity to the Location in the World that the Entity currently resides in and applies the Rotation to the Entity.
     *
     * @param entity      Entity to be moved.
     * @param newLocation Vector3d location to move the Entity to.
     * @param rotation    New Rotation of the Entity.
     */
    public static void moveEntity(Entity entity, Vector3d newLocation, float rotation) {
        boolean entityMoved = entity.setLocationAndRotation(new Location<>(entity.getWorld(), newLocation), entity.getRotation().add(0, rotation, 0));

        if (Settings.Debug && !entityMoved) {
            Movecraft.getInstance().getLogger().info("Failed to move Entity of type: " + entity.getType().getName());
        }
    }

    public static BlockPos locationToBlockPos(Vector3i loc) {
        return new BlockPos(loc.getX(), loc.getY(), loc.getZ());
    }
}
