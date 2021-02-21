package io.github.pulverizer.movecraft.sign;

import io.github.pulverizer.movecraft.craft.Craft;
import io.github.pulverizer.movecraft.craft.CraftManager;
import io.github.pulverizer.movecraft.utils.BlockSnapshotSignDataUtil;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.text.Text;

/**
 * No Permissions
 * Code to be reviewed
 *
 * @author BernardisGood
 * @version 1.4 - 23 Apr 2020
 */
public final class ReleaseSign {

    private static final String HEADER = "Release";

    public static void onSignClick(InteractBlockEvent.Secondary.MainHand event, Player player, BlockSnapshot block) {

        if (!block.getLocation().isPresent()) {
            return;
        }

        if (!BlockSnapshotSignDataUtil.getTextLine(block, 1).get().equalsIgnoreCase(HEADER)) {
            return;
        }
        Craft craft = CraftManager.getInstance().getCraftByPlayer(player.getUniqueId());
        if (craft == null) {
            player.sendMessage(Text.of("You are not in command of a craft."));
            return;
        }

        if (craft.getCommander() == player.getUniqueId()) {
            craft.release(player);
            player.sendMessage(Text.of("You have released your craft."));
        } else {
            player.sendMessage(Text.of("You are not the commander of this craft."));
        }
    }
}