package io.github.pulverizer.movecraft.listener;

import com.flowpowered.math.imaginary.Quaterniond;
import com.flowpowered.math.vector.Vector3d;
import io.github.pulverizer.movecraft.config.Settings;
import io.github.pulverizer.movecraft.config.craft_settings.Defaults;
import io.github.pulverizer.movecraft.craft.Craft;
import io.github.pulverizer.movecraft.craft.CraftManager;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.tileentity.carrier.Dispenser;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.projectile.explosive.fireball.SmallFireball;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.Optional;

public class FireballListener {

    private final HashMap<SmallFireball, Long> FireballTracking = new HashMap<>();

    @Listener
    public void fireballTracking(MoveEntityEvent event, @Getter("getTargetEntity") SmallFireball fireball) {

        if (!(fireball.getShooter() instanceof Dispenser)) {
            return;
        }

        if (!FireballTracking.containsKey(fireball)) {

            Craft craft = CraftManager.getInstance().getCraftFromLocation(((Dispenser) fireball.getShooter()).getLocation());

            if (craft != null && craft.getType().getSetting(Defaults.CanHaveAADirectors.class).get().getValue()) {

                Player player = craft.getAADirectorFor(fireball);

                if (player != null && player.getItemInHand(HandTypes.MAIN_HAND).isPresent()
                        && player.getItemInHand(HandTypes.MAIN_HAND).get().getType() == Settings.PilotTool) {

                    if (craft.getHitBox().contains(((Dispenser) fireball.getShooter()).getLocation().getBlockPosition())) {
                        Vector3d fireballVelocity = fireball.getVelocity();
                        double speed = fireballVelocity
                                .length(); // store the speed to add it back in later, since all the values we will be using are "normalized", IE:
                        // have a speed of 1
                        fireballVelocity = fireballVelocity
                                .normalize(); // you normalize it for comparison with the new direction to see if we are trying to steer too far

                        BlockSnapshot targetBlock = null;
                        Optional<BlockRayHit<World>> blockRayHit = BlockRay
                                .from(player)
                                .distanceLimit((player.getViewDistance() + 1) * 16)
                                .select(hit -> !CraftManager.getInstance().getTransparentBlocks().contains(hit.getLocation().getBlockType()))
                                .build()
                                .end();

                        if (blockRayHit.isPresent()) {
                            // Target is Block :)
                            targetBlock = blockRayHit.get().getLocation().createSnapshot();
                        }

                        Vector3d targetVector;
                        if (targetBlock == null) {

                            // the player is looking at nothing, shoot in that general direction
                            final Vector3d rotation = player.getRotation();
                            targetVector = Quaterniond.fromAxesAnglesDeg(rotation.getX(), -rotation.getY(), rotation.getZ()).getDirection();

                        } else {

                            // shoot directly at the block the player is looking at (IE: with convergence)
                            targetVector = targetBlock.getLocation().get().getPosition().sub(fireball.getLocation().getPosition());
                            targetVector = targetVector.normalize();

                        }

                        if (targetVector.getX() - fireballVelocity.getX() > 0.5) {
                            fireballVelocity = fireballVelocity.add(0.5, 0, 0);
                        } else if (targetVector.getX() - fireballVelocity.getX() < -0.5) {
                            fireballVelocity = fireballVelocity.sub(0.5, 0, 0);
                        } else {
                            fireballVelocity = new Vector3d(targetVector.getX(), fireballVelocity.getY(), fireballVelocity.getZ());
                        }

                        if (targetVector.getY() - fireballVelocity.getY() > 0.5) {
                            fireballVelocity = fireballVelocity.add(0, 0.5, 0);
                        } else if (targetVector.getY() - fireballVelocity.getY() < -0.5) {
                            fireballVelocity = fireballVelocity.sub(0, 0.5, 0);
                        } else {
                            fireballVelocity = new Vector3d(fireballVelocity.getX(), targetVector.getY(), fireballVelocity.getZ());
                        }

                        if (targetVector.getZ() - fireballVelocity.getZ() > 0.5) {
                            fireballVelocity = fireballVelocity.add(0, 0, 0.5);
                        } else if (targetVector.getZ() - fireballVelocity.getZ() < -0.5) {
                            fireballVelocity = fireballVelocity.sub(0, 0, 0.5);
                        } else {
                            fireballVelocity = new Vector3d(fireballVelocity.getX(), fireballVelocity.getY(), targetVector.getZ());
                        }

                        fireballVelocity = fireballVelocity.mul(speed); // put the original speed back in, but now along a different trajectory

                        fireball.setVelocity(fireballVelocity);
                        fireball.offer(Keys.ACCELERATION, fireballVelocity);
                    }

                    //add fireball to tracking
                    FireballTracking.put(fireball, System.currentTimeMillis());
                }
            }
        }

        int timeLimit = 20 * Settings.FireballLifespan * 50;
        // then, removed any expired fireballs from tracking
        FireballTracking.keySet().removeIf(testFireball -> {

            if (testFireball == null) {
                return true;
            }

            if (System.currentTimeMillis() - FireballTracking.get(testFireball) > timeLimit) {
                testFireball.remove();
                return true;
            }

            return false;

        });
    }
}
