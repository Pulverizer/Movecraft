package io.github.pulverizer.movecraft.craft.crew;

import io.github.pulverizer.movecraft.Movecraft;
import io.github.pulverizer.movecraft.config.Settings;
import io.github.pulverizer.movecraft.craft.Craft;
import io.github.pulverizer.movecraft.craft.CraftManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CrewManager {
    private static CrewManager ourInstance;
    private final HashSet<CrewInvite> pendingInvites = new HashSet<>();

    private CrewManager() {}

    private static void initialize(){
        ourInstance = new CrewManager();

        Task.builder()
                .async()
                .intervalTicks(1)
                .execute(() -> CrewManager.getInstance().cleanPendingInvites())
                .submit(Movecraft.getInstance());
    }

    public static CrewManager getInstance() {
        if (ourInstance == null) initialize();

        return ourInstance;
    }

    private void cleanPendingInvites() {
        pendingInvites.removeIf(invite -> invite.getTicksSinceSent() > Settings.CrewInviteTimeout);
    }

    public void createInvite(Player sender, Player invited) {
        if (sender.equals(invited)) {
            return;
        }

        Craft craft = CraftManager.getInstance().getCraftByPlayer(sender.getUniqueId());

        if (!craft.getCommander().equals(sender.getUniqueId())) {
            sender.sendMessage(Text.of("You are not the commander of your craft."));
            return;
        }

        if (!sender.hasPermission("movecraft." + craft.getType().getName() + ".crew.invite") && (craft.getType().requiresSpecificPerms() || !sender.hasPermission("movecraft.crew.invite"))) {
            sender.sendMessage(Text.of("Insufficient Permissions"));
            return;
        }

        if (!invited.hasPermission("movecraft." + craft.getType().getName() + ".crew.join") && (craft.getType().requiresSpecificPerms() || !invited.hasPermission("movecraft.crew.join"))) {
            sender.sendMessage(Text.of("Insufficient Permissions - Target player does not have necessary permissions."));
            return;
        }

        CrewInvite invite;

        if (craft.getName() != null && !craft.getName().isEmpty()) {
            invite = new CrewInvite(invited.getUniqueId(), sender, craft, craft.getName());
        } else {
            invite = new CrewInvite(invited.getUniqueId(), sender, craft);
        }

        sender.sendMessage(Text.of(String.format("You have invited %s to your crew.", invited.getName())));

        invited.sendMessage(Text.of(invite.getMessage()));
        invited.sendMessage(Text.of(String.format("Use \"/crew join %s\" to join.", sender.getName())));

        pendingInvites.add(invite);
    }

    public void acceptInvite(Player player, Player invitedBy) {
        CrewInvite foundInvite = null;

        for (CrewInvite invite : pendingInvites) {
            if (invite.getInvitedPlayerUUID().equals(player.getUniqueId()) && invite.getInvitedBy().equals(invitedBy.getUniqueId())) {
                foundInvite = invite;
                Craft craft = invite.getCraft();
                if (craft != null) {
                    craft.addCrewMember(player.getUniqueId());
                    player.sendMessage(Text.of("Welcome to the crew!"));
                    Sponge.getServer().getPlayer(craft.getCommander()).ifPresent(commander -> commander.sendMessage(Text.of(player.getName() + " has joined your crew.")));
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

    public void kickCrewMember(Player sender, Player target) {
        if (sender.equals(target)) {
            return;
        }

        Craft craft = CraftManager.getInstance().getCraftByPlayer(sender.getUniqueId());

        if (!craft.getCommander().equals(sender.getUniqueId())) {
            sender.sendMessage(Text.of("You are not the commander of your craft."));
        }

        if (!sender.hasPermission("movecraft." + craft.getType().getName() + ".crew.kick") && (craft.getType().requiresSpecificPerms() || !sender.hasPermission("movecraft.crew.kick"))) {
            sender.sendMessage(Text.of("Insufficient Permissions"));
            return;
        }

        sender.sendMessage(Text.of(String.format("You have kicked %s from your crew.", target.getName())));

        craft.removeCrewMember(target.getUniqueId());
        target.sendMessage(Text.of("You have been kicked from the crew."));
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
            sender.sendMessage(Text.of(String.format("%s is now the Commander.", player.getName())));
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

        if (!craft.getType().canHaveLoaders()) {
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

        if (!craft.getType().canHaveRepairmen()) {
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

    public void leaveCrew(Player sender) {
        Craft craft = CraftManager.getInstance().getCraftByPlayer(sender.getUniqueId());

        if (craft == null) {
            sender.sendMessage(Text.of("You are not in a crew!"));
            return;
        }

        craft.removeCrewMember(sender.getUniqueId());
        sender.sendMessage(Text.of("You have left the crew."));
    }

    public ArrayList<String> getPrintedCrewList(Player player) {
        Craft craft = CraftManager.getInstance().getCraftByPlayer(player.getUniqueId());

        ArrayList<String> lines = new ArrayList<>();

        if (craft == null) {
            lines.add("You are not in a crew!");
            return lines;
        }

        lines.add("");
        Sponge.getServer().getPlayer(craft.getCommander()).ifPresent(crewMember -> lines.add("Commander: " + crewMember.getName()));
        Sponge.getServer().getPlayer(craft.getNextInCommand()).ifPresent(crewMember -> lines.add("Next-In-Command: " + crewMember.getName()));
        Sponge.getServer().getPlayer(craft.getPilot()).ifPresent(crewMember -> lines.add("Pilot: " + crewMember.getName()));
        lines.add("");

        Set<UUID> crewMembers = craft.getAADirectors();
        if (!crewMembers.isEmpty()) {
            lines.add("AA Directors: ");
            crewMembers.forEach(aaDirector -> Sponge.getServer().getPlayer(aaDirector).ifPresent(crewMember -> lines.add("                  " + crewMember.getName())));
            lines.add("");
        }

        crewMembers = craft.getCannonDirectors();
        if (!crewMembers.isEmpty()) {
            lines.add("Cannon Directors: ");
            crewMembers.forEach(cannonDirector -> Sponge.getServer().getPlayer(cannonDirector).ifPresent(crewMember -> lines.add("                  " + crewMember.getName())));
            lines.add("");
        }

        crewMembers = craft.getLoaders();
        if (!crewMembers.isEmpty()) {
            lines.add("Loaders: ");
            crewMembers.forEach(loader -> Sponge.getServer().getPlayer(loader).ifPresent(crewMember -> lines.add("                  " + crewMember.getName())));
            lines.add("");
        }

        crewMembers = craft.getRepairmen();
        if (!crewMembers.isEmpty()) {
            lines.add("Repairmen: ");
            craft.getRepairmen().forEach(repairman -> Sponge.getServer().getPlayer(repairman).ifPresent(crewMember -> lines.add("                  " + crewMember.getName())));
            lines.add("");
        }

        return lines;
    }
}
