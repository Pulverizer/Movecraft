package io.github.pulverizer.movecraft.map_updater.update;

import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.world.BlockChangeFlags;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Objects;


public class BlockCreateCommand extends UpdateCommand {

    final private Vector3i newBlockLocation;
    final private BlockSnapshot block;
    final private World world;

    public BlockCreateCommand(World world, Vector3i newBlockLocation, BlockSnapshot block) {
        this.newBlockLocation = newBlockLocation;
        this.block = block.withLocation(new Location<>(world, newBlockLocation));
        this.world = world;
    }


    public BlockCreateCommand(World world, Vector3i newBlockLocation, BlockType block) {
        this.newBlockLocation = newBlockLocation;
        this.block = block.getDefaultState().snapshotFor(new Location<>(world, newBlockLocation));
        this.world = world;
    }


    @Override
    public void doUpdate() {
        // now do the block updates, move entities when you set the block they are on
        world.restoreSnapshot(newBlockLocation, block, true, BlockChangeFlags.NONE);
    }

    @Override
    public int hashCode() {
        return Objects.hash(newBlockLocation, block, world.getUniqueId());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BlockCreateCommand)) {
            return false;
        }
        BlockCreateCommand other = (BlockCreateCommand) obj;
        return other.newBlockLocation.equals(this.newBlockLocation) &&
                other.block.equals(this.block) &&
                other.world.equals(this.world);
    }
}