package io.github.pulverizer.movecraft.config;

import io.github.pulverizer.movecraft.Movecraft;
import io.github.pulverizer.movecraft.config.craft_settings.CraftSetting;
import io.github.pulverizer.movecraft.config.craft_settings.Defaults;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.spongepowered.api.Sponge;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.config.category.EntityActivationModCategory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;

public abstract class ConfigManager {

    public static ConfigurationLoader<ConfigurationNode> createConfigLoader(Path file) {
        return YAMLConfigurationLoader.builder().setPath(file).setDefaultOptions(ConfigurationOptions.defaults()).build();
    }

    //TODO - Improve this
    public static void checkSpongeConfig() {
        if (!SpongeImpl.getGlobalConfigAdapter().getConfig().getOptimizations().usePandaRedstone()) {
            Movecraft.getInstance().getLogger().warn("Panda redstone patch not applied! Please edit your Sponge config.");
        }
        if (SpongeImpl.getGlobalConfigAdapter().getConfig().getEntity().getMaxSpeed() < 1200) {
            Movecraft.getInstance().getLogger().warn("Max entity speed is less than 1200! Please edit your Sponge config.");
        }

        for (Map.Entry<String, Integer> entry : SpongeImpl.getGlobalConfigAdapter().getConfig().getEntityActivationRange().getDefaultRanges()
                .entrySet()) {
            if (entry.getValue() < 256) {
                Movecraft.getInstance().getLogger().warn("An entity activation range is less than 256! Please edit your Sponge config.");
                break;
            }
        }

        for (Map.Entry<String, EntityActivationModCategory> mod :
                SpongeImpl.getGlobalConfigAdapter().getConfig().getEntityActivationRange().getModList().entrySet()) {
            for (Map.Entry<String, Integer> entity : mod.getValue().getEntityList().entrySet()) {
                if (entity.getValue() < 256) {
                    Movecraft.getInstance().getLogger()
                            .warn("A mod entity activation range is less than 256! Please edit your Sponge config.");
                    break;
                }
            }
        }
    }

    public static void loadMainConfig() {

        Path mainConfigPath = Movecraft.getInstance().getConfigDir().resolve("movecraft.cfg");

        if (!mainConfigPath.toFile().exists()) {
            Movecraft.getInstance().getLogger().info("Main config missing! Generating default config...");

            try {
                Sponge.getAssetManager().getAsset(Movecraft.getInstance(), "movecraft.cfg").get().copyToFile(mainConfigPath);

            } catch (Exception e) {
                Movecraft.getInstance().getLogger().warn("Failed to copy default main config.");
                e.printStackTrace();
            }
        }

        ConfigurationLoader<ConfigurationNode> mainConfigLoader = createConfigLoader(mainConfigPath);
        ConfigurationNode mainConfigNode;

        try {
            mainConfigNode = mainConfigLoader.load();

            Settings.load(mainConfigNode);

            //TODO - Re-add when it doesn't break tidy configs
            //mainConfigLoader.save(mainConfigNode);
        } catch (IOException error) {
            Movecraft.getInstance().getLogger().error("Error loading main config!");
            error.printStackTrace();
        }
    }

    public static HashSet<CraftType> loadCraftTypes() {

        Path typesDir = Movecraft.getInstance().getConfigDir().resolve("types");

        HashSet<CraftType> craftTypes = new HashSet<>();
        File[] files = typesDir.toFile().listFiles();
        if (files == null || files.length == 0) {
            Movecraft.getInstance().getLogger().info("No craft configs found! Generating defaults...");
            try {
                Sponge.getAssetManager().getAsset(Movecraft.getInstance(), "types/Airship.craft").get().copyToDirectory(typesDir);
                Sponge.getAssetManager().getAsset(Movecraft.getInstance(), "types/Airskiff.craft").get().copyToDirectory(typesDir);
                Sponge.getAssetManager().getAsset(Movecraft.getInstance(), "types/BigAirskiff.craft").get().copyToDirectory(typesDir);
                Sponge.getAssetManager().getAsset(Movecraft.getInstance(), "types/Bomb.craft").get().copyToDirectory(typesDir);
                Sponge.getAssetManager().getAsset(Movecraft.getInstance(), "types/elevator.craft").get().copyToDirectory(typesDir);
                Sponge.getAssetManager().getAsset(Movecraft.getInstance(), "types/GroundVehicle.craft").get().copyToDirectory(typesDir);
                Sponge.getAssetManager().getAsset(Movecraft.getInstance(), "types/Ship.craft").get().copyToDirectory(typesDir);
                Sponge.getAssetManager().getAsset(Movecraft.getInstance(), "types/SubAirship.craft").get().copyToDirectory(typesDir);
                Sponge.getAssetManager().getAsset(Movecraft.getInstance(), "types/SubAirskiff.craft").get().copyToDirectory(typesDir);
                Sponge.getAssetManager().getAsset(Movecraft.getInstance(), "types/SubBigAirskiff.craft").get().copyToDirectory(typesDir);
                Sponge.getAssetManager().getAsset(Movecraft.getInstance(), "types/SubTurret.craft").get().copyToDirectory(typesDir);
                Sponge.getAssetManager().getAsset(Movecraft.getInstance(), "types/Torpedo.craft").get().copyToDirectory(typesDir);
                Sponge.getAssetManager().getAsset(Movecraft.getInstance(), "types/Turret.craft").get().copyToDirectory(typesDir);

            } catch (Exception e) {
                Movecraft.getInstance().getLogger().warn("Failed to copy default craft configs.");
                e.printStackTrace();
            }
        }

        files = typesDir.toFile().listFiles();

        if (files == null) {
            return craftTypes;
        }

        registerDefaultCraftSettings();

        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".craft")) {
                Movecraft.getInstance().getLogger().info("Loading craft config: " + file.getName());

                try {
                    CraftType type = new CraftType(file.toPath());
                    craftTypes.add(type);

                } catch (Exception e) {
                    Movecraft.getInstance().getLogger().error("Error when loading craft config: " + file.getName());
                    e.printStackTrace();
                }
            }
        }

        Movecraft.getInstance().getLogger().info("Loaded " + craftTypes.size() + " craft configs.");
        return craftTypes;
    }

    private static void registerDefaultCraftSettings() {
        for (Class<?> setting : Defaults.class.getDeclaredClasses()) {
            CraftType.registerSetting((Class<? extends CraftSetting>) setting);
        }
    }
}