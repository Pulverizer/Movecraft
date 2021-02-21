package io.github.pulverizer.movecraft.sign;

import io.github.pulverizer.movecraft.config.CraftType;
import io.github.pulverizer.movecraft.config.craft_settings.Defaults;
import io.github.pulverizer.movecraft.craft.Craft;
import io.github.pulverizer.movecraft.craft.CraftManager;
import io.github.pulverizer.movecraft.utils.BlockSnapshotSignDataUtil;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.block.tileentity.ChangeSignEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

/**
 * Permissions Checked
 * Code complete EXCEPT: TODOs and code clean up related to SignListener
 *
 * @author BernardisGood
 * @version 1.4 - 20 Apr 2020
 */
public final class CraftSign {

    public static void onSignChange(ChangeSignEvent event, Player player, String craftType) {
        if (!player.hasPermission("movecraft." + craftType + ".create")) {
            player.sendMessage(Text.of("Insufficient Permissions"));
            event.setCancelled(true);
        }
    }

    public static void onSignClick(InteractBlockEvent.Secondary.MainHand event, Player player, BlockSnapshot block) {

        if (!block.getLocation().isPresent()) {
            return;
        }

        String craftTypeString = BlockSnapshotSignDataUtil.getTextLine(block, 1).get();

        CraftType type = CraftManager.getInstance().getCraftTypeFromString(craftTypeString);
        if (type == null) {
            return;
        }

        // Valid sign, check player has command permission
        if (!player.hasPermission("movecraft." + craftTypeString + ".crew.command") && (
                type.getSetting(Defaults.RequiresSpecificPerms.class).get().getValue() || !player.hasPermission("movecraft.crew.command"))) {
            player.sendMessage(Text.of("Insufficient Permissions"));
            return;
        }

        // Attempt to run detection
        Location<World> loc = block.getLocation().get();

        if (type.getSetting(Defaults.CruiseOnPilot.class).get().getValue()) {

            //get Cruise Direction
            Direction cruiseDirection = block.get(Keys.DIRECTION).get();
            if (cruiseDirection != Direction.NORTH && cruiseDirection != Direction.WEST && cruiseDirection != Direction.SOUTH
                    && cruiseDirection != Direction.EAST) {
                player.sendMessage(Text.of("Invalid Cruise Direction"));
                return;
            }

            final Craft craft = new Craft(type, player, loc);

            craft.setCruising(craft.getType().getSetting(Defaults.CruiseOnPilotVertDirection.class).get().getValue(), cruiseDirection);

        } else {
            final Craft oldCraft = CraftManager.getInstance().getCraftByPlayer(player.getUniqueId());

            if (oldCraft == null) {
                new Craft(type, player, loc);

            } else if (!oldCraft.isProcessing()) {
                // TODO - do this via CraftManager after detection
                //oldCraft.removeCrewMember(player.getUniqueId());
                new Craft(type, player, loc);
            }
        }
        event.setCancelled(true);

    }
}