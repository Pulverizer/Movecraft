package io.github.pulverizer.movecraft.commands;

import io.github.pulverizer.movecraft.Movecraft;
import io.github.pulverizer.movecraft.craft.crew.CrewManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

public class CrewCommand implements CommandExecutor {

    public static void register() {
        // TODO - these might be buggy as it does not enforce exact matches!
        CommandSpec crewJoinCommand = CommandSpec.builder()
                .description(Text.of("Join the crew of the craft after being invited"))
                .arguments(GenericArguments.onlyOne(GenericArguments.player(Text.of("username"))))
                .executor(new Join())
                .build();

        CommandSpec crewInviteCommand = CommandSpec.builder()
                .description(Text.of("Invite a player to join your crew"))
                .arguments(GenericArguments.onlyOne(GenericArguments.player(Text.of("username"))))
                .executor(new Invite())
                .build();

        CommandSpec crewKickCommand = CommandSpec.builder()
                .description(Text.of("Kick a player from your crew"))
                .arguments(GenericArguments.onlyOne(GenericArguments.player(Text.of("username"))))
                .executor(new Kick())
                .build();

        CommandSpec crewLeaveCommand = CommandSpec.builder()
                .description(Text.of("Leave the your current crew"))
                .executor(new Leave())
                .build();

        CommandSpec crewCommand = CommandSpec.builder()
                .description(Text.of("Information on your current crew"))
                .child(crewJoinCommand, "join")
                .child(crewInviteCommand, "invite")
                .child(crewKickCommand, "kick")
                .child(crewLeaveCommand, "leave")
                .executor(new CrewCommand())
                .build();

        Sponge.getCommandManager().register(Movecraft.getInstance(), crewCommand, "crew");
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {

        if (src instanceof Player) {
            CrewManager.getInstance().getPrintedCrewList((Player) src).forEach(line -> src.sendMessage(Text.of(line)));
        }

        return CommandResult.success();
    }

    public static class Join extends CrewCommand {

        @Override
        public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
            if (src instanceof Player) {
                CrewManager.getInstance().acceptInvite((Player) src, ((Player) args.getOne("username").get()));
                return CommandResult.success();
            }

            return CommandResult.empty();
        }
    }

    public static class Invite extends CrewCommand {

        @Override
        public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
            if (src instanceof Player) {
                CrewManager.getInstance().createInvite((Player) src, ((Player) args.getOne("username").get()));
                return CommandResult.success();
            }

            return CommandResult.empty();
        }
    }

    public static class Kick extends CrewCommand {

        @Override
        public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
            if (src instanceof Player) {
                CrewManager.getInstance().kickCrewMember((Player) src, ((Player) args.getOne("username").get()));
                return CommandResult.success();
            }

            return CommandResult.empty();
        }
    }

    public static class Leave extends CrewCommand {

        @Override
        public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
            if (src instanceof Player) {
                CrewManager.getInstance().leaveCrew((Player) src);
                return CommandResult.success();
            }

            return CommandResult.empty();
        }
    }
}
