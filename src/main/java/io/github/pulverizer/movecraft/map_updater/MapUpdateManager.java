package io.github.pulverizer.movecraft.map_updater;

import io.github.pulverizer.movecraft.config.Settings;
import io.github.pulverizer.movecraft.map_updater.update.UpdateCommand;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.text.Text;

import java.util.Collection;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MapUpdateManager implements Runnable {

    private static MapUpdateManager ourInstance;

    private final Queue<UpdateCommand> updates = new ConcurrentLinkedQueue<>();

    private MapUpdateManager() {
    }

    public static MapUpdateManager getInstance() {
        if (ourInstance == null) {
            initialize();
        }

        return ourInstance;
    }

    private static void initialize() {
        ourInstance = new MapUpdateManager();
    }

    public void run() {
        if (updates.isEmpty()) {
            return;
        }

        long startTime = System.currentTimeMillis();
        // and set all crafts that were updated to not processing

        updates.forEach(update -> {
            update.doUpdate();
            updates.remove(update);
        });

        if (Settings.Debug) {
            long endTime = System.currentTimeMillis();
            Sponge.getServer().getBroadcastChannel().send(Text.of("Map update took (ms): " + (endTime - startTime)));
        }
    }


    public void scheduleUpdate(UpdateCommand update) {
        updates.add(update);
    }

    public void scheduleUpdates(UpdateCommand... updates) {
        Collections.addAll(this.updates, updates);
    }

    public void scheduleUpdates(Collection<UpdateCommand> updates) {
        this.updates.addAll(updates);
    }

}