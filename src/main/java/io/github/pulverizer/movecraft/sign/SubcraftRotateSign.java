package io.github.pulverizer.movecraft.sign;

import com.flowpowered.math.vector.Vector3i;
import io.github.pulverizer.movecraft.Movecraft;
import io.github.pulverizer.movecraft.config.CraftType;
import io.github.pulverizer.movecraft.config.Settings;
import io.github.pulverizer.movecraft.config.craft_settings.Defaults;
import io.github.pulverizer.movecraft.craft.Craft;
import io.github.pulverizer.movecraft.craft.CraftManager;
import io.github.pulverizer.movecraft.enums.Rotation;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.block.tileentity.ChangeSignEvent;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;


/**
 * Permissions checked
 * Code to be reviewed
 *
 * @author BernardisGood
 * @version 1.4 - 20 Apr 2020
 */
public final class SubcraftRotateSign {

    private static final String HEADER = "Subcraft Rotate";

    public static void onSignChange(ChangeSignEvent event, Player player) {

        if (Settings.RequireCreateSignPerm && !player.hasPermission("movecraft.createsign.subcraftrotate")) {
            player.sendMessage(Text.of("Insufficient Permissions"));
            event.setCancelled(true);
        }
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

        if (!block.getLocation().isPresent() || !block.getLocation().get().getTileEntity().isPresent()) {
            return;
        }

        Sign sign = (Sign) block.getLocation().get().getTileEntity().get();
        ListValue<Text> lines = sign.lines();

        if (!lines.get(0).toPlain().equalsIgnoreCase(HEADER)) {
            return;
        }

        // add subcraft
        String craftTypeStr = sign.lines().get(1).toPlain().toLowerCase();
        CraftType type = CraftManager.getInstance().getCraftTypeFromString(craftTypeStr);
        if (type == null) {
            return;
        }

        if (lines.get(2).toPlain().equalsIgnoreCase("") && lines.get(3).toPlain().equalsIgnoreCase("")) {
            lines.set(2, Text.of("_\\ /_"));
            lines.set(3, Text.of("/ \\"));
            sign.offer(lines);
        }

        if (!player.hasPermission("movecraft." + craftTypeStr + ".crew.command") && (
                type.getSetting(Defaults.RequiresSpecificPerms.class).get().getValue() || !player.hasPermission("movecraft.crew.command"))) {
            player.sendMessage(Text.of("Insufficient Permissions"));
            return;
        }

        Craft craft = null;
        Vector3i signPosition = block.getLocation().get().getBlockPosition();
        for (Craft testCraft : CraftManager.getInstance().getCraftsInWorld(block.getLocation().get().getExtent())) {
            if (testCraft.getHitBox().contains(signPosition)) {
                craft = testCraft;
                break;
            }
        }
        if (craft != null) {
            if (craft.isProcessing()) {
                player.sendMessage(Text.of("Parent Craft is busy!"));
                return;
            }

            if (!player.hasPermission("movecraft." + craft.getType().getSetting(Defaults.Name.class).get().getValue().toLowerCase() +
                    ".rotatesubcraft") && (craft.getType().getSetting(Defaults.RequiresSpecificPerms.class).get().getValue() || !player
                    .hasPermission("movecraft.rotatesubcraft"))) {
                player.sendMessage(Text.of("Insufficient Permissions"));
                return;
            }

            craft.setProcessing(true); // prevent the parent craft from moving or updating until the subcraft is done

            //TODO - This is bad practice! Never assume anything!
            Craft finalCraft = craft;
            Task.builder()
                    .delayTicks(10)
                    .execute(() -> finalCraft.setProcessing(false))
                    .submit(Movecraft.getInstance());
        }

        final Location<World> loc = event.getTargetBlock().getLocation().get();
        final Craft subCraft = new Craft(type, player, loc);
        Vector3i startPoint = new Vector3i(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        Task.builder()
                .delayTicks(3)
                .execute(() -> {
                    subCraft.rotate(startPoint, rotation);
                    Task.builder()
                            .delayTicks(3)
                            .execute(() -> subCraft.release(player))
                            .submit(Movecraft.getInstance());
                })
                .submit(Movecraft.getInstance());
    }

}