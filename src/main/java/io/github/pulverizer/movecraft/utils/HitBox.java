package io.github.pulverizer.movecraft.utils;

import com.flowpowered.math.vector.Vector3i;
import io.github.pulverizer.movecraft.exception.EmptyHitBoxException;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public interface HitBox extends Iterable<Vector3i> {

    int getMinX();

    int getMinY();

    int getMinZ();

    int getMaxX();

    int getMaxY();

    int getMaxZ();

    default int getXLength() {
        if (this.isEmpty()) {
            return 0;
        }
        return Math.abs(this.getMaxX() - this.getMinX());
    }

    default int getYLength() {
        if (this.isEmpty()) {
            return 0;
        }
        return Math.abs(this.getMaxY() - this.getMinY());
    }

    default int getZLength() {
        if (this.isEmpty()) {
            return 0;
        }
        return Math.abs(this.getMaxZ() - this.getMinZ());
    }

    default boolean isEmpty() {
        return this.size() == 0;
    }

    int size();

    default Vector3i getMidPoint() {
        if (this.isEmpty()) {
            throw new EmptyHitBoxException();
        }
        // divide by 2 using bit shift
        return new Vector3i((this.getMinX() + this.getMaxX()) >> 1, (this.getMinY() + this.getMaxY()) >> 1, (this.getMinZ() + this.getMaxZ()) >> 1);
    }

    @Override
    Iterator<Vector3i> iterator();

    boolean contains(Vector3i location);

    default boolean contains(int x, int y, int z) {
        return this.contains(new Vector3i(x, y, z));
    }

    boolean containsAll(Collection<? extends Vector3i> collection);

    default boolean inBounds(double x, double y, double z) {
        if (this.isEmpty()) {
            return false;
        }
        return x >= this.getMinX() && x <= this.getMaxX() &&
                y >= this.getMinY() && y <= this.getMaxY() &&
                z >= this.getMinZ() && z <= this.getMaxZ();
    }

    default boolean inBounds(Vector3i location) {
        return this.inBounds(location.getX(), location.getY(), location.getZ());
    }

    default SolidHitBox boundingHitBox() {
        return new SolidHitBox(new Vector3i(this.getMinX(), this.getMinY(), this.getMinZ()),
                new Vector3i(this.getMaxX(), this.getMaxY(), this.getMaxZ()));
    }

    default Set<Vector3i> asSet() {
        Set<Vector3i> output = new HashSet<>();
        for (Vector3i location : this) {
            output.add(location);
        }
        return output;
    }
}