package io.github.pulverizer.movecraft;

import com.google.inject.Inject;
import io.github.pulverizer.movecraft.commands.*;
import io.github.pulverizer.movecraft.config.ConfigManager;
import io.github.pulverizer.movecraft.craft.crew.CrewManager;
import io.github.pulverizer.movecraft.listener.*;
import io.github.pulverizer.movecraft.sign.*;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.*;
import org.spongepowered.api.plugin.Plugin;

import io.github.pulverizer.movecraft.async.AsyncManager;
//import io.github.pulverizer.movecraft.commands.*;
import io.github.pulverizer.movecraft.config.Settings;
import io.github.pulverizer.movecraft.craft.CraftManager;
import io.github.pulverizer.movecraft.map_updater.MapUpdateManager;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.sql.SqlService;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

@Plugin(
        id = "movecraft",
        name = "Movecraft for Sponge",
        description = "Allows players to create moving things out of blocks. Airships, Turrets, Submarines, Etc.",
        version = "0.4.0",
        url = "https://github.com/Pulverizer/Movecraft-for-Sponge",
        authors = {"BernardisGood", "https://github.com/Pulverizer/Movecraft-for-Sponge/graphs/contributors"})

public class Movecraft {

    private static Movecraft instance;

    private SqlService sql;
    private final String databaseSettings = "jdbc:h2:";
    private final String databaseName = "/movecraft.db";

    @Inject
    @ConfigDir(sharedRoot = false)
    private Path configDir;


    /**
     * Fetches the PATH of the config directory.
     * @return The PATH of the config directory.
     */
    public Path getConfigDir() {
        return configDir;
    }

    @Inject
    private Logger logger;

    /**
     * Fetches the Logger for this Plugin.
     * @return This Plugin's logger.
     */
    public Logger getLogger() {
        return this.logger;
    }

    /**
     * Fetches this Plugin's instance.
     * @return The instance of this Plugin.
     */
    public static synchronized Movecraft getInstance() {
        return instance;
    }

    public Connection connectToSQL() {
        if (sql == null) {
            sql = Sponge.getServiceManager().provide(SqlService.class).get();
        }

        try {
            return sql.getDataSource(databaseSettings + configDir.toString() + databaseName).getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Listener for GamePreInitializationEvent. Loads the Plugin's settings.
     * @param event GamePreInitializationEvent from Listener.
     */
    @Listener
    public void onLoad(GamePreInitializationEvent event) {

        instance = this;
        logger = getLogger();

        ConfigManager.checkSpongeConfig();
        ConfigManager.loadMainConfig();

        //TODO: Re-add commands!

        /*this.getCommand("movecraft").setExecutor(new MovecraftCommand());
        this.getCommand("release").setExecutor(new ReleaseCommand());
        this.getCommand("pilot").setExecutor(new PilotCommand());
        this.getCommand("add").setExecutor(new RotateCommand());
        this.getCommand("cruise").setExecutor(new CruiseCommand());
        this.getCommand("manoverboard").setExecutor(new ManOverboardCommand());
        this.getCommand("scuttle").setExecutor(new ScuttleCommand());*/

        CraftReportCommand.register();
        CrewCommand.register();
        CraftTypesCommand.register();
        DockCommand.register();
        ContactsCommand.register();


        Sponge.getEventManager().registerListeners(this, new InteractListener());
        Sponge.getEventManager().registerListeners(this, new BlockListener());
        Sponge.getEventManager().registerListeners(this, new PlayerListener());
        Sponge.getEventManager().registerListeners(this, new SignListener());
        Sponge.getEventManager().registerListeners(this, new CrewSign());
        Sponge.getEventManager().registerListeners(this, new CommanderSign());
        Sponge.getEventManager().registerListeners(this, new TNTListener());
        Sponge.getEventManager().registerListeners(this, new FireballListener());

        logger.info("Movecraft Enabled.");
    }

    /**
     * <pre>
     * Listener for GameStartedServerEvent. Loads the Plugin's various content managers and databases.
     *
     * Commander Sign Database
     * Crew Sign Database
     * </pre>
     * @param event GameStartedServerEvent from Listener.
     */
    @Listener
    public void initializeManagers(GameStartedServerEvent event) {

        CommanderSign.initDatabase();
        if (Settings.EnableCrewSigns) {
            CrewSign.initDatabase();
        }

        // Startup procedure
        AsyncManager asyncManager = AsyncManager.getInstance();
        MapUpdateManager mapUpdateManager = MapUpdateManager.getInstance();
        Task.builder()
                .execute(asyncManager)
                .intervalTicks(1)
                .submit(this);
        Task.builder()
                .execute(mapUpdateManager)
                .intervalTicks(1)
                .submit(this);
    }

}
