package io.github.pulverizer.movecraft.async;

import io.github.pulverizer.movecraft.Movecraft;
import io.github.pulverizer.movecraft.config.Settings;
import io.github.pulverizer.movecraft.craft.Craft;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;

import java.util.Optional;

public abstract class AsyncTask {

    protected final Craft craft;
    protected final String type;

    // Task Failed?
    private boolean failed;
    private String failMessage;

    public AsyncTask(Craft craft, String taskType) {
        this.craft = craft;
        type = taskType;
    }

    public void run() {

        String taskName = "Movecraft - " + type + " Task - " + craft.getId();

        Task.builder()
                .async()
                .name(taskName)
                .execute(this::task)
                .submit(Movecraft.getInstance());

        if (Settings.Debug) {
            Movecraft.getInstance().getLogger().info(taskName);
        }

    }

    private void task() {
        try {
            long startTime = System.currentTimeMillis();

            execute();

            long endTime = System.currentTimeMillis();
            if (Settings.Debug) {
                Movecraft.getInstance().getLogger().info(type + " Task Took: " + (endTime - startTime) + "ms");
            }

            // Only submit Task if it hasn't failed
            if (!failed()) {
                AsyncManager.getInstance().submitCompletedTask(this);
            }
        } catch (Exception e) {
            Movecraft.getInstance().getLogger().error("Internal Error - Processor thread encountered an error!");
            e.printStackTrace();
        }
    }

    protected abstract void execute() throws InterruptedException;

    protected abstract void postProcess();

    protected abstract Optional<Player> getNotificationPlayer();

    protected void fail(String message) {
        failed = true;
        failMessage = message;

        getNotificationPlayer().ifPresent(player -> player.sendMessage(Text.of(type + " Failed: " + message)));

        Movecraft.getInstance().getLogger().info("Craft " + type + " Failed: " + getFailMessage());
    }

    public boolean failed() {
        return failed;
    }

    public String getFailMessage() {
        return failMessage;
    }

    public Craft getCraft() {
        return craft;
    }
}