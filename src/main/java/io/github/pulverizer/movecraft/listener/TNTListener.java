package io.github.pulverizer.movecraft.listener;

import static org.spongepowered.api.event.Order.LAST;

import com.flowpowered.math.imaginary.Quaterniond;
import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import io.github.pulverizer.movecraft.Movecraft;
import io.github.pulverizer.movecraft.config.Settings;
import io.github.pulverizer.movecraft.craft.Craft;
import io.github.pulverizer.movecraft.craft.CraftManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.property.block.MatterProperty;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.explosive.PrimedTNT;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.BlockChangeFlags;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.explosion.Explosion;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

public class TNTListener {

    private final HashMap<PrimedTNT, Vector3d> trackedTNT = new HashMap<>();
    private final HashMap<PrimedTNT, Integer> tracerTNT = new HashMap<>();
    private final HashSet<PrimedTNT> shrapnelControlList = new HashSet<>();
    private final HashSet<PrimedTNT> tntControlList = new HashSet<>();
    private int tntControlTimer = 0;

    private final HashMap<Explosion, HashSet<Explosion>> ammoDetonation = new HashMap<>();

    public TNTListener() {
        Task.builder()
                .intervalTicks(1)
                .execute(this::tntTasks)
                .submit(Movecraft.getInstance());
    }

    private void tntTasks() {
        processContactExplosives();
        processTracers();
        cleanData();
    }

    private void processContactExplosives() {

        Sponge.getServer().getWorlds().forEach(world ->
                world.getEntities(entity -> entity instanceof PrimedTNT).forEach(entity -> {

                    PrimedTNT primedTNT = (PrimedTNT) entity;
                    //Contact Explosives

                    Vector3d velocity = primedTNT.getVelocity();

                    if (!trackedTNT.containsKey(primedTNT) && velocity.lengthSquared() > 0.35) {
                        trackedTNT.put(primedTNT, velocity);

                    } else if (trackedTNT.containsKey(primedTNT)) {
                        if (velocity.lengthSquared() < trackedTNT.get(primedTNT).lengthSquared() / 10) {
                            primedTNT.detonate();
                            trackedTNT.remove(primedTNT);
                            tracerTNT.remove(primedTNT);

                        } else {
                            if (isShrapnel(primedTNT, velocity)) {
                                primedTNT.offer(Keys.TICKS_REMAINING, 1);
                                shrapnelControlList.add(primedTNT);
                            }

                            trackedTNT.put(primedTNT, velocity);
                        }
                    }
                }));
    }

    private boolean isShrapnel(PrimedTNT primedTNT, Vector3d velocity) {

        boolean x = velocity.getX() * velocity.getX() < (trackedTNT.get(primedTNT).getX() * trackedTNT.get(primedTNT).getX()) / 10
                && (velocity.getY() * velocity.getY() > trackedTNT.get(primedTNT).getY() * trackedTNT.get(primedTNT).getY()
                || velocity.getZ() * velocity.getZ() > trackedTNT.get(primedTNT).getZ() * trackedTNT.get(primedTNT).getZ());

        boolean y = velocity.getY() * velocity.getY() < (trackedTNT.get(primedTNT).getY() * trackedTNT.get(primedTNT).getY()) / 10
                && (velocity.getX() * velocity.getX() > trackedTNT.get(primedTNT).getX() * trackedTNT.get(primedTNT).getX()
                || velocity.getZ() * velocity.getZ() > trackedTNT.get(primedTNT).getZ() * trackedTNT.get(primedTNT).getZ());

        boolean z = velocity.getZ() * velocity.getZ() < (trackedTNT.get(primedTNT).getZ() * trackedTNT.get(primedTNT).getZ()) / 10
                && (velocity.getY() * velocity.getY() > trackedTNT.get(primedTNT).getY() * trackedTNT.get(primedTNT).getY()
                || velocity.getX() * velocity.getX() > trackedTNT.get(primedTNT).getX() * trackedTNT.get(primedTNT).getX());

        return x || y || z;
    }

    private void processTracers() {
        HashSet<Vector3i> tracers = new HashSet<>();

        for (Map.Entry<PrimedTNT, Integer> entry : tracerTNT.entrySet()) {
            PrimedTNT primedTNT = entry.getKey();

            if (entry.getValue() < Sponge.getServer().getRunningTimeTicks() - Settings.TracerRateTicks) {
                final World world = primedTNT.getWorld();
                final Vector3i loc = primedTNT.getLocation().getBlockPosition();

                boolean invalid = false;
                for (Vector3i pos : tracers) {
                    if (pos.distance(loc) <= 3) {
                        invalid = true;
                        break;
                    }
                }

                if (invalid) {
                    continue;
                }

                tracers.add(loc);

                // place a cobweb to look like smoke,
                // place it a little later so it isn't right
                // in the middle of the volley
                Task.builder()
                        .delayTicks(5)
                        .execute(() -> world.sendBlockChange(loc, BlockTypes.WEB.getDefaultState()))
                        .submit(Movecraft.getInstance());

                // then remove it
                Task.builder()
                        .delayTicks(65)
                        .execute(() -> world.resetBlockChange(loc))
                        .submit(Movecraft.getInstance());
            }
        }
    }

    private void cleanData() {
        //Clean up any exploded TNT from Tracking
        trackedTNT.keySet().removeIf(Entity::isRemoved);
        tracerTNT.keySet().removeIf(Entity::isRemoved);
    }

    @Listener
    public void tntTracking(MoveEntityEvent event, @Getter("getTargetEntity") PrimedTNT primedTNT) {

        double velocity = primedTNT.getVelocity().lengthSquared();

        //Cannon Directors
        if (velocity > 0.25 && !tracerTNT.containsKey(primedTNT)) {
            Craft craft = CraftManager.getInstance().fastNearestCraftToLoc(primedTNT.getLocation());

            //TODO - make it use the spawning Dispenser location to check against craft hitbox

            if (craft != null && craft.getType().allowCannonDirectors()) {
                Player player = craft.getCannonDirectorFor(primedTNT);

                if (player != null && player.getItemInHand(HandTypes.MAIN_HAND).get().getType() == Settings.PilotTool) {

                    Vector3d tntVelocity = primedTNT.getVelocity();
                    double speed = tntVelocity.length();
                    // store the speed to add it back in later, since all the values we will be using are "normalized", IE: have
                    // a speed of 1
                    tntVelocity = tntVelocity.normalize();
                    // you normalize it for comparison with the new direction to see if we are trying to steer too far
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
                        targetVector = targetBlock.getLocation().get().getPosition().sub(primedTNT.getLocation().getPosition());
                        targetVector = targetVector.normalize();
                    }

                    //leave the original Y (or vertical axis) trajectory as it was
                    if (targetVector.getX() - tntVelocity.getX() > 0.7) {
                        tntVelocity = tntVelocity.add(0.7, 0, 0);
                    } else if (targetVector.getX() - tntVelocity.getX() < -0.7) {
                        tntVelocity = tntVelocity.sub(0.7, 0, 0);
                    } else {
                        tntVelocity = new Vector3d(targetVector.getX(), tntVelocity.getY(), tntVelocity.getZ());
                    }
                    if (targetVector.getZ() - tntVelocity.getZ() > 0.7) {
                        tntVelocity = tntVelocity.add(0, 0, 0.7);
                    } else if (targetVector.getZ() - tntVelocity.getZ() < -0.7) {
                        tntVelocity = tntVelocity.sub(0, 0, 0.7);
                    } else {
                        tntVelocity = new Vector3d(tntVelocity.getX(), tntVelocity.getY(), targetVector.getZ());
                    }
                    tntVelocity = tntVelocity.mul(speed); // put the original speed back in, but now along a different trajectory
                    tntVelocity = new Vector3d(tntVelocity.getX(), primedTNT.getVelocity().getY(),
                            tntVelocity.getZ()); // you leave the original Y (or vertical axis) trajectory as it was
                    primedTNT.setVelocity(tntVelocity);
                }
            }
        }

        //TNT Tracers
        if (Settings.TracerRateTicks != 0 && !tracerTNT.containsKey(primedTNT) && velocity > 0.25) {
            tracerTNT.put(primedTNT, Sponge.getServer().getRunningTimeTicks() - Settings.TracerRateTicks);
        }
    }

    @Listener
    public void tntBlastCondenser(ExplosionEvent.Pre event) {

        if (Settings.Debug) {
            Movecraft.getInstance().getLogger().info("Was BOOM: " + event.getExplosion().getRadius());
        }

        if (!event.getExplosion().getSourceExplosive().isPresent() || !(event.getExplosion().getSourceExplosive().get() instanceof PrimedTNT)) {
            return;
        }

        if (tntControlTimer < Sponge.getServer().getRunningTimeTicks()) {
            tntControlTimer = Sponge.getServer().getRunningTimeTicks();
            tntControlList.clear();
        }

        PrimedTNT eventTNT = (PrimedTNT) event.getExplosion().getSourceExplosive().get();
        Location<World> tntLoc = eventTNT.getLocation();

        if (tntControlList.contains(eventTNT)) {
            event.setCancelled(true);
            eventTNT.remove();
            return;
        }

        Collection<Entity> entities = trackedTNT.containsKey(eventTNT) ? event.getTargetWorld().getNearbyEntities(tntLoc.getPosition(), 3) :
                event.getTargetWorld().getNearbyEntities(tntLoc.getPosition(), 1.9);

        entities.removeIf(entity -> {

            if (!(entity instanceof PrimedTNT)) {
                return true;
            }

            PrimedTNT tnt = (PrimedTNT) entity;

            if (tnt.getFuseData().ticksRemaining().get() > eventTNT.getFuseData().ticksRemaining().get() + 1) {
                return true;
            }

            if (tnt.getFuseData().ticksRemaining().get() < eventTNT.getFuseData().ticksRemaining().get() - 1) {
                return true;
            }

            return false;
        });

        if (Settings.Debug) {
            Movecraft.getInstance().getLogger().info("Entity Count: " + entities.size());
        }

        int tntFound = 0;
        int shrapnelFound = 0;
        Vector3d explosionPosition = Vector3d.ZERO;

        for (Entity entity : entities) {

            PrimedTNT tnt = (PrimedTNT) entity;

            tnt.remove();
            tntControlList.add(tnt);

            if (shrapnelControlList.contains(tnt)) {
                shrapnelFound++;
            } else {
                tntFound++;
            }

            shrapnelControlList.remove(tnt);

            explosionPosition = explosionPosition.add(tnt.getLocation().getPosition());
        }

        Location<World> explosionLocation = new Location<>(event.getTargetWorld(), explosionPosition.div(tntFound));

        //30 breaks the water block it's in and has a large AoE, going to max out at 16.
        int power16Explosions = tntFound / 16;
        tntFound = tntFound - (power16Explosions * 16);

        Explosion explosion;
        for (int i = 0; i < power16Explosions; i++) {

            if (Settings.Debug) {
                Movecraft.getInstance().getLogger().info("Should BOOM: 16");
            }

            explosion = Explosion.builder()
                    .from(event.getExplosion())
                    .sourceExplosive(null)
                    .location(explosionLocation)
                    .radius(16)
                    .knockback(16)
                    .randomness(0)
                    .resolution(32)
                    .build();

            event.getTargetWorld().triggerExplosion(explosion);
        }

        float finalExplosion = Math.min((float) shrapnelFound / 4 + tntFound, 16);

        if (Settings.Debug) {
            Movecraft.getInstance().getLogger().info(String.format("Did Boom: %.2f (%d, %.2f)", finalExplosion, tntFound,
                    (float) shrapnelFound / 4));
        }

        explosion = Explosion.builder()
                .from(event.getExplosion())
                .location(explosionLocation)
                .radius(finalExplosion)
                .knockback(shrapnelFound + tntFound)
                .randomness(0)
                .resolution((int) finalExplosion)
                .build();

        event.setExplosion(explosion);
    }


    @Listener(order = LAST)
    public void explodeEvent(ExplosionEvent.Detonate event) {

        // Remove any blocks from the list that were adjacent to water, to prevent spillage
        if (!Settings.DisableSpillProtection) {

            HashSet<Location<World>> affectedLocations = new HashSet<>(event.getAffectedLocations());

            for (Location<World> affectedLocation : affectedLocations) {

                for (Craft craft : CraftManager.getInstance().getCraftsInWorld(affectedLocation.getExtent())) {

                    if (craft == null || !craft.getHitBox()
                            .contains(affectedLocation.getBlockX(), affectedLocation.getBlockY(), affectedLocation.getBlockZ())) {
                        continue;
                    }

                    HashSet<Location<World>> blockList = new HashSet<>();

                    Location<World> relativeBlockPos = affectedLocation.getBlockRelative(Direction.NORTH);

                    if (craft.getHitBox().contains(relativeBlockPos.getBlockPosition())) {
                        blockList.add(relativeBlockPos);
                    }

                    relativeBlockPos = affectedLocation.getBlockRelative(Direction.WEST);

                    if (craft.getHitBox().contains(relativeBlockPos.getBlockPosition())) {
                        blockList.add(relativeBlockPos);
                    }

                    relativeBlockPos = affectedLocation.getBlockRelative(Direction.EAST);

                    if (craft.getHitBox().contains(relativeBlockPos.getBlockPosition())) {
                        blockList.add(relativeBlockPos);
                    }

                    relativeBlockPos = affectedLocation.getBlockRelative(Direction.SOUTH);

                    if (craft.getHitBox().contains(relativeBlockPos.getBlockPosition())) {
                        blockList.add(relativeBlockPos);
                    }

                    relativeBlockPos = affectedLocation.getBlockRelative(Direction.UP);

                    if (craft.getHitBox().contains(relativeBlockPos.getBlockPosition())) {
                        blockList.add(relativeBlockPos);
                    }

                    relativeBlockPos = affectedLocation.getBlockRelative(Direction.UP).getBlockRelative(Direction.NORTH);

                    if (craft.getHitBox().contains(relativeBlockPos.getBlockPosition())) {
                        blockList.add(relativeBlockPos);
                    }

                    relativeBlockPos = affectedLocation.getBlockRelative(Direction.UP).getBlockRelative(Direction.WEST);

                    if (craft.getHitBox().contains(relativeBlockPos.getBlockPosition())) {
                        blockList.add(relativeBlockPos);
                    }

                    relativeBlockPos = affectedLocation.getBlockRelative(Direction.UP).getBlockRelative(Direction.EAST);

                    if (craft.getHitBox().contains(relativeBlockPos.getBlockPosition())) {
                        blockList.add(relativeBlockPos);
                    }

                    relativeBlockPos = affectedLocation.getBlockRelative(Direction.UP).getBlockRelative(Direction.SOUTH);

                    if (craft.getHitBox().contains(relativeBlockPos.getBlockPosition())) {
                        blockList.add(relativeBlockPos);
                    }


                    //TODO: Can't seem to get Fluid Level???
                    // Test anti-spill
                    for (Location<World> testLoc : blockList) {

                        if (testLoc.getProperty(MatterProperty.class).get().getValue() == MatterProperty.Matter.LIQUID && testLoc.getBlock()
                                .get(Keys.FLUID_LEVEL).isPresent()) {// && testLoc.get(Keys.FLUID_LEVEL).get() == 1) {
                            Movecraft.getInstance().getLogger().info("Fluid Level: " + testLoc.getBlock().get(Keys.FLUID_LEVEL).get());
                            testLoc.restoreSnapshot(BlockTypes.AIR.getDefaultState().snapshotFor(testLoc), true, BlockChangeFlags.ALL);
                        }

                        if (testLoc.getProperty(MatterProperty.class).isPresent()
                                && testLoc.getProperty(MatterProperty.class).get().getValue() == MatterProperty.Matter.LIQUID) {
                            event.getAffectedLocations().remove(affectedLocation);
                        }
                    }
                }
            }
        }


        if (Settings.AmmoDetonationMultiplier > 0) {

            HashSet<Explosion> explosions = new HashSet<>();

            for (Location<World> location : event.getAffectedLocations()) {

                Optional<TileEntity> tileEntity = location.getTileEntity();

                if (!tileEntity.isPresent() || !(tileEntity.get() instanceof TileEntityCarrier)) {
                    continue;
                }

                Inventory inventory = ((TileEntityCarrier) tileEntity.get()).getInventory();

                float tntCount =
                        inventory.query(QueryOperationTypes.ITEM_TYPE.of(ItemTypes.TNT), QueryOperationTypes.ITEM_TYPE.of(ItemTypes.TNT_MINECART))
                                .totalItems();
                float fireChargeCount = inventory.query(QueryOperationTypes.ITEM_TYPE.of(ItemTypes.FIRE_CHARGE)).totalItems();
                float otherCount = inventory
                        .query(QueryOperationTypes.ITEM_TYPE.of(ItemTypes.FIREWORK_CHARGE), QueryOperationTypes.ITEM_TYPE.of(ItemTypes.FIREWORKS),
                                QueryOperationTypes.ITEM_TYPE.of(ItemTypes.GUNPOWDER)).totalItems();

                float chance =
                        ((tntCount / (Settings.AmmoDetonationMultiplier * 32)) + (fireChargeCount / (Settings.AmmoDetonationMultiplier * 128)) + (
                                otherCount / (Settings.AmmoDetonationMultiplier * 256)));

                int diceRolled = new Random().nextInt(100);

                if (diceRolled <= chance) {
                    float size = Math.min(chance, 16);

                    Explosion explosion = Explosion.builder()
                            .location(location.add(0.5, 0.5, 0.5))
                            .shouldBreakBlocks(true)
                            .shouldDamageEntities(true)
                            .shouldPlaySmoke(true)
                            .radius(size)
                            .resolution((int) (size * 2))
                            .knockback(size)
                            .canCauseFire(fireChargeCount > 0)
                            .build();

                    explosions.add(explosion);
                }
            }

            ammoDetonation.put(event.getExplosion(), explosions);
        }

        if (!event.getExplosion().getSourceExplosive().isPresent() || !(event.getExplosion().getSourceExplosive().get() instanceof PrimedTNT)
                || Settings.TracerRateTicks == 0) {
            return;
        }

        Vector3i explosionPos = event.getExplosion().getLocation().getBlockPosition();

        // place a glowstone to look like the explosion, place it a little later so it isn't right in the middle of the volley
        Task.builder()
                .delayTicks(8)
                .execute(() -> event.getTargetWorld().sendBlockChange(explosionPos, BlockTypes.GLOWSTONE.getDefaultState()))
                .submit(Movecraft.getInstance());

        // then remove it
        Task.builder()
                .delayTicks(108)
                .execute(() -> event.getTargetWorld().resetBlockChange(explosionPos))
                .submit(Movecraft.getInstance());


    }

    @Listener(order = LAST)
    public void explosionPOST(ExplosionEvent.Post event, @Getter("getExplosion") Explosion explosion) {
        if (ammoDetonation.containsKey(explosion)) {
            ammoDetonation.get(explosion).forEach(ammoExplosion -> ammoExplosion.getLocation().getExtent().triggerExplosion(ammoExplosion));
            ammoDetonation.remove(explosion);
        }
    }
}
