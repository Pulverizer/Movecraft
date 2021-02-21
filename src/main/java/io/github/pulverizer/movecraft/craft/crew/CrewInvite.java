package io.github.pulverizer.movecraft.craft.crew;

import io.github.pulverizer.movecraft.config.craft_settings.Defaults;
import io.github.pulverizer.movecraft.craft.Craft;
import io.github.pulverizer.movecraft.craft.CraftManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;

import java.util.UUID;

public class CrewInvite {

    private UUID invitedPlayer;
    private UUID invitedBy;
    private UUID craftUUID;
    private String message;
    private int tickSent;

    public CrewInvite(UUID invitedPlayer, Player invitedBy, Craft craft) {
        this.invitedPlayer = invitedPlayer;
        this.invitedBy = invitedBy.getUniqueId();
        craftUUID = craft.getId();

        tickSent = Sponge.getServer().getRunningTimeTicks();
        message = String.format("You have been invited by %s to serve under them aboard their %s.", invitedBy.getName(), craft.getType().getSetting(
                Defaults.Name.class).get().getValue());
    }

    public CrewInvite(UUID invitedPlayer, Player invitedBy, Craft craft, String craftName) {
        this.invitedPlayer = invitedPlayer;
        this.invitedBy = invitedBy.getUniqueId();
        craftUUID = craft.getId();

        tickSent = Sponge.getServer().getRunningTimeTicks();
        message = String.format("You have been invited by %s to serve under them aboard their %s, the %s.", invitedBy.getName(),
                craft.getType().getSetting(
                        Defaults.Name.class).get().getValue(), craftName);
    }

    public UUID getInvitedPlayerUUID() {
        return invitedPlayer;
    }

    public UUID getInvitedBy() {
        return invitedBy;
    }

    public Craft getCraft() {
        return CraftManager.getInstance().getCraftByUUID(craftUUID);
    }

    public int getTicksSinceSent() {
        return Sponge.getServer().getRunningTimeTicks() - tickSent;
    }

    public String getMessage() {
        return message;
    }
}
