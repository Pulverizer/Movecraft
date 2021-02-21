package io.github.pulverizer.movecraft.listener;

import static org.spongepowered.api.event.Order.FIRST;
import static org.spongepowered.api.event.Order.LAST;

import com.flowpowered.math.vector.Vector3i;
import io.github.pulverizer.movecraft.config.Settings;
import io.github.pulverizer.movecraft.config.craft_settings.Defaults;
import io.github.pulverizer.movecraft.craft.Craft;
import io.github.pulverizer.movecraft.craft.CraftManager;
import io.github.pulverizer.movecraft.sign.CommanderSign;
import io.github.pulverizer.movecraft.sign.CrewSign;
import io.github.pulverizer.movecraft.utils.CollectionUtils;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.property.block.MatterProperty;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.HashSet;

public class BlockListener {

    private long lastDamagesUpdate = 0;

    @Listener(order = LAST)
    public void onBlockBreak(ChangeBlockEvent.Break event, @Root Player player) {

        for (Transaction<BlockSnapshot> transaction : event.getTransactions()) {
            BlockSnapshot blockSnapshot = transaction.getOriginal();

            if (Settings.ProtectPilotedCrafts) {
                for (Craft craft : CraftManager.getInstance().getCraftsInWorld(blockSnapshot.getLocation().get().getExtent())) {

                    if (craft == null || craft.isSinking()) {
                        continue;
                    }

                    if (craft.getHitBox().contains(blockSnapshot.getLocation().get().getBlockPosition())) {

                        transaction.setValid(false);
                        player.sendMessage(Text.of("BLOCK IS PART OF A PILOTED CRAFT"));
                        break;
                    }
                }
            }

            if (transaction.isValid()) {
                CommanderSign.onSignBreak(event, transaction);
                CrewSign.onSignBreak(event, transaction);
            }
        }
    }

    @Listener(order = LAST)
    public void onBlockPlace(ChangeBlockEvent.Place event, @Root Player player) {
        if (Settings.ProtectPilotedCrafts) {

            for (Transaction<BlockSnapshot> transaction : event.getTransactions()) {
                Location<World> location = transaction.getOriginal().getLocation().orElse(null);

                boolean foundCrafts = false;
                HashSet<Craft> repairingCrafts = new HashSet<>();

                for (Vector3i blockPosition : CollectionUtils.neighbors(location.getBlockPosition())) {
                    HashSet<Craft> craftsAtLocation =
                            CraftManager.getInstance().getCraftsFromLocation(new Location<>(location.getExtent(), blockPosition));

                    if (!craftsAtLocation.isEmpty()) {
                        foundCrafts = true;

                        for (Craft craft : craftsAtLocation) {

                            if (craft.isSinking() || !craft.isRepairman(player.getUniqueId())
                                    || !craft.getType().getSetting(Defaults.AllowedBlocks.class).get().getValue().contains(transaction.getFinal().getState().getType())
                                    || craft.getSize() >= craft.getType().getSetting(Defaults.MaxSize.class).get().getValue()) {
                                continue;
                            }

                            repairingCrafts.addAll(craftsAtLocation);
                            break;
                        }
                    }
                }


                if (repairingCrafts.isEmpty() && foundCrafts) {
                    transaction.setValid(false);
                    player.sendMessage(Text.of("You are not a repairman!"));

                } else {

                    boolean isProcessing = false;
                    for (Craft craft : repairingCrafts) {
                        if (craft.isProcessing()) {
                            isProcessing = true;
                            break;
                        }
                    }

                    if (!isProcessing) {
                        repairingCrafts.removeIf(craft -> !craft.getType().getSetting(Defaults.AllowedBlocks.class).get().getValue().contains(transaction.getFinal().getState().getType()));
                        repairingCrafts.forEach(craft -> craft.getHitBox().add(location.getBlockPosition()));
                    } else {
                        player.sendMessage(Text.of("Craft is Busy"));
                    }
                }
            }
        }
    }

    // prevent water and lava from spreading on moving crafts
    //TODO: This doesn't actually seem to work.
    @Listener(order = FIRST)
    public void onBlockFromTo(ChangeBlockEvent.Modify event) {

        if (!event.getContext().containsKey(EventContextKeys.LIQUID_FLOW)) {
            return;
        }

        for (Transaction<BlockSnapshot> transaction : event.getTransactions()) {

            if (!transaction.getOriginal().getLocation().isPresent()
                    || transaction.getOriginal().getProperty(MatterProperty.class).get().getValue() != MatterProperty.Matter.LIQUID) {
                continue;
            }

            for (Craft craft : CraftManager.getInstance().getCraftsInWorld(transaction.getOriginal().getLocation().get().getExtent())) {
                if (craft.isProcessing() && craft.getHitBox().contains(transaction.getOriginal().getLocation().get().getBlockPosition())) {
                    transaction.setValid(false);
                    return;
                }
            }
        }
    }

    // prevent pistons from moving on processing crafts
    // TODO
    //  else if - piston extends - add locations to hitbox
    //  else if - piston retracts - remove locations from hitbox
    @Listener(order = FIRST)
    public void onPistonEvent(ChangeBlockEvent.Pre event) {
        if (event.getContext().containsKey(EventContextKeys.PISTON_EXTEND) || event.getContext().containsKey(EventContextKeys.PISTON_RETRACT)) {

            for (Location<World> worldLocation : event.getLocations()) {

                HashSet<Craft> crafts = CraftManager.getInstance().getCraftsFromLocation(worldLocation);

                for (Craft craft : crafts) {
                    if (craft.isProcessing()) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    //TODO: Is this listener needed?

    // Should not need this due to blocks still ticking?

    /*@Listener(order = LAST)
    public void onBlockIgnite(BlockIgniteEvent event) {
        // replace blocks with fire occasionally, to prevent fast crafts from simply ignoring fire
        if (!Settings.FireballPenetration || event.isCancelled() || event.getCause() != BlockIgniteEvent.IgniteCause.FIREBALL) {
            return;
        }
        BlockSnapshot testBlock = event.getBlock().getRelative(-1, 0, 0);
        if (!testBlock.getType().isBurnable())
            testBlock = event.getBlock().getRelative(1, 0, 0);

        if (!testBlock.getType().isBurnable())
            testBlock = event.getBlock().getRelative(0, 0, -1);

        if (!testBlock.getType().isBurnable())
            testBlock = event.getBlock().getRelative(0, 0, 1);

        if (!testBlock.getType().isBurnable()) {
            return;
        }

        testBlock.setType(BlockTypes.AIR);
    }*/

}