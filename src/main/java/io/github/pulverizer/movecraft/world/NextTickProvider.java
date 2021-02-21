package io.github.pulverizer.movecraft.world;

import com.flowpowered.math.vector.Vector3i;
import io.github.pulverizer.movecraft.utils.WorldUtils;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.WorldServer;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class NextTickProvider {

    private final Map<WorldServer, ImmutablePair<TreeSet<NextTickListEntry>, List<NextTickListEntry>>> tickMap = new HashMap<>();

    private boolean isRegistered(WorldServer world) {
        return tickMap.containsKey(world);
    }

    private void registerWorld(WorldServer world) {
        List<NextTickListEntry> pendingForThisTick = world.pendingTickListEntriesThisTick;
        TreeSet<NextTickListEntry> pendingTickList = world.pendingTickListEntriesTreeSet;

        tickMap.put(world, new ImmutablePair<>(pendingTickList, pendingForThisTick));
    }

    public NextTickListEntry getNextTick(WorldServer world, Vector3i vecPosition) {
        BlockPos position = WorldUtils.locationToBlockPos(vecPosition);

        if (!isRegistered(world)) {
            registerWorld(world);
        }
        ImmutablePair<TreeSet<NextTickListEntry>, List<NextTickListEntry>> listPair = tickMap.get(world);
        if (listPair.left.contains(new NextTickListEntry(position, Blocks.AIR))) {
            for (Iterator<NextTickListEntry> iterator = listPair.left.iterator(); iterator.hasNext(); ) {
                NextTickListEntry listEntry = iterator.next();
                if (position.equals(listEntry.position)) {
                    iterator.remove();
                    return listEntry;
                }
            }
        }
        for (Iterator<NextTickListEntry> iterator = listPair.right.iterator(); iterator.hasNext(); ) {
            NextTickListEntry listEntry = iterator.next();
            if (position.equals(listEntry.position)) {
                iterator.remove();
                return listEntry;
            }
        }

        return null;
    }
}
