package io.github.pulverizer.movecraft.async;

import com.flowpowered.math.vector.Vector3i;
import io.github.pulverizer.movecraft.Movecraft;
import io.github.pulverizer.movecraft.config.craft_settings.Defaults;
import io.github.pulverizer.movecraft.craft.Craft;
import io.github.pulverizer.movecraft.craft.CraftManager;
import io.github.pulverizer.movecraft.event.CraftDetectEvent;
import io.github.pulverizer.movecraft.sign.CommanderSign;
import io.github.pulverizer.movecraft.utils.CollectionUtils;
import io.github.pulverizer.movecraft.utils.HashHitBox;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.sql.Date;
import java.sql.Time;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.UUID;

/**
 * Permissions Checked
 * Code reviewed on 25 Apr 2020
 *
 * @author BernardisGood
 * @version 1.2 - 25 Apr 2020
 */
public class DetectionTask extends AsyncTask {

    private final World world;
    private final Vector3i startLocation;
    private final Stack<Vector3i> blockStack = new Stack<>();
    private final HashHitBox detectedHitBox = new HashHitBox();
    private final HashSet<Vector3i> visited = new HashSet<>();
    private final HashMap<List<BlockType>, Integer> blockTypeCount = new HashMap<>();
    protected Player player;
    private HashHitBox hitBox;
    private boolean waterContact = false;
    private Map.Entry<Date, Time> commanderSignTimeStamp;
    private String commanderSignUsername;
    private int commanderSignId;

    public DetectionTask(Craft craft, Location<World> startLocation, Player player) {
        super(craft, "Detection");
        world = startLocation.getExtent();
        this.startLocation = startLocation.getBlockPosition();
        this.player = player;
    }

    @Override
    public void execute() {
        // Detect Blocks
        detectBlocks();

        // Return early if failed
        if (failed()) {
            return;
        }

        // Run final checks
        confirmRequirements();

        // Return early if failed
        if (failed()) {
            return;
        }

        // Check player is still online, if offline, cancel Detection Task
        isPlayerStillOnline();

        // Return early if failed
        if (failed()) {
            return;
        }

        // Set the hitBox from the detectedHitBox
        hitBox = new HashHitBox(detectedHitBox);
    }

    private void detectBlocks() {
        blockStack.push(startLocation);

        do {
            detectSurrounding(blockStack.pop());
        } while (!blockStack.isEmpty() && !failed());
    }

    private void detectSurrounding(Vector3i blockPosition) {
        // Detect blocks in surrounding locations
        CollectionUtils.neighbors(blockPosition).forEach(this::detectBlock);
    }

    private void detectBlock(Vector3i blockPosition) {
        // Return if we've already visited this location
        if (!notVisited(blockPosition)) {
            return;
        }

        // Get the BlockType
        BlockType blockType = world.getBlockType(blockPosition);

        // Return if BlockType is Air OR is forbidden on CraftType
        if (blockType.equals(BlockTypes.AIR)) {
            return;
        }

        if (craft.getType().getSetting(Defaults.ForbiddenBlocks.class).get().getValue().contains(blockType)) {
            fail(String.format("Found forbidden block %s at %s", blockType, blockPosition));
            return;
        }

        // Check for water contact
        if (blockType.equals(BlockTypes.FLOWING_WATER) || blockType.equals(BlockTypes.WATER)) {
            waterContact = true;
        }

        // Check if sign, if so, do additional processing
        if (blockType.equals(BlockTypes.STANDING_SIGN) || blockType.equals(BlockTypes.WALL_SIGN)) {
            processSign(blockPosition);
        }

        // Return if not one of the CraftType's allowed BlockTypes
        if (!craft.getType().getSetting(Defaults.AllowedBlocks.class).get().getValue().contains(blockType)) {
            return;
        }

        // Add to blockList
        detectedHitBox.add(blockPosition);

        // Count fly block
        countFlyBlock(blockType);

        // Check if we have exceeded the max size for the CraftType
        if (isUnderMaxSize()) {
            blockStack.push(blockPosition);
        }
    }

    private boolean notVisited(Vector3i blockPosition) {
        if (visited.contains(blockPosition)) {
            return false;
        } else {
            visited.add(blockPosition);
            return true;
        }
    }

    private void processSign(Vector3i blockPosition) {
        if (world.getTileEntity(blockPosition).isPresent()) {

            ListValue<Text> signText = ((Sign) world.getTileEntity(blockPosition).get()).lines();
            if (signText.get(0).toPlain().equalsIgnoreCase("Commander:")) {

                String testUsername = signText.get(1).toPlain();
                int testId = Integer.parseInt(signText.get(2).toPlain());

                Map.Entry<Date, Time> timestamp = CommanderSign.getCreationTimeStamp(testUsername, testId);

                if (timestamp != null) {

                    if (commanderSignUsername == null) {
                        commanderSignTimeStamp = timestamp;
                        commanderSignUsername = testUsername;
                        commanderSignId = testId;

                    } else if (timestamp.getKey().before(commanderSignTimeStamp.getKey())) {
                        commanderSignTimeStamp = timestamp;
                        commanderSignUsername = testUsername;
                        commanderSignId = testId;

                    } else if (timestamp.getKey().equals(commanderSignTimeStamp.getKey()) && timestamp.getValue()
                            .before(commanderSignTimeStamp.getValue())) {
                        commanderSignTimeStamp = timestamp;
                        commanderSignUsername = testUsername;
                        commanderSignId = testId;

                    }
                }
            }

            for (Text line : signText) {
                if (craft.getType().getSetting(Defaults.ForbiddenSignStrings.class).get().getValue().contains(line.toString())) {
                    fail("Detection Failed - Forbidden sign string found.");
                    break;
                }
            }
        }
    }

    private void countFlyBlock(BlockType blockType) {
        for (List<BlockType> flyBlockDef : craft.getType().getSetting(Defaults.FlyBlocks.class).get().getValue().keySet()) {
            if (flyBlockDef.contains(blockType)) {
                blockTypeCount.merge(flyBlockDef, 1, Integer::sum);
            } else {
                blockTypeCount.merge(null, 1, Integer::sum);
            }
        }
    }

    private void confirmRequirements() {
        if (failed()) {
            return;
        }

        // Check Commander Sign
        checkCommanderSign();

        // Check we are within CraftType size limits
        isWithinSizeLimits();

        // Check if CraftType requires contact with water
        if (craft.getType().getSetting(Defaults.RequireWaterContact.class).get().getValue() && !waterContact) {
            fail("Detection Failed - Water contact required but not found!");
            return;
        }

        // Check craft meets CraftType fly block requirements
        checkFlyBlocks();
    }

    private void isWithinSizeLimits() {
        isOverMinSize();
        isUnderMaxSize();
    }

    private void isOverMinSize() {
        if (detectedHitBox.size() < craft.getType().getSetting(Defaults.MinSize.class).get().getValue()) {
            fail(String.format("Craft too small! Min Size: %d", craft.getType().getSetting(Defaults.MinSize.class).get().getValue()));
        }
    }

    private boolean isUnderMaxSize() {
        if (detectedHitBox.size() > craft.getType().getSetting(Defaults.MaxSize.class).get().getValue()) {
            fail(String.format("Craft too large! Max Size: %d", craft.getType().getSetting(Defaults.MaxSize.class).get().getValue()));
            return false;
        }

        return true;
    }

    private void checkCommanderSign() {
        if (commanderSignUsername != null) {
            HashMap<UUID, Boolean> commanderSignMemberMap = CommanderSign.getMembers(commanderSignUsername, commanderSignId);

            if (commanderSignMemberMap == null) {
                Movecraft.getInstance().getLogger().warn("Commander sign not registered in database");

            } else if (!commanderSignMemberMap.containsKey(craft.commandeeredBy())
                    && !Sponge.getServer().getPlayer(craft.commandeeredBy()).get().hasPermission("movecraft.bypasslock")) {

                fail("Not one of the registered commanders for this craft.");
            }
        }
    }

    private void checkFlyBlocks() {
        Map<List<BlockType>, List<Double>> flyBlocks = craft.getType().getSetting(Defaults.FlyBlocks.class).get().getValue();

        for (List<BlockType> i : flyBlocks.keySet()) {
            Integer numberOfBlocks = blockTypeCount.get(i);

            if (numberOfBlocks == null) {
                numberOfBlocks = 0;
            }

            float blockPercentage = (((float) numberOfBlocks / detectedHitBox.size()) * 100);
            Double minPercentage = flyBlocks.get(i).get(0);
            Double maxPercentage = flyBlocks.get(i).get(1);
            if (minPercentage < 10000.0) {
                if (blockPercentage < minPercentage) {
                    fail(String.format("Not enough flyblock" + ": %s %.2f%% < %.2f%%", i.get(0).getName(), blockPercentage, minPercentage));
                    return;
                }

            } else if (numberOfBlocks < flyBlocks.get(i).get(0) - 10000.0) {
                fail(String.format("Not enough flyblock" + ": %s %d < %d", i.get(0).getName(), numberOfBlocks,
                        flyBlocks.get(i).get(0).intValue() - 10000));
                return;

            }

            if (maxPercentage < 10000.0) {
                if (blockPercentage > maxPercentage) {
                    fail(String.format("Too much flyblock" + ": %s %.2f%% > %.2f%%", i.get(0).getName(), blockPercentage, maxPercentage));
                    return;
                }
            } else if (numberOfBlocks > flyBlocks.get(i).get(1) - 10000.0) {
                fail(String.format("Too much flyblock" + ": %s %d > %d", i.get(0).getName(), numberOfBlocks,
                        flyBlocks.get(i).get(1).intValue() - 10000));
                return;

            }
        }
    }

    private void isPlayerStillOnline() {
        if (!player.isOnline()) {
            fail("Player has left the game!");
        }
    }

    @Override
    public void postProcess() {
        // Check if subcraft or new parent craft from existing craft
        Craft originCraft = getParentCraft();

        // Return early if failed
        if (failed()) {
            return;
        }

        // Act based on subcraft or new parent from existing craft
        processIsSubcraft(originCraft);

        // Return early if failed
        if (failed()) {
            return;
        }

        // Set craft hitbox
        craft.setInitialSize(hitBox.size());
        craft.setHitBox(hitBox);

        // Sort out waterline/phased block
        processPhasedBlocks();

        // Check if CraftType can have crew
        if (!craft.isSubCraft() && craft.getType().getSetting(Defaults.CanHaveCrew.class).get().getValue()) {
            craft.addCrewMember(player.getUniqueId());
            craft.setCommander(player.getUniqueId());
        }

        // Check player is still online
        isPlayerStillOnline();

        // Return early if failed
        if (failed()) {
            return;
        }

        // Fire craft detection event - Event should notify player and log itself in the console
        CraftDetectEvent event = new CraftDetectEvent(craft, player);
        Sponge.getEventManager().post(event);

        // Act on whether the event was cancelled or not
        if (event.isCancelled()) {
            craft.release(player);

        } else {
            // Add craft to CraftManager if it is not a Subcraft
            CraftManager.getInstance().addCraft(craft);
        }

        // Set craft to not processing
        craft.setProcessing(false);
    }

    private Craft getParentCraft() {
        HashSet<Craft> intersectingCrafts = CraftManager.getInstance().getCraftsIntersectingWith(hitBox, world);

        if (!intersectingCrafts.isEmpty()) {
            if (intersectingCrafts.size() > 1) {
                fail("Intersecting with too many crafts!");
                return null;
            }

            for (Craft testCraft : intersectingCrafts) {
                if (testCraft.getType() == craft.getType() || testCraft.getHitBox().size() <= hitBox.size()) {
                    if (testCraft.isCrewMember(craft.commandeeredBy())) {
                        fail("You are already in the crew of this craft.");
                    } else {
                        fail("Craft is already being controlled by another player.");
                    }

                    break;
                }

                // Removing to fix issue #63 (Subcrafts disconnecting from parent craft)
                // Can't remember why this exists...
                //if (craft.getType().limitToParentHitBox()) {
                craft.setIsSubCraft();
                //}

                return testCraft;
            }
        }

        return null;
    }

    private void processIsSubcraft(Craft originCraft) {
        if (originCraft == null && craft.getType().getSetting(Defaults.MustBeSubcraft.class).get().getValue()) {
            fail("Craft must be part of another craft!");
            return;
        }

        if (originCraft != null) {
            if (!originCraft.isCrewMember(player.getUniqueId())) {
                fail("You are not in the crew of the parent craft.");
                return;
            }

            if (!craft.isSubCraft()) {
                originCraft.setHitBox(new HashHitBox(CollectionUtils.filter(originCraft.getHitBox(), hitBox)));
                originCraft.setInitialSize(originCraft.getInitialSize() - hitBox.size());
            }
        }
    }

    private void processPhasedBlocks() {
        final int waterLine = craft.getWaterLine();
        if (!(!craft.getType().getSetting(Defaults.PassthroughBlocks.class).get().getValue().contains(BlockTypes.WATER) || !craft.getType().getSetting(Defaults.PassthroughBlocks.class).get().getValue().contains(BlockTypes.FLOWING_WATER)) && craft.getHitBox().getMinY() <= waterLine) {
            for (Vector3i location : craft.getHitBox().boundingHitBox()) {
                if (location.getY() <= waterLine) {
                    craft.getPhasedBlocks().add(BlockTypes.WATER.getDefaultState().snapshotFor(new Location<>(craft.getWorld(), location)));
                }
            }
        }
    }

    @Override
    protected Optional<Player> getNotificationPlayer() {
        return Optional.ofNullable(player);
    }
}