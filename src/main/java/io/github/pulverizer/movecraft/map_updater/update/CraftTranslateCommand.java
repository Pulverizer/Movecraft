package io.github.pulverizer.movecraft.map_updater.update;

import com.flowpowered.math.vector.Vector3i;
import io.github.pulverizer.movecraft.Movecraft;
import io.github.pulverizer.movecraft.config.Settings;
import io.github.pulverizer.movecraft.config.craft_settings.Defaults;
import io.github.pulverizer.movecraft.craft.Craft;
import io.github.pulverizer.movecraft.event.SignTranslateEvent;
import io.github.pulverizer.movecraft.utils.CollectionUtils;
import io.github.pulverizer.movecraft.utils.HashHitBox;
import io.github.pulverizer.movecraft.utils.HitBox;
import io.github.pulverizer.movecraft.utils.MutableHitBox;
import io.github.pulverizer.movecraft.utils.SolidHitBox;
import io.github.pulverizer.movecraft.world.ChunkDataManager;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.world.BlockChangeFlags;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

public class CraftTranslateCommand extends UpdateCommand {

    private final Craft craft;
    private final Vector3i displacement;
    private final HashHitBox newHitBox;
    private final World world;


    public CraftTranslateCommand(Craft craft, Vector3i displacement, HashHitBox newHitBox) {
        this.craft = craft;
        this.displacement = displacement;
        this.newHitBox = newHitBox;
        this.world = craft.getWorld();
    }

    @Override
    public void doUpdate() {
        //A craftTranslateCommand should only occur if the craft is moving to a valid position

        long time = System.currentTimeMillis();

        final Logger logger = Movecraft.getInstance().getLogger();
        if (craft.getHitBox().isEmpty()) {
            logger.warn("Attempted to move craft with empty HashHitBox!");
            craft.release(null);
            return;
        }

        final Set<BlockType> passthroughBlocks = new HashSet<>(craft.getType().getSetting(Defaults.PassthroughBlocks.class).get().getValue());
        if (craft.isSinking()) {
            passthroughBlocks.add(BlockTypes.WATER);
            passthroughBlocks.add(BlockTypes.FLOWING_WATER);
            passthroughBlocks.add(BlockTypes.LEAVES);
            passthroughBlocks.add(BlockTypes.LEAVES2);
            passthroughBlocks.add(BlockTypes.GRASS);
            passthroughBlocks.add(BlockTypes.TALLGRASS);
            passthroughBlocks.add(BlockTypes.DOUBLE_PLANT);
        }

        if (!passthroughBlocks.isEmpty() && !newHitBox.isEmpty()) {

            MutableHitBox originalLocations = new HashHitBox(craft.getHitBox());

            final HitBox to = CollectionUtils.filter(newHitBox, originalLocations);
            for (Vector3i location : to) {
                BlockSnapshot material = craft.getWorld().createSnapshot(location);
                if (passthroughBlocks.contains(material.getState().getType())) {
                    craft.getPhasedBlocks().add(material);
                }
            }

            //place phased blocks
            //The subtraction of the set of coordinates in the HitBox cube and the HitBox itself
            final HitBox invertedHitBox = CollectionUtils.filter(newHitBox.boundingHitBox(), newHitBox);

            //A set of locations that are confirmed to be "exterior" locations
            final MutableHitBox exterior = new HashHitBox();
            final MutableHitBox interior = new HashHitBox();

            //place phased blocks
            final int minX = newHitBox.getMinX();
            final int maxX = newHitBox.getMaxX();
            final int minY = newHitBox.getMinY();
            final int maxY = newHitBox.getMaxY();
            final int minZ = newHitBox.getMinZ();
            final int maxZ = newHitBox.getMaxZ();
            final HitBox[] surfaces = {
                    new SolidHitBox(new Vector3i(minX, minY, minZ), new Vector3i(minX, maxY, maxZ)),
                    new SolidHitBox(new Vector3i(minX, minY, minZ), new Vector3i(maxX, minY, maxZ)),
                    new SolidHitBox(new Vector3i(minX, minY, minZ), new Vector3i(maxX, maxY, minZ)),
                    new SolidHitBox(new Vector3i(maxX, maxY, maxZ), new Vector3i(minX, maxY, maxZ)),
                    new SolidHitBox(new Vector3i(maxX, maxY, maxZ), new Vector3i(maxX, minY, maxZ)),
                    new SolidHitBox(new Vector3i(maxX, maxY, maxZ), new Vector3i(maxX, maxY, minZ))};

            //Valid exterior starts as the 6 surface planes of the HitBox with the locations that lie in the HitBox removed
            final Set<Vector3i> validExterior = new HashSet<>();
            for (HitBox hitBox : surfaces) {
                validExterior.addAll(CollectionUtils.filter(hitBox, newHitBox).asSet());
            }

            //Check to see which locations in the from set are actually outside of the craft
            for (Vector3i location : validExterior) {
                if (newHitBox.contains(location) || exterior.contains(location)) {
                    continue;
                }
                //use a modified BFS for multiple origin elements
                Set<Vector3i> visited = new HashSet<>();
                Queue<Vector3i> queue = new LinkedList<>();
                queue.add(location);
                while (!queue.isEmpty()) {
                    Vector3i node = queue.poll();
                    //If the node is already a valid member of the exterior of the HitBox, continued search is unitary.
                    for (Vector3i neighbor : CollectionUtils.neighbors(invertedHitBox, node)) {
                        if (visited.contains(neighbor)) {
                            continue;
                        }
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
                exterior.addAll(visited);
            }

            interior.addAll(CollectionUtils.filter(invertedHitBox, exterior));

            for (Vector3i location : CollectionUtils.filter(invertedHitBox, exterior)) {
                BlockSnapshot material = craft.getWorld().createSnapshot(location);
                if (passthroughBlocks.contains(material.getState().getType())) {
                    craft.getPhasedBlocks().add(material);
                }
            }

            //add the craft
            translateCraft();

            // update the craft hitbox
            craft.setHitBox(newHitBox);
            craft.setLastMoveVector(displacement);

            //trigger sign event
            for (Vector3i location : craft.getHitBox()) {
                BlockSnapshot block = craft.getWorld().createSnapshot(location);
                if (block.getState().getType() == BlockTypes.WALL_SIGN || block.getState().getType() == BlockTypes.STANDING_SIGN) {
                    Sponge.getEventManager().post(new SignTranslateEvent(block.getPosition(), craft));
                }
            }

            //place confirmed blocks if they have been un-phased
            final HashSet<BlockSnapshot> phasedBlocks = new HashSet<>(craft.getPhasedBlocks());
            for (BlockSnapshot block : phasedBlocks) {

                if (exterior.contains(block.getPosition())
                        || (originalLocations.contains(block.getPosition()) && !newHitBox.contains(block.getPosition()))) {

                    craft.getWorld().restoreSnapshot(block, true, BlockChangeFlags.NONE);
                    craft.getPhasedBlocks().remove(block);
                }
            }

            for (Vector3i location : interior) {
                final BlockSnapshot material = craft.getWorld().createSnapshot(location);
                if (passthroughBlocks.contains(material.getState().getType())) {
                    craft.getPhasedBlocks().add(material);
                    craft.getWorld()
                            .restoreSnapshot(location, BlockTypes.AIR.getDefaultState().snapshotFor(new Location<>(craft.getWorld(), location)), true,
                                    BlockChangeFlags.NONE);

                }
            }
        } else {

            //add the craft
            translateCraft();

            // update the craft hitbox
            craft.setHitBox(newHitBox);
            craft.setLastMoveVector(displacement);

            //trigger sign event
            for (Vector3i location : craft.getHitBox()) {
                if (world.getBlockType(location) == BlockTypes.WALL_SIGN || world.getBlockType(location) == BlockTypes.STANDING_SIGN) {
                    Sponge.getEventManager().post(new SignTranslateEvent(location, craft));
                }
            }


        }

        time = System.currentTimeMillis() - time;
        craft.addMoveTime(time);
        craft.updateLastMoveTick();
        craft.setProcessing(false);

        if (Settings.Debug) {
            logger.info("Total time: " + time + " ms. Moving with cooldown of " + craft.getTickCooldown() + ". Speed of: " + String
                    .format("%.2f", craft.getActualSpeed()));
        }
    }

    private void translateCraft() {
        // Set up the chunk data manager
        ChunkDataManager chunkDataManager = new ChunkDataManager(craft.getWorld(), new HashSet<>(craft.getHitBox().asSet()));

        // Get the tile entities
        chunkDataManager.fetchTilesAndTranslate(displacement);
        //get the blocks and translate the positions
        chunkDataManager.fetchBlocksAndTranslate(displacement);

        // Create the new blocks
        chunkDataManager.setBlocks();

        // Place the tiles in the new positions
        chunkDataManager.placeTiles();

        // Destroy the leftovers
        chunkDataManager.destroyLeftovers();

        //Process fire spread
        chunkDataManager.processFireSpread();
    }

    public Craft getCraft() {
        return craft;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CraftTranslateCommand)) {
            return false;
        }
        CraftTranslateCommand other = (CraftTranslateCommand) obj;
        return other.craft.equals(this.craft) &&
                other.displacement.equals(this.displacement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(craft, displacement);
    }
}
