package io.github.pulverizer.movecraft.async;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.Sets;
import io.github.pulverizer.movecraft.config.Settings;
import io.github.pulverizer.movecraft.config.craft_settings.Defaults;
import io.github.pulverizer.movecraft.craft.Craft;
import io.github.pulverizer.movecraft.craft.CraftManager;
import io.github.pulverizer.movecraft.enums.DirectControlMode;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.entity.PlayerInventory;
import org.spongepowered.api.util.Direction;

import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AsyncManager implements Runnable {

    private static AsyncManager ourInstance;
    private final BlockingQueue<AsyncTask> taskQueue = new LinkedBlockingQueue<>();
    private final HashSet<Craft> clearanceSet = new HashSet<>();
    private long lastFadeCheck = 0;
    private int lastContactCheck = 0;

    public static AsyncManager getInstance() {
        if (ourInstance == null) {
            initialize();
        }

        return ourInstance;
    }

    private static void initialize() {
        ourInstance = new AsyncManager();
    }

    public void submitCompletedTask(AsyncTask task) {
        taskQueue.add(task);
    }

    private void processAlgorithmQueue() {
        int runLength = 10;
        int queueLength = taskQueue.size();

        runLength = Math.min(runLength, queueLength);

        for (int i = 0; i < runLength; i++) {
            AsyncTask poll = taskQueue.poll();

            if (poll != null) {
                poll.postProcess();
            }
        }
    }

    private void processCruise() {
        for (Craft craft : CraftManager.getInstance()) {
            if (craft == null || craft.isProcessing() || !craft.isCruising() || craft.isSinking()) {
                continue;
            }

            if (!craft.hasCooldownExpired()) {
                continue;
            }

            if (craft.getType().getSetting(Defaults.CruiseOnPilot.class).get().getValue() && craft.getType().getSetting(Defaults.CruiseOnPilotMaxMoves.class).get().getValue() > 0
                    && craft.getType().getSetting(Defaults.CruiseOnPilotMaxMoves.class).get().getValue() <= craft.getNumberOfMoves()) {
                craft.sink();
            }

            // check direct controls to modify movement
            boolean bankLeft = false;
            boolean bankRight = false;
            boolean dive = false;
            if (craft.getDirectControlMode().equals(DirectControlMode.B)) {

                Player pilot = Sponge.getServer().getPlayer(craft.getPilot()).get();
                if (pilot.get(Keys.IS_SNEAKING).get()) {
                    dive = true;
                }
                if (((PlayerInventory) pilot.getInventory()).getHotbar().getSelectedSlotIndex() == 3) {
                    bankLeft = true;
                }
                if (((PlayerInventory) pilot.getInventory()).getHotbar().getSelectedSlotIndex() == 5) {
                    bankRight = true;
                }
            }

            int dx = 0;
            int dz = 0;
            int dy = 0;

            // ascend
            if (craft.getVerticalCruiseDirection() == Direction.UP) {
                if (craft.getHorizontalCruiseDirection() != Direction.NONE) {
                    dy = (1 + craft.getType().getSetting(Defaults.VertCruiseSkipBlocks.class).get().getValue()) >> 1;
                } else {
                    dy = 1 + craft.getType().getSetting(Defaults.VertCruiseSkipBlocks.class).get().getValue();
                }
            }
            // descend
            if (craft.getVerticalCruiseDirection() == Direction.DOWN) {
                if (craft.getHorizontalCruiseDirection() != Direction.NONE) {
                    dy = (-1 - craft.getType().getSetting(Defaults.VertCruiseSkipBlocks.class).get().getValue()) >> 1;
                } else {
                    dy = -1 - craft.getType().getSetting(Defaults.VertCruiseSkipBlocks.class).get().getValue();
                }
            } else if (dive) {
                dy = -((craft.getType().getSetting(Defaults.CruiseSkipBlocks.class).get().getValue() + 1) >> 1);
                if (craft.getHitBox().getMinY() <= craft.getWorld().getSeaLevel()) {
                    dy = -1;
                }
            }

            switch (craft.getHorizontalCruiseDirection()) {
                case WEST:
                    dx = 1 + craft.getType().getSetting(Defaults.CruiseSkipBlocks.class).get().getValue();
                    if (bankRight) {
                        dz = (-1 - craft.getType().getSetting(Defaults.CruiseSkipBlocks.class).get().getValue()) >> 1;
                    }

                    if (bankLeft) {
                        dz = (1 + craft.getType().getSetting(Defaults.CruiseSkipBlocks.class).get().getValue()) >> 1;
                    }

                    break;

                case EAST:
                    dx = -1 - craft.getType().getSetting(Defaults.CruiseSkipBlocks.class).get().getValue();
                    if (bankRight) {
                        dz = (1 + craft.getType().getSetting(Defaults.CruiseSkipBlocks.class).get().getValue()) >> 1;
                    }

                    if (bankLeft) {
                        dz = (-1 - craft.getType().getSetting(Defaults.CruiseSkipBlocks.class).get().getValue()) >> 1;
                    }

                    break;

                case NORTH:
                    dz = 1 + craft.getType().getSetting(Defaults.CruiseSkipBlocks.class).get().getValue();
                    if (bankRight) {
                        dx = (-1 - craft.getType().getSetting(Defaults.CruiseSkipBlocks.class).get().getValue()) >> 1;
                    }

                    if (bankLeft) {
                        dx = (1 + craft.getType().getSetting(Defaults.CruiseSkipBlocks.class).get().getValue()) >> 1;
                    }

                    break;

                case SOUTH:
                    dz = -1 - craft.getType().getSetting(Defaults.CruiseSkipBlocks.class).get().getValue();
                    if (bankRight) {
                        dx = (1 + craft.getType().getSetting(Defaults.CruiseSkipBlocks.class).get().getValue()) >> 1;
                    }

                    if (bankLeft) {
                        dx = (-1 - craft.getType().getSetting(Defaults.CruiseSkipBlocks.class).get().getValue()) >> 1;
                    }

                    break;
            }

            craft.translate(new Vector3i(dx, dy, dz));
        }
    }

    private void detectSinking() {
        HashSet<Craft> crafts = Sets.newHashSet(CraftManager.getInstance());
        crafts.forEach(craft -> {

            if (Sponge.getServer().getRunningTimeTicks() - craft.getLastCheckTick() <= Settings.SinkCheckTicks) {
                return;
            }

            craft.runChecks();
        });
    }

    //Controls sinking crafts
    private void processSinking() {
        //copy the crafts before iteration to prevent concurrent modifications
        HashSet<Craft> crafts = Sets.newHashSet(CraftManager.getInstance());
        crafts.forEach(craft -> {
            if (craft == null || !craft.isSinking()) {
                return;
            }

            if (craft.getHitBox().isEmpty() || craft.getHitBox().getMinY() < 5) {
                craft.release(null);
                return;
            }

            long ticksElapsed = Sponge.getServer().getRunningTimeTicks() - craft.getLastMoveTick();
            if (Math.abs(ticksElapsed) < craft.getType().getSetting(Defaults.SinkRateTicks.class).get().getValue()) {
                return;
            }

            int dx = 0;
            int dz = 0;
            if (craft.getType().getSetting(Defaults.KeepMovingOnSink.class).get().getValue()) {
                dx = craft.getLastMoveVector().getX();
                dz = craft.getLastMoveVector().getZ();
            }

            craft.translate(new Vector3i(dx, -1, dz));
        });
    }

    private void processDetection() {
        long ticksElapsed = (Sponge.getServer().getRunningTimeTicks() - lastContactCheck);

        if (ticksElapsed < 22) {
            return;
        }

        CraftManager.getInstance().forEach(Craft::runContacts);

        lastContactCheck = Sponge.getServer().getRunningTimeTicks();
    }

    public void run() {

        processCruise();
        detectSinking();
        processSinking();
        processDetection();
        processAlgorithmQueue();

        // Cleanup crafts that are bugged and have not moved in the past 60 seconds, but have no crew or are still processing.
        for (Craft craft : CraftManager.getInstance()) {

            if (craft.crewIsEmpty() && craft.getLastMoveTick() < Sponge.getServer().getRunningTimeTicks() - 1200) {
                CraftManager.getInstance().forceRemoveCraft(craft);
            }

            // Stop crafts from moving if they have taken too long to process.
            if (craft.isProcessing() && craft.isCruising() && craft.getProcessingStartTime() < Sponge.getServer().getRunningTimeTicks() - 1200) {
                craft.setProcessing(false);
            }
        }
    }
}