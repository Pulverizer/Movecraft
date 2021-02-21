package io.github.pulverizer.movecraft.sign;

import com.flowpowered.math.vector.Vector3i;
import io.github.pulverizer.movecraft.config.Settings;
import io.github.pulverizer.movecraft.config.craft_settings.Defaults;
import io.github.pulverizer.movecraft.craft.Craft;
import io.github.pulverizer.movecraft.craft.CraftManager;
import io.github.pulverizer.movecraft.utils.BlockSnapshotSignDataUtil;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.block.tileentity.ChangeSignEvent;
import org.spongepowered.api.text.Text;

/**
 * Permissions checked
 * Code to be reviewed
 *
 * @author BernardisGood
 * @version 1.4 - 20 Apr 2020
 */
public final class TeleportSign {

    private static final String HEADER = "Teleport:";

    public static void onSignChange(ChangeSignEvent event, Player player) {

        if (Settings.RequireCreateSignPerm && !player.hasPermission("movecraft.createsign.teleport")) {
            player.sendMessage(Text.of("Insufficient Permissions"));
            event.setCancelled(true);
        }
    }

    public static void onSignClick(InteractBlockEvent.Secondary.MainHand event, Player player, BlockSnapshot block) {

        if (!block.getLocation().isPresent()) {
            return;
        }

        if (!BlockSnapshotSignDataUtil.getTextLine(block, 1).get().equalsIgnoreCase(HEADER)) {
            return;
        }

        Craft craft = CraftManager.getInstance().getCraftByPlayer(player.getUniqueId());

        if (craft == null || player.getUniqueId() != craft.getPilot()) {
            player.sendMessage(Text.of("You are not piloting a craft."));
            return;
        }
        String[] numbers = BlockSnapshotSignDataUtil.getTextLine(block, 2).get().split(",");
        int tX = Integer.parseInt(numbers[0]);
        int tY = Integer.parseInt(numbers[1]);
        int tZ = Integer.parseInt(numbers[2]);

        if (!player.hasPermission("movecraft." + craft.getType().getSetting(Defaults.Name.class).get().getValue() + ".movement.teleport") && (
                craft.getType().getSetting(Defaults.RequiresSpecificPerms.class).get().getValue() || !player
                        .hasPermission("movecraft.movement.teleport"))) {
            player.sendMessage(Text.of("Insufficient Permissions"));
            return;
        }

        if (craft.getType().getSetting(Defaults.CanTeleport.class).get().getValue()) {
            int dx = tX - block.getLocation().get().getBlockPosition().getX();
            int dy = tY - block.getLocation().get().getBlockPosition().getY();
            int dz = tZ - block.getLocation().get().getBlockPosition().getZ();
            craft.translate(new Vector3i(dx, dy, dz));
        }
    }
}