package io.github.pulverizer.movecraft.listener;

import com.flowpowered.math.vector.Vector3d;
import io.github.pulverizer.movecraft.enums.DirectControlMode;
import io.github.pulverizer.movecraft.utils.MathUtils;
import io.github.pulverizer.movecraft.craft.Craft;
import io.github.pulverizer.movecraft.config.Settings;
import io.github.pulverizer.movecraft.craft.CraftManager;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.entity.damage.DamageTypes;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.InventoryTransformations;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.explosion.Explosion;

import java.util.*;

public class PlayerListener {
    private final Map<Craft, Long> timeToReleaseAfter = new WeakHashMap<>();

    @Listener
    public void onPLayerLogout(ClientConnectionEvent.Disconnect event, @Getter("getTargetEntity") Player player) {

        Craft craft = CraftManager.getInstance().getCraftByPlayer(player.getUniqueId());

        if (craft != null) {
            craft.removeCrewMember(player.getUniqueId());

            if (craft.crewIsEmpty())
                craft.release(player);
        }
    }


    @Listener
    public void onPlayerDeath(DestructEntityEvent.Death event, @Getter("getTargetEntity") Player player) {

        Craft craft = CraftManager.getInstance().getCraftByPlayer(player.getUniqueId());

        if (craft == null)
            return;

        if (craft.getCommander() == player.getUniqueId()) {
            craft.setCommander(craft.getNextInCommand());
        }

        craft.removeCrewMember(player.getUniqueId());

        //TODO: Change to not release but allow the ship to keep cruising and be sunk or claimed.
        if (craft.crewIsEmpty())
            craft.release(player);
    }

    @Listener
    public void playerFireDamage(DamageEntityEvent event, @Getter("getTargetEntity") Player player, @Getter("getSource") DamageSource damageSource) {
        if (Settings.AmmoDetonationMultiplier > 0 && damageSource.getType().equals(DamageTypes.FIRE)) {
            float tntCount = player.getInventory().query(QueryOperationTypes.ITEM_TYPE.of(ItemTypes.TNT), QueryOperationTypes.ITEM_TYPE.of(ItemTypes.TNT_MINECART)).totalItems();
            float fireChargeCount = player.getInventory().query(QueryOperationTypes.ITEM_TYPE.of(ItemTypes.FIRE_CHARGE)).totalItems();
            float otherCount = player.getInventory().query(QueryOperationTypes.ITEM_TYPE.of(ItemTypes.FIREWORK_CHARGE), QueryOperationTypes.ITEM_TYPE.of(ItemTypes.FIREWORKS), QueryOperationTypes.ITEM_TYPE.of(ItemTypes.GUNPOWDER)).totalItems();

            float chance = ((tntCount / (Settings.AmmoDetonationMultiplier * 128)) + (fireChargeCount / (Settings.AmmoDetonationMultiplier * 512)) + (otherCount / (Settings.AmmoDetonationMultiplier * 1024)));

            int diceRolled = new Random().nextInt(100);

            if (diceRolled <= chance) {
                float size = Math.min(chance * 2, 16);

                Explosion explosion = Explosion.builder()
                        .location(player.getLocation().add(0, 1.5, 0))
                        .shouldBreakBlocks(true)
                        .shouldDamageEntities(true)
                        .shouldPlaySmoke(true)
                        .radius(size)
                        .resolution((int) (size * 2))
                        .knockback(1)
                        .canCauseFire(fireChargeCount > 0)
                        .build();

                if (tntCount > 0) {
                    player.getInventory().query(QueryOperationTypes.ITEM_TYPE.of(ItemTypes.TNT), QueryOperationTypes.ITEM_TYPE.of(ItemTypes.TNT_MINECART)).transform(InventoryTransformations.REVERSE).poll((int) tntCount / 2);
                }

                if (fireChargeCount > 0 ) {
                    player.getInventory().query(QueryOperationTypes.ITEM_TYPE.of(ItemTypes.FIRE_CHARGE)).transform(InventoryTransformations.REVERSE).poll((int) fireChargeCount / 2);
                }

                if (otherCount > 0) {
                    player.getInventory().query(QueryOperationTypes.ITEM_TYPE.of(ItemTypes.FIREWORK_CHARGE), QueryOperationTypes.ITEM_TYPE.of(ItemTypes.FIREWORKS), QueryOperationTypes.ITEM_TYPE.of(ItemTypes.GUNPOWDER)).transform(InventoryTransformations.REVERSE).poll((int) otherCount / 2);
                }

                player.getWorld().triggerExplosion(explosion);
            }
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
            playerDisplacement = playerDisplacement.mul(craft.getType().getCruiseSkipBlocks());

            craft.translate(playerDisplacement.toInt());
            return;
        }

        if(MathUtils.locationNearHitBox(craft.getHitBox(), player.getPosition(), 2)){
            timeToReleaseAfter.remove(craft);
            return;
        }

        if(timeToReleaseAfter.containsKey(craft) && timeToReleaseAfter.get(craft) < System.currentTimeMillis()){
            craft.removeCrewMember(player.getUniqueId());
            timeToReleaseAfter.remove(craft);
            return;
        }

        if (!craft.isProcessing() && craft.getType().getMoveEntities() && !timeToReleaseAfter.containsKey(craft)) {
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