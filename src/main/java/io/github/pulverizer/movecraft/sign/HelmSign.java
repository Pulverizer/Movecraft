package io.github.pulverizer.movecraft.sign;

import io.github.pulverizer.movecraft.config.Settings;
import io.github.pulverizer.movecraft.config.craft_settings.Defaults;
import io.github.pulverizer.movecraft.craft.Craft;
import io.github.pulverizer.movecraft.craft.CraftManager;
import io.github.pulverizer.movecraft.enums.Rotation;
import io.github.pulverizer.movecraft.utils.BlockSnapshotSignDataUtil;
import io.github.pulverizer.movecraft.utils.MathUtils;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.block.tileentity.ChangeSignEvent;
import org.spongepowered.api.text.Text;

/**
 * Permissions Checked
 * Code to be reviewed
 *
 * @author BernardisGood
 * @version 1.4 - 20 Apr 2020
 */
public final class HelmSign {

    public static void onSignChange(ChangeSignEvent event, Player player) {

        if (Settings.RequireCreateSignPerm && !player.hasPermission("movecraft.createsign.cruise")) {
            player.sendMessage(Text.of("Insufficient Permissions"));
            event.setCancelled(true);
            return;
        }

        ListValue<Text> lines = event.getText().lines();

        lines.set(0, Text.of("\\  ||  /"));
        lines.set(1, Text.of("==      =="));
        lines.set(2, Text.of("/  ||  \\"));
        event.getText().set(lines);
    }

    public static void onSignClick(InteractBlockEvent event, Player player, BlockSnapshot block) {
        Rotation rotation;
        if (event instanceof InteractBlockEvent.Secondary) {
            rotation = Rotation.CLOCKWISE;
        } else if (event instanceof InteractBlockEvent.Primary) {
            rotation = Rotation.ANTICLOCKWISE;
        } else {
            return;
        }

        if (!block.getLocation().isPresent()) {
            return;
        }

        if (!BlockSnapshotSignDataUtil.getTextLine(block, 1).get().equalsIgnoreCase("\\\\  ||  /") ||
                !BlockSnapshotSignDataUtil.getTextLine(block, 2).get().equalsIgnoreCase("\\u003d\\u003d      \\u003d\\u003d") || // \\u003d is =
                !BlockSnapshotSignDataUtil.getTextLine(block, 3).get().equalsIgnoreCase("/  ||  \\\\")) {
            return;
        }

        Craft craft = CraftManager.getInstance().getCraftByPlayer(player.getUniqueId());

        if (craft == null || player.getUniqueId() != craft.getPilot()) {
            player.sendMessage(Text.of("You are not piloting a craft."));
            return;
        }

        if (!player.hasPermission("movecraft." + craft.getType().getSetting(Defaults.Name.class).get().getValue() + ".movement.rotate") && (
                craft.getType().getSetting(Defaults.RequiresSpecificPerms.class).get().getValue() || !player
                        .hasPermission("movecraft.movement.rotate"))) {
            player.sendMessage(Text.of("Insufficient Permissions"));
            return;
        }

        if (!MathUtils.locationInHitbox(craft.getHitBox(), player.getLocation())) {
            return;
        }

        if (craft.getType().getSetting(Defaults.RotateAtMidpoint.class).get().getValue()) {
            craft.rotate(craft.getHitBox().getMidPoint(), rotation);
        } else {
            craft.rotate(block.getLocation().get().getBlockPosition(), rotation);
        }

        event.setCancelled(true);

        //TODO: Lower speed while turning

    }
}