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
 * @version 1.2 - 17 Apr 2020
 */
public final class DescendSign {

    public static void onSignChange(ChangeSignEvent event, Player player) {

        if (Settings.RequireCreateSignPerm && !player.hasPermission("movecraft.createsign.descend")) {
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

            if (lines.get(0).toPlain().equalsIgnoreCase("Descend: ON")) {
                lines.set(0, Text.of("Descend: OFF"));
                sign.offer(lines);
            }
        }
    }

    public static void onSignClick(InteractBlockEvent.Secondary.MainHand event, Player player, BlockSnapshot block) {

        if (!block.getLocation().isPresent() || !block.getLocation().get().getTileEntity().isPresent()) {
            return;
        }

        Sign sign = (Sign) block.getLocation().get().getTileEntity().get();
        ListValue<Text> lines = sign.lines();

        Craft craft = CraftManager.getInstance().getCraftByPlayer(player.getUniqueId());

        if (craft == null) {
            return;
        }

        if (lines.get(0).toPlain().equalsIgnoreCase("Descend: OFF")) {

            if (!craft.getType().getSetting(Defaults.CanCruise.class).get().getValue() || player.getUniqueId() != craft.getPilot()) {
                return;
            }

            if (!player.hasPermission("movecraft." + craft.getType().getSetting(Defaults.Name.class).get().getValue() + ".movement.descend") && (
                    craft.getType().getSetting(Defaults.RequiresSpecificPerms.class).get().getValue() || !player
                            .hasPermission("movecraft.movement.descend"))) {
                player.sendMessage(Text.of("Insufficient Permissions"));
                return;
            }

            //craft.resetSigns(true, true, false);
            lines.set(0, Text.of("Descend: ON"));
            sign.offer(lines);

            craft.setCruising(Direction.DOWN, craft.getHorizontalCruiseDirection());

            return;
        }

        if (lines.get(0).toPlain().equalsIgnoreCase("Descend: ON")) {
            if (craft.getType().getSetting(Defaults.CanCruise.class).get().getValue() && player.getUniqueId() == craft.getPilot()) {
                lines.set(0, Text.of("Descend: OFF"));
                sign.offer(lines);
                craft.setCruising(Direction.NONE, craft.getHorizontalCruiseDirection());
            }
        }
    }
}