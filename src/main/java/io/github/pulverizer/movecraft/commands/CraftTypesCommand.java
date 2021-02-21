package io.github.pulverizer.movecraft.commands;

import io.github.pulverizer.movecraft.Movecraft;
import io.github.pulverizer.movecraft.config.CraftType;
import io.github.pulverizer.movecraft.config.craft_settings.Defaults;
import io.github.pulverizer.movecraft.craft.CraftManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.text.Text;

import java.util.ArrayList;

public class CraftTypesCommand implements CommandExecutor {

    public static void register() {
        CommandSpec listCommand = CommandSpec.builder()
                .description(Text.of("Lists the name of all the craft types available on the server."))
                .executor(new CraftTypesCommand.List())
                .build();

        CommandSpec infoCommand = CommandSpec.builder()
                .description(Text.of("Provides a list of allowed, fly and move blocks for the provided craft type."))
                .arguments(GenericArguments.string(Text.of("crafttype")))
                .executor(new CraftTypesCommand.Info())
                .build();

        CommandSpec commandSpec = CommandSpec.builder()
                .description(Text.of("/crafttypes [list] || [info <crafttype>]"))
                .child(listCommand, "list")
                .child(infoCommand, "info")
                .permission("movecraft.admin")
                .executor(new CraftTypesCommand())
                .build();

        Sponge.getCommandManager().register(Movecraft.getInstance(), commandSpec, "crafttypes");
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        // send Output
        src.sendMessage(Text.of("/crafttypes [list] || [info <crafttype>]"));

        return CommandResult.success();
    }

    public static class List extends CraftTypesCommand {

        // TODO - Change to show only craft types the player has permissions for?
        @Override
        public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
            ArrayList<Text> messages = new ArrayList<>();

            CraftManager.getInstance().getCraftTypes()
                    .forEach(craftType -> messages.add(Text.of(craftType.getSetting(Defaults.Name.class).get().getValue())));

            // send Output
            src.sendMessage(Text.of("Craft Types:"));
            src.sendMessages(messages);

            return CommandResult.success();
        }
    }

    public static class Info extends CraftTypesCommand {

        @Override
        public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
            ArrayList<Text> messages = new ArrayList<>();

            CraftType craftType = CraftManager.getInstance().getCraftTypeFromString((String) args.getOne("crafttype").get());

            messages.add(Text.of("Name: " + craftType.getSetting(Defaults.Name.class).get().getValue()));
            messages.add(Text.of("Allowed Blocks:"));
            craftType.getSetting(Defaults.AllowedBlocks.class).get().getValue().forEach(blockType -> messages.add(Text.of("    " + blockType.getTranslation().get(src.getLocale()))));

            // send Output
            src.sendMessages(messages);

            return CommandResult.success();
        }
    }
}
