package io.github.pulverizer.movecraft.utils;


import com.flowpowered.math.vector.Vector3i;

import java.util.Collection;

public interface MutableHitBox extends HitBox {

    boolean add(Vector3i location);

    boolean addAll(Collection<? extends Vector3i> collection);

    boolean addAll(HitBox hitBox);

    boolean remove(Vector3i location);

    boolean removeAll(Collection<? extends Vector3i> collection);

    boolean removeAll(HitBox hitBox);

    void clear();
}