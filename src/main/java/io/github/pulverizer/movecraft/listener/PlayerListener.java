package io.github.pulverizer.movecraft.listener;

import com.flowpowered.math.vector.Vector3d;
import io.github.pulverizer.movecraft.config.Settings;
import io.github.pulverizer.movecraft.config.craft_settings.Defaults;
import io.github.pulverizer.movecraft.craft.Craft;
import io.github.pulverizer.movecraft.craft.CraftManager;
import io.github.pulverizer.movecraft.enums.DirectControlMode;
import io.github.pulverizer.movecraft.utils.MathUtils;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.Text;

import java.util.Map;
import java.util.WeakHashMap;

public class PlayerListener {

    private final Map<Craft, Long> timeToReleaseAfter = new WeakHashMap<>();

    @Listener
    public void onPLayerLogout(ClientConnectionEvent.Disconnect event, @Getter("getTargetEntity") Player player) {

        Craft craft = CraftManager.getInstance().getCraftByPlayer(player.getUniqueId());

        if (craft != null) {
            craft.removeCrewMember(player.getUniqueId());

            if (craft.crewIsEmpty()) {
                craft.release(player);
            }
        }
    }


    @Listener
    public void onPlayerDeath(DestructEntityEvent.Death event, @Getter("getTargetEntity") Player player) {

        Craft craft = CraftManager.getInstance().getCraftByPlayer(player.getUniqueId());

        if (craft == null) {
            return;
        }

        if (craft.getCommander() == player.getUniqueId()) {
            craft.setCommander(craft.getNextInCommand());
        }

        craft.removeCrewMember(player.getUniqueId());

        //TODO: Change to not release but allow the ship to keep cruising and be sunk or claimed.
        if (craft.crewIsEmpty()) {
            craft.release(player);
        }
    }

    //TODO - Needs major rework to be compatible with crews
    @Listener
    public void onPlayerMove(MoveEntityEvent event, @Root Player player) {
        final Craft craft = CraftManager.getInstance().getCraftByPlayer(player.getUniqueId());

        if (craft == null) {
            return;
        }

        if (!(event instanceof MoveEntityEvent.Teleport)
                && craft.getDirectControlMode() == DirectControlMode.A
                && craft.getPilot() != null
                && craft.getPilot().equals(player.getUniqueId())) {

            event.setCancelled(true);

            if (!craft.hasCooldownExpired()) {
                return;
            }

            Vector3d playerDisplacement = event.getToTransform().getPosition().sub(event.getFromTransform().getPosition());

            if (playerDisplacement.getY() > 0) {
                playerDisplacement = new Vector3d(playerDisplacement.getX(), 1, playerDisplacement.getZ());
            }

            if (player.get(Keys.IS_SNEAKING).get()) {
                playerDisplacement = new Vector3d(playerDisplacement.getX(), -1, playerDisplacement.getZ());
            }

            playerDisplacement = playerDisplacement.normalize();
            playerDisplacement = playerDisplacement.mul(craft.getType().getSetting(Defaults.CruiseSkipBlocks.class).get().getValue());

            craft.translate(playerDisplacement.toInt());
            return;
        }

        if (MathUtils.locationNearHitBox(craft.getHitBox(), player.getPosition(), 2)) {
            timeToReleaseAfter.remove(craft);
            return;
        }

        if (timeToReleaseAfter.containsKey(craft) && timeToReleaseAfter.get(craft) < System.currentTimeMillis()) {
            craft.removeCrewMember(player.getUniqueId());
            timeToReleaseAfter.remove(craft);
            return;
        }

        if (!craft.isProcessing() && craft.getType().getSetting(Defaults.MoveEntities.class).get().getValue() && !timeToReleaseAfter.containsKey(craft)) {
            if (Settings.ManOverboardTimeout != 0) {
                player.sendMessage(Text.of("You have left your craft.")); //TODO: Re-add /manoverboard
            } else {
                player.sendMessage(Text.of("You have released your craft."));
            }
            if (craft.getHitBox().size() > 11000) {
                player.sendMessage(Text.of("Craft is too big to check its borders. Make sure this area is safe to release your craft in."));
            }
            timeToReleaseAfter.put(craft, System.currentTimeMillis() + 30000); //30 seconds to release
        }
    }
}