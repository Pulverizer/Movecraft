package io.github.pulverizer.movecraft.config;

import io.github.pulverizer.movecraft.Movecraft;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.yaml.snakeyaml.DumperOptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;

public class ConfigManager {

    private ConfigManager() {}

    public static ConfigurationLoader<ConfigurationNode> createConfigLoader(Path file) {
        return YAMLConfigurationLoader.builder().setPath(file).setDefaultOptions(ConfigurationOptions.defaults()).build();
    }

    public static void loadMainConfig() {

        Path mainConfigPath = Movecraft.getInstance().getConfigDir().resolve("movecraft.cfg");

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

        File craftsFile = Movecraft.getInstance().getConfigDir().resolve("types").toFile();

        HashSet<CraftType> craftTypes = new HashSet<>();
        File[] files = craftsFile.listFiles();
        if (files == null) {
            Movecraft.getInstance().getLogger().error("No CraftTypes Found!");
            return craftTypes;
        }

        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".craft")) {
                Movecraft.getInstance().getLogger().info("Loading CraftType: " + file.getName());

                try {
                    CraftType type = new CraftType(file);
                    craftTypes.add(type);

                } catch (Exception e) {
                    Movecraft.getInstance().getLogger().error("Error when loading CraftType: " + file.getName());
                    e.printStackTrace();
                }
            }
        }

        Movecraft.getInstance().getLogger().info("Loaded " + craftTypes.size() + " CraftTypes.");
        return craftTypes;
    }
}