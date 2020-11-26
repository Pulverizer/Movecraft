package io.github.pulverizer.movecraft.config;

import com.google.common.reflect.TypeToken;
import io.github.pulverizer.movecraft.Movecraft;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.slf4j.Logger;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

//TODO - Switch to getters / setter 'load()'
public class Settings {

    // Main Settings
    public static boolean Debug = false;
    public static String LOCALE;


    // Craft
    public static ItemType PilotTool = ItemTypes.STICK;
    public static boolean ProtectPilotedCrafts = false;
    public static boolean DisableSpillProtection = false;
    public static boolean CraftsUseNetherPortals = false;
    public static HashSet<BlockType> DisableShadowBlocks;

    //   Silhouettes
    public static int SilhouetteViewDistance = 200;
    public static int SilhouetteBlockCount = 20;

    //   Carrier Deck
    public static HashSet<BlockType> FlightDeckBlocks;

    //   Sinking
    public static double SinkRateTicks = 20.0;
    public static double SinkCheckTicks = 100.0;

    //   Wrecks
    public static int FadeWrecksAfter = 0;
    public static int FadeTickCooldown = 20;
    public static double FadePercentageOfWreckPerCycle = 10.0;
    public static Map<BlockType, Integer> ExtraFadeTimePerBlock = new HashMap<>();

    //   Repairs
    public static int RepairTicksPerBlock = 0;
    public static double RepairMaxPercent = 50;
    public static double RepairMoneyPerBlock = 0.0;


    // Crew
    public static boolean ReleaseOnCrewDeath;
    public static int CrewInviteTimeout;

    // ManOverboard
    public static int ManOverboardTimeout = 60;
    public static double ManOverboardDistance = 1000000;

    // Signs
    public static boolean EnableCrewSigns = true;
    public static HashSet<String> ForbiddenRemoteSigns;
    public static boolean RequireCreateSignPerm = false;
    public static boolean RequireNamePerm = false;
    public static int MaxRemoteSigns = -1;
    // TODO - Should we be overriding /home ?
    //public static boolean SetHomeToCrewSign = true;


    // Combat Related
    //   Armour
    public static Map<BlockType, ArrayList<Double>> DurabilityOverride;

    //   TNT
    public static int TracerRateTicks = 5;
    public static boolean TNTContactExplosives = true;

    //   Fireballs
    public static int FireballLifespan = 6;
    public static boolean FireballPenetration = true;

    //   Ammo
    public static int AmmoDetonationMultiplier;

    //   Torpedo Style Crafts
    public static int CollisionPrimer = 1000;


    static void load(ConfigurationNode mainConfigNode) {
        Logger logger = Movecraft.getInstance().getLogger();

        // Read in config

        Settings.LOCALE = mainConfigNode.getNode("Locale").getString("en");
        Settings.Debug = mainConfigNode.getNode("Debug").getBoolean(false);
        Settings.DisableSpillProtection = mainConfigNode.getNode("DisableSpillProtection").getBoolean(false);

        ItemType pilotStick = ItemTypes.AIR;

        try {
            // if the PilotTool is specified in the movecraft.cfg file, use it
            ItemType temp = mainConfigNode.getNode("PilotTool").getValue(TypeToken.of(ItemType.class));
            if (temp != null) {
                logger.info("Recognized PilotTool setting of: " + temp);
                pilotStick = temp;
            }
        } catch (ObjectMappingException e) {
            e.printStackTrace();
        }

        if (pilotStick == ItemTypes.AIR) {
            logger.info("No PilotTool setting, using default of minecraft:stick");
            Settings.PilotTool = ItemTypes.STICK;
        } else {
            Settings.PilotTool = pilotStick;
        }


        Settings.SinkCheckTicks = mainConfigNode.getNode("SinkCheckTicks").getDouble(100.0);
        Settings.TracerRateTicks = mainConfigNode.getNode("TracerRateTicks").getInt(5);
        Settings.ManOverboardTimeout = mainConfigNode.getNode("ManOverBoardTimeout").getInt(30);
        Settings.SilhouetteViewDistance = mainConfigNode.getNode("SilhouetteViewDistance").getInt(200);
        Settings.SilhouetteBlockCount = mainConfigNode.getNode("SilhouetteBlockCount").getInt(20);
        Settings.FireballLifespan = mainConfigNode.getNode("FireballLifespan").getInt(6);
        Settings.FireballPenetration = mainConfigNode.getNode("FireballPenetration").getBoolean(true);
        Settings.ProtectPilotedCrafts = mainConfigNode.getNode("ProtectPilotedCrafts").getBoolean(true);
        Settings.EnableCrewSigns = mainConfigNode.getNode("AllowCrewSigns").getBoolean(true);
        //Settings.SetHomeToCrewSign = mainConfigNode.getNode("SetHomeToCrewSign").getBoolean(true);
        Settings.RequireCreateSignPerm = mainConfigNode.getNode("RequireCreatePerm").getBoolean(false);
        Settings.TNTContactExplosives = mainConfigNode.getNode("TNTContactExplosives").getBoolean(true);
        Settings.FadeWrecksAfter = mainConfigNode.getNode("FadeWrecksAfter").getInt(0);
        Settings.ReleaseOnCrewDeath = mainConfigNode.getNode("ReleaseOnCrewDeath").getBoolean(true);
        Settings.DurabilityOverride = new HashMap<>();

        try {
            Map<BlockType, ArrayList<Double>> tempMap =
                    mainConfigNode.getNode("DurabilityOverride").getValue(new TypeToken<Map<BlockType, ArrayList<Double>>>() {
                    });

            if (tempMap != null) {
                tempMap.forEach((blockType, doubles) -> {
                    if (doubles.size() >= 3) {
                        Settings.DurabilityOverride.put(blockType, doubles);
                    }
                });
            }

        } catch (ObjectMappingException e) {
            e.printStackTrace();
        }

        try {
            Settings.DisableShadowBlocks =
                    new HashSet<>(mainConfigNode.getNode("DisableShadowBlocks").getList(TypeToken.of(BlockType.class)));  //REMOVE FOR PUBLIC VERSION
        } catch (ObjectMappingException e) {
            e.printStackTrace();

            Settings.DisableShadowBlocks = new HashSet<>();
        }

        Settings.CrewInviteTimeout = mainConfigNode.getNode("InviteTimeout").getInt(60 * 20); // default = 1 minute

        try {
            Settings.FlightDeckBlocks = new HashSet<>(mainConfigNode.getNode("FlightDeckBlocks").getList(TypeToken.of(BlockType.class)));
        } catch (ObjectMappingException e) {
            e.printStackTrace();

            Settings.FlightDeckBlocks = new HashSet<>();
        }

        Settings.AmmoDetonationMultiplier = mainConfigNode.getNode("AmmoDetonationMultiplier").getInt(0);


        // TODO: Re-enable this?
        /*
        for (BlockType typ : Settings.DisableShadowBlocks) {
            worldHandler.disableShadow(typ);
        }
        */
    }
}