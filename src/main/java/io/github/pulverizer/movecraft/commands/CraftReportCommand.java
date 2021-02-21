package io.github.pulverizer.movecraft.commands;

import io.github.pulverizer.movecraft.Movecraft;
import io.github.pulverizer.movecraft.craft.CraftManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.text.Text;

import java.util.ArrayList;

public class CraftReportCommand implements CommandExecutor {

    public static void register() {
        CommandSpec commandSpec = CommandSpec.builder()
                .description(Text.of("Provides details on all active crafts on the server"))
                .permission("movecraft.admin")
                .executor(new CraftReportCommand())
                .build();

        Sponge.getCommandManager().register(Movecraft.getInstance(), commandSpec, "craftreport");
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        ArrayList<Text> messages = new ArrayList<>();

        // TODO - Fix output
        CraftManager.getInstance().forEach(craft -> messages.add(Text.of(craft.toString())));

        // send Output
        src.sendMessage(Text.of("Craft Report:"));
        src.sendMessages(messages);

        return CommandResult.success();
    }
}
