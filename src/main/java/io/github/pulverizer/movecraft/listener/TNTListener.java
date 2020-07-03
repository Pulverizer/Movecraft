package io.github.pulverizer.movecraft.listener;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import io.github.pulverizer.movecraft.Movecraft;
import io.github.pulverizer.movecraft.config.Settings;
import io.github.pulverizer.movecraft.craft.Craft;
import io.github.pulverizer.movecraft.craft.CraftManager;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
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
import org.spongepowered.api.world.explosion.ResistanceCalculator;

import java.util.*;

import static org.spongepowered.api.event.Order.LAST;

public class TNTListener {

    private final HashMap<PrimedTNT, Vector3d> TNTTracking = new HashMap<>();
    private final HashMap<PrimedTNT, Integer> TNTTracers = new HashMap<>();
    private final HashSet<PrimedTNT> tntControlList = new HashSet<>();
    private int tntControlTimer = 0;

    private final HashMap<Explosion, HashSet<Explosion>> ammoDetonation = new HashMap<>();

    public TNTListener() {
        Task.builder()
                .intervalTicks(1)
                .execute(this::processContactExplosives)
                .submit(Movecraft.getInstance());
    }

    private void processContactExplosives() {

        Sponge.getServer().getWorlds().forEach(world ->
                world.getEntities(entity -> entity instanceof PrimedTNT).forEach(entity -> {

                    PrimedTNT primedTNT = (PrimedTNT) entity;
                    //Contact Explosives

                    Vector3d velocity = primedTNT.getVelocity();

                    if (!TNTTracking.containsKey(primedTNT) && velocity.lengthSquared() > 0.35) {
                        TNTTracking.put(primedTNT, velocity);

                    } else if (TNTTracking.containsKey(primedTNT)) {
                        if (velocity.lengthSquared() < TNTTracking.get(primedTNT).lengthSquared() / 10) {
                            primedTNT.detonate();
                            TNTTracking.remove(primedTNT);
                            TNTTracers.remove(primedTNT);

                        } else {
                            if (isShrapnel(primedTNT, velocity)) {
                                primedTNT.offer(Keys.TICKS_REMAINING, 1);
                            }

                            TNTTracking.put(primedTNT, velocity);
                        }
                    }
                }));
    }

    private boolean isShrapnel(PrimedTNT primedTNT, Vector3d velocity) {

        boolean x = velocity.getX() * velocity.getX() < (TNTTracking.get(primedTNT).getX() * TNTTracking.get(primedTNT).getX()) / 10
                && (velocity.getY() * velocity.getY() > TNTTracking.get(primedTNT).getY() * TNTTracking.get(primedTNT).getY()
                || velocity.getZ() * velocity.getZ() > TNTTracking.get(primedTNT).getZ() * TNTTracking.get(primedTNT).getZ());

        boolean y = velocity.getY() * velocity.getY() < (TNTTracking.get(primedTNT).getY() * TNTTracking.get(primedTNT).getY()) / 10
                && (velocity.getX() * velocity.getX() > TNTTracking.get(primedTNT).getX() * TNTTracking.get(primedTNT).getX()
                || velocity.getZ() * velocity.getZ() > TNTTracking.get(primedTNT).getZ() * TNTTracking.get(primedTNT).getZ());

        boolean z = velocity.getZ() * velocity.getZ() < (TNTTracking.get(primedTNT).getZ() * TNTTracking.get(primedTNT).getZ()) / 10
                && (velocity.getY() * velocity.getY() > TNTTracking.get(primedTNT).getY() * TNTTracking.get(primedTNT).getY()
                || velocity.getX() * velocity.getX() > TNTTracking.get(primedTNT).getX() * TNTTracking.get(primedTNT).getX());

        return x || y || z;
    }

    @Listener
    public void tntTracking(MoveEntityEvent event, @Getter("getTargetEntity") PrimedTNT primedTNT) {

        double velocity = primedTNT.getVelocity().lengthSquared();

        //Cannon Directors
        //TODO - Check that craft type allows cannon directors
        if (velocity > 0.25 && !TNTTracers.containsKey(primedTNT)) {
            Craft c = CraftManager.getInstance().fastNearestCraftToLoc(primedTNT.getLocation());

            //TODO - make it use the spawning Dispenser location to check against craft hitbox

            if (c != null) {
                Player player = c.getCannonDirectorFor(primedTNT);

                if (player != null && player.getItemInHand(HandTypes.MAIN_HAND).get().getType() == Settings.PilotTool) {

                    Vector3d tntVelocity = primedTNT.getVelocity();
                    double speed = tntVelocity.length(); // store the speed to add it back in later, since all the values we will be using are "normalized", IE: have a speed of 1
                    tntVelocity = tntVelocity.normalize(); // you normalize it for comparison with the new direction to see if we are trying to steer too far
                    BlockSnapshot targetBlock = null;
                    Optional<BlockRayHit<World>> blockRayHit = BlockRay
                            .from(player)
                            .distanceLimit((player.getViewDistance() + 1) * 16)
                            .skipFilter(hit -> CraftManager.getInstance().getTransparentBlocks().contains(hit.getLocation().getBlockType()))
                            .stopFilter(BlockRay.allFilter())
                            .build()
                            .end();

                    if (blockRayHit.isPresent())
                        // Target is Block :)
                        targetBlock = blockRayHit.get().getLocation().createSnapshot();

                    Vector3d targetVector;
                    if (targetBlock == null) { // the player is looking at nothing, shoot in that general direction
                        targetVector = player.getHeadRotation();
                    } else { // shoot directly at the block the player is looking at (IE: with convergence)
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
                    tntVelocity = new Vector3d(tntVelocity.getX(), primedTNT.getVelocity().getY(), tntVelocity.getZ()); // you leave the original Y (or vertical axis) trajectory as it was
                    primedTNT.setVelocity(tntVelocity);
                }
            }
        }

        //TNT Tracers
        if (Settings.TracerRateTicks != 0) {
            if (!TNTTracers.containsKey(primedTNT) && velocity > 0.25) {
                TNTTracers.put(primedTNT, Sponge.getServer().getRunningTimeTicks() -  (int) Settings.TracerRateTicks);
            }

            if (TNTTracers.containsKey(primedTNT) && TNTTracers.get(primedTNT) < Sponge.getServer().getRunningTimeTicks() -  (int) Settings.TracerRateTicks) {

                for (Player player : primedTNT.getWorld().getPlayers()) {
                    // is the TNT within the render distance of the player?
                    long maxDistSquared = player.getViewDistance() * 16;
                    maxDistSquared = maxDistSquared - 16;
                    maxDistSquared = maxDistSquared * maxDistSquared;

                    if (player.getLocation().getBlockPosition().distanceSquared(primedTNT.getLocation().getBlockPosition()) < maxDistSquared) {
                        // we
                        // use
                        // squared
                        // because
                        // its
                        // faster
                        final Vector3i loc = primedTNT.getLocation().getBlockPosition();
                        final Player fp = player;
                        // then make a cobweb to look like smoke,
                        // place it a little later so it isn't right
                        // in the middle of the volley
                        Task.builder()
                                .delayTicks(5)
                                .execute(() -> fp.sendBlockChange(loc, BlockTypes.WEB.getDefaultState()))
                                .submit(Movecraft.getInstance());

                        // then remove it
                        Task.builder()
                                .delayTicks(65)
                                .execute(() -> fp.resetBlockChange(loc))
                                .submit(Movecraft.getInstance());
                    }
                }
            }
        }

        //Clean up any exploded TNT from Tracking
        TNTTracking.keySet().removeIf(tnt -> tnt.getFuseData().ticksRemaining().get() <= 0);
        TNTTracers.keySet().removeIf(tnt -> tnt.getFuseData().ticksRemaining().get() <= 0);
    }

    @Listener
    public void tntBlastCondenser(ExplosionEvent.Pre event) {

        final ResistanceCalculator oldResistanceCalculator = event.getExplosion().getResistanceCalculator().get();
        final ResistanceCalculator newResistanceCalculator = ((blockState, blockPosition, explosion) -> {

            float resistance =
                    (Settings.DurabilityOverride != null && Settings.DurabilityOverride.containsKey(blockState.getType()))
                            ? Settings.DurabilityOverride.get(blockState.getType())
                            : oldResistanceCalculator.calculateResistance(blockState, blockPosition, explosion);

            //TODO: Add to config
            for (Craft craft : CraftManager.getInstance().getCraftsFromLocation(new Location<>(event.getTargetWorld(), blockPosition))) {
                float testNum = resistance * (1F + ((float) craft.getSize() / 50000F));

                if (testNum > resistance) {
                    resistance = testNum;
                }
            }

            return resistance;
        });

        Explosion newExplosion = Explosion.builder()
                .from(event.getExplosion())
                .resistanceCalculator(newResistanceCalculator)
                .build();

        event.setExplosion(newExplosion);

        //-------------------//

        if (Settings.Debug)
            Movecraft.getInstance().getLogger().info("Was BOOM: " + event.getExplosion().getRadius());

        if (!event.getExplosion().getSourceExplosive().isPresent() || !(event.getExplosion().getSourceExplosive().get() instanceof PrimedTNT))
            return;

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

        Collection<Entity> entities = TNTTracking.containsKey(eventTNT) ? event.getTargetWorld().getNearbyEntities(tntLoc.getPosition(), 3) : event.getTargetWorld().getNearbyEntities(tntLoc.getPosition(), 1.9);

        entities.removeIf(entity -> {

            if (!(entity instanceof PrimedTNT))
                return true;

            PrimedTNT tnt = (PrimedTNT) entity;

            if (tnt.getFuseData().ticksRemaining().get() > eventTNT.getFuseData().ticksRemaining().get() + 1)
                return true;

            if (tnt.getFuseData().ticksRemaining().get() < eventTNT.getFuseData().ticksRemaining().get() - 1)
                return true;

            return false;
        });

        if (Settings.Debug)
            Movecraft.getInstance().getLogger().info("Entity Count: " + entities.size());

        int tntFound = 0;
        Vector3d explosionPosition = Vector3d.ZERO;

        for (Entity entity : entities) {

            PrimedTNT tnt = (PrimedTNT) entity;

            tnt.remove();
            tntControlList.add(tnt);
            tntFound++;

            explosionPosition = explosionPosition.add(tnt.getLocation().getPosition());
        }

        Location<World> explosionLocation  = new Location<>(event.getTargetWorld(), explosionPosition.div(tntFound));



        //30 breaks the water block it's in and has a large AoE, going to max out at 16.
        int num16explosions = tntFound / 16;
        tntFound = tntFound - (num16explosions * 16);

        Explosion explosion;
        for (int i = 0; i < num16explosions; i++) {

            if (Settings.Debug)
                Movecraft.getInstance().getLogger().info("Should BOOM: 16");

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

        if (Settings.Debug)
            Movecraft.getInstance().getLogger().info("Did BOOM: " + tntFound);

        explosion = Explosion.builder()
                .from(event.getExplosion())
                .location(explosionLocation)
                .radius(tntFound)
                .knockback(tntFound)
                .randomness(0)
                .resolution(tntFound * 2)
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

                    if (craft == null || !craft.getHitBox().contains(affectedLocation.getBlockX(), affectedLocation.getBlockY(), affectedLocation.getBlockZ()))
                        continue;

                    HashSet<Location<World>> blockList = new HashSet<>();

                    Location<World> relativeBlockPos = affectedLocation.getBlockRelative(Direction.NORTH);

                    if (craft.getHitBox().contains(relativeBlockPos.getBlockPosition()))
                        blockList.add(relativeBlockPos);

                    relativeBlockPos = affectedLocation.getBlockRelative(Direction.WEST);

                    if (craft.getHitBox().contains(relativeBlockPos.getBlockPosition()))
                        blockList.add(relativeBlockPos);

                    relativeBlockPos = affectedLocation.getBlockRelative(Direction.EAST);

                    if (craft.getHitBox().contains(relativeBlockPos.getBlockPosition()))
                        blockList.add(relativeBlockPos);

                    relativeBlockPos = affectedLocation.getBlockRelative(Direction.SOUTH);

                    if (craft.getHitBox().contains(relativeBlockPos.getBlockPosition()))
                        blockList.add(relativeBlockPos);

                    relativeBlockPos = affectedLocation.getBlockRelative(Direction.UP);

                    if (craft.getHitBox().contains(relativeBlockPos.getBlockPosition()))
                        blockList.add(relativeBlockPos);

                    relativeBlockPos = affectedLocation.getBlockRelative(Direction.UP).getBlockRelative(Direction.NORTH);

                    if (craft.getHitBox().contains(relativeBlockPos.getBlockPosition()))
                        blockList.add(relativeBlockPos);

                    relativeBlockPos = affectedLocation.getBlockRelative(Direction.UP).getBlockRelative(Direction.WEST);

                    if (craft.getHitBox().contains(relativeBlockPos.getBlockPosition()))
                        blockList.add(relativeBlockPos);

                    relativeBlockPos = affectedLocation.getBlockRelative(Direction.UP).getBlockRelative(Direction.EAST);

                    if (craft.getHitBox().contains(relativeBlockPos.getBlockPosition()))
                        blockList.add(relativeBlockPos);

                    relativeBlockPos = affectedLocation.getBlockRelative(Direction.UP).getBlockRelative(Direction.SOUTH);

                    if (craft.getHitBox().contains(relativeBlockPos.getBlockPosition()))
                        blockList.add(relativeBlockPos);



                    //TODO: Can't seem to get Fluid Level???
                    // Test anti-spill
                    for (Location<World> testLoc : blockList) {

                        if (testLoc.getProperty(MatterProperty.class).get().getValue() == MatterProperty.Matter.LIQUID && testLoc.getBlock().get(Keys.FLUID_LEVEL).isPresent()) {// && testLoc.get(Keys.FLUID_LEVEL).get() == 1) {
                            Movecraft.getInstance().getLogger().info("Fluid Level: " + testLoc.getBlock().get(Keys.FLUID_LEVEL).get());
                            testLoc.restoreSnapshot(BlockTypes.AIR.getDefaultState().snapshotFor(testLoc), true, BlockChangeFlags.ALL);
                        }

                        if (testLoc.getProperty(MatterProperty.class).isPresent() && testLoc.getProperty(MatterProperty.class).get().getValue() == MatterProperty.Matter.LIQUID) {
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

                float tntCount = inventory.query(QueryOperationTypes.ITEM_TYPE.of(ItemTypes.TNT), QueryOperationTypes.ITEM_TYPE.of(ItemTypes.TNT_MINECART)).totalItems();
                float fireChargeCount = inventory.query(QueryOperationTypes.ITEM_TYPE.of(ItemTypes.FIRE_CHARGE)).totalItems();
                float otherCount = inventory.query(QueryOperationTypes.ITEM_TYPE.of(ItemTypes.FIREWORK_CHARGE), QueryOperationTypes.ITEM_TYPE.of(ItemTypes.FIREWORKS), QueryOperationTypes.ITEM_TYPE.of(ItemTypes.GUNPOWDER)).totalItems();

                float chance = ((tntCount / (Settings.AmmoDetonationMultiplier * 32)) + (fireChargeCount / (Settings.AmmoDetonationMultiplier * 128)) + (otherCount / (Settings.AmmoDetonationMultiplier * 256)));

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
                            .knockback(1)
                            .canCauseFire(fireChargeCount > 0)
                            .build();

                    explosions.add(explosion);
                }
            }

            ammoDetonation.put(event.getExplosion(), explosions);
        }

        if (!event.getExplosion().getSourceExplosive().isPresent() || !(event.getExplosion().getSourceExplosive().get() instanceof PrimedTNT) || Settings.TracerRateTicks == 0)
            return;

        Location<World> explosionLocation = event.getExplosion().getLocation();

        for (Player player : event.getTargetWorld().getPlayers()) {

            double renderDistance = (player.getViewDistance() + 1) * 16;

            // is the TNT within the view distance (rendered world) of the player, yet further than 60 blocks?
            if (player.getPosition().distance(explosionLocation.getPosition()) < renderDistance) {
                final Location<World> location = explosionLocation;
                final Player finalisedPlayer = player;

                // make a glowstone to look like the explosion, place it a little later so it isn't right in the middle of the volley
                Task.builder()
                        .delayTicks(5)
                        .execute(() -> finalisedPlayer.sendBlockChange(location.getBlockPosition(), BlockTypes.GLOWSTONE.getDefaultState()))
                        .submit(Movecraft.getInstance());

                // then remove it
                Task.builder()
                        .delayTicks(105)
                        .execute(() -> finalisedPlayer.resetBlockChange(location.getBlockPosition()))
                        .submit(Movecraft.getInstance());
            }
        }
    }

    @Listener(order = LAST)
    public void explosionPOST(ExplosionEvent.Post event, @Getter("getExplosion") Explosion explosion) {
        if (ammoDetonation.containsKey(explosion)) {
            ammoDetonation.get(explosion).forEach(ammoExplosion -> ammoExplosion.getLocation().getExtent().triggerExplosion(ammoExplosion));
            ammoDetonation.clear();
        }
    }
}
