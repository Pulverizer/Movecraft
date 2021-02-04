package io.github.pulverizer.movecraft.craft;

import com.flowpowered.math.vector.Vector3i;
import io.github.pulverizer.movecraft.Movecraft;
import io.github.pulverizer.movecraft.async.AsyncTask;
import io.github.pulverizer.movecraft.async.DetectionTask;
import io.github.pulverizer.movecraft.async.RotationTask;
import io.github.pulverizer.movecraft.async.TranslationTask;
import io.github.pulverizer.movecraft.config.CraftType;
import io.github.pulverizer.movecraft.enums.DirectControlMode;
import io.github.pulverizer.movecraft.enums.Rotation;
import io.github.pulverizer.movecraft.event.CraftSinkEvent;
import io.github.pulverizer.movecraft.utils.ChatUtils;
import io.github.pulverizer.movecraft.utils.HashHitBox;
import io.github.pulverizer.movecraft.utils.MathUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.explosive.PrimedTNT;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.projectile.explosive.fireball.SmallFireball;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a player controlled craft
 * Needs JavaDocs
 *
 * Last ported from Craft and ICraft on 18 Apr 2020
 * @author BernardisGood
 * @version 1.1 - 27 May 2020
 */
public class Craft {

    // Facts
    private final CraftType type;
    private final UUID id = UUID.randomUUID();
    private final UUID commandeeredBy;
    private int initialSize;
    private final long commandeeredAt = System.currentTimeMillis();
    private final int maxHeightLimit;


    // State
    private String name;
    private final AtomicBoolean processing = new AtomicBoolean();
    private int processingStartTime = 0;
    private HashHitBox hitBox;
    // TODO - Sinking related - Use Implementation
    //  makes it so the craft can't collapse on itself
    //  CollapsedHitbox is to prevent the wrecks from despawning when sinking into water
    protected HashHitBox collapsedHitBox = new HashHitBox();
    private long lastCheckTick = 0L;
    private World world; //TODO - Make cross-dimension travel possible
    private boolean sinking = false;
    private boolean disabled = false;
    private boolean isSubCraft = false;


    // Crew
    private UUID commander;
    private UUID nextInCommand;
    private UUID pilot;
    private final HashSet<UUID> aaDirectors = new HashSet<>();
    private final HashSet<UUID> cannonDirectors = new HashSet<>();
    private final HashSet<UUID> loaders = new HashSet<>();
    private final HashSet<UUID> repairmen = new HashSet<>();
    private final HashSet<UUID> crewList = new HashSet<>();


    // Movement
    private Vector3i lastMoveVector = new Vector3i();
    @Deprecated
    private HashSet<BlockSnapshot> phasedBlocks = new HashSet<>(); //TODO - move to listener database thingy
    private double movePoints;
    //   Cruise
    private Direction verticalCruiseDirection = Direction.NONE;
    private Direction horizontalCruiseDirection = Direction.NONE;
    //   Speed
    private int numberOfMoves = 0;
    // Init with value > 0 or else getTickCooldown() will silently fail
    private float meanMoveTime = 1;
    private long lastRotateTime = 0;
    private int lastMoveTick = Sponge.getServer().getRunningTimeTicks() - 100;
    private float speedBlockEffect = 1;


    // Direct Control
    private DirectControlMode directControl = DirectControlMode.OFF;

    //Contacts
    HashMap<Craft, Integer> contactTracking = new HashMap<>();


    /**
     * Initialises the craft and detects the craft's hitbox.
     *
     * @param type          The type of craft to detect
     * @param player        The player that triggered craft detection
     * @param startLocation The location from which to start craft detection
     */
    public Craft(CraftType type, Player player, Location<World> startLocation) {
        this.type = type;
        world = startLocation.getExtent();

        commandeeredBy = player.getUniqueId();
        maxHeightLimit = Math.min(type.getMaxHeightLimit(), world.getDimension().getBuildHeight() - 1);

        submitTask(new DetectionTask(this, startLocation, player));
    }


    // Facts
    public CraftType getType() {
        return type;
    }

    public UUID getId() {
        return id;
    }

    public UUID commandeeredBy() {
        return commandeeredBy;
    }

    public int getInitialSize() {
        return initialSize;
    }

    public void setInitialSize(int updatedInitialSize) {
        initialSize = updatedInitialSize;
    }

    public long commandeeredAt() {
        return commandeeredAt;
    }

    public int getMaxHeightLimit() {
        return maxHeightLimit;
    }


    // State

    public String getName() {
        return name;
    }

    public void setName(String newName) {
        name = newName;
    }

    @Deprecated
    public boolean isNotProcessing() {
        return !processing.get();
    }

    public boolean isProcessing() {
        return processing.get();
    }

    public void setProcessing(boolean isProcessing) {
        processingStartTime = Sponge.getServer().getRunningTimeTicks();
        processing.set(isProcessing);
    }

    public int getProcessingStartTime() {
        return processingStartTime;
    }

    public HashHitBox getHitBox() {
        return hitBox;
    }

    public void setHitBox(HashHitBox newHitBox) {
        hitBox = newHitBox;
    }

    public HashHitBox getCollapsedHitBox() {
        return collapsedHitBox;
    }

    public int getSize() {
        return hitBox.size();
    }

    public long getLastCheckTick() {
        return lastCheckTick;
    }

    public void runChecks() {

        if (!isProcessing()) {

            // map the blocks in the hitbox
            Map<BlockType, Set<Vector3i>> blockMap = hitBox.map(world);

            // Update hitbox by removing blocks that are not on the allowed list
            HashSet<BlockType> toRemove = new HashSet<>();
            blockMap.forEach((blockType, positions) -> {
                if (!type.getAllowedBlocks().contains(blockType)) {
                    hitBox.removeAll(positions);
                    toRemove.add(blockType);
                }
            });
            toRemove.forEach(blockMap::remove);

            if (!type.getSpeedBlocks().isEmpty()) {
                float newSpeedBlockEffect = 1;

                for (Set<BlockType> blockTypes : type.getSpeedBlocks().keySet()) {
                    int count = 0;

                    for (BlockType blockType : blockTypes) {
                        count += blockMap.containsKey(blockType) ? blockMap.get(blockType).size() : 0;
                    }


                    float effect = (float) count / hitBox.size();
                    effect = (float) (effect / type.getSpeedBlocks().get(blockTypes));
                    newSpeedBlockEffect = newSpeedBlockEffect * Math.min(effect, 1);
                }

                speedBlockEffect = 1 + (1 - newSpeedBlockEffect);
            }

            //TODO - Implement Exposed Speed Blocks
        /*
        if (!type.getExposedSpeedBlocks().isEmpty()) {
            for (Set<BlockType> blockTypes : type.getExposedSpeedBlocks().keySet()) {
                int count = 0;

                for (BlockType blockType : blockTypes) {
                    count += blockMap.containsKey(blockType) ? blockMap.get(blockType).size() : 0;
                }

                cooldown = cooldown * (((double) count / hitBox.size()) * count);
            }
        }*/

            if (!sinking && type.getSinkPercent() != 0.0) {
                boolean shouldSink = false;

                HashMap<List<BlockType>, Integer> foundFlyBlocks = new HashMap<>();
                HashMap<List<BlockType>, Integer> foundMoveBlocks = new HashMap<>();

                // check of the hitbox is actually empty
                if (hitBox.isEmpty()) {
                    shouldSink = true;

                } else {

                    // count fly blocks
                    type.getFlyBlocks().keySet().forEach(blockTypes -> {
                        int count = 0;

                        for (BlockType blockType : blockTypes) {
                            count += blockMap.containsKey(blockType) ? blockMap.get(blockType).size() : 0;
                        }

                        foundFlyBlocks.put(blockTypes, count);
                    });

                    // count move blocks
                    type.getMoveBlocks().keySet().forEach(blockTypes -> {
                        int count = 0;

                        for (BlockType blockType : blockTypes) {
                            count += blockMap.containsKey(blockType) ? blockMap.get(blockType).size() : 0;
                        }

                        foundMoveBlocks.put(blockTypes, count);
                    });

                    // calculate percentages

                    // now see if any of the resulting percentages
                    // are below the threshold specified in
                    // SinkPercent

                    // check we have enough of each fly block
                    for (List<BlockType> blockTypes : type.getFlyBlocks().keySet()) {
                        int numfound = 0;
                        if (foundFlyBlocks.get(blockTypes) != null) {
                            numfound = foundFlyBlocks.get(blockTypes);
                        }
                        double percent = ((double) numfound / (double) hitBox.size()) * 100.0;
                        double flyPercent = type.getFlyBlocks().get(blockTypes).get(0);
                        double sinkPercent = flyPercent * type.getSinkPercent() / 100.0;

                        if (percent < sinkPercent) {
                            shouldSink = true;
                        }
                    }

                    // check we have enough of each move block
                    for (List<BlockType> blockTypes : type.getMoveBlocks().keySet()) {
                        int numfound = 0;
                        if (foundMoveBlocks.get(blockTypes) != null) {
                            numfound = foundMoveBlocks.get(blockTypes);
                        }
                        double percent = ((double) numfound / (double) hitBox.size()) * 100.0;
                        double movePercent = type.getMoveBlocks().get(blockTypes).get(0);
                        double disablePercent = movePercent * type.getSinkPercent() / 100.0;

                        if (percent < disablePercent && !isDisabled() && !processing.get()) {
                            disable();
                            if (pilot != null) {
                                Location<World> loc = Sponge.getServer().getPlayer(pilot).get().getLocation();
                                world.playSound(SoundTypes.ENTITY_IRONGOLEM_DEATH, loc.getPosition(), 5.0f, 5.0f);
                            }
                        }
                    }

                    // And check the overallsinkpercent
                    if (type.getOverallSinkPercent() != 0.0) {
                        double percent = ((double) hitBox.size() / (double) initialSize) * 100.0;
                        if (percent < type.getOverallSinkPercent()) {
                            shouldSink = true;
                        }
                    }
                }

                // if the craft is sinking, let the player know and release the craft. Otherwise update the time for the next check
                if (shouldSink) {
                    sink();

                    CraftManager.getInstance().removeReleaseTask(this);

                } else {
                    lastCheckTick = Sponge.getServer().getRunningTimeTicks();
                }
            }
        }
    }

    public World getWorld() {
        return world;
    }

    public boolean isSinking() {
        return sinking;
    }

    public void sink() {
        // Throw an event
        CraftSinkEvent event = new CraftSinkEvent(this);
        Sponge.getEventManager().post(event);

        // Check if the event has been cancelled
        if (event.isCancelled()) {
            return;
        }

        // The event was not cancelled
        // notify the crew
        notifyCrew("Craft is sinking!");

        // and change the craft's state
        sinking = true;

        // And log it in the console
        Movecraft.getInstance().getLogger().info("Craft " + id + " is sinking: \r" +
                "Originally commandeered by " + Sponge.getServer().getPlayer(commandeeredBy).orElse(null) + " at " + commandeeredAt + " ticks \r" +
                "Currently commanded by " + Sponge.getServer().getPlayer(commander).orElse(null) + "\r" +
                "Currently piloted by " + Sponge.getServer().getPlayer(pilot).orElse(null) + "\r" +
                "\r" +
                "Craft type: " + type.getName() + "\r" +
                "Size " + hitBox.size() + " of original " + initialSize + "\r" +
                "Position: " + hitBox.getMidPoint());
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void disable() {
        //TODO - Create an event for this
        disabled = true;
    }

    public void enable() {
        //TODO - Create an event for this
        disabled = false;
    }

    public void setIsSubCraft() {
        isSubCraft = true;
    }

    public boolean isSubCraft() {
        return isSubCraft;
    }


    // Crew

    public UUID getCommander() {
        return commander;
    }

    public boolean setCommander(UUID player) {
        if (crewList.contains(player) && player != null) {
            commander = player;

            if (player == nextInCommand || nextInCommand == null) resetNextInCommand();
            return true;
        }

        return false;
    }

    public UUID getNextInCommand() {
        return nextInCommand;
    }

    public boolean setNextInCommand(UUID player) {
        if (crewList.contains(player) && player != null) {
            nextInCommand = player;
            return true;
        }

        return false;
    }

    public void resetNextInCommand() {
        nextInCommand = null;

        //TODO - Notify commander that it is recommended to assign a Next-In-Command
    }

    public UUID getPilot() {
        return pilot;
    }

    public boolean setPilot(UUID player) {
        if (crewList.contains(player) && player != null) {
            resetCrewRole(player);

            pilot = player;
            return true;
        }

        return false;
    }

    public boolean isAADirector(UUID player) {
        return aaDirectors.contains(player);
    }

    public boolean addAADirector(UUID player) {
        if (crewList.contains(player)) {
            resetCrewRole(player);

            aaDirectors.add(player);
            return true;
        }

        return false;
    }

    public Player getAADirectorFor(SmallFireball fireball) {
        Player player = null;
        double distance = 16 * 64;
        HashSet<UUID> testSet = new HashSet<>(aaDirectors);

        for (UUID testUUID : testSet) {
            Player testPlayer = Sponge.getServer().getPlayer(testUUID).orElse(null);

            if (testPlayer == null) {
                CraftManager.getInstance().removePlayer(testUUID);
                continue;
            }

            //TODO - Add test for matching facing direction
            double testDistance = testPlayer.getLocation().getPosition().distance(fireball.getLocation().getPosition());

            if (testDistance < distance) {
                player = testPlayer;
                distance = testDistance;
            }
        }

        return player;
    }

    public Set<UUID> getAADirectors() {
        return Collections.unmodifiableSet(aaDirectors);
    }

    public boolean isCannonDirector(UUID player) {
        return cannonDirectors.contains(player);
    }

    public boolean addCannonDirector(UUID player) {
        if (crewList.contains(player)) {
            resetCrewRole(player);

            cannonDirectors.add(player);
            return true;
        }

        return false;
    }

    public Player getCannonDirectorFor(PrimedTNT primedTNT) {
        Player player = null;
        double distance = 16 * 64;
        HashSet<UUID> testSet = new HashSet<>(cannonDirectors);

        for (UUID testUUID : testSet) {
            Player testPlayer = Sponge.getServer().getPlayer(testUUID).orElse(null);

            if (testPlayer == null) {
                CraftManager.getInstance().removePlayer(testUUID);
                continue;
            }

            //TODO - Add test for matching facing direction
            double testDistance = testPlayer.getLocation().getPosition().distance(primedTNT.getLocation().getPosition());

            if (testDistance < distance) {
                player = testPlayer;
                distance = testDistance;
            }
        }

        return player;
    }

    public Set<UUID> getCannonDirectors() {
        return Collections.unmodifiableSet(cannonDirectors);
    }

    public boolean isLoader(UUID player) {
        return loaders.contains(player);
    }

    public boolean addLoader(UUID player) {
        if (crewList.contains(player)) {
            resetCrewRole(player);

            loaders.add(player);
            return true;
        }

        return false;
    }

    public Set<UUID> getLoaders() {
        return Collections.unmodifiableSet(loaders);
    }

    public boolean isRepairman(UUID player) {
        return repairmen.contains(player);
    }

    public boolean addRepairman(UUID player) {
        if (crewList.contains(player)) {
            resetCrewRole(player);

            repairmen.add(player);
            return true;
        }

        return false;
    }

    public Set<UUID> getRepairmen() {
        return Collections.unmodifiableSet(repairmen);
    }

    public boolean crewIsEmpty() {
        return crewList.isEmpty();
    }

    public boolean isCrewMember(UUID player) {
        return crewList.contains(player);
    }

    public void addCrewMember(UUID player) {
        crewList.add(player);
    }

    public void removeCrewMember(UUID player) {
        if (player == null) return;

        if (player == commander && nextInCommand != null) setCommander(nextInCommand);

        if (player == nextInCommand) resetNextInCommand();

        resetCrewRole(player);
        crewList.remove(player);
    }

    public void resetCrewRole(UUID player) {
        if (pilot == player) pilot = null;
        aaDirectors.remove(player);
        cannonDirectors.remove(player);
        loaders.remove(player);
        repairmen.remove(player);
    }

    public void removeCrew() {
        commander = null;
        nextInCommand = null;
        pilot = null;
        aaDirectors.clear();
        cannonDirectors.clear();
        loaders.clear();
        repairmen.clear();
        crewList.clear();
    }

    public void notifyCrew(String message) {
        //TODO - Add Main Config setting to decide if the crew should be notified of such events
        crewList.forEach(crewMember -> Sponge.getServer().getPlayer(crewMember).ifPresent(player -> player.sendMessage(Text.of(message))));
    }


    // Movement

    public Vector3i getLastMoveVector() {
        return lastMoveVector;
    }

    public void setLastMoveVector(Vector3i vector) {
        lastMoveVector = vector;
    }

    @Deprecated
    public HashSet<BlockSnapshot> getPhasedBlocks() {
        return phasedBlocks;
    }

    public double getMovePoints() {
        return movePoints;
    }

    public boolean useFuel(double requiredPoints) {

        if (requiredPoints < 0)
            return false;

        if (movePoints < requiredPoints) {

            HashSet<Vector3i> furnaceBlocks = new HashSet<>();
            Map<BlockType, Set<Vector3i>> blockMap = hitBox.map(world);

            //Find all the furnace blocks
            getType().getFurnaceBlocks().forEach(blockType -> {
                if (blockMap.containsKey(blockType)) {
                    furnaceBlocks.addAll(blockMap.get(blockType));
                }
            });

            //Find and burn fuel
            for (Vector3i furnaceLocation : furnaceBlocks) {
                if (world.getTileEntity(furnaceLocation).isPresent() && world.getTileEntity(furnaceLocation).get() instanceof TileEntityCarrier) {
                    Inventory inventory = ((TileEntityCarrier) world.getTileEntity(furnaceLocation).get()).getInventory();

                    Set<ItemType> fuelItems = getType().getFuelTypes().keySet();

                    for (ItemType fuelItem : fuelItems) {
                        if (inventory.contains(fuelItem)) {

                            double fuelItemValue = getType().getFuelTypes().get(fuelItem);

                            int oldValue = (int) Math.ceil((requiredPoints - movePoints) / fuelItemValue);
                            int newValue = inventory.query(QueryOperationTypes.ITEM_TYPE.of(fuelItem)).poll(oldValue).get().getQuantity();

                            movePoints += newValue * fuelItemValue;
                        }

                        if (movePoints >= requiredPoints)
                            break;
                    }
                }

                if (movePoints >= requiredPoints)
                    break;

            }
        }

        if (movePoints >= requiredPoints) {
            movePoints -= requiredPoints;
            return true;
        }

        return false;
    }

    public int checkFuelStored() {

        int fuelStored = 0;
        HashSet<Vector3i> furnaceBlocks = new HashSet<>();
        Map<BlockType, Set<Vector3i>> blockMap = hitBox.map(world);

        //Find all the furnace blocks
        getType().getFurnaceBlocks().forEach(blockType -> {
            if (blockMap.containsKey(blockType)) {
                furnaceBlocks.addAll(blockMap.get(blockType));
            }
        });

        //Find and count the fuel
        for (Vector3i furnaceLocation : furnaceBlocks) {
            if (world.getTileEntity(furnaceLocation).isPresent() && world.getTileEntity(furnaceLocation).get() instanceof TileEntityCarrier) {
                Inventory inventory = ((TileEntityCarrier) world.getTileEntity(furnaceLocation).get()).getInventory();

                Set<ItemType> fuelItems = getType().getFuelTypes().keySet();

                for (ItemType fuelItem : fuelItems) {
                    if (inventory.contains(fuelItem)) {

                        fuelStored += inventory.query(QueryOperationTypes.ITEM_TYPE.of(fuelItem)).totalItems() * getType().getFuelTypes().get(fuelItem);

                    }
                }
            }
        }

        return fuelStored;
    }

    public int getNumberOfMoves() {
        return numberOfMoves;
    }

    public float getMeanMoveTime() {
        return meanMoveTime;
    }

    public void addMoveTime(float moveTime) {
        meanMoveTime = (meanMoveTime * numberOfMoves + moveTime) / (++numberOfMoves);
    }

    public int getLastMoveTick() {
        return lastMoveTick;
    }

    public void updateLastMoveTick() {
        lastMoveTick = Sponge.getServer().getRunningTimeTicks();
    }

    public void setCruising(Direction vertical, Direction horizontal) {

        if (vertical != Direction.NONE && !vertical.isUpright()) {
            return;
        }

        if (horizontal != Direction.NONE && !horizontal.isCardinal()) {
            return;
        }

        if (pilot != null) {
            Sponge.getServer().getPlayer(pilot).ifPresent(player -> player.sendMessage(ChatTypes.ACTION_BAR, Text.of("Cruising " + ((vertical != Direction.NONE || horizontal != Direction.NONE) ? "Enabled" : "Disabled"))));
        }

        horizontalCruiseDirection = horizontal;
        verticalCruiseDirection = vertical;

        // Change signs aboard the craft to reflect new cruising state
        if (!isProcessing()) {
            resetSigns();
        }
    }

    public Direction getVerticalCruiseDirection() {
        return verticalCruiseDirection;
    }

    public Direction getHorizontalCruiseDirection() {
        return horizontalCruiseDirection;
    }

    public boolean isCruising() {
        return verticalCruiseDirection != Direction.NONE || horizontalCruiseDirection != Direction.NONE;
    }

    private void resetSigns() {
        for (Vector3i loc : hitBox) {
            final TileEntity tileEntity = world.getTileEntity(loc).orElse(null);

            if (!(tileEntity instanceof Sign)) {
                continue;
            }

            final Sign sign = (Sign) tileEntity;

            ListValue<Text> signLines = sign.lines();

            // Reset ALL signs
            // Do Vertical first
            if (signLines.get(0).toPlain().equalsIgnoreCase("Ascend: ON") && verticalCruiseDirection != Direction.UP) {
                signLines.set(0, Text.of("Ascend: OFF"));

            } else if (signLines.get(0).toPlain().equalsIgnoreCase("Ascend: OFF") && verticalCruiseDirection == Direction.UP) {
                signLines.set(0, Text.of("Ascend: ON"));

            } else if (signLines.get(0).toPlain().equalsIgnoreCase("Descend: ON") && verticalCruiseDirection != Direction.DOWN) {
                signLines.set(0, Text.of("Descend: OFF"));

            } else if (signLines.get(0).toPlain().equalsIgnoreCase("Descend: OFF") && verticalCruiseDirection == Direction.DOWN) {
                signLines.set(0, Text.of("Descend: ON"));

                // Then do Horizontal
            } else if (signLines.get(0).toPlain().equalsIgnoreCase("Cruise: ON") && sign.getBlock().get(Keys.DIRECTION).get() != horizontalCruiseDirection) {
                signLines.set(0, Text.of("Cruise: OFF"));

            } else if (signLines.get(0).toPlain().equalsIgnoreCase("Cruise: OFF") && sign.getBlock().get(Keys.DIRECTION).get() == horizontalCruiseDirection) {
                signLines.set(0, Text.of("Cruise: ON"));
            }

            sign.offer(signLines);
        }
    }

    public void translate(Vector3i displacement) {
        // check to see if the craft is trying to move in a direction not permitted by the type
        if (!this.getType().allowHorizontalMovement() && !sinking) {
            displacement = new Vector3i(0, displacement.getY(), 0);
        }

        if (!this.getType().allowVerticalMovement() && !sinking) {
            displacement = new Vector3i(displacement.getX(), 0, displacement.getZ());
        }

        if (displacement.getX() == 0 && displacement.getY() == 0 && displacement.getZ() == 0) {
            return;
        }

        if (!this.getType().allowVerticalTakeoffAndLanding() && displacement.getY() != 0 && !sinking) {
            if (displacement.getX() == 0 && displacement.getZ() == 0) {
                return;
            }
        }

        AsyncTask task = new TranslationTask(this, new Vector3i(displacement.getX(), displacement.getY(), displacement.getZ()));

        if (!isProcessing()) {
            submitTask(task);
        }
    }

    public void rotate(Vector3i originPoint, Rotation rotation) {
        if (lastRotateTime + 1e9 > System.nanoTime() && !isSubCraft()) {
            if (pilot != null)
                Sponge.getServer().getPlayer(pilot).ifPresent(player -> player.sendMessage(Text.of("Rotation - Turning Too Quickly")));
            return;
        }

        lastRotateTime = System.nanoTime();
        AsyncTask task = new RotationTask(this, originPoint, rotation, world);

        if (!isProcessing()) {
            submitTask(task);
        }
    }

    public float getActualSpeed() {
        return (float) Sponge.getServer().getTicksPerSecond() * lastMoveVector.length() / getTickCooldown();
    }

    public float getSimulatedSpeed() {
        return 20 * lastMoveVector.length() / getTickCooldown();
    }

    public int getTickCooldown() {
        float cooldown;

        if (isCruising()) {
            cooldown = type.getCruiseTickCooldown();
        } else {
            cooldown = type.getTickCooldown();
        }

        // Apply speed blocks
        cooldown = cooldown * speedBlockEffect;

        // Apply map update punishment if applicable
        if (!type.ignoreMapUpdateTime() && meanMoveTime > type.getTargetMoveTime()) {
            cooldown = cooldown * (meanMoveTime / type.getTargetMoveTime());
        }

        return (int) cooldown;
    }


    // Direct Control

    public void setDirectControl(DirectControlMode mode) {
        directControl = mode;
    }

    public boolean isUnderDirectControl() {
        return directControl != DirectControlMode.OFF;
    }

    public DirectControlMode getDirectControlMode() {
        return directControl;
    }


    // MISC

    public Set<Craft> getContacts() {
        return contactTracking.keySet();
    }

    public String getContactChatMessage(Craft contact) {
        // TODO - Code correctly
        String commanderName = null;
        if (contact.getCommander() != null) {
            commanderName = Sponge.getServer().getPlayer(contact.getCommander()).get().getName();
        }

        return String.format("%s commanded by %s, size: %d, range: %d to the %s.",
                contact.getType().getName(),
                commanderName,
                contact.getInitialSize(),
                (int) hitBox.getMidPoint().toFloat().distance(contact.getHitBox().getMidPoint().toFloat()),
                ChatUtils.abbreviateDirection(Direction.getClosest(MathUtils.vector3iDirectionalDiff(hitBox.getMidPoint(),
                        contact.getHitBox().getMidPoint()).toDouble())));
    }

    public String getContactSignMessage(Craft contact) {

        float distance = hitBox.getMidPoint().toFloat().distance(contact.getHitBox().getMidPoint().toFloat());

        String type = contact.getType().getName();

        if (type.length() > 7) {
            type = type.substring(0, 7);
        }

        String string;

        if (distance > 9999) {
            string = String.format("%s %.0fk %s",
                    type,
                    distance / 1000,
                    ChatUtils.abbreviateDirection(Direction.getClosest(MathUtils.vector3iDirectionalDiff(hitBox.getMidPoint(),
                            contact.getHitBox().getMidPoint()).toDouble())));

        } else {
            string = String.format("%s %.0f %s",
                    type,
                    distance,
                    ChatUtils.abbreviateDirection(Direction.getClosest(MathUtils.vector3iDirectionalDiff(hitBox.getMidPoint(),
                            contact.getHitBox().getMidPoint()).toDouble())));
        }



        if (string.length() > 16) {
            string = string.substring(0, 16);
        }

        return string;
    }

    public void runContacts() {
        if (!crewIsEmpty() && type.getSpottingMultiplier() > 0) {

            float viewRange = hitBox.size() * type.getSpottingMultiplier();

            Vector3i spotterMiddle = hitBox.getMidPoint();

            float viewRangeSquared = viewRange * viewRange;

            for (final Craft contact : CraftManager.getInstance().getCraftsInWorld(world)) {
                if (contact.getType().getDetectionMultiplier() > 0) {
                    Vector3i contactMiddle = contact.getHitBox().getMidPoint();

                    float distanceSquared = contactMiddle.toFloat().distanceSquared(spotterMiddle.toFloat());

                    if (distanceSquared <= viewRangeSquared && !hitBox.intersects(contact.getHitBox())) {
                        //TODO - implement Underwater Detection Multiplier
                        float contactDetectability = (float) (contact.getHitBox().size() * contact.getType().getDetectionMultiplier());
                        float detectableSizeAtDistanceSquared = hitBox.size() - ((distanceSquared / viewRangeSquared) * hitBox.size());

                        if (contactDetectability > detectableSizeAtDistanceSquared) {
                            // craft has been detected
                            // has the craft not been seen in the last minute, or is completely new?
                            if (contactTracking.get(contact) == null
                                    || Sponge.getServer().getRunningTimeTicks() - contactTracking.get(contact) > 1200) {

                                // TODO - should be entire crew
                                Sponge.getServer().getPlayer(commander).ifPresent(player -> {
                                    player.sendMessage(Text.of("New Contact: " + getContactChatMessage(contact)));
                                    world.playSound(SoundTypes.BLOCK_ANVIL_LAND, player.getLocation().getPosition(), 1.0f, 2.0f);
                                });
                            }

                            int timestamp = Sponge.getServer().getRunningTimeTicks();
                            contactTracking.put(contact, timestamp);
                        }
                    }
                }
            }
        }

       for (Map.Entry<Craft, Integer> entry : contactTracking.entrySet()) {
            if (Sponge.getServer().getRunningTimeTicks() - entry.getValue() > 1200) {
                contactTracking.remove(entry.getKey());
            }
        }
    }

    public void submitTask(AsyncTask task) {
        if (isNotProcessing()) {
            setProcessing(true);
            task.run();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Craft)) {
            return false;
        }
        return this.id.equals(((Craft) obj).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }


    // OLD STUFF -------------------------------------------------------------------------
    //TODO - Sort these methods

    public int getWaterLine() {
        //TODO: Remove this temporary system in favor of passthrough blocks. How tho???
        // Find the waterline from the surrounding terrain or from the static level in the craft type
        int waterLine = 0;
        if (hitBox.isEmpty())
            return waterLine;

        // figure out the water level by examining blocks next to the outer boundaries of the craft
        for (int posY = hitBox.getMaxY() + 1; posY >= hitBox.getMinY() - 1; posY--) {
            int numWater = 0;
            int numAir = 0;
            int posX;
            int posZ;
            posZ = hitBox.getMinZ() - 1;
            for (posX = hitBox.getMinX() - 1; posX <= hitBox.getMaxX() + 1; posX++) {
                BlockType typeID = world.getBlock(posX, posY, posZ).getType();
                if (typeID == BlockTypes.WATER)
                    numWater++;
                if (typeID == BlockTypes.AIR)
                    numAir++;
            }
            posZ = hitBox.getMaxZ() + 1;
            for (posX = hitBox.getMinX() - 1; posX <= hitBox.getMaxX() + 1; posX++) {
                BlockType typeID = world.getBlock(posX, posY, posZ).getType();
                if (typeID == BlockTypes.WATER)
                    numWater++;
                if (typeID == BlockTypes.AIR)
                    numAir++;
            }
            posX = hitBox.getMinX() - 1;
            for (posZ = hitBox.getMinZ(); posZ <= hitBox.getMaxZ(); posZ++) {
                BlockType typeID = world.getBlock(posX, posY, posZ).getType();
                if (typeID == BlockTypes.WATER)
                    numWater++;
                if (typeID == BlockTypes.AIR)
                    numAir++;
            }
            posX = hitBox.getMaxX() + 1;
            for (posZ = hitBox.getMinZ(); posZ <= hitBox.getMaxZ(); posZ++) {
                BlockType typeID = world.getBlock(posX, posY, posZ).getType();
                if (typeID == BlockTypes.WATER)
                    numWater++;
                if (typeID == BlockTypes.AIR)
                    numAir++;
            }
            if (numWater > numAir) {
                return posY;
            }
        }
        return waterLine;
    }

    public void release(Player player) {
        if (player == null) {
            player = Sponge.getServer().getPlayer(commander).orElse(null);
        }

        CraftManager.getInstance().removeCraft(this, player);
        notifyCrew("Your craft has been released.");

        removeCrew();
    }

    public Optional<Player> getNotificationPlayer() {
        Optional<Player> player = Sponge.getServer().getPlayer(pilot);

        if (!player.isPresent()) {
            player = Sponge.getServer().getPlayer(commander);
        }

        if (!player.isPresent()) {
            player = Sponge.getServer().getPlayer(commandeeredBy);
        }

        return player;
    }

    public boolean hasCooldownExpired() {
        double ticksElapsed = Sponge.getServer().getRunningTimeTicks() - lastMoveTick;
        //TODO: Replace world.getSeaLevel() with something better
        if (type.getUnderwaterSpeedMultiplier() > 0 && hitBox.getMinY() < world.getSeaLevel())
            ticksElapsed = ticksElapsed * type.getUnderwaterSpeedMultiplier();

        return getTickCooldown() < ticksElapsed;
    }
}