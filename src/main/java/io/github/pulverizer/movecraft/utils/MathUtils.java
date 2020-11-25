package io.github.pulverizer.movecraft.utils;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import io.github.pulverizer.movecraft.enums.Rotation;
import io.github.pulverizer.movecraft.craft.Craft;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class MathUtils {

    /**
     * checks if the given <code>location</code> is within <code>hitbox</code>
     * @param hitBox the bounding box to check within
     * @param location the location to check
     * @return True if the player is within the given bounding box
     */

    public static boolean locationInHitbox(final HashHitBox hitBox, final Location<World> location) {
        return hitBox.inBounds(location.getX(),location.getY(),location.getZ());
    }

    /**
     * Checks if a given {@code location} is within some distance, {@code distance}, from a given {@link HashHitBox}
     * @param hitBox the hitbox to check
     * @param location the location to check
     * @param distance
     * @return True if the {@code location} is within the {@code distance} from the {@link HashHitBox}
     */
    public static boolean locationNearHitBox(final HashHitBox hitBox, final Vector3d location, double distance) {
        return !hitBox.isEmpty() &&
                location.getX() >= hitBox.getMinX() - distance &&
                location.getZ() >= hitBox.getMinZ() - distance &&
                location.getX() <= hitBox.getMaxX() + distance &&
                location.getZ() <= hitBox.getMaxZ() + distance &&
                location.getY() >= hitBox.getMinY() - distance &&
                location.getY() <= hitBox.getMaxY() + distance;
    }

    /**
     * Checks if a given <code>Location</code> is within 3 blocks from a given <code>Craft</code>
     * @param craft the craft to check
     * @param location the location to check
     * @return True if <code>location</code> is less or equal to 3 blocks from <code>craft</code>
     */

    public static boolean locIsNearCraftFast(final Craft craft, final Vector3d location) {
        // optimized to be as fast as possible, it checks the easy ones first, then the more computationally intensive later
        return locationNearHitBox(craft.getHitBox(), location, 3);
    }

    /**
     * Rotates a Vector3i towards a supplied <code>Rotation</code>.
     * The resulting MovecraftRotation is based on a center of (0,0,0).
     * @param rotation the direction to add
     * @param vector3i the location to add
     * @return a rotated Movecraft location
     */

    public static Vector3i rotateVec(final Rotation rotation, final Vector3i vector3i) {
        double theta;
        if (rotation == Rotation.CLOCKWISE) {
            theta = 0.5 * Math.PI;
        } else {
            theta = -1 * 0.5 * Math.PI;
        }

        int newX = (int) Math.round((vector3i.getX() * Math.cos(theta)) + (vector3i.getZ() * (-1 * Math.sin(theta))));
        int newZ = (int) Math.round((vector3i.getX() * Math.sin(theta)) + (vector3i.getZ() * Math.cos(theta)));

        return new Vector3i(newX, vector3i.getY(), newZ);
    }

    @Deprecated
    public static double[] rotateVecNoRound(Rotation rotation, double x, double z) {
        double newX;
        double newZ;

        if (rotation == Rotation.CLOCKWISE) {

            newX = 0 - z;
            newZ = x;

        } else {

            newX = z;
            newZ = 0 - x;

        }

        return new double[]{newX, newZ};
    }

    /**
     * Calculates the directional difference between two coordinates.
     *
     * For example:
     * {@code ((5, 10, -6), (-4, 5, -4)) -> (-9, -5, 2) }
     *
     * @param origin The origin coordinate
     * @param target The destination or target coordinate
     * @return A vector of the difference
     */
    public static Vector3i vector3iDirectionalDiff(Vector3i origin, Vector3i target) {

        final int x = -(origin.getX() - target.getX());
        final int y = -(origin.getY() - target.getY());
        final int z = -(origin.getZ() - target.getZ());

        return new Vector3i(x, y, z);
    }
}