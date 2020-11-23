package io.github.pulverizer.movecraft.sign;

import io.github.pulverizer.movecraft.craft.crew.CrewManager;
import io.github.pulverizer.movecraft.utils.BlockSnapshotSignDataUtil;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.data.manipulator.immutable.tileentity.ImmutableSignData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.block.InteractBlockEvent;

/**
 * Permissions Checked
 * Code to be reviewed
 *
 * @author BernardisGood
 * @version 1.5 - 23 Apr 2020
 */
public class RepairmanSign {
    private static final String HEADER = "Repairman";

    public static void onSignClick(InteractBlockEvent event, Player player, BlockSnapshot block) {

        if (!BlockSnapshotSignDataUtil.getTextLine(block, 1).get().equalsIgnoreCase(HEADER)) {
            return;
        }

        if (event instanceof InteractBlockEvent.Primary) {
            CrewManager.getInstance().resetRole(player);
            return;
        }

        CrewManager.getInstance().addRepairman(player);

        event.setCancelled(true);
    }
}