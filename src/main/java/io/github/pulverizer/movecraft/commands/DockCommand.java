package io.github.pulverizer.movecraft.commands;

import com.flowpowered.math.vector.Vector3i;
import io.github.pulverizer.movecraft.Movecraft;
import io.github.pulverizer.movecraft.config.Settings;
import io.github.pulverizer.movecraft.craft.Craft;
import io.github.pulverizer.movecraft.craft.CraftManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class DockCommand implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {

        if (src instanceof Player) {
            Player player = (Player) src;

            Craft craft = CraftManager.getInstance().getCraftByPlayer(player.getUniqueId());

            if (craft != null && craft.getCommander().equals(player.getUniqueId())) {

                Vector3i midpoint = craft.getHitBox().getMidPoint();
                midpoint = new Vector3i(midpoint.getX(), craft.getHitBox().getMinY() -1, midpoint.getZ());

                Location<World> location = new Location<>(craft.getWorld(), midpoint);

                if (Settings.FlightDeckBlocks.contains(location.getBlockType())) {

                    Craft parentCraft = null;
                    for (Craft testCraft : CraftManager.getInstance().getCraftsFromLocation(location)) {
                        if (parentCraft == null) {
                            parentCraft = testCraft;
                        } else if (testCraft.getSize() > parentCraft.getSize()) {
                            parentCraft = testCraft;
                        }
                    }

                    // Merge to carrier
                    if (parentCraft != null && !parentCraft.isProcessing()) {
                        parentCraft.getHitBox().addAll(craft.getHitBox());
                        craft.release(player);
                        player.sendMessage(Text.of("You have docked"));
                    }
                }
            }
        }

        return CommandResult.success();
    }

    public static void register() {
        CommandSpec dockCommand = CommandSpec.builder()
                .description(Text.of("Merge to the craft you are landed on"))
                .permission("movecraft.command.dock")
                .executor(new DockCommand())
                .build();

        Sponge.getCommandManager().register(Movecraft.getInstance(), dockCommand, "dock");
    }
}
