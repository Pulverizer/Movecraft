package io.github.pulverizer.movecraft.config;

import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.util.Direction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final public class CraftType {

    // General
    private final String name;
    private final int minSize;
    private final int maxSize;

    //private final boolean canBeNamed;
    private final boolean mustBeSubcraft;
    private final Set<String> forbiddenSignStrings;
    private final boolean requiresSpecificPerms;
    private final boolean limitToParentHitBox;


    // Entities
    private final boolean moveEntities;
    private final boolean onlyMovePlayers;


    // Crew
    private final boolean canHaveCrew;
    private final boolean canHaveLoaders;
    private final boolean canHaveRepairmen;
    private final boolean canHaveCannonDirectors;
    private final boolean canHaveAADirectors;


    // Control
    private final boolean canDirectControl;
    private final boolean allowRemoteSigns;


    // Fuel
    private final Set<BlockType> furnaceBlocks;
    private final Map<ItemType, Double> fuelTypes;
    private final double fuelBurnRate;
    //private final Map<String, Double> perWorldFuelBurnRate;


    // Blocks
    private final Set<BlockType> allowedBlocks;
    private final Set<BlockType> forbiddenBlocks;
    private final Map<List<BlockType>, List<Double>> flyBlocks;
    private final Map<List<BlockType>, List<Double>> moveBlocks;
    private final List<BlockType> harvestBlocks;
    private final List<BlockType> harvesterBladeBlocks;
    private final Set<BlockType> passthroughBlocks;
    private final Map<Set<BlockType>, Double> speedBlocks;
    private final Map<Set<BlockType>, List<Double>> exposedSpeedBlocks;


    // Spotting
    private final float spottingMultiplier;
    //private final Map<String, Double> perWorldSpottingMultiplier;
    private final double detectionMultiplier;
    //private final Map<String, Double> perWorldDetectionMultiplier;
    private final double underwaterDetectionMultiplier;
    //private final Map<String, Double> perWorldUnderwaterDetectionMultiplier;


    // Movement
    private final boolean rotateAtMidpoint;
    private final boolean requireWaterContact;
    private final double underwaterSpeedMultipler;

    //   Limits
    private final boolean allowHorizontalMovement;
    private final boolean allowVerticalMovement;
    private final boolean allowVerticalTakeoffAndLanding;

    //   Bounds
    private final int minHeightLimit;
    //private final Map<String, Integer> perWorldMinHeightLimit;
    private final int maxHeightLimit;
    //private final Map<String, Integer> perWorldMaxHeightLimit;
    private final int maxHeightAboveGround;
    //private final Map<String, Integer> perWorldMaxHeightAboveGround;

    //   Hover
    private final boolean canHover;
    private final int hoverLimit;
    private final boolean canHoverOverWater;
    private final Set<BlockType> forbiddenHoverOverBlocks;

    //   Gravity
    private final boolean useGravity;
    //private final int gravityDropDistance;
    //private final int gravityInclineDistance;

    //   Cruising
    private final boolean canCruise;
    private final int cruiseSkipBlocks;
    //private final Map<String, Integer> perWorldCruiseSkipBlocks;
    private final int vertCruiseSkipBlocks;
    //private final Map<String, Integer> perWorldVertCruiseSkipBlocks;

    //     Cruise on Pilot
    private final boolean cruiseOnPilot;
    private final int cruiseOnPilotMaxMoves;
    private final Direction cruiseOnPilotVertDirection;

    //   Teleport / Static Move
    private final boolean canTeleport;
    private final boolean canStaticMove;
    //private final boolean canSwitchWorld;
    //private final Set<String> disableTeleportToWorlds;
    //private final int teleportationCooldown;
    private final int maxStaticMove;

    //   Cooldown
    private final double tickCooldown;
    //private final Map<String, Integer> perWorldTickCooldown; // speed setting
    private final double cruiseTickCooldown;
    //private final Map<String, Integer> perWorldCruiseTickCooldown; // cruise speed setting
    //private final double cruiseVertTickCooldown;
    //private final Map<String, Integer> perWorldVertCruiseTickCooldown; // cruise speed setting
    private final boolean ignoreMapUpdateTime;
    private final float targetMoveTime;

    //   Collisions
    private final float collisionExplosion;
    private final boolean focusedExplosion;
    //private final SoundType collisionSound;


    // Sinking
    private final double sinkPercent;
    private final double overallSinkPercent;
    private final float explodeOnCrash;
    private final boolean keepMovingOnSink;
    private final int sinkRateTicks;
    private final int smokeOnSink;
    private final int smokeOnSinkQuantity;


    public CraftType(File file) throws NullPointerException, IOException, ObjectMappingException {

        ConfigurationLoader<ConfigurationNode> configLoader = ConfigManager.createConfigLoader(file.toPath());
        ConfigurationNode configNode = configLoader.load();

        // Load config file

        name = configNode.getNode("name").getString().toLowerCase();
        maxSize = configNode.getNode("maxSize").getInt();
        minSize = configNode.getNode("minSize").getInt();

        requiresSpecificPerms = configNode.getNode("requiresSpecificPerms").getBoolean(true);
        allowedBlocks = configNode.getNode("allowedBlocks").getValue(new TypeToken<Set<BlockType>>() {
        }, new HashSet<>());
        furnaceBlocks = configNode.getNode("furnaceBlocks").getValue(new TypeToken<Set<BlockType>>() {
        }, new HashSet<>());
        if (furnaceBlocks.isEmpty()) {
            furnaceBlocks.add(BlockTypes.FURNACE);
            furnaceBlocks.add(BlockTypes.LIT_FURNACE);
        }

        fuelTypes = configNode.getNode("fuelItems").getValue(new TypeToken<Map<ItemType, Double>>() {
        }, new HashMap<>());
        if (fuelTypes.isEmpty()) {
            fuelTypes.put(ItemTypes.COAL, 8.0);
            fuelTypes.put(ItemTypes.COAL_BLOCK, 72.0);
        }


        forbiddenBlocks = configNode.getNode("forbiddenBlocks").getValue(new TypeToken<Set<BlockType>>() {
        }, new HashSet<>());
        forbiddenSignStrings = configNode.getNode("forbiddenSignStrings").getValue(new TypeToken<Set<String>>() {
        }, new HashSet<>());

        requireWaterContact = configNode.getNode("requireWaterContact").getBoolean(false);
        tickCooldown = configNode.getNode("speed").getDouble();
        cruiseTickCooldown = configNode.getNode("cruiseSpeed").getDouble();

        flyBlocks = blockTypeMapListFromNode(configNode.getNode("flyblocks").getValue(new TypeToken<Map<List<BlockType>, List<String>>>() {
        }, new HashMap<>()));
        moveBlocks = blockTypeMapListFromNode(configNode.getNode("moveBlocks").getValue(new TypeToken<Map<List<BlockType>, List<String>>>() {
        }, new HashMap<>()));

        canCruise = configNode.getNode("canCruise").getBoolean(false);
        canTeleport = configNode.getNode("canTeleport").getBoolean(false);
        cruiseOnPilot = configNode.getNode("cruiseOnPilot").getBoolean(false);

        int temp = configNode.getNode("cruiseOnPilotVertDirection").getInt(0);

        if (temp < 0) {
            cruiseOnPilotVertDirection = Direction.DOWN;
        } else if (temp > 0) {
            cruiseOnPilotVertDirection = Direction.UP;
        } else {
            cruiseOnPilotVertDirection = Direction.NONE;
        }

        allowVerticalMovement = configNode.getNode("allowVerticalMovement").getBoolean(true);
        rotateAtMidpoint = configNode.getNode("rotateAtMidpoint").getBoolean(false);
        allowHorizontalMovement = configNode.getNode("allowHorizontalMovement").getBoolean(true);
        allowRemoteSigns = configNode.getNode("allowRemoteSign").getBoolean(true);
        canHaveCannonDirectors = configNode.getNode("allowCannonDirectorSign").getBoolean(true);
        canHaveAADirectors = configNode.getNode("allowAADirectorSign").getBoolean(true);
        canStaticMove = configNode.getNode("canStaticMove").getBoolean(false);
        maxStaticMove = configNode.getNode("maxStaticMove").getInt(10000);
        cruiseSkipBlocks = configNode.getNode("cruiseSkipBlocks").getInt(0);
        vertCruiseSkipBlocks = configNode.getNode("vertCruiseSkipBlocks").getInt(cruiseSkipBlocks);
        underwaterSpeedMultipler = configNode.getNode("underwaterSpeedMultipler").getDouble(0);
        focusedExplosion = configNode.getNode("focusedExplosion").getBoolean(false);
        mustBeSubcraft = configNode.getNode("mustBeSubcraft").getBoolean(false);
        fuelBurnRate = configNode.getNode("fuelBurnRate").getDouble(0);
        sinkPercent = configNode.getNode("sinkPercent").getDouble(0);
        overallSinkPercent = configNode.getNode("overallSinkPercent").getDouble(0);
        detectionMultiplier = configNode.getNode("detectionMultiplier").getDouble(0);
        underwaterDetectionMultiplier = configNode.getNode("underwaterDetectionMultiplier").getDouble(detectionMultiplier);
        //TODO - should the default be Settings.SinkRateTicks?
        sinkRateTicks = configNode.getNode("sinkTickRate").getInt(0);
        keepMovingOnSink = configNode.getNode("keepMovingOnSink").getBoolean(false);
        smokeOnSink = configNode.getNode("smokeOnSink").getInt(0);
        smokeOnSinkQuantity = configNode.getNode("smokeOnSinkQuantity").getInt(1);
        explodeOnCrash = configNode.getNode("explodeOnCrash").getFloat(0);
        collisionExplosion = configNode.getNode("collisionExplosion").getFloat(0);
        minHeightLimit = configNode.getNode("minHeightLimit").getInt(0);
        maxHeightLimit = configNode.getNode("maxHeightLimit").getInt(255);
        maxHeightAboveGround = configNode.getNode("maxHeightAboveGround").getInt(-1);
        canDirectControl = configNode.getNode("canDirectControl").getBoolean(true);
        canHover = configNode.getNode("canHover").getBoolean(false);
        forbiddenHoverOverBlocks = configNode.getNode("forbiddenHoverOverBlocks").getValue(new TypeToken<Set<BlockType>>() {
        }, new HashSet<>());
        canHoverOverWater = configNode.getNode("canHoverOverWater").getBoolean(true);
        moveEntities = configNode.getNode("moveEntities").getBoolean(true);

        onlyMovePlayers = configNode.getNode("onlyMovePlayers").getBoolean(true);

        useGravity = configNode.getNode("useGravity").getBoolean(false);

        hoverLimit = configNode.getNode("hoverLimit").getInt(0);
        harvestBlocks = configNode.getNode("harvestBlocks").getList(TypeToken.of(BlockType.class), new ArrayList<>());
        harvesterBladeBlocks = configNode.getNode("harvesterBladeBlocks").getList(TypeToken.of(BlockType.class), new ArrayList<>());
        passthroughBlocks = configNode.getNode("passthroughBlocks").getValue(new TypeToken<Set<BlockType>>() {
        }, new HashSet<>());
        allowVerticalTakeoffAndLanding = configNode.getNode("allowVerticalTakeoffAndLanding").getBoolean(true);

        speedBlocks = configNode.getNode("speedBlocks").getValue(new TypeToken<Map<Set<BlockType>, Double>>() {
        }, new HashMap<>());
        exposedSpeedBlocks = configNode.getNode("exposedSpeedBlocks").getValue(new TypeToken<Map<Set<BlockType>, List<Double>>>() {
        }, new HashMap<>());
        ignoreMapUpdateTime = configNode.getNode("ignoreMapUpdateTime").getBoolean(false);
        targetMoveTime = configNode.getNode("targetMoveTime").getFloat(((float) maxSize) / 1000f);
        spottingMultiplier = configNode.getNode("spottingMultiplier").getFloat(0.5f);
        limitToParentHitBox = configNode.getNode("limitToParentHitBox").getBoolean(false);
        canHaveLoaders = configNode.getNode("allowLoaders").getBoolean(true);
        canHaveRepairmen = configNode.getNode("allowRepairmen").getBoolean(true);
        canHaveCrew = configNode.getNode("canHaveCrew").getBoolean(true);
        cruiseOnPilotMaxMoves = configNode.getNode("cruiseOnPilotMaxMoves").getInt(300);

        // Save config file
        //TODO - When I've got it to stop destroying tidy configs
        //configLoader.save(configNode);
    }

    private Map<List<BlockType>, List<Double>> blockTypeMapListFromNode(Map<List<BlockType>, List<String>> configMap) {
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

    public String getName() {
        return name;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public int getMinSize() {
        return minSize;
    }

    public boolean requiresSpecificPerms() {
        return requiresSpecificPerms;
    }

    public Set<BlockType> getAllowedBlocks() {
        return allowedBlocks;
    }

    public Set<BlockType> getForbiddenBlocks() {
        return forbiddenBlocks;
    }

    public Set<String> getForbiddenSignStrings() {
        return forbiddenSignStrings;
    }

    //TODO - Remove this temp method
    @Deprecated
    public boolean blockedByWater() {
        return !passthroughBlocks.contains(BlockTypes.WATER) || !passthroughBlocks.contains(BlockTypes.FLOWING_WATER);
    }

    public boolean getRequireWaterContact() {
        return requireWaterContact;
    }

    public boolean getCanCruise() {
        return canCruise;
    }

    public int getCruiseSkipBlocks() {
        return cruiseSkipBlocks;
    }

    public int getVertCruiseSkipBlocks() {
        return vertCruiseSkipBlocks;
    }

    public int maxStaticMove() {
        return maxStaticMove;
    }

    public boolean getCanTeleport() {
        return canTeleport;
    }

    public boolean getCanStaticMove() {
        return canStaticMove;
    }

    public boolean getCruiseOnPilot() {
        return cruiseOnPilot;
    }

    public Direction getCruiseOnPilotVertDirection() {
        return cruiseOnPilotVertDirection;
    }

    public boolean allowVerticalMovement() {
        return allowVerticalMovement;
    }

    public boolean rotateAtMidpoint() {
        return rotateAtMidpoint;
    }

    public boolean allowHorizontalMovement() {
        return allowHorizontalMovement;
    }

    public boolean allowRemoteSigns() {
        return allowRemoteSigns;
    }

    public boolean allowCannonDirectors() {
        return canHaveCannonDirectors;
    }

    public boolean allowAADirectors() {
        return canHaveAADirectors;
    }

    public double getFuelBurnRate() {
        return fuelBurnRate;
    }

    public Map<ItemType, Double> getFuelTypes() {
        return fuelTypes;
    }

    public double getSinkPercent() {
        return sinkPercent;
    }

    public double getOverallSinkPercent() {
        return overallSinkPercent;
    }

    public double getDetectionMultiplier() {
        return detectionMultiplier;
    }

    public double getUnderwaterDetectionMultiplier() {
        return underwaterDetectionMultiplier;
    }

    public int getSinkRateTicks() {
        return sinkRateTicks;
    }

    public boolean getKeepMovingOnSink() {
        return keepMovingOnSink;
    }

    public float getExplodeOnCrash() {
        return explodeOnCrash;
    }

    public int getSmokeOnSink() {
        return smokeOnSink;
    }

    public float getCollisionExplosion() {
        return collisionExplosion;
    }

    public int getTickCooldown() {
        return (int) Math.ceil(20 / tickCooldown);
    }

    public int getCruiseTickCooldown() {
        return cruiseTickCooldown == 0 ? getTickCooldown() : (int) Math.ceil(20 / cruiseTickCooldown);
    }

    public double getUnderwaterSpeedMultiplier() {
        return underwaterSpeedMultipler;
    }

    public boolean getFocusedExplosion() {
        return focusedExplosion;
    }

    public boolean mustBeSubcraft() {
        return mustBeSubcraft;
    }

    public Map<List<BlockType>, List<Double>> getFlyBlocks() {
        return flyBlocks;
    }

    public Map<List<BlockType>, List<Double>> getMoveBlocks() {
        return moveBlocks;
    }

    public int getMaxHeightLimit() {
        return maxHeightLimit;
    }

    public int getMinHeightLimit() {
        return minHeightLimit;
    }

    public int getMaxHeightAboveGround() {
        return maxHeightAboveGround;
    }

    public boolean getCanDirectControl() {
        return canDirectControl;
    }

    public List<BlockType> getHarvestBlocks() {
        return harvestBlocks;
    }

    public List<BlockType> getHarvesterBladeBlocks() {
        return harvesterBladeBlocks;
    }

    public Set<BlockType> getFurnaceBlocks() {
        return furnaceBlocks;
    }

    public Set<BlockType> getForbiddenHoverOverBlocks() {
        return forbiddenHoverOverBlocks;
    }

    public boolean getCanHoverOverWater() {
        return canHoverOverWater;
    }

    public boolean getMoveEntities() {
        return moveEntities;
    }

    public boolean getUseGravity() {
        return useGravity;
    }

    public boolean allowVerticalTakeoffAndLanding() {
        return allowVerticalTakeoffAndLanding;
    }

    public Set<BlockType> getPassthroughBlocks() {
        return passthroughBlocks;
    }

    public boolean onlyMovePlayers() {
        return onlyMovePlayers;
    }

    public Map<Set<BlockType>, Double> getSpeedBlocks() {
        return speedBlocks;
    }

    // TODO - Implement Usage
    public Map<Set<BlockType>, List<Double>> getExposedSpeedBlocks() {
        return exposedSpeedBlocks;
    }

    public boolean ignoreMapUpdateTime() {
        return ignoreMapUpdateTime;
    }

    public float getTargetMoveTime() {
        return targetMoveTime;
    }

    public float getSpottingMultiplier() {
        return spottingMultiplier;
    }

    public boolean limitToParentHitBox() {
        return limitToParentHitBox;
    }

    public boolean canHaveLoaders() {
        return canHaveLoaders;
    }

    public boolean canHaveRepairmen() {
        return canHaveRepairmen;
    }

    public boolean canHaveCrew() {
        return canHaveCrew;
    }

    public int getCruiseOnPilotMaxMoves() {
        return cruiseOnPilotMaxMoves;
    }

    public int getSmokeOnSinkQuantity() {
        return smokeOnSinkQuantity;
    }

    public int getHoverLimit() {
        return hoverLimit;
    }

    public boolean canHover() {
        return canHover;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

}