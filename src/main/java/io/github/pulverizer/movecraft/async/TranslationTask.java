package io.github.pulverizer.movecraft.async;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableSet;
import io.github.pulverizer.movecraft.Movecraft;
import io.github.pulverizer.movecraft.config.Settings;
import io.github.pulverizer.movecraft.craft.Craft;
import io.github.pulverizer.movecraft.craft.CraftManager;
import io.github.pulverizer.movecraft.event.CraftTranslateEvent;
import io.github.pulverizer.movecraft.map_updater.MapUpdateManager;
import io.github.pulverizer.movecraft.map_updater.update.*;
import io.github.pulverizer.movecraft.utils.HashHitBox;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.AABB;
import org.spongepowered.api.world.World;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public class TranslationTask extends AsyncTask {
    //TODO: Move to config.
    private static final ImmutableSet<BlockType> FALL_THROUGH_BLOCKS = ImmutableSet.of(BlockTypes.AIR, BlockTypes.FLOWING_WATER, BlockTypes.FLOWING_LAVA, BlockTypes.TALLGRASS, BlockTypes.YELLOW_FLOWER, BlockTypes.RED_FLOWER, BlockTypes.BROWN_MUSHROOM, BlockTypes.RED_MUSHROOM, BlockTypes.TORCH, BlockTypes.FIRE, BlockTypes.REDSTONE_WIRE, BlockTypes.WHEAT, BlockTypes.STANDING_SIGN, BlockTypes.LADDER, BlockTypes.WALL_SIGN, BlockTypes.LEVER, BlockTypes.LIGHT_WEIGHTED_PRESSURE_PLATE, BlockTypes.HEAVY_WEIGHTED_PRESSURE_PLATE, BlockTypes.STONE_PRESSURE_PLATE, BlockTypes.WOODEN_PRESSURE_PLATE, BlockTypes.UNLIT_REDSTONE_TORCH, BlockTypes.REDSTONE_TORCH, BlockTypes.STONE_BUTTON, BlockTypes.SNOW_LAYER, BlockTypes.REEDS, BlockTypes.FENCE, BlockTypes.ACACIA_FENCE, BlockTypes.BIRCH_FENCE, BlockTypes.DARK_OAK_FENCE, BlockTypes.JUNGLE_FENCE, BlockTypes.NETHER_BRICK_FENCE, BlockTypes.SPRUCE_FENCE, BlockTypes.UNPOWERED_REPEATER, BlockTypes.POWERED_REPEATER, BlockTypes.WATERLILY, BlockTypes.CARROTS, BlockTypes.POTATOES, BlockTypes.WOODEN_BUTTON, BlockTypes.CARPET);

    private Vector3i displacement;
    private HashHitBox newHitBox;
    private final HashHitBox oldHitBox;
    private boolean collisionExplosion = false;
    private final Collection<UpdateCommand> updates = new HashSet<>();
    private World world;

    private final List<BlockType> harvestBlocks = craft.getType().getHarvestBlocks();
    private final HashSet<Vector3i> harvestedBlocks = new HashSet<>();
    private final List<BlockType> harvesterBladeBlocks = craft.getType().getHarvesterBladeBlocks();
    private final HashHitBox collisionBox = new HashHitBox();

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
        if (oldHitBox.isEmpty() || craft.isDisabled())
            return;

        // Check Craft height
        if (!checkCraftHeight()) {
            return;
        }

        // Use some fuel if needed
        double fuelBurnRate = getCraft().getType().getFuelBurnRate();
        if (fuelBurnRate > 0 && !getCraft().isSinking()) {
            if (!getCraft().useFuel(fuelBurnRate)) {
                fail("Translation Failed - Craft out of fuel");
                return;
            }
        }

        // Check if Craft is obstructed
        if (craftObstructed())
            return;

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
        // TODO - Move to correct location
        //  What is the correct location?
        if (!collisionBox.isEmpty() && craft.getType().getCruiseOnPilot()) {
            craft.release(null);
            for (Vector3i location : oldHitBox) {
                updates.add(new BlockCreateCommand(craft.getWorld(), location, BlockTypes.AIR));
            }
            newHitBox = new HashHitBox();
        }

        // Add Craft Translation Map Update to list of updates
        updates.add(new CraftTranslateCommand(craft, new Vector3i(displacement.getX(), displacement.getY(), displacement.getZ()), newHitBox));

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
            if (craft.getMaxHeightLimit() < maxY) {
                displacement = new Vector3i(displacement.getX(), -1, displacement.getZ());

            } else if (craft.getType().getMaxHeightAboveGround() > 0) {

                final Vector3i middle = oldHitBox.getMidPoint().add(displacement);
                int testY = minY;

                while (testY > 0) {
                    testY -= 1;

                    if (craft.getWorld().getBlockType(middle.getX(), testY, middle.getZ()) != BlockTypes.AIR)
                        break;
                }

                if (minY - testY > craft.getType().getMaxHeightAboveGround()) {
                    displacement = new Vector3i(displacement.getX(), -1, displacement.getZ());
                }
            }
        }

        // Fail the movement if the craft is going to be too high
        if (displacement.getY() > 0 && maxY + displacement.getY() > craft.getMaxHeightLimit()) {
            fail("Translation Failed - Craft hit height limit.");
            return false;

            // Or too low
        } else if (displacement.getY() < 0 && minY + displacement.getY() < craft.getType().getMinHeightLimit() && !craft.isSinking()) {
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

            if ((testMaterial.equals(BlockTypes.CHEST) || testMaterial.equals(BlockTypes.TRAPPED_CHEST)) && checkChests(testMaterial, newLocation)) {
                //prevent chests collision
                fail(String.format("Translation Failed - Craft is obstructed" + " @ %d,%d,%d,%s", newLocation.getX(), newLocation.getY(), newLocation.getZ(), craft.getWorld().getBlockType(newLocation).toString()));
                return true;
            }

            boolean blockObstructed;
            if (craft.isSinking()) {
                blockObstructed = !FALL_THROUGH_BLOCKS.contains(testMaterial);
            } else {
                blockObstructed = !testMaterial.equals(BlockTypes.AIR) && !craft.getType().getPassthroughBlocks().contains(testMaterial);
            }

            boolean ignoreBlock = false;
            // air never obstructs anything (changed 4/18/2017 to prevent drilling machines)
            if (craft.getWorld().getBlockType(oldLocation).equals(BlockTypes.AIR) && blockObstructed) {
                ignoreBlock = true;
            }

            if (blockObstructed && harvestBlocks.contains(testMaterial)) {
                BlockType tmpType = craft.getWorld().getBlockType(oldLocation);
                if (harvesterBladeBlocks.contains(tmpType)) {
                    blockObstructed = false;
                    harvestedBlocks.add(newLocation);
                }
            }

            if (blockObstructed) {
                if (!craft.isSinking() && craft.getType().getCollisionExplosion() == 0.0F) {
                    fail(String.format("Translation Failed - Craft is obstructed" + " @ %d,%d,%d,%s", newLocation.getX(), newLocation.getY(), newLocation.getZ(), testMaterial.getName()));
                    return true;
                }
                collisionBox.add(newLocation);
            } else {
                if (!ignoreBlock) {
                    newHitBox.add(newLocation);
                }
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

    private static final Vector3i[] SHIFTS = {
            new Vector3i(1, 0, 0),
            new Vector3i(-1, 0, 0),
            new Vector3i(0, 0, 1),
            new Vector3i(0, 0, -1)};

    private void processSinking() {
        for (Vector3i location : collisionBox) {
            if (craft.getType().getExplodeOnCrash() > 0.0F
                    && !world.getBlockType(location).equals(BlockTypes.AIR)
                    && ThreadLocalRandom.current().nextDouble(1) < .05) {

                updates.add(new ExplosionUpdateCommand(world, location, craft.getType().getExplodeOnCrash()));
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
    }

    private void processNotSinking() {
        //TODO - Make arming time a craft config setting
        //  Use number of moves instead? - System time doesn't work so well if the server is lagging
        if (craft.getType().getCollisionExplosion() > 0.0F && System.currentTimeMillis() - craft.commandeeredAt() >= 1000) {

            for (Vector3i location : collisionBox) {

                float explosionKey;
                float explosionForce = craft.getType().getCollisionExplosion();

                if (craft.getType().getFocusedExplosion()) {
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

                if (craft.getType().getFocusedExplosion()) { // don't handle any further collisions if it is set to focusedexplosion
                    break;
                }
            }
        }
    }

    private void moveEntities() throws InterruptedException {
        if (craft.getType().getMoveEntities() && !craft.isSinking()) {

            AtomicBoolean processedEntities = new AtomicBoolean(false);

            Task.builder()
                    .execute(() -> {
                        for (Entity entity : craft.getWorld().getIntersectingEntities(new AABB(oldHitBox.getMinX() - 0.5, oldHitBox.getMinY() - 0.5, oldHitBox.getMinZ() - 0.5, oldHitBox.getMaxX() + 1.5, oldHitBox.getMaxY() + 1.5, oldHitBox.getMaxZ() + 1.5))) {

                            if (entity.getType() == EntityTypes.PLAYER || entity.getType() == EntityTypes.PRIMED_TNT || entity.getType() == EntityTypes.ITEM || !craft.getType().onlyMovePlayers()) {
                                EntityUpdateCommand eUp = new EntityUpdateCommand(entity, entity.getLocation().getPosition().add(displacement.getX(), displacement.getY(), displacement.getZ()), 0);
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
                while (!processedEntities.get()) this.wait(1);
            }

        } else {
            // Handle auto Craft release
            //add releaseTask without playermove to manager
            if (!craft.getType().getCruiseOnPilot() && !craft.isSinking())  // not necessary to release cruiseonpilot crafts, because they will already be released
                CraftManager.getInstance().addReleaseTask(craft);
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
        Optional<Player> player = Sponge.getServer().getPlayer(craft.getPilot());

        if (!player.isPresent()) {
            player = Sponge.getServer().getPlayer(craft.getCommander());
        }

        return player;
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

                        drops.addAll(Arrays.asList(StreamSupport.stream(((TileEntityCarrier) tileEntity).getInventory().<Slot>slots().spliterator(), false).map(Slot::peek).toArray(ItemStack[]::new)));

                    }
                });
            });

            for (ItemStack drop : drops) {
                ItemStack retStack = putInToChests(drop, chests);
                if (retStack != null)
                    //drop items on position
                    updates.add(new ItemDropUpdateCommand(new Location<>(craft.getWorld(), harvestedBlock.getX(), harvestedBlock.getY(), harvestedBlock.getZ()), retStack));
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
