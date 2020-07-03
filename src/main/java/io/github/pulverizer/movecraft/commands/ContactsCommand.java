package io.github.pulverizer.movecraft.commands;

import io.github.pulverizer.movecraft.Movecraft;
import io.github.pulverizer.movecraft.config.CraftType;
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

import java.util.ArrayList;

public class ContactsCommand implements CommandExecutor {
    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        // Contacts:
        //          [CraftType] [Size] [Name] [Commanded/Piloted By] [Range] <Compass Heading || Center Block Position>
        //     FULL: Airship (8,560) MSV Grasshopper commanded by BernardisGood - 3,500m - North-West - (250, 178, -3000)
        // FAR PING: UNKNOWN CONTACT (600) - 3750m - North-West

        if (src instanceof Player) {

            ArrayList<Text> messages = new ArrayList<>();

            messages.add(Text.of("Contacts:"));

            Craft craft = CraftManager.getInstance().getCraftByPlayer(((Player) src).getUniqueId());
            craft.getContacts().forEach(contact -> messages.add(Text.of(craft.getContactChatMessage(contact))));

            // send Output
            src.sendMessages(messages);

            return CommandResult.success();
        } else {
            return CommandResult.empty();
        }
    }

    public static void register() {
        CommandSpec commandSpec = CommandSpec.builder()
                .description(Text.of("Lists all contacts from radar"))
                .executor(new ContactsCommand())
                .build();

        Sponge.getCommandManager().register(Movecraft.getInstance(), commandSpec, "contacts");
    }
}
