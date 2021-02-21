package io.github.pulverizer.movecraft.world;

import com.flowpowered.math.vector.Vector3i;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.NextTickListEntry;

public class TileHolder {

    private final TileEntity tile;
    private final NextTickListEntry nextTick;
    private final Vector3i tilePosition;

    public TileHolder(TileEntity tile, NextTickListEntry nextTick, Vector3i tilePosition) {
        this.tile = tile;
        this.nextTick = nextTick;
        this.tilePosition = tilePosition;
    }

    public TileEntity getTile() {
        return tile;
    }

    public NextTickListEntry getNextTick() {
        return nextTick;
    }

    public Vector3i getTilePosition() {
        return tilePosition;
    }
}