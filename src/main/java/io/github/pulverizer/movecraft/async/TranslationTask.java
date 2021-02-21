package io.github.pulverizer.movecraft.async;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableSet;
import io.github.pulverizer.movecraft.Movecraft;
import io.github.pulverizer.movecraft.config.Settings;
import io.github.pulverizer.movecraft.config.craft_settings.Defaults;
import io.github.pulverizer.movecraft.craft.Craft;
import io.github.pulverizer.movecraft.craft.CraftManager;
import io.github.pulverizer.movecraft.event.CraftTranslateEvent;
import io.github.pulverizer.movecraft.map_updater.MapUpdateManager;
import io.github.pulverizer.movecraft.map_updater.update.BlockCreateCommand;
import io.github.pulverizer.movecraft.map_updater.update.CraftTranslateCommand;
import io.github.pulverizer.movecraft.map_updater.update.EntityUpdateCommand;
import io.github.pulverizer.movecraft.map_updater.update.ExplosionUpdateCommand;
import io.github.pulverizer.movecraft.map_updater.update.ParticleUpdateCommand;
import io.github.pulverizer.movecraft.map_updater.update.UpdateCommand;
import io.github.pulverizer.movecraft.utils.HashHitBox;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.AABB;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public class TranslationTask extends AsyncTask {

    //TODO: Move to config.
    private static final ImmutableSet<BlockType> FALL_THROUGH_BLOCKS = ImmutableSet
            .of(BlockTypes.AIR, BlockTypes.WATER, BlockTypes.LAVA, BlockTypes.FLOWING_WATER, BlockTypes.FLOWING_LAVA, BlockTypes.TALLGRASS,
                    BlockTypes.YELLOW_FLOWER, BlockTypes.RED_FLOWER, BlockTypes.BROWN_MUSHROOM, BlockTypes.RED_MUSHROOM, BlockTypes.TORCH,
                    BlockTypes.FIRE, BlockTypes.REDSTONE_WIRE, BlockTypes.WHEAT, BlockTypes.STANDING_SIGN, BlockTypes.LADDER, BlockTypes.WALL_SIGN,
                    BlockTypes.LEVER, BlockTypes.LIGHT_WEIGHTED_PRESSURE_PLATE, BlockTypes.HEAVY_WEIGHTED_PRESSURE_PLATE,
                    BlockTypes.STONE_PRESSURE_PLATE, BlockTypes.WOODEN_PRESSURE_PLATE, BlockTypes.UNLIT_REDSTONE_TORCH, BlockTypes.REDSTONE_TORCH,
                    BlockTypes.STONE_BUTTON, BlockTypes.SNOW_LAYER, BlockTypes.REEDS, BlockTypes.FENCE, BlockTypes.ACACIA_FENCE,
                    BlockTypes.BIRCH_FENCE, BlockTypes.DARK_OAK_FENCE, BlockTypes.JUNGLE_FENCE, BlockTypes.NETHER_BRICK_FENCE,
                    BlockTypes.SPRUCE_FENCE, BlockTypes.UNPOWERED_REPEATER, BlockTypes.POWERED_REPEATER, BlockTypes.WATERLILY, BlockTypes.CARROTS,
                    BlockTypes.POTATOES, BlockTypes.WOODEN_BUTTON, BlockTypes.CARPET);
    private static final Vector3i[] SHIFTS = {
            new Vector3i(1, 0, 0),
            new Vector3i(-1, 0, 0),
            new Vector3i(0, 0, 1),
            new Vector3i(0, 0, -1)
    };
    private final HashHitBox oldHitBox;
    private final Collection<UpdateCommand> updates = new HashSet<>();
    private final World world;
    private final List<BlockType> harvestBlocks = craft.getType().getSetting(Defaults.HarvestBlocks.class).get().getValue();
    private final HashSet<Vector3i> harvestedBlocks = new HashSet<>();
    private final List<BlockType> harvesterBladeBlocks = craft.getType().getSetting(Defaults.HarvesterBladeBlocks.class).get().getValue();
    private final HashHitBox collisionBox = new HashHitBox();
    private Vector3i displacement;
    private HashHitBox newHitBox;
    private boolean collisionExplosion = false;

    public TranslationTask(Craft craft, Vector3i displacement) {
        super(craft, "Translation");
        world = craft.getWorld();
        this.displacement = displacement;
        newHitBox = new HashHitBox();
        oldHitBox = new HashHitBox(craft.getHitBox());
    }

    @Override
    protected void execute() throws InterruptedException {
        // Check can move
        if (oldHitBox.isEmpty() || craft.isDisabled()) {
            return;
        }

        // Check Craft height
        if (!checkCraftHeight()) {
            return;
        }

        // Use some fuel if needed
        double fuelBurnRate = getCraft().getType().getSetting(Defaults.FuelBurnRate.class).get().getValue();
        if (fuelBurnRate > 0 && !getCraft().isSinking()) {
            if (!getCraft().useFuel(fuelBurnRate)) {
                fail("Craft out of fuel");
                return;
            }
        }

        // Check if Craft is obstructed
        if (craftObstructed()) {
            return;
        }

        // Call the Craft Translate Event
        CraftTranslateEvent event = new CraftTranslateEvent(craft, oldHitBox, newHitBox);
        Sponge.getEventManager().post(event);
        if (event.isCancelled()) {
            this.fail(event.getFailMessage());
            return;
        }

        // Process Task based on if the Craft is sinking or not
        if (craft.isSinking()) {
            processSinking();
        } else {
            processNotSinking();
        }

        // Clean up torpedoes after explosion
        if (!collisionBox.isEmpty() && craft.getType().getSetting(Defaults.CruiseOnPilot.class).get().getValue() && craft.getType().getSetting(Defaults.CollisionExplosion.class).get().getValue() > 0) {
            craft.release(null);

            for (Vector3i location : oldHitBox) {
                updates.add(new BlockCreateCommand(craft.getWorld(), location, BlockTypes.AIR));
            }

            newHitBox = new HashHitBox();

        } else {
            // Add Craft Translation Map Update to list of updates
            updates.add(new CraftTranslateCommand(craft, displacement, newHitBox));
        }

        // Move Entities
        moveEntities();

        // Harvest Blocks
        //TODO: Re-add!
        //captureYield(harvestedBlocks);
    }

    private boolean checkCraftHeight() {

        // Get current min and max Y
        final int minY = oldHitBox.getMinY();
        final int maxY = oldHitBox.getMaxY();

        // Check if the craft is too high
        if (displacement.getY() > -1) {
            if (craft.getMaxHeightLimit() < maxY || (displacement.getY() == 0 && craft.getType().getSetting(Defaults.UseGravity.class).get().getValue())) {
                displacement = new Vector3i(displacement.getX(), -1, displacement.getZ());

            } else if (craft.getType().getSetting(Defaults.MaxHeightAboveGround.class).get().getValue() > 0) {

                final Vector3i middle = oldHitBox.getMidPoint().add(displacement);
                int testY = minY;

                while (testY > 0) {
                    testY -= 1;

                    // TODO - Probably won't work in newer versions with kelp, etc
                    if (craft.getWorld().getBlockType(middle.getX(), testY, middle.getZ()) != BlockTypes.AIR && (
                            !craft.getType().getSetting(Defaults.CanHoverOverWater.class).get().getValue()
                                    || craft.getWorld().getBlockType(middle.getX(), testY, middle.getZ()) != BlockTypes.WATER)) {
                        break;
                    }
                }

                if (minY - testY > craft.getType().getSetting(Defaults.MaxHeightAboveGround.class).get().getValue()) {
                    displacement = new Vector3i(displacement.getX(), -1, displacement.getZ());
                }
            }
        }

        // Fail the movement if the craft is going to be too high
        if (displacement.getY() > 0 && maxY + displacement.getY() > craft.getMaxHeightLimit()) {
            fail("Translation Failed - Craft hit height limit.");
            return false;

            // Or too low
        } else if (displacement.getY() < 0 && minY + displacement.getY() < craft.getType().getSetting(Defaults.MinHeightLimit.class).get().getValue() && !craft.isSinking()) {
            fail("Translation Failed - Craft hit minimum height limit.");
            return false;
        }

        return true;
    }

    // TODO - Review code
    private boolean craftObstructed() {
        for (Vector3i oldLocation : oldHitBox) {
            final Vector3i newLocation = oldLocation.add(displacement.getX(), displacement.getY(), displacement.getZ());
            //If the new location already exists in the old hitbox than this is unnecessary because a craft can't hit itself.
            if (oldHitBox.contains(newLocation)) {
                newHitBox.add(newLocation);
                continue;
            }

            final BlockType testMaterial = craft.getWorld().getBlockType(newLocation);

            //prevent chests collision
            BlockType oldMaterial = world.getBlockType(oldLocation);
            if ((oldMaterial.equals(BlockTypes.CHEST) || oldMaterial.equals(BlockTypes.TRAPPED_CHEST)) && checkChests(oldMaterial, newLocation)) {
                fail(String.format("Translation Failed - Craft is obstructed" + " @ %d,%d,%d,%s", newLocation.getX(), newLocation.getY(),
                        newLocation.getZ(), craft.getWorld().getBlockType(newLocation).toString()));
                return true;
            }

            boolean blockObstructed;
            if (craft.isSinking()) {
                blockObstructed = !FALL_THROUGH_BLOCKS.contains(testMaterial);
            } else {
                blockObstructed = !testMaterial.equals(BlockTypes.AIR)
                        && !craft.getType().getSetting(Defaults.PassthroughBlocks.class).get().getValue().contains(testMaterial);
            }

            boolean ignoreBlock = false;
            // air never obstructs anything (changed 4/18/2017 to prevent drilling machines)
            if (blockObstructed && craft.getWorld().getBlockType(oldLocation).equals(BlockTypes.AIR)) {
                blockObstructed = false;
            }

            if (blockObstructed && harvestBlocks.contains(testMaterial)) {
                BlockType tmpType = craft.getWorld().getBlockType(oldLocation);
                if (harvesterBladeBlocks.contains(tmpType)) {
                    blockObstructed = false;
                    harvestedBlocks.add(newLocation);
                }

            }

            if (blockObstructed) {
                if (Math.abs(displacement.getX()) > 1 || Math.abs(displacement.getY()) > 1 || Math.abs(displacement.getZ()) > 1) {
                    int x = displacement.getX();
                    int y = displacement.getY();
                    int z = displacement.getZ();

                    if (x < 0) {
                        x++;
                    } else if (x > 0) {
                        x--;
                    }

                    if (y < 0) {
                        y++;
                    } else if (y > 0) {
                        y--;
                    }

                    if (z < 0) {
                        z++;
                    } else if (z > 0) {
                        z--;
                    }

                    displacement = new Vector3i(x, y, z);
                    newHitBox.clear();
                    harvestedBlocks.clear();
                    return craftObstructed();

                } else if (!craft.isSinking() && craft.getType().getSetting(Defaults.CollisionExplosion.class).get().getValue() == 0.0F) {
                    fail(String.format("Translation Failed - Craft is obstructed" + " @ %d,%d,%d,%s", newLocation.getX(), newLocation.getY(),
                            newLocation.getZ(), testMaterial.getName()));
                    return true;
                }

                collisionBox.add(newLocation);

            } else {
                newHitBox.add(newLocation);
            } //END OF: if (blockObstructed)
        }

        return false;
    }

    private boolean checkChests(BlockType mBlock, Vector3i newLoc) {
        for (Vector3i shift : SHIFTS) {
            Vector3i aroundNewLoc = newLoc.add(shift);
            BlockType testMaterial = craft.getWorld().getBlockType(aroundNewLoc.getX(), aroundNewLoc.getY(), aroundNewLoc.getZ());
            if (testMaterial.equals(mBlock) && !oldHitBox.contains(aroundNewLoc)) {
                return true;
            }
        }
        return false;
    }

    private void processSinking() {
        for (Vector3i location : collisionBox) {
            if (craft.getType().getSetting(Defaults.ExplodeOnCrash.class).get().getValue() > 0.0F
                    && !world.getBlockType(location).equals(BlockTypes.AIR)
                    && ThreadLocalRandom.current().nextDouble(1) < .05) {

                updates.add(new ExplosionUpdateCommand(world, location,
                        craft.getType().getSetting(Defaults.ExplodeOnCrash.class).get().getValue()));
                collisionExplosion = true;
            }

            HashSet<Vector3i> toRemove = new HashSet<>();
            Vector3i next = location;

            do {
                toRemove.add(next);
                next = next.add(new Vector3i(0, 1, 0));
            } while (newHitBox.contains(next));

            newHitBox.removeAll(toRemove);
        }

        if (craft.getType().getSetting(Defaults.SmokeOnSink.class).get().getValue() > 0) {
            for (Vector3i location : newHitBox) {
                if (ThreadLocalRandom.current().nextInt(craft.getType().getSetting(Defaults.SmokeOnSink.class).get().getValue()) < 1) {
                    updates.add(new ParticleUpdateCommand(new Location<>(world, location), ParticleTypes.LARGE_SMOKE,
                            craft.getType().getSetting(Defaults.SmokeOnSinkQuantity.class).get().getValue()));
                }
            }
        }
    }

    private void processNotSinking() {
        //TODO - Make arming time a craft config setting
        //  Use number of moves instead? - System time doesn't work so well if the server is lagging
        if (craft.getType().getSetting(Defaults.CollisionExplosion.class).get().getValue() > 0.0F
                && System.currentTimeMillis() - craft.commandeeredAt() >= 1000) {

            for (Vector3i location : collisionBox) {

                float explosionKey;
                float explosionForce = craft.getType().getSetting(Defaults.CollisionExplosion.class).get().getValue();

                if (craft.getType().getSetting(Defaults.FocusedExplosion.class).get().getValue()) {
                    explosionForce *= oldHitBox.size();
                }

                //TODO: Account for underwater explosions
            /*if (location.getY() < waterLine) { // underwater explosions require more force to do anything
                explosionForce += 25; //find the correct amount
            }*/

                explosionKey = explosionForce;

                if (!world.getBlockType(location).equals(BlockTypes.AIR)) {
                    updates.add(new ExplosionUpdateCommand(world, location, explosionKey));
                    collisionExplosion = true;
                }

                if (craft.getType().getSetting(Defaults.FocusedExplosion.class).get().getValue()) { // don't handle any further collisions if
                    // it is set to focusedexplosion
                    break;
                }
            }
        }
    }

    private void moveEntities() throws InterruptedException {
        if (craft.getType().getSetting(Defaults.MoveEntities.class).get().getValue() && !craft.isSinking()) {

            AtomicBoolean processedEntities = new AtomicBoolean(false);

            Task.builder()
                    .execute(() -> {
                        for (Entity entity : craft.getWorld().getIntersectingEntities(
                                new AABB(oldHitBox.getMinX() - 0.5, oldHitBox.getMinY() - 0.5, oldHitBox.getMinZ() - 0.5, oldHitBox.getMaxX() + 1.5,
                                        oldHitBox.getMaxY() + 1.5, oldHitBox.getMaxZ() + 1.5))) {

                            if (entity.getType() == EntityTypes.PLAYER || entity.getType() == EntityTypes.PRIMED_TNT
                                    || entity.getType() == EntityTypes.ITEM || !craft.getType().getSetting(Defaults.OnlyMovePlayers.class).get()
                                    .getValue()) {
                                EntityUpdateCommand eUp = new EntityUpdateCommand(entity,
                                        entity.getLocation().getPosition().add(displacement.getX(), displacement.getY(), displacement.getZ()), 0);
                                updates.add(eUp);

                                if (Settings.Debug) {
                                    StringBuilder debug = new StringBuilder()
                                            .append("Submitting Entity Update: ")
                                            .append(entity.getType().getName());

                                    if (entity instanceof Item) {
                                        debug.append(" - Item Type: ")
                                                .append(((Item) entity).getItemType().getName());
                                    }

                                    Movecraft.getInstance().getLogger().info(debug.toString());
                                }
                            }
                        }

                        processedEntities.set(true);
                    })
                    .submit(Movecraft.getInstance());


            synchronized (this) {
                while (!processedEntities.get()) {
                    this.wait(1);
                }
            }

        } else {
            // Handle auto Craft release
            //add releaseTask without playermove to manager
            if (!craft.getType().getSetting(Defaults.CruiseOnPilot.class).get().getValue() && !craft
                    .isSinking())  // not necessary to release cruiseonpilot crafts, because they will already be released
            {
                CraftManager.getInstance().addReleaseTask(craft);
            }
        }
    }

    @Override
    public void postProcess() {
        // Check if task failed
        if (failed()) {

            // Check for collision explosion
            if (collisionExplosion) {
                MapUpdateManager.getInstance().scheduleUpdates(updates);
                CraftManager.getInstance().addReleaseTask(craft);
            }

            // Set craft processing to false
            craft.setProcessing(false);

            // Schedule map updates
        } else {
            MapUpdateManager.getInstance().scheduleUpdates(updates);
        }
    }

    @Override
    protected Optional<Player> getNotificationPlayer() {
        return craft.getNotificationPlayer();
    }

    //TODO: Reactivate and review code once possible to get a block's potential drops.
    /*
    private void captureYield(List<Vector3i> harvestedBlocks) {
        if (harvestedBlocks.isEmpty()) {
            return;
        }
        ArrayList<Inventory> chests = new ArrayList<>();
        //find chests
        for (Vector3i loc : oldHitBox) {
            BlockSnapshot block = craft.getWorld().createSnapshot(loc.getX(), loc.getY(), loc.getZ());
            block.getLocation().ifPresent(worldLocation -> {
                worldLocation.getTileEntity().ifPresent(tileEntity -> {
                    if (tileEntity.getType() == TileEntityTypes.CHEST) {
                        chests.add(((TileEntityCarrier) tileEntity).getInventory());
                    }
                });
            });
        }

        for (Vector3i harvestedBlock : harvestedBlocks) {
            BlockSnapshot block = craft.getWorld().createSnapshot(harvestedBlock.getX(), harvestedBlock.getY(), harvestedBlock.getZ());
            List<ItemStack> drops = new ArrayList<>(block.getDrops());
            //generate seed drops
            if (block.getState().getType() == BlockTypes.CROPS) {
                Random rand = new Random();
                int amount = rand.nextInt(4);
                if (amount > 0) {
                    ItemStack seeds = new ItemStack(ItemTypes.SEEDS, amount);
                    drops.add(seeds);
                }
            }
            //get contents of inventories before depositing
            block.getLocation().ifPresent(worldLocation -> {
                worldLocation.getTileEntity().ifPresent(tileEntity -> {

                    if (tileEntity instanceof TileEntityCarrier) {

                        drops.addAll(Arrays.asList(StreamSupport.stream(((TileEntityCarrier) tileEntity).getInventory().<Slot>slots().spliterator()
                        , false).map(Slot::peek).toArray(ItemStack[]::new)));

                    }
                });
            });

            for (ItemStack drop : drops) {
                ItemStack retStack = putInToChests(drop, chests);
                if (retStack != null)
                    //drop items on position
                    updates.add(new ItemDropUpdateCommand(new Location<>(craft.getWorld(), harvestedBlock.getX(), harvestedBlock.getY(),
                    harvestedBlock.getZ()), retStack));
            }
        }
    }


    private ItemStack putInToChests(ItemStack stack, ArrayList<Inventory> inventories) {
        if (stack == null)
            return null;
        if (inventories == null || inventories.isEmpty())
            return stack;
        for (Inventory inv : inventories) {

            inv.offer(stack);

            if (stack.getQuantity() == 0) {
                return null;
            }

        }
        return stack;
    }*/

    public HashHitBox getNewHitBox() {
        return newHitBox;
    }

    public Collection<UpdateCommand> getUpdates() {
        return updates;
    }

    public Vector3i getDisplacement() {
        return displacement;
    }

    public boolean isCollisionExplosion() {
        return collisionExplosion;
    }
}
