package io.github.pulverizer.movecraft.config;

import io.github.pulverizer.movecraft.Movecraft;
import io.github.pulverizer.movecraft.config.craft_settings.CraftSetting;
import io.github.pulverizer.movecraft.config.craft_settings.Defaults;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.util.Direction;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@SuppressWarnings("OptionalGetWithoutIsPresent") //TODO - Un-suppress
final public class CraftType {


    @SuppressWarnings("rawtypes") // Raw use of CraftSetting
    private final static HashSet<Class<? extends CraftSetting>> REGISTERED_SETTINGS = new HashSet<>();
    @SuppressWarnings("rawtypes") // Raw use of CraftSetting
    private final HashMap<Class<? extends CraftSetting>, CraftSetting> settings = new HashMap<>();

    @SuppressWarnings("rawtypes") // Raw use of CraftSetting
    public CraftType(Path file) throws IOException, IllegalAccessException, InstantiationException, ObjectMappingException {

        ConfigurationLoader<ConfigurationNode> configLoader = ConfigManager.createConfigLoader(file);
        ConfigurationNode craftConfigNode = configLoader.load();

        // Load config file

        for (Class<? extends CraftSetting> setting : REGISTERED_SETTINGS) {
            settings.put(setting, setting.newInstance());
            settings.get(setting).load(craftConfigNode);
        }
    }

    /**
     * Adds the provided setting to be loaded from each craft config on config load.
     *
     * @param craftSetting a setting that crafts may have
     *
     * @return true if the setting was not already registered
     */
    @SuppressWarnings("rawtypes") // Raw use of CraftSetting
    public static boolean registerSetting(Class<? extends CraftSetting> craftSetting) {
        return REGISTERED_SETTINGS.add(craftSetting);
    }

    /**
     * Returns true if the specified setting is registered for loading.
     * More formally, returns true if and only if this set contains an element e such that (o==null ? e==null : o.equals(e)).
     *
     * @param craftSetting a setting that crafts may have
     *
     * @return true if the specified setting is registered for loading
     */
    @SuppressWarnings("rawtypes") // Raw use of CraftSetting
    public static boolean isSettingRegistered(Class<? extends CraftSetting> craftSetting) {
        return REGISTERED_SETTINGS.contains(craftSetting);
    }

    /**
     * Removes the provided setting from being loaded from each craft config on config load.
     *
     * @param craftSetting a setting that crafts may have
     *
     * @return true if the setting was registered prior to calling this method
     */
    @SuppressWarnings({"rawtypes"}) // Raw use of CraftSetting
    public static boolean unregisterSetting(Class<? extends CraftSetting> craftSetting) {
        return REGISTERED_SETTINGS.remove(craftSetting);
    }

    //TODO - JavaDoc
    @SuppressWarnings({"unchecked", "rawtypes"}) // Unchecked cast to T - Raw use of CraftSetting
    public <T extends CraftSetting> Optional<T> getSetting(Class<T> craftSetting) {
        return Optional.ofNullable((T) settings.get(craftSetting));
    }

    @Override
    public int hashCode() {
        return getSetting(Defaults.Name.class).get().getValue().hashCode();
    }
}