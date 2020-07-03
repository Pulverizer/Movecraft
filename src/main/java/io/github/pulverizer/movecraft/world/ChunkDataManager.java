package io.github.pulverizer.movecraft.world;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import io.github.pulverizer.movecraft.enums.Rotation;
import io.github.pulverizer.movecraft.utils.MathUtils;
import io.github.pulverizer.movecraft.utils.WorldUtils;
import net.minecraft.block.BlockFire;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Manages block and tileEntity movement
 *
 * Last ported from WorldHandler on 8 Apr 2020
 * @author BernardisGood
 * @version 1.0 - 20 Apr 2020
 */
public class ChunkDataManager {
    private WorldServer worldServer;
    private final HashMap<Vector2i, Chunk> chunkMap = new HashMap<>();

    private static final NextTickProvider tickProvider = new NextTickProvider();
    private static final net.minecraft.util.Rotation[] ROTATION;

    static {
        ROTATION = new net.minecraft.util.Rotation[3];
        ROTATION[Rotation.NONE.ordinal()] = net.minecraft.util.Rotation.NONE;
        ROTATION[Rotation.CLOCKWISE.ordinal()] = net.minecraft.util.Rotation.CLOCKWISE_90;
        ROTATION[Rotation.ANTICLOCKWISE.ordinal()] = net.minecraft.util.Rotation.COUNTERCLOCKWISE_90;
    }

    // Set of old block positions
    private final HashSet<Vector3i> oldPositions;

    // Chunk Position and Section : Set of Block Positions
    private final HashMap<Vector3i, HashSet<Vector3i>> blockPosMap = new HashMap<>();
    // Block Positions : BlockState
    private final HashMap<Vector3i, IBlockState> blockStateMap = new HashMap<>();

    // Chunk Position and Section : Set of Tile Positions
    private final HashMap<Vector3i, HashSet<Vector3i>> tilePosMap = new HashMap<>();
    // Block Positions : TileHolder
    private final HashMap<Vector3i, TileHolder> tiles = new HashMap<>();



    public ChunkDataManager(World world, HashSet<Vector3i> oldPositions) {
        worldServer = (WorldServer) world;

        this.oldPositions = oldPositions;
    }

    public void fetchBlocksAndTranslate(Vector3i translateVector) {
        HashSet<Vector3i> positions = new HashSet<>(oldPositions);
        positions.forEach(position -> addBlock(position.add(translateVector), worldServer.getBlockState(WorldUtils.locationToBlockPos(position))));
    }

    public void fetchBlocksAndRotate(Rotation rotation, Vector3i originPoint) {
        HashSet<Vector3i> positions = new HashSet<>(oldPositions);
        positions.forEach(position -> addBlock(MathUtils.rotateVec(rotation, position.sub(originPoint)).add(originPoint), worldServer.getBlockState(WorldUtils.locationToBlockPos(position)).withRotation(ROTATION[rotation.ordinal()])));
    }

    public void fetchTilesAndTranslate(Vector3i translateVector) {
        oldPositions.forEach(position -> {
            BlockPos blockPos = WorldUtils.locationToBlockPos(position);

            if (!worldServer.getBlockState(blockPos).getBlock().hasTileEntity())
                return;

            TileEntity tile = removeTileEntity(position);

            if (tile == null)
                return;

            //get the nextTick to move with the tile
            addTile(position.add(translateVector), new TileHolder(tile, tickProvider.getNextTick(worldServer, position), position));
        });
    }

    public void fetchTilesAndRotate(Rotation rotation, Vector3i originPoint) {
        oldPositions.forEach(position -> {
            BlockPos blockPos = WorldUtils.locationToBlockPos(position);

            if (!worldServer.getBlockState(blockPos).getBlock().hasTileEntity())
                return;

            TileEntity tile = removeTileEntity(position);
            if (tile == null)
                return;

            //get the nextTick to move with the tile
            addTile(MathUtils.rotateVec(rotation, position.sub(originPoint)).add(originPoint), new TileHolder(tile, tickProvider.getNextTick(worldServer, position), position));
        });
    }

    public void addBlock(Vector3i blockPosition, IBlockState blockState) {
        //TODO - Don't move unnecessary blocks
        Vector3i chunkBlockPos = new Vector3i(blockPosition.getX() >> 4, blockPosition.getY() >> 4, blockPosition.getZ() >> 4);

        if (!blockPosMap.containsKey(chunkBlockPos)) {
            HashSet<Vector3i> blockPosSet = new HashSet<>();
            blockPosMap.put(chunkBlockPos, blockPosSet);
        }

        blockPosMap.get(chunkBlockPos).add(blockPosition);
        blockStateMap.put(blockPosition, blockState);
        oldPositions.remove(blockPosition);
    }

    public void addTile(Vector3i blockPosition, TileHolder tileHolder) {
        Vector3i chunkBlockPos = new Vector3i(blockPosition.getX() >> 4, blockPosition.getY() >> 4, blockPosition.getZ() >> 4);

        if (!tilePosMap.containsKey(chunkBlockPos)) {
            HashSet<Vector3i> tilePosSet = new HashSet<>();
            tilePosMap.put(chunkBlockPos, tilePosSet);
        }

        tilePosMap.get(chunkBlockPos).add(blockPosition);
        tiles.put(blockPosition, tileHolder);
    }

    private Chunk getOrFetchChunk(int x, int z) {
        Vector2i chunkPosition = new Vector2i(x, z);

        if (!chunkMap.containsKey(chunkPosition)) {
            chunkMap.put(chunkPosition, worldServer.getChunk(x, z));
        }

        return chunkMap.get(chunkPosition);
    }

    public void setBlocks() {
        blockPosMap.forEach((chunkSectionPos, blockPosSet) -> {
            Chunk chunk = getOrFetchChunk(chunkSectionPos.getX(), chunkSectionPos.getZ());

            ExtendedBlockStorage chunkSection = chunk.getBlockStorageArray()[chunkSectionPos.getY()];

            for (Vector3i newPosition : blockPosSet) {
                BlockPos blockPos = WorldUtils.locationToBlockPos(newPosition);
                IBlockState iBlockState = blockStateMap.get(newPosition);

                if (chunkSection == null) {
                    // Put a GLASS block to initialize the section. It will be replaced next with the real block.
                    chunk.setBlockState(blockPos, Blocks.GLASS.getDefaultState());
                    chunkSection = chunk.getBlockStorageArray()[chunkSectionPos.getY()];
                }


                chunkSection.set(newPosition.getX() & 15, newPosition.getY() & 15, newPosition.getZ() & 15, iBlockState);
                worldServer.notifyBlockUpdate(blockPos, iBlockState, iBlockState, 3);
            }

            chunk.markDirty();
        });
    }

    public void placeTiles() {
        tilePosMap.forEach((chunkSectionPos, tilePosSet) -> {
            Chunk chunk = getOrFetchChunk(chunkSectionPos.getX(), chunkSectionPos.getZ());

            tilePosSet.forEach(newPosition -> {
                TileHolder tileHolder = tiles.get(newPosition);
                TileEntity tile = tileHolder.getTile();

                // Convert Vector3i to Minecraft's BlockPos
                BlockPos blockPos = WorldUtils.locationToBlockPos(newPosition);

                // Same as Spigot's invalidateBlockCache()
                tile.blockMetadata = -1;
                tile.blockType = null;
                // END

                // Optimised version of Chunk#addTileEntity(BlockPos, TileEntity)
                tile.setWorld(worldServer);
                tile.setPos(blockPos);
                tile.validate();

                chunk.getTileEntityMap().put(blockPos, tile);
                // END

                if (tileHolder.getNextTick() != null) {
                    final long currentTime = worldServer.getWorldTime();
                    worldServer.scheduleBlockUpdate(blockPos, tileHolder.getTile().getBlockType(), (int) (tileHolder.getNextTick().scheduledTime - currentTime), tileHolder.getNextTick().priority);
                }
            });

        });
    }

    public void destroyLeftovers() {

        oldPositions.forEach(position -> {
            BlockPos blockPos = WorldUtils.locationToBlockPos(position);
            Chunk chunk = getOrFetchChunk(position.getX() >> 4, position.getZ() >> 4);

            ExtendedBlockStorage chunkSection = chunk.getBlockStorageArray()[position.getY() >> 4];
            if (chunkSection == null) {
                // Put a GLASS block to initialize the section. It will be replaced next with the real block.
                chunk.setBlockState(blockPos, Blocks.GLASS.getDefaultState());
                chunkSection = chunk.getBlockStorageArray()[position.getY() >> 4];
            }


            chunkSection.set(position.getX() & 15, position.getY() & 15, position.getZ() & 15, Blocks.AIR.getDefaultState());
            worldServer.notifyBlockUpdate(blockPos, Blocks.AIR.getDefaultState(), Blocks.AIR.getDefaultState(), 3);
            chunk.markDirty();
        });
    }

    public void processFireSpread() {
        blockStateMap.forEach((blockPosition, blockState) -> {

            if (blockState instanceof BlockFire) {
                ((BlockFire) blockState).randomTick(worldServer, WorldUtils.locationToBlockPos(blockPosition), blockState, worldServer.rand);
            }
        });
    }

    private TileEntity removeTileEntity(Vector3i position) {
        BlockPos blockPos = WorldUtils.locationToBlockPos(position);
        Chunk chunk = getOrFetchChunk(position.getX() >> 4, position.getZ() >> 4);

        TileEntity tile = null;

        // Optimized version of WorldServer#getTileEntity()
        if (worldServer.processingLoadedTiles) {
            for (TileEntity tileEntity : worldServer.addedTileEntityList) {
                if (!tileEntity.isInvalid() && tileEntity.getPos().equals(blockPos)) {
                    tile = tileEntity;
                    break;
                }
            }
        }

        if (tile == null) {
            tile = chunk.getTileEntity(blockPos, Chunk.EnumCreateEntityType.IMMEDIATE);
        }

        if (tile == null) {
            for (TileEntity tileEntity : worldServer.addedTileEntityList) {
                if (!tileEntity.isInvalid() && tileEntity.getPos().equals(blockPos)) {
                    tile = tileEntity;
                    break;
                }
            }
        }
        // END

        if (tile == null) {
            return null;
        }

        //cleanup
        //Optimized version of WorldServer#removeTileEntity();
        tile.invalidate();
        worldServer.addedTileEntityList.remove(tile);
        worldServer.loadedTileEntityList.remove(tile);

        if (!worldServer.processingLoadedTiles) {
            worldServer.tickableTileEntities.remove(tile);

            chunk.getTileEntityMap().remove(blockPos);
        }
        // END

        return tile;
    }
}
