package io.github.pulverizer.movecraft.config.craft_settings;

import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.util.Direction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage") // getValue(new TypeToken<>) is UNSTABLE
public abstract class Defaults {
    //TODO - JavaDoc

    private static Map<List<BlockType>, List<Double>> blockTypeMapListFromNode(Map<List<BlockType>, List<String>> configMap) {
        HashMap<List<BlockType>, List<Double>> returnMap = new HashMap<>();

        configMap.forEach((blockTypeList, minMaxValues) -> {
            // then read in the limitation values, low and high
            ArrayList<Double> limitList = new ArrayList<>();
            for (String str : minMaxValues) {
                if (str.contains("N")) { // a # indicates a specific quantity, IE: #2 for exactly 2 of the block
                    String[] parts = str.split("N");
                    Double val = Double.valueOf(parts[1]);
                    limitList.add(10000d + val);  // limit greater than 10000 indicates an specific quantity (not a ratio)
                } else {
                    Double val = Double.valueOf(str);
                    limitList.add(val);
                }
            }

            returnMap.put(blockTypeList, limitList);
        });

        return returnMap;
    }

    // General
    public static class Name extends CraftSetting<String> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("name").getString().toLowerCase();
        }
    }

    public static class MinSize extends CraftSetting<Integer> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("minSize").getInt();
        }
    }

    public static class MaxSize extends CraftSetting<Integer> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("maxSize").getInt();
        }
    }

    //TODO - Implement
    public static class Nameable extends CraftSetting<Boolean> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("canBeNamed").getBoolean(false);
        }
    }

    public static class MustBeSubcraft extends CraftSetting<Boolean> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("mustBeSubcraft").getBoolean(false);
        }
    }

    public static class ForbiddenSignStrings extends CraftSetting<Set<String>> {

        @Override public void load(ConfigurationNode craftConfigNode) throws ObjectMappingException {
            value = craftConfigNode.getNode("forbiddenSignStrings").getValue(new TypeToken<Set<String>>() {
            }, new HashSet<>());
        }

        @Override public Set<String> getValue() {
            return Collections.unmodifiableSet(value);
        }
    }

    public static class RequiresSpecificPerms extends CraftSetting<Boolean> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("requiresSpecificPerms").getBoolean(true);
        }
    }

    //TODO - Implement
    public static class LimitToParentHitBox extends CraftSetting<Boolean> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("limitToParentHitBox").getBoolean(false);
        }
    }

    // Entities
    public static class MoveEntities extends CraftSetting<Boolean> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("moveEntities").getBoolean(true);
        }
    }

    public static class OnlyMovePlayers extends CraftSetting<Boolean> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("onlyMovePlayers").getBoolean(true);
        }
    }

    // Crew
    public static class CanHaveCrew extends CraftSetting<Boolean> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("canHaveCrew").getBoolean(true);
        }
    }

    public static class CanHaveLoaders extends CraftSetting<Boolean> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("allowLoaders").getBoolean(true);
        }
    }

    public static class CanHaveRepairmen extends CraftSetting<Boolean> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("allowRepairmen").getBoolean(true);
        }
    }

    public static class CanHaveCannonDirectors extends CraftSetting<Boolean> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("allowCannonDirectorSign").getBoolean(true);
        }
    }

    public static class CanHaveAADirectors extends CraftSetting<Boolean> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("allowAADirectorSign").getBoolean(true);
        }
    }

    // Control
    public static class CanDirectControl extends CraftSetting<Boolean> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("canDirectControl").getBoolean(true);
        }
    }

    public static class AllowRemoteSigns extends CraftSetting<Boolean> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("allowRemoteSign").getBoolean(true);
        }
    }

    // Fuel
    public static class FurnaceBlocks extends CraftSetting<Set<BlockType>> {

        @Override public void load(ConfigurationNode craftConfigNode) throws ObjectMappingException {
            value = craftConfigNode.getNode("furnaceBlocks").getValue(new TypeToken<Set<BlockType>>() {
            }, new HashSet<>());

            if (value.isEmpty()) {
                value.add(BlockTypes.FURNACE);
                value.add(BlockTypes.LIT_FURNACE);
            }
        }

        @Override public Set<BlockType> getValue() {
            return Collections.unmodifiableSet(value);
        }
    }

    public static class FuelTypes extends CraftSetting<Map<ItemType, Double>> {

        @Override public void load(ConfigurationNode craftConfigNode) throws ObjectMappingException {
            value = craftConfigNode.getNode("fuelItems").getValue(new TypeToken<Map<ItemType, Double>>() {
            }, new HashMap<>());

            if (value.isEmpty()) {
                value.put(ItemTypes.COAL, 8.0);
                value.put(ItemTypes.COAL_BLOCK, 72.0);
            }
        }

        @Override public Map<ItemType, Double> getValue() {
            return Collections.unmodifiableMap(value);
        }
    }

    public static class FuelBurnRate extends CraftSetting<Double> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("fuelBurnRate").getDouble(0);
        }
    }

    //TODO - Implement
    public static class PerWorldFuelBurnRate extends CraftSetting<Map<String, Double>> {

        @Override public void load(ConfigurationNode craftConfigNode) throws ObjectMappingException {
            value = craftConfigNode.getNode("perWorldFuelBurnRate").getValue(new TypeToken<Map<String, Double>>() {
            }, new HashMap<>());
        }

        @Override public Map<String, Double> getValue() {
            return Collections.unmodifiableMap(value);
        }
    }

    // Blocks
    public static class AllowedBlocks extends CraftSetting<Set<BlockType>> {

        @Override public void load(ConfigurationNode craftConfigNode) throws ObjectMappingException {
            value = craftConfigNode.getNode("allowedBlocks").getValue(new TypeToken<Set<BlockType>>() {
            }, new HashSet<>());
        }

        @Override public Set<BlockType> getValue() {
            return Collections.unmodifiableSet(value);
        }
    }

    public static class ForbiddenBlocks extends CraftSetting<Set<BlockType>> {

        @Override public void load(ConfigurationNode craftConfigNode) throws ObjectMappingException {
            value = craftConfigNode.getNode("forbiddenBlocks").getValue(new TypeToken<Set<BlockType>>() {
            }, new HashSet<>());
        }

        @Override public Set<BlockType> getValue() {
            return Collections.unmodifiableSet(value);
        }
    }

    public static class FlyBlocks extends CraftSetting<Map<List<BlockType>, List<Double>>> {

        @Override public void load(ConfigurationNode craftConfigNode) throws ObjectMappingException {
            value = blockTypeMapListFromNode(craftConfigNode.getNode("flyBlocks").getValue(new TypeToken<Map<List<BlockType>, List<String>>>() {
            }, new HashMap<>()));
        }

        @Override public Map<List<BlockType>, List<Double>> getValue() {
            return Collections.unmodifiableMap(value);
        }
    }

    public static class MoveBlocks extends CraftSetting<Map<List<BlockType>, List<Double>>> {

        @Override public void load(ConfigurationNode craftConfigNode) throws ObjectMappingException {
            value = blockTypeMapListFromNode(craftConfigNode.getNode("moveBlocks").getValue(new TypeToken<Map<List<BlockType>, List<String>>>() {
            }, new HashMap<>()));
        }

        @Override public Map<List<BlockType>, List<Double>> getValue() {
            return Collections.unmodifiableMap(value);
        }
    }

    public static class HarvestBlocks extends CraftSetting<List<BlockType>> {

        @Override public void load(ConfigurationNode craftConfigNode) throws ObjectMappingException {
            value = craftConfigNode.getNode("harvestBlocks").getList(TypeToken.of(BlockType.class), new ArrayList<>());
        }

        @Override public List<BlockType> getValue() {
            return Collections.unmodifiableList(value);
        }
    }

    public static class HarvesterBladeBlocks extends CraftSetting<List<BlockType>> {

        @Override public void load(ConfigurationNode craftConfigNode) throws ObjectMappingException {
            value = craftConfigNode.getNode("harvesterBladeBlocks").getList(TypeToken.of(BlockType.class), new ArrayList<>());
        }

        @Override public List<BlockType> getValue() {
            return Collections.unmodifiableList(value);
        }
    }

    public static class PassthroughBlocks extends CraftSetting<Set<BlockType>> {

        @Override public void load(ConfigurationNode craftConfigNode) throws ObjectMappingException {
            value = craftConfigNode.getNode("passthroughBlocks").getValue(new TypeToken<Set<BlockType>>() {
            }, new HashSet<>());
        }

        @Override public Set<BlockType> getValue() {
            return Collections.unmodifiableSet(value);
        }
    }

    public static class SpeedBlocks extends CraftSetting<Map<Set<BlockType>, Double>> {

        @Override public void load(ConfigurationNode craftConfigNode) throws ObjectMappingException {
            value = craftConfigNode.getNode("speedBlocks").getValue(new TypeToken<Map<Set<BlockType>, Double>>() {
            }, new HashMap<>());
        }

        @Override public Map<Set<BlockType>, Double> getValue() {
            return Collections.unmodifiableMap(value);
        }
    }

    //TODO - Implement
    public static class ExposedSpeedBlocks extends CraftSetting<Map<Set<BlockType>, List<Double>>> {

        @Override public void load(ConfigurationNode craftConfigNode) throws ObjectMappingException {
            value = craftConfigNode.getNode("exposedSpeedBlocks").getValue(new TypeToken<Map<Set<BlockType>, List<Double>>>() {
            }, new HashMap<>());
        }

        @Override public Map<Set<BlockType>, List<Double>> getValue() {
            return Collections.unmodifiableMap(value);
        }
    }

    // Spotting
    public static class SpottingMultiplier extends CraftSetting<Double> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("spottingMultiplier").getDouble(0.5f);
        }
    }

    //TODO - Implement
    public static class PerWorldSpottingMultiplier extends CraftSetting<Map<String, Double>> {

        @Override public void load(ConfigurationNode craftConfigNode) throws ObjectMappingException {
            value = craftConfigNode.getNode("perWorldSpottingMultiplier").getValue(new TypeToken<Map<String, Double>>() {
            }, new HashMap<>());
        }

        @Override public Map<String, Double> getValue() {
            return Collections.unmodifiableMap(value);
        }
    }

    public static class DetectionMultiplier extends CraftSetting<Double> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("detectionMultiplier").getDouble(0);
        }
    }

    //TODO - Implement
    public static class PerWorldDetectionMultiplier extends CraftSetting<Map<String, Double>> {

        @Override public void load(ConfigurationNode craftConfigNode) throws ObjectMappingException {
            value = craftConfigNode.getNode("perWorldDetectionMultiplier").getValue(new TypeToken<Map<String, Double>>() {
            }, new HashMap<>());
        }

        @Override public Map<String, Double> getValue() {
            return Collections.unmodifiableMap(value);
        }
    }

    //TODO - Implement
    public static class UnderwaterDetectionMultiplier extends CraftSetting<Double> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("underwaterDetectionMultiplier").getDouble(craftConfigNode.getNode("detectionMultiplier").getDouble(0));
        }
    }

    //TODO - Implement
    public static class PerWorldUnderwaterDetectionMultiplier extends CraftSetting<Map<String, Double>> {

        @Override public void load(ConfigurationNode craftConfigNode) throws ObjectMappingException {
            value = craftConfigNode.getNode("perWorldUnderwaterDetectionMultiplier").getValue(new TypeToken<Map<String, Double>>() {
            }, new HashMap<>());
        }

        @Override public Map<String, Double> getValue() {
            return Collections.unmodifiableMap(value);
        }
    }

    // Movement
    public static class RotateAtMidpoint extends CraftSetting<Boolean> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("rotateAtMidpoint").getBoolean(false);
        }
    }

    public static class RequireWaterContact extends CraftSetting<Boolean> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("requireWaterContact").getBoolean(false);
        }
    }

    public static class UnderwaterSpeedMultiplier extends CraftSetting<Double> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("underwaterSpeedMultiplier").getDouble(0);
        }
    }

    //   Limits
    public static class AllowHorizontalMovement extends CraftSetting<Boolean> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("allowHorizontalMovement").getBoolean(true);
        }
    }

    public static class AllowVerticalMovement extends CraftSetting<Boolean> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("allowVerticalMovement").getBoolean(true);
        }
    }

    public static class AllowVerticalTakeoffAndLanding extends CraftSetting<Boolean> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("allowVerticalTakeoffAndLanding").getBoolean(true);
        }
    }

    //   Bounds
    public static class MinHeightLimit extends CraftSetting<Integer> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("minHeightLimit").getInt(0);
        }
    }

    //TODO - Implement
    public static class PerWorldMinHeightLimit extends CraftSetting<Map<String, Integer>> {

        @Override public void load(ConfigurationNode craftConfigNode) throws ObjectMappingException {
            value = craftConfigNode.getNode("perWorldMinHeightLimit").getValue(new TypeToken<Map<String, Integer>>() {
            }, new HashMap<>());
        }

        @Override public Map<String, Integer> getValue() {
            return Collections.unmodifiableMap(value);
        }
    }

    public static class MaxHeightLimit extends CraftSetting<Integer> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("maxHeightLimit").getInt(255);
        }
    }

    //TODO - Implement
    public static class PerWorldMaxHeightLimit extends CraftSetting<Map<String, Integer>> {

        @Override public void load(ConfigurationNode craftConfigNode) throws ObjectMappingException {
            value = craftConfigNode.getNode("perWorldMaxHeightLimit").getValue(new TypeToken<Map<String, Integer>>() {
            }, new HashMap<>());
        }

        @Override public Map<String, Integer> getValue() {
            return Collections.unmodifiableMap(value);
        }
    }

    public static class MaxHeightAboveGround extends CraftSetting<Integer> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("maxHeightAboveGround").getInt(-1);
        }
    }

    //TODO - Implement
    public static class PerWorldMaxHeightAboveGround extends CraftSetting<Map<String, Integer>> {

        @Override public void load(ConfigurationNode craftConfigNode) throws ObjectMappingException {
            value = craftConfigNode.getNode("perWorldMaxHeightAboveGround").getValue(new TypeToken<Map<String, Integer>>() {
            }, new HashMap<>());
        }

        @Override public Map<String, Integer> getValue() {
            return Collections.unmodifiableMap(value);
        }
    }

    //   Hover
    //TODO - Implement
    public static class CanHover extends CraftSetting<Boolean> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("canHover").getBoolean(false);
        }
    }

    //TODO - Implement
    public static class HoverLimit extends CraftSetting<Integer> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("hoverLimit").getInt(0);
        }
    }

    public static class CanHoverOverWater extends CraftSetting<Boolean> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("canHoverOverWater").getBoolean(true);
        }
    }

    //TODO - Implement
    public static class ForbiddenHoverOverBlocks extends CraftSetting<Set<BlockType>> {

        @Override public void load(ConfigurationNode craftConfigNode) throws ObjectMappingException {
            value = craftConfigNode.getNode("forbiddenHoverOverBlocks").getValue(new TypeToken<Set<BlockType>>() {
            }, new HashSet<>());
        }

        @Override public Set<BlockType> getValue() {
            return Collections.unmodifiableSet(value);
        }
    }

    //   Gravity
    public static class UseGravity extends CraftSetting<Boolean> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("useGravity").getBoolean(false);
        }
    }

    //TODO - Implement
    public static class GravityDropDistance extends CraftSetting<Integer> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("gravityDropDistance").getInt(-1);
        }
    }

    //TODO - Implement
    public static class GravityInclineDistance extends CraftSetting<Integer> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("gravityInclineDistance").getInt(-1);
        }
    }

    //   Cruising
    public static class CanCruise extends CraftSetting<Boolean> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("canCruise").getBoolean(false);
        }
    }

    public static class CruiseSkipBlocks extends CraftSetting<Integer> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("cruiseSkipBlocks").getInt(0);
        }
    }

    //TODO - Implement
    public static class PerWorldCruiseSkipBlocks extends CraftSetting<Map<String, Integer>> {

        @Override public void load(ConfigurationNode craftConfigNode) throws ObjectMappingException {
            value = craftConfigNode.getNode("perWorldCruiseSkipBlocks").getValue(new TypeToken<Map<String, Integer>>() {
            }, new HashMap<>());
        }

        @Override public Map<String, Integer> getValue() {
            return Collections.unmodifiableMap(value);
        }
    }

    public static class VertCruiseSkipBlocks extends CraftSetting<Integer> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("vertCruiseSkipBlocks").getInt(craftConfigNode.getNode("cruiseSkipBlocks").getInt(0));
        }
    }

    //TODO - Implement
    public static class PerWorldVertCruiseSkipBlocks extends CraftSetting<Map<String, Integer>> {

        @Override public void load(ConfigurationNode craftConfigNode) throws ObjectMappingException {
            value = craftConfigNode.getNode("perWorldVertCruiseSkipBlocks").getValue(new TypeToken<Map<String, Integer>>() {
            }, new HashMap<>());
        }

        @Override public Map<String, Integer> getValue() {
            return Collections.unmodifiableMap(value);
        }
    }

    //     Cruise on Pilot
    public static class CruiseOnPilot extends CraftSetting<Boolean> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("cruiseOnPilot").getBoolean(false);
        }
    }

    public static class CruiseOnPilotMaxMoves extends CraftSetting<Integer> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("cruiseOnPilotMaxMoves").getInt(300);
        }
    }

    public static class CruiseOnPilotVertDirection extends CraftSetting<Direction> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            int temp = craftConfigNode.getNode("cruiseOnPilotVertDirection").getInt(0);

            if (temp < 0) {
                value = Direction.DOWN;
            } else if (temp > 0) {
                value = Direction.UP;
            } else {
                value = Direction.NONE;
            }
        }
    }

    //   Teleport / Static Move
    public static class CanTeleport extends CraftSetting<Boolean> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("canTeleport").getBoolean(false);
        }
    }

    //TODO - Implement
    //private final boolean canSwitchWorld;
    //private final Set<String> disableTeleportToWorlds;
    //private final int teleportationCooldown;

    public static class CanStaticMove extends CraftSetting<Boolean> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("canStaticMove").getBoolean(false);
        }
    }

    public static class MaxStaticMove extends CraftSetting<Integer> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("maxStaticMove").getInt(10000);
        }
    }


    //TODO - Implement
    //private final Map<String, Integer> perWorldTickCooldown; // speed setting

    //   Cooldown
    public static class TickCooldown extends CraftSetting<Double> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = Math.ceil(20 /craftConfigNode.getNode("speed").getDouble(1));
        }
    }

    //TODO - Implement
    //private final Map<String, Integer> perWorldCruiseTickCooldown; // cruise speed setting
    //private final double cruiseVertTickCooldown;
    //private final Map<String, Integer> perWorldVertCruiseTickCooldown; // cruise speed setting

    public static class CruiseTickCooldown extends CraftSetting<Double> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = Math.ceil(20 /craftConfigNode.getNode("cruiseSpeed").getDouble(craftConfigNode.getNode("speed").getDouble()));
        }
    }

    public static class IgnoreMapUpdateTime extends CraftSetting<Boolean> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("ignoreMapUpdateTime").getBoolean(false);
        }
    }

    public static class TargetMoveTime extends CraftSetting<Double> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("targetMoveTime").getDouble(((double) craftConfigNode.getNode("maxSize").getInt()) / 1000f);
        }
    }

    //   Collisions
    public static class CollisionExplosion extends CraftSetting<Float> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("collisionExplosion").getFloat(0);
        }
    }

    //TODO - Implement
    //private final SoundType collisionSound;

    public static class FocusedExplosion extends CraftSetting<Boolean> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("focusedExplosion").getBoolean(false);
        }
    }

    // Sinking
    public static class SinkPercent extends CraftSetting<Double> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("sinkPercent").getDouble(0);
        }
    }

    public static class OverallSinkPercent extends CraftSetting<Double> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("overallSinkPercent").getDouble(0);
        }
    }

    public static class ExplodeOnCrash extends CraftSetting<Float> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("explodeOnCrash").getFloat(0);
        }
    }

    public static class KeepMovingOnSink extends CraftSetting<Boolean> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("keepMovingOnSink").getBoolean(false);
        }
    }

    public static class SinkRateTicks extends CraftSetting<Integer> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            //TODO - should the default be Settings.SinkRateTicks?
            value = craftConfigNode.getNode("sinkTickRate").getInt(0);
        }
    }

    public static class SmokeOnSink extends CraftSetting<Integer> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("smokeOnSink").getInt(0);
        }
    }

    public static class SmokeOnSinkQuantity extends CraftSetting<Integer> {

        @Override public void load(ConfigurationNode craftConfigNode) {
            value = craftConfigNode.getNode("smokeOnSinkQuantity").getInt(1);
        }
    }
}