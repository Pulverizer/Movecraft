package io.github.pulverizer.movecraft.listener;

import com.flowpowered.math.vector.Vector3i;
import io.github.pulverizer.movecraft.config.Settings;
import io.github.pulverizer.movecraft.config.craft_settings.Defaults;
import io.github.pulverizer.movecraft.craft.Craft;
import io.github.pulverizer.movecraft.craft.CraftManager;
import io.github.pulverizer.movecraft.enums.DirectControlMode;
import io.github.pulverizer.movecraft.utils.MathUtils;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.filter.type.Include;
import org.spongepowered.api.event.item.inventory.InteractItemEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.BlockChangeFlags;

public final class InteractListener {

    @Listener
    public final void onPlayerInteractPrimary(InteractBlockEvent.Primary event, @Root Player player) {

        BlockSnapshot block = event.getTargetBlock();
        if (!block.getState().getType().equals(BlockTypes.WOODEN_BUTTON) && !block.getState().getType().equals(BlockTypes.STONE_BUTTON)) {
            return;
        }
        // if they left click a button which is pressed, unpress it
        if (block.get(Keys.POWERED).orElse(false)) {
            block = block.with(Keys.POWERED, false).get();
            block.getLocation().get().restoreSnapshot(block, true, BlockChangeFlags.ALL);
        }
    }

    @Listener
    @Include({InteractItemEvent.Primary.class, InteractItemEvent.Secondary.MainHand.class})
    public void onPlayerInteractStick(InteractItemEvent event, @Root Player player) {

        if (!player.getItemInHand(HandTypes.MAIN_HAND).isPresent() || !player.getItemInHand(HandTypes.MAIN_HAND).get().getType()
                .equals(Settings.PilotTool)) {
            return;
        }

        Craft craft = CraftManager.getInstance().getCraftByPlayer(player.getUniqueId());
        // if not in command of craft, don't process pilot tool clicks
        if (craft == null) {
            return;
        }

        if (player.getUniqueId() != craft.getPilot()) {
            player.sendMessage(Text.of("You are not the pilot of the craft."));
            return;
        }

        if (event instanceof InteractItemEvent.Secondary) {
            event.setCancelled(true);

            if (!craft.hasCooldownExpired()) {
                return;
            }


            if (!MathUtils.locationNearHitBox(craft.getHitBox(), player.getPosition(), 2)) {
                return;
            }


            if (!player.hasPermission("movecraft." + craft.getType().getSetting(Defaults.Name.class).get().getValue() + ".movement.move")) {
                player.sendMessage(Text.of("Insufficient Permissions"));
                return;
            }

            if (craft.isUnderDirectControl()) {
                // right click moves up or down if using direct control
                int dy = 1;
                if (player.get(Keys.IS_SNEAKING).get()) {
                    dy = -1;
                }

                craft.translate(new Vector3i(0, dy, 0));

            } else {

                // Player is onboard craft and right clicking
                float rotation = (float) Math.PI * (float) player.getRotation().getY() / 180f;

                float nx = -(float) Math.sin(rotation);
                float nz = (float) Math.cos(rotation);

                int dx = (Math.abs(nx) >= 0.5 ? 1 : 0) * (int) Math.signum(nx);
                int dz = (Math.abs(nz) > 0.5 ? 1 : 0) * (int) Math.signum(nz);
                int dy;

                float p = (float) player.getRotation().getX();

                dy = -(Math.abs(p) >= 25 ? 1 : 0) * (int) Math.signum(p);

                if (Math.abs(player.getRotation().getX()) >= 75) {
                    dx = 0;
                    dz = 0;
                }

                craft.translate(new Vector3i(dx, dy, dz));
            }

            return;
        }

        if (event instanceof InteractItemEvent.Primary) {

            if (craft.getDirectControlMode() == DirectControlMode.B) {
                craft.setDirectControl(DirectControlMode.OFF);
                player.sendMessage(Text.of("Leaving Direct Control Mode"));
                event.setCancelled(true);
                return;
            }

            if (!player.hasPermission("movecraft." + craft.getType().getSetting(Defaults.Name.class).get().getValue() + ".movement.move") || !craft
                    .getType().getSetting(Defaults.CanDirectControl.class).get().getValue()) {
                player.sendMessage(Text.of("Insufficient Permissions"));
                return;
            }

            DirectControlMode mode = DirectControlMode.OFF;
            if (craft.getDirectControlMode() == DirectControlMode.OFF) {
                mode = DirectControlMode.A;
            } else if (craft.getDirectControlMode() == DirectControlMode.A) {
                mode = DirectControlMode.B;
            }

            craft.setDirectControl(mode);
            player.sendMessage(Text.of("Entering Direct Control Mode " + mode));
            event.setCancelled(true);
        }

    }

}