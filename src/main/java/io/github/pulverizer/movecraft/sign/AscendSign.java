package io.github.pulverizer.movecraft.sign;

import com.flowpowered.math.vector.Vector3i;
import io.github.pulverizer.movecraft.config.Settings;
import io.github.pulverizer.movecraft.config.craft_settings.Defaults;
import io.github.pulverizer.movecraft.craft.Craft;
import io.github.pulverizer.movecraft.craft.CraftManager;
import io.github.pulverizer.movecraft.event.CraftDetectEvent;
import io.github.pulverizer.movecraft.utils.HashHitBox;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.block.tileentity.ChangeSignEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.World;

/**
 * Permissions checked
 * Code to be reviewed
 *
 * @author BernardisGood
 * @version 1.3 - 17 Apr 2020
 */
public class AscendSign {

    public static void onSignChange(ChangeSignEvent event, Player player) {

        if (Settings.RequireCreateSignPerm && !player.hasPermission("movecraft.createsign.ascend")) {
            player.sendMessage(Text.of("Insufficient Permissions"));
            event.setCancelled(true);
        }
    }

    public static void onCraftDetect(CraftDetectEvent event, World world, HashHitBox hitBox) {

        for (Vector3i location : hitBox) {

            if (world.getBlockType(location) != BlockTypes.WALL_SIGN && world.getBlockType(location) != BlockTypes.STANDING_SIGN || !world
                    .getTileEntity(location).isPresent()) {
                continue;
            }

            Sign sign = (Sign) world.getTileEntity(location).get();
            ListValue<Text> lines = sign.lines();

            if (lines.get(0).toPlain().equalsIgnoreCase("Ascend: ON") || lines.get(0).toPlain().equalsIgnoreCase("Ascend:")) {
                lines.set(0, Text.of("Ascend: OFF"));
                sign.offer(lines);
            }
        }
    }

    public static void onSignClick(InteractBlockEvent.Secondary.MainHand event, Player player, BlockSnapshot block) {

        Craft craft = CraftManager.getInstance().getCraftByPlayer(player.getUniqueId());

        if (!block.getLocation().isPresent() || !block.getLocation().get().getTileEntity().isPresent() || craft == null || craft.getPilot() != player
                .getUniqueId()) {
            return;
        }

        Sign sign = (Sign) block.getLocation().get().getTileEntity().get();
        ListValue<Text> lines = sign.lines();
        if (lines.get(0).toPlain().equalsIgnoreCase("Ascend: OFF")) {

            if (!craft.getType().getSetting(Defaults.CanCruise.class).get().getValue()) {
                player.sendMessage(Text.of("This CraftType does not support this action"));
                return;
            }

            if (!player.hasPermission("movecraft." + craft.getType().getSetting(Defaults.Name.class).get().getValue() + ".movement"
                    + ".ascend") && (craft.getType().getSetting(Defaults.RequiresSpecificPerms.class).get().getValue() || !player
                    .hasPermission("movecraft.movement.ascend"))) {
                player.sendMessage(Text.of("Insufficient Permissions"));
                return;
            }

            event.setCancelled(true);

            lines.set(0, Text.of("Ascend: ON"));
            sign.offer(lines);

            craft.setCruising(Direction.UP, craft.getHorizontalCruiseDirection());

            return;
        }

        if (lines.get(0).toPlain().equalsIgnoreCase("Ascend: ON")) {

            event.setCancelled(true);
            lines.set(0, Text.of("Ascend: OFF"));
            sign.offer(lines);
            craft.setCruising(Direction.NONE, craft.getHorizontalCruiseDirection());
        }
    }
}