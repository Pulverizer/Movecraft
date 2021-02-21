package io.github.pulverizer.movecraft.map_updater.update;

import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.explosion.Explosion;

import java.util.Objects;

public class ExplosionUpdateCommand extends UpdateCommand {

    private final Location<World> explosionLocation;
    private final float explosionStrength;

    public ExplosionUpdateCommand(World world, Vector3i location, float explosionStrength) throws IllegalArgumentException {
        if (explosionStrength < 0) {
            throw new IllegalArgumentException("Explosion strength cannot be negative");
        }
        this.explosionLocation = new Location<>(world, location);
        this.explosionStrength = explosionStrength;
    }

    public Location<World> getLocation() {
        return explosionLocation;
    }

    public float getStrength() {
        return explosionStrength;
    }

    @Override
    public void doUpdate() {
        explosionLocation.getExtent().triggerExplosion(this.createExplosion(explosionLocation.add(.5, .5, .5), explosionStrength));
    }

    private Explosion createExplosion(Location<World> loc, float explosionPower) {

        // TODO - make defaults
        return Explosion.builder()
                .location(loc)
                .shouldBreakBlocks(true)
                .shouldDamageEntities(true)
                .shouldPlaySmoke(true)
                .radius(explosionPower)
                .canCauseFire(false)
                .randomness(0.5f)
                .knockback(explosionPower)
                .resolution((int) (explosionPower * 2))
                .build();
    }

    @Override
    public int hashCode() {
        return Objects.hash(explosionLocation, explosionStrength);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ExplosionUpdateCommand)) {
            return false;
        }
        ExplosionUpdateCommand other = (ExplosionUpdateCommand) obj;
        return this.explosionLocation.equals(other.explosionLocation) &&
                this.explosionStrength == other.explosionStrength;
    }
}
