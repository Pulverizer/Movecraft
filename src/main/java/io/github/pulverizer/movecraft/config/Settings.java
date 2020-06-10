package io.github.pulverizer.movecraft.config;

import com.google.common.reflect.TypeToken;
import io.github.pulverizer.movecraft.Movecraft;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.slf4j.Logger;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Settings {
    public static boolean Debug = false;
    public static String LOCALE;
    public static ItemType PilotTool = ItemTypes.STICK;
    public static int SilhouetteViewDistance = 200;
    public static int SilhouetteBlockCount = 20;
    public static double SinkRateTicks = 20.0;
    public static double SinkCheckTicks = 100.0;
    public static double TracerRateTicks = 5.0;
    public static boolean ProtectPilotedCrafts = false;
    public static boolean DisableSpillProtection = false;
    public static boolean RequireCreateSignPerm = false;
    public static boolean TNTContactExplosives = true;
    public static int FadeWrecksAfter = 0;
    public static int ManOverBoardTimeout = 60;
    public static int FireballLifespan = 6;
    public static int RepairTicksPerBlock = 0;
    public static int BlockQueueChunkSize = 1000;
    public static double RepairMoneyPerBlock = 0.0;
    public static boolean FireballPenetration = true;
    public static boolean EnableCrewSigns = true;
    // TODO - Should we be overriding /home ?
    //public static boolean SetHomeToCrewSign = true;
    public static Map<BlockType, Integer> DurabilityOverride;
    public static HashSet<BlockType> DisableShadowBlocks;
    public static boolean ReleaseOnCrewDeath;
    public static int InviteTimeout;
    public static int AmmoDetonationMultiplier;


    static void load(ConfigurationNode mainConfigNode) {
        Logger logger = Movecraft.getInstance().getLogger();

        // Read in config

        Settings.LOCALE = mainConfigNode.getNode("Locale").getString("en");
        Settings.Debug = mainConfigNode.getNode("Debug").getBoolean(false);
        Settings.DisableSpillProtection = mainConfigNode.getNode("DisableSpillProtection").getBoolean(false);

        ItemType pilotStick = ItemTypes.AIR;

        try {
            // if the PilotTool is specified in the movecraft.cfg file, use it
            if (mainConfigNode.getNode("PilotTool").getValue(TypeToken.of(ItemType.class)) != null) {
                logger.info("Recognized PilotTool setting of: " + mainConfigNode.getNode("PilotTool").getValue(TypeToken.of(ItemType.class)));
                pilotStick = mainConfigNode.getNode("PilotTool").getValue(TypeToken.of(ItemType.class));
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
        Settings.TracerRateTicks = mainConfigNode.getNode("TracerRateTicks").getDouble(5.0);
        Settings.ManOverBoardTimeout = mainConfigNode.getNode("ManOverBoardTimeout").getInt(30);
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
            Map<BlockType, Integer> tempMap = mainConfigNode.getNode("DurabilityOverride").getValue(new TypeToken<Map<BlockType, Integer>>() {});
            if (tempMap != null)
                for (Object blockType : tempMap.keySet().toArray())
                    Settings.DurabilityOverride.put((BlockType) blockType, tempMap.get(blockType));

        } catch (ObjectMappingException e) {
            e.printStackTrace();
        }

        try {
            Settings.DisableShadowBlocks = new HashSet<>(mainConfigNode.getNode("DisableShadowBlocks").getList(TypeToken.of(BlockType.class)));  //REMOVE FOR PUBLIC VERSION
        } catch (ObjectMappingException e) {
            e.printStackTrace();

            Settings.DisableShadowBlocks = new HashSet<>();
        }

        Settings.InviteTimeout = mainConfigNode.getNode("InviteTimeout").getInt(60*20); // default = 1 minute
        Settings.AmmoDetonationMultiplier = mainConfigNode.getNode("AmmoDetonationMultiplier").getInt(0);


        /* TODO: Re-enable this?
        if (!Settings.CompatibilityMode) {
            for (BlockType typ : Settings.DisableShadowBlocks) {
                worldHandler.disableShadow(typ);
            }
        }
        */
    }
}