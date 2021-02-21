package io.github.pulverizer.movecraft.map_updater.update;

import io.github.pulverizer.movecraft.config.Settings;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleType;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Random;

public class ParticleUpdateCommand extends UpdateCommand {

    private final Location<World> location;
    private final ParticleType particleType;
    private final int quantity;
    private final Random rand = new Random();

    public ParticleUpdateCommand(Location<World> location, ParticleType particleType, int quantity) {
        this.location = location;
        this.particleType = particleType;
        this.quantity = quantity;
    }

    @Override
    public void doUpdate() {
        // put in smoke
        location.getExtent().spawnParticles(ParticleEffect.builder().type(particleType).quantity(quantity).build(), location.getPosition());

        //if (Settings.SilhouetteViewDistance > 0) {
        //    sendSilhouetteToPlayers();
        //}

    }

    private void sendSilhouetteToPlayers() {
        if (rand.nextInt(100) < 15) {

            ParticleEffect particle = ParticleEffect.builder().type(ParticleTypes.HAPPY_VILLAGER).build();

            for (Entity entity : location.getExtent().getEntities(
                    entity -> entity instanceof Player)) { // this is necessary because signs do not get updated client side correctly without
                // refreshing the chunks, which causes a memory leak in the clients

                Player p = (Player) entity;
                double distSquared = location.getPosition().distanceSquared(p.getLocation().getPosition());
                if ((distSquared < Settings.SilhouetteViewDistance * Settings.SilhouetteViewDistance) && (distSquared
                        > (location.getExtent().getViewDistance() - 1) * (location.getExtent().getViewDistance() - 1))) {
                    p.spawnParticles(particle, location.getPosition().toDouble(), 9);
                }
            }
        }
    }
}
