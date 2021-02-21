package io.github.pulverizer.movecraft.mixin;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(PlayerChunkMapEntry.class)
public abstract class PlayerChunkMapEntryMixin_Forge {

    @Final @Shadow private static Logger LOGGER;
    @Final @Shadow private List<EntityPlayerMP> players;
    @Final @Shadow private ChunkPos pos;
    @Shadow private long lastUpdateInhabitedTime;
    @Final @Shadow private PlayerChunkMap playerChunkMap;
    @Shadow private boolean sentToPlayers;
    @Shadow private Chunk chunk;

    @Shadow public abstract void sendToPlayer(EntityPlayerMP player);

    /**
     * @author BernardisGood - July 17th, 2020
     * @reason Chunks are only generated within 16 view distance. However,
     * chunks within 64 view distance will be sent to the player if loaded by
     * another player.
     */
    @Overwrite
    public void addPlayer(EntityPlayerMP player) {
        if ((int) player.posX >> 4 < pos.x - 16 || (int) player.posX >> 4 > pos.x + 16 || (int) player.posZ >> 4 < pos.z - 16
                || (int) player.posZ >> 4 > pos.z + 16) {

            if (chunk.isLoaded() && this.sentToPlayers) {
                this.sendToPlayer(player);
                // chunk watch event - the chunk is ready
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkWatchEvent.Watch(this.chunk, player));
            }

        } else {
            if (this.players.contains(player)) {
                LOGGER.debug("Failed to add player. {} already is in chunk {}, {}", player, Integer.valueOf(this.pos.x), Integer.valueOf(this.pos.z));

            } else {
                if (this.players.isEmpty()) {
                    this.lastUpdateInhabitedTime = this.playerChunkMap.getWorldServer().getTotalWorldTime();
                }

                this.players.add(player);

                if (this.sentToPlayers) {
                    this.sendToPlayer(player);
                    // chunk watch event - the chunk is ready
                    net.minecraftforge.common.MinecraftForge.EVENT_BUS
                            .post(new net.minecraftforge.event.world.ChunkWatchEvent.Watch(this.chunk, player));
                }
            }
        }
    }

}

