package io.github.pulverizer.movecraft.craft;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableSet;
import io.github.pulverizer.movecraft.Movecraft;
import io.github.pulverizer.movecraft.config.ConfigManager;
import io.github.pulverizer.movecraft.config.CraftType;
import io.github.pulverizer.movecraft.utils.HashHitBox;
import io.github.pulverizer.movecraft.utils.MathUtils;
import io.netty.util.internal.ConcurrentSet;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CraftManager implements Iterable<Craft> {
    private static CraftManager ourInstance;
    private final Set<Craft> craftList = ConcurrentHashMap.newKeySet();
    private HashSet<CraftType> craftTypes;
    private final HashSet<BlockType> transparent = new HashSet<>();

    @Deprecated
    private final ConcurrentMap<Craft, Task> releaseEvents = new ConcurrentHashMap<>();

    private static void initialize(){
        ourInstance = new CraftManager();
    }

    private CraftManager() {
        transparent.add(BlockTypes.AIR);
        transparent.add(BlockTypes.GLASS);
        transparent.add(BlockTypes.GLASS_PANE);
        transparent.add(BlockTypes.STAINED_GLASS);
        transparent.add(BlockTypes.STAINED_GLASS_PANE);
        transparent.add(BlockTypes.IRON_BARS);
        transparent.add(BlockTypes.REDSTONE_WIRE);
        transparent.add(BlockTypes.IRON_TRAPDOOR);
        transparent.add(BlockTypes.TRAPDOOR);
        transparent.add(BlockTypes.NETHER_BRICK_STAIRS);
        transparent.add(BlockTypes.LEVER);
        transparent.add(BlockTypes.STONE_BUTTON);
        transparent.add(BlockTypes.WOODEN_BUTTON);
        transparent.add(BlockTypes.ACACIA_STAIRS);
        transparent.add(BlockTypes.SANDSTONE_STAIRS);
        transparent.add(BlockTypes.BIRCH_STAIRS);
        transparent.add(BlockTypes.BRICK_STAIRS);
        transparent.add(BlockTypes.DARK_OAK_STAIRS);
        transparent.add(BlockTypes.JUNGLE_STAIRS);
        transparent.add(BlockTypes.OAK_STAIRS);
        transparent.add(BlockTypes.PURPUR_STAIRS);
        transparent.add(BlockTypes.QUARTZ_STAIRS);
        transparent.add(BlockTypes.RED_SANDSTONE_STAIRS);
        transparent.add(BlockTypes.SPRUCE_STAIRS);
        transparent.add(BlockTypes.STONE_BRICK_STAIRS);
        transparent.add(BlockTypes.STONE_STAIRS);
        transparent.add(BlockTypes.WALL_SIGN);
        transparent.add(BlockTypes.STANDING_SIGN);


        this.craftTypes = ConfigManager.loadCraftTypes();
    }

    public static CraftManager getInstance() {
        if (ourInstance == null) initialize();

        return ourInstance;
    }

    public HashSet<BlockType> getTransparentBlocks() {
        return new HashSet<>(transparent);
    }

    public Set<CraftType> getCraftTypes() {
        return Collections.unmodifiableSet(craftTypes);
    }

    public void reloadCraftTypes() {
        this.craftTypes = ConfigManager.loadCraftTypes();
    }

    public void addCraft(Craft craft) {
        this.craftList.add(craft);
    }

    public void removeCraft(Craft c, Player player) {
        removeReleaseTask(c);

        // if its sinking, just remove the craft without notifying or checking
        this.craftList.remove(c);
        if(c.getHitBox() != null && !c.getHitBox().isEmpty()) {
            if (player != null) {
                Movecraft.getInstance().getLogger().info(String.format(player.getName() + " has released a craft of type %s with size %d at coordinates : %d x , %d z", c.getType().getName(), c.getHitBox().size(), c.getHitBox().getMinX(), c.getHitBox().getMinZ()));
            } else {
                Movecraft.getInstance().getLogger().info(String.format("NULL Player has released a craft of type %s with size %d at coordinates : %d x , %d z", c.getType().getName(), c.getHitBox().size(), c.getHitBox().getMinX(), c.getHitBox().getMinZ()));
            }
        }else{
            Movecraft.getInstance().getLogger().warn("Releasing empty craft!");
        }
    }

    public void forceRemoveCraft(Craft c) {
        this.craftList.remove(c);
    }

    public Set<Craft> getCraftsInWorld(World world) {
        Set<Craft> crafts = new HashSet<>();
        for(Craft c : this.craftList){
            if(c.getWorld() == world)
                crafts.add(c);
        }
        return crafts;
    }

    public Craft getCraftByPlayer(UUID player) {

        if (!Sponge.getServer().getPlayer(player).isPresent())
            return null;

        for (Craft craft : craftList) {
            if (craft.isCrewMember(player))
                return craft;
        }

        return null;
    }

    public Craft getCraftByUUID(UUID id) {
        for (Craft craft : craftList) {
            if (craft.getId().equals(id))
                return craft;
        }

        return null;
    }

    @Deprecated
    public final void addReleaseTask(final Craft c) {
        Task releaseTask = Task.builder().delayTicks(20*15).execute(player -> c.release(null)).submit(Movecraft.getInstance());
        releaseEvents.put(c, releaseTask);

    }

    @Deprecated
    public final void removeReleaseTask(final Craft c) {
        if (!c.crewIsEmpty()) {
            if (releaseEvents.containsKey(c)) {
                if (releaseEvents.get(c) != null)
                    releaseEvents.get(c).cancel();
                releaseEvents.remove(c);
            }
        }
    }

    public CraftType getCraftTypeFromString(String string) {
        for (CraftType craftType : craftTypes) {
            if (string.equalsIgnoreCase(craftType.getName())) {
                return craftType;
            }
        }
        return null;
    }

    public Craft fastNearestCraftToLoc(Location<World> loc) {
        Craft returnedCraft = null;
        long closestDistSquared = 1000000000L;
        Set<Craft> craftsList = CraftManager.getInstance().getCraftsInWorld(loc.getExtent());
        for (Craft craft : craftsList) {

            Vector3i hitBoxMidPoint = craft.getHitBox().getMidPoint();
            int distSquared = hitBoxMidPoint.distanceSquared(loc.getBlockPosition());

            if (distSquared < closestDistSquared) {
                closestDistSquared = distSquared;
                returnedCraft = craft;
            }
        }
        return returnedCraft;
    }

    public boolean isEmpty(){
        return this.craftList.isEmpty();
    }

    @Override
    public Iterator<Craft> iterator() {
        return Collections.unmodifiableSet(this.craftList).iterator();
    }

    public void removePlayer(UUID player) {
        craftList.forEach(craft -> craft.removeCrewMember(player));
    }

    public HashSet<Craft> getCraftsFromLocation(Location<World> location) {
        HashSet<Craft> results = new HashSet<>();

        for (Craft testCraft : getCraftsInWorld(location.getExtent())) {
            if (testCraft.getHitBox().contains(location.getBlockPosition())) {
                results.add(testCraft);
            }
        }

        return results;
    }

    public Craft getCraftFromLocation(Location<World> location) {
        for (Craft testCraft : getCraftsInWorld(location.getExtent())) {
            if (testCraft.getHitBox().contains(location.getBlockPosition())) {
                return testCraft;
            }
        }

        return null;
    }

    public HashSet<Craft> getCraftsIntersectingWith(HashHitBox hitBox, World world) {
        HashSet<Craft> results = new HashSet<>();

        for (Craft testCraft : getCraftsInWorld(world)) {
            if (testCraft.getHitBox().intersects(hitBox)) {
                results.add(testCraft);
            }
        }

        return results;
    }
}