package io.github.pulverizer.movecraft.utils;

import com.flowpowered.math.vector.Vector3i;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

public class CollectionUtils {

    private final static Vector3i[] SHIFTS = {
            new Vector3i(0, 0, 1),
            new Vector3i(0, 1, 0),
            new Vector3i(1, 0, 0),
            new Vector3i(0, 0, -1),
            new Vector3i(0, -1, 0),
            new Vector3i(-1, 0, 0)};

    /**
     * Removes the elements from <code>collection</code> that also exist in <code>filter</code> without modifying either.
     * @param collection
     * @param filter
     * @param <E>  the element type
     * @return a <code>Collection</code> containing all the elements of <code>collection</code> except those in <code>filter</code>
     */
    public static <E> Collection<E> filter(final Collection<E> collection, final Collection<E> filter) {
        final Collection<E> returnList = new HashSet<>();
        final HashSet<E> filterSet = new HashSet<>(filter);
        for (E object : collection) {
            if (!filterSet.contains(object)) {
                returnList.add(object);
            }
        }
        return returnList;
    }

    public static Collection<Vector3i> filter(final HitBox collection, final Collection<Vector3i> filter) {
        final Collection<Vector3i> returnList = new HashSet<>();
        final HashSet<Vector3i> filterSet = new HashSet<>(filter);
        for (Vector3i object : collection) {
            if (!filterSet.contains(object)) {
                returnList.add(object);
            }
        }
        return returnList;
    }

    public static HitBox filter(final HitBox collection, final HitBox filter) {
        final MutableHitBox returnList = new HashHitBox();
        for (Vector3i object : collection) {
            if (!filter.contains(object)) {
                returnList.add(object);
            }
        }
        return returnList;
    }

    /**
     * Provides the axial neighbors of a location. Neighbors are defined as locations that exist within one meter of a given location
     *
     * @param location the location to provide neighbors for
     * @return an iterable set of neighbors to the given location
     */

    public static HashSet<Vector3i> neighbors(Vector3i location) {
        final HashSet<Vector3i> neighbors = new HashSet<>(6);

        for (Vector3i test : SHIFTS) {
            neighbors.add(location.add(test));
        }

        return neighbors;
    }

    /**
     * Finds the axial neighbors to a location. Neighbors are defined as locations that exist within one meter of a given location.
     * @param hitbox
     * @param location the location to search for neighbors
     * @return an iterable set of neighbors to the given location
     */
    public static Iterable<Vector3i> neighbors(HitBox hitbox, Vector3i location) {
        if (hitbox.isEmpty()) {
            return Collections.emptyList();
        }
        final HashSet<Vector3i> neighbors = new HashSet<>(6);
        for (Vector3i test : SHIFTS) {
            if (hitbox.contains(location.add(test))) {
                neighbors.add(location.add(test));
            }
        }
        return neighbors;
    }
}