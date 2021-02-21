package io.github.pulverizer.movecraft.sign;

import com.flowpowered.math.vector.Vector3i;
import io.github.pulverizer.movecraft.config.Settings;
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

/**
 * Permissions Checked
 * Code to be reviewed
 *
 * @author BernardisGood
 * @version 1.34 - 20 Apr 2020
 */
public final class RelativeMoveSign {

    private static final String HEADER = "RMove:";

    public static void onSignChange(ChangeSignEvent event, Player player) {

        if (Settings.RequireCreateSignPerm && !player.hasPermission("movecraft.createsign.relativemove")) {
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

        if (craft == null) {
            return;
        }

        if (!craft.getType().getSetting(Defaults.CanStaticMove.class).get().getValue()) {
            return;
        }

        // Use permissions
        if (!player.hasPermission("movecraft." + craft.getType().getSetting(Defaults.Name.class).get().getValue() + ".movement.relativemove") && (
                craft.getType().getSetting(Defaults.RequiresSpecificPerms.class).get().getValue() || !player
                        .hasPermission("movecraft.movement.relativemove"))) {
            player.sendMessage(Text.of("Insufficient Permissions"));
            return;
        }

        String[] numbers = BlockSnapshotSignDataUtil.getTextLine(block, 2).get().split(",");
        int dLeftRight = Integer.parseInt(numbers[0]);
        // negative = left,
        // positive = right
        int dy = Integer.parseInt(numbers[1]);
        int dBackwardForward = Integer.parseInt(numbers[2]);
        // negative = backwards,
        // positive = forwards
        int maxMove = craft.getType().getSetting(Defaults.MaxStaticMove.class).get().getValue();

        if (dLeftRight > maxMove) {
            dLeftRight = maxMove;
        }
        if (dLeftRight < -maxMove) {
            dLeftRight = -maxMove;
        }
        if (dy > maxMove) {
            dy = maxMove;
        }
        if (dy < -maxMove) {
            dy = -maxMove;
        }
        if (dBackwardForward > maxMove) {
            dBackwardForward = maxMove;
        }
        if (dBackwardForward < -maxMove) {
            dBackwardForward = -maxMove;
        }
        int dx = 0;
        int dz = 0;

        //get Orientation
        Direction orientation = block.get(Keys.DIRECTION).get().getOpposite();
        if (orientation != Direction.NORTH && orientation != Direction.WEST && orientation != Direction.SOUTH && orientation != Direction.EAST) {
            if (orientation == Direction.NORTH_NORTHEAST || orientation == Direction.NORTH_NORTHWEST) {
                orientation = Direction.NORTH;
            } else if (orientation == Direction.SOUTH_SOUTHEAST || orientation == Direction.SOUTH_SOUTHWEST) {
                orientation = Direction.SOUTH;
            } else if (orientation == Direction.WEST_NORTHWEST || orientation == Direction.WEST_SOUTHWEST) {
                orientation = Direction.WEST;
            } else if (orientation == Direction.EAST_NORTHEAST || orientation == Direction.EAST_SOUTHEAST) {
                orientation = Direction.EAST;
            } else {
                player.sendMessage(Text.of("Invalid Sign Orientation!"));
                return;
            }
        }


        switch (orientation) {
            case NORTH:
                // North
                dx = dLeftRight;
                dz = -dBackwardForward;
                break;
            case SOUTH:
                // South
                dx = -dLeftRight;
                dz = dBackwardForward;
                break;
            case EAST:
                // East
                dx = dBackwardForward;
                dz = dLeftRight;
                break;
            case WEST:
                // West
                dx = -dBackwardForward;
                dz = -dLeftRight;
                break;
        }

        CraftManager.getInstance().getCraftByPlayer(player.getUniqueId()).translate(new Vector3i(dx, dy, dz));
    }
}