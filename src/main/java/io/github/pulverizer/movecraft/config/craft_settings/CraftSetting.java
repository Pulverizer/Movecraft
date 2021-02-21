package io.github.pulverizer.movecraft.config.craft_settings;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

public abstract class CraftSetting<T> {

    protected T value;

    /**
     * Loads the setting from the provided craft config.
     *
     * @param craftConfigNode a Configurate ConfigurationNode that represents the craft config
     */
    public abstract void load(ConfigurationNode craftConfigNode) throws ObjectMappingException;

    /**
     * Returns the value of the setting.
     *
     * @return the value loaded from the config
     */
    public T getValue() {
        return value;
    }
}
