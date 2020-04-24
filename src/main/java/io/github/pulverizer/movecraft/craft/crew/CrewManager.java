package io.github.pulverizer.movecraft.craft.crew;

import io.github.pulverizer.movecraft.Movecraft;
import io.github.pulverizer.movecraft.config.Settings;
import io.github.pulverizer.movecraft.craft.Craft;
import io.github.pulverizer.movecraft.craft.CraftManager;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;

import java.util.HashSet;

public class CrewManager {
    private static CrewManager ourInstance;
    private final HashSet<CrewInvite> pendingInvites = new HashSet<>();

    public static void initialize(){
        ourInstance = new CrewManager();

        Task.builder()
                .async()
                .intervalTicks(1)
                .execute(() -> CrewManager.getInstance().cleanPendingInvites())
                .submit(Movecraft.getInstance());
    }

    public static CrewManager getInstance() {
        return ourInstance;
    }

    private void cleanPendingInvites() {
        pendingInvites.removeIf(invite -> invite.getTicksSinceSent() > Settings.InviteTimeout);
    }

    public void createInvite(Player sender, Craft craft, Player invited) {
        CrewInvite invite;

        if (!craft.getCommander().equals(sender.getUniqueId())) {
            sender.sendMessage(Text.of("You are not the commander of your craft."));
        }

        if (!sender.hasPermission("movecraft." + craft.getType().getName() + ".crew.invite") && (craft.getType().requiresSpecificPerms() || !invited.hasPermission("movecraft.crew.invite"))) {
            sender.sendMessage(Text.of("Insufficient Permissions"));
            return;
        }

        if (!invited.hasPermission("movecraft." + craft.getType().getName() + ".crew.join") && (craft.getType().requiresSpecificPerms() || !invited.hasPermission("movecraft.crew.join"))) {
            sender.sendMessage(Text.of("Insufficient Permissions - Target player does not have necessary permissions."));
            return;
        }

        if (craft.getName() != null && !craft.getName().isEmpty()) {
            invite = new CrewInvite(invited.getUniqueId(), sender.getName(), craft, craft.getName());
        } else {
            invite = new CrewInvite(invited.getUniqueId(), sender.getName(), craft);
        }

        invited.sendMessage(Text.of(invite.getMessage()));
        invited.sendMessage(Text.of(String.format("Use \"/crew accept %s\" to join.", sender.getName())));
    }

    public void acceptInvite(Player player, String invitedBy) {
        CrewInvite foundInvite = null;

        for (CrewInvite invite : pendingInvites) {
            if (invite.getInvitedPlayerUUID().equals(player.getUniqueId()) && invite.getInvitedBy().equalsIgnoreCase(invitedBy)) {
                foundInvite = invite;
                Craft craft = invite.getCraft();
                if (craft != null) {
                    craft.addCrewMember(player.getUniqueId());
                    player.sendMessage(Text.of("Welcome to the crew!"));
                } else {
                    player.sendMessage(Text.of("Invite Invalid - Craft no longer exists."));
                }

                break;
            }
        }

        if (foundInvite != null) {
            pendingInvites.remove(foundInvite);
        } else {
            player.sendMessage(Text.of("Invite has expired!"));
        }
    }

    public void transferCommand(Player sender, Player player) {
        Craft craft = CraftManager.getInstance().getCraftByPlayer(player.getUniqueId());

        if (craft == null) {
            player.sendMessage(Text.of("You are not commanding a craft!"));
            return;
        }

        if (!craft.getCommander().equals(sender.getUniqueId())) {
            sender.sendMessage(Text.of("You are not the commander of your craft."));
            return;
        }

        if (!player.hasPermission("movecraft." + craft.getType().getName() + ".crew.command") && (craft.getType().requiresSpecificPerms() || !player.hasPermission("movecraft.crew.command"))) {
            sender.sendMessage(Text.of("Insufficient Permissions - Target player does not have necessary permissions."));
            return;
        }

        if (craft.setCommander(player.getUniqueId())) {
            sender.sendMessage(Text.of(String.format("%s is now in the Commander.", player.getName())));
            player.sendMessage(Text.of("You are now the Commander."));
        }
    }

    public void setNextInCommand(Player sender, Player player) {
        Craft craft = CraftManager.getInstance().getCraftByPlayer(player.getUniqueId());

        if (craft == null) {
            player.sendMessage(Text.of("You are not commanding a craft!"));
            return;
        }

        if (!craft.getCommander().equals(sender.getUniqueId())) {
            sender.sendMessage(Text.of("You are not the commander of your craft."));
            return;
        }

        if (!player.hasPermission("movecraft." + craft.getType().getName() + ".crew.command") && (craft.getType().requiresSpecificPerms() || !player.hasPermission("movecraft.crew.command"))) {
            sender.sendMessage(Text.of("Insufficient Permissions - Target player does not have necessary permissions."));
            return;
        }

        if (craft.setNextInCommand(player.getUniqueId())) {
            sender.sendMessage(Text.of(String.format("%s is now your Second-In-Command.", player.getName())));
            player.sendMessage(Text.of(String.format("You are now Second-In-Command for %s.", sender.getName())));
        }
    }

    public void setPilot(Player player) {
        Craft craft = CraftManager.getInstance().getCraftByPlayer(player.getUniqueId());

        if (craft == null) {
            player.sendMessage(Text.of("You are not in a crew!"));
            return;
        }

        if (!player.hasPermission("movecraft." + craft.getType().getName() + ".crew.pilot") && (craft.getType().requiresSpecificPerms() || !player.hasPermission("movecraft.crew.pilot"))) {
            player.sendMessage(Text.of("Insufficient Permissions"));
            return;
        }

        if (craft.setPilot(player.getUniqueId())) {
            player.sendMessage(Text.of("You are now the pilot of your craft."));
        }
    }

    public void addAADirector(Player player) {
        Craft craft = CraftManager.getInstance().getCraftByPlayer(player.getUniqueId());

        if (craft == null) {
            player.sendMessage(Text.of("You are not in a crew!"));
            return;
        }

        if (!craft.getType().allowAADirectors()) {
            player.sendMessage(Text.of("ERROR: Craft type does not support the AA director role!"));
            return;
        }

        if (!player.hasPermission("movecraft." + craft.getType().getName() + ".crew.directors.aa") && (craft.getType().requiresSpecificPerms() || !player.hasPermission("movecraft.crew.directors.aa"))) {
            player.sendMessage(Text.of("Insufficient Permissions"));
            return;
        }

        if (craft.addAADirector(player.getUniqueId())) {
            player.sendMessage(Text.of("You are now a AA director aboard your craft."));
        }
    }

    public void addCannonDirector(Player player) {
        Craft craft = CraftManager.getInstance().getCraftByPlayer(player.getUniqueId());

        if (craft == null) {
            player.sendMessage(Text.of("You are not in a crew!"));
            return;
        }

        if (!craft.getType().allowCannonDirectors()) {
            player.sendMessage(Text.of("ERROR: Craft type does not support the cannon director role!"));
            return;
        }

        if (!player.hasPermission("movecraft." + craft.getType().getName() + ".crew.directors.cannons") && (craft.getType().requiresSpecificPerms() || !player.hasPermission("movecraft.crew.directors.cannons"))) {
            player.sendMessage(Text.of("Insufficient Permissions"));
            return;
        }

        if (craft.addCannonDirector(player.getUniqueId())) {
            player.sendMessage(Text.of("You are now a cannon director aboard your craft."));
        }
    }

    public void addLoader(Player player) {
        Craft craft = CraftManager.getInstance().getCraftByPlayer(player.getUniqueId());

        if (craft == null) {
            player.sendMessage(Text.of("You are not in a crew!"));
            return;
        }

        if (!craft.getType().allowLoaders()) {
            player.sendMessage(Text.of("ERROR: Craft type does not support the loader role!"));
            return;
        }

        if (!player.hasPermission("movecraft." + craft.getType().getName() + ".crew.loader") && (craft.getType().requiresSpecificPerms() || !player.hasPermission("movecraft.crew.loader"))) {
            player.sendMessage(Text.of("Insufficient Permissions"));
            return;
        }

        if (craft.addLoader(player.getUniqueId())) {
            player.sendMessage(Text.of("You are now a loader aboard your craft."));
        }
    }

    public void addRepairman(Player player) {
        Craft craft = CraftManager.getInstance().getCraftByPlayer(player.getUniqueId());

        if (craft == null) {
            player.sendMessage(Text.of("You are not in a crew!"));
            return;
        }

        if (!craft.getType().allowRepairmen()) {
            player.sendMessage(Text.of("ERROR: Craft type does not support the repairman role!"));
            return;
        }

        if (!player.hasPermission("movecraft." + craft.getType().getName() + ".crew.repairman") && (craft.getType().requiresSpecificPerms() || !player.hasPermission("movecraft.crew.repairman"))) {
            player.sendMessage(Text.of("Insufficient Permissions"));
            return;
        }

        if (craft.addRepairman(player.getUniqueId())) {
            player.sendMessage(Text.of("You are now a repairman aboard your craft."));
        }
    }

    public void resetRole(Player player) {
        Craft craft = CraftManager.getInstance().getCraftByPlayer(player.getUniqueId());

        if (craft == null) {
            player.sendMessage(Text.of("You are not in a crew!"));
            return;
        }

        craft.resetCrewRole(player.getUniqueId());
        player.sendMessage(Text.of("Your role aboard your craft has been reset."));
    }
}
