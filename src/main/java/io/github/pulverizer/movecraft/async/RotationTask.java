package io.github.pulverizer.movecraft.async;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import io.github.pulverizer.movecraft.Movecraft;
import io.github.pulverizer.movecraft.config.Settings;
import io.github.pulverizer.movecraft.craft.Craft;
import io.github.pulverizer.movecraft.craft.CraftManager;
import io.github.pulverizer.movecraft.enums.Rotation;
import io.github.pulverizer.movecraft.event.CraftRotateEvent;
import io.github.pulverizer.movecraft.map_updater.MapUpdateManager;
import io.github.pulverizer.movecraft.map_updater.update.CraftRotateCommand;
import io.github.pulverizer.movecraft.map_updater.update.EntityUpdateCommand;
import io.github.pulverizer.movecraft.map_updater.update.UpdateCommand;
import io.github.pulverizer.movecraft.utils.HashHitBox;
import io.github.pulverizer.movecraft.utils.MathUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.AABB;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class RotationTask extends AsyncTask {

    private final Vector3i originPoint;
    private final Rotation rotation;
    private final World world;
    private final Set<UpdateCommand> updates = new HashSet<>();

    private final HashHitBox oldHitBox;
    private final HashHitBox newHitBox;

    public RotationTask(Craft craft, Vector3i originPoint, Rotation rotation, World world) {
        super(craft, "Rotation");
        this.originPoint = originPoint;
        this.rotation = rotation;
        this.world = world;
        this.newHitBox = new HashHitBox();
        this.oldHitBox = new HashHitBox(craft.getHitBox());
    }

    @Override
    protected void execute() throws InterruptedException {

        // Check if task is valid
        if (oldHitBox.isEmpty() || craft.isDisabled()) {
            return;
        }

        // Use some fuel if needed
        double fuelBurnRate = getCraft().getType().getFuelBurnRate();
        if (fuelBurnRate > 0 && !getCraft().isSinking()) {
            if (!getCraft().useFuel(fuelBurnRate)) {
                fail("Craft out of fuel");
                return;
            }
        }

        // Check for obstruction
        checkCraftObstructed();

        // Check if failed
        if (failed()) {
            return;
        }

        // Call event
        CraftRotateEvent event = new CraftRotateEvent(craft, oldHitBox, newHitBox);
        Sponge.getEventManager().post(event);
        if (event.isCancelled()) {
            fail(event.getFailMessage());
            return;
        }

        // Add rotation to updates
        updates.add(new CraftRotateCommand(getCraft(), originPoint, rotation, newHitBox));

        //add entities in the craft
        moveEntities();

        // if you rotated a subcraft, update the parent with the new blocks
        if (craft.isSubCraft()) {
            // also find the furthest extent from center and notify the player of the new direction
            notifyPlayerOfNewDirection();

            Set<Craft> craftsInWorld = CraftManager.getInstance().getCraftsInWorld(getCraft().getWorld());
            for (Craft craft : craftsInWorld) {
                if (oldHitBox.intersects(craft.getHitBox()) && craft != getCraft()) {
                    craft.getHitBox().removeAll(oldHitBox);
                    craft.getHitBox().addAll(newHitBox);
                    break;
                }
            }
        }
    }

    private void moveEntities() throws InterruptedException {
        final Vector3d tOP = originPoint.toDouble().add(0.5, 0, 0.5);

        //prevents torpedo and rocket passengers
        if (craft.getType().getMoveEntities() && !craft.isSinking()) {

            if (Settings.Debug) {
                Movecraft.getInstance().getLogger().info("Craft moves Entities.");
            }

            AtomicBoolean processedEntities = new AtomicBoolean(false);

            Task.builder()
                    .execute(() -> {
                        for (Entity entity : craft.getWorld().getIntersectingEntities(
                                new AABB(oldHitBox.getMinX() - 0.5, oldHitBox.getMinY() - 0.5, oldHitBox.getMinZ() - 0.5, oldHitBox.getMaxX() + 1.5,
                                        oldHitBox.getMaxY() + 1.5, oldHitBox.getMaxZ() + 1.5))) {

                            if (entity.getType() == EntityTypes.PLAYER || entity.getType() == EntityTypes.PRIMED_TNT
                                    || entity.getType() == EntityTypes.ITEM || !craft.getType().onlyMovePlayers()) {

                                Location<World> adjustedPLoc = entity.getLocation().sub(tOP);

                                double[] rotatedCoords = MathUtils.rotateVecNoRound(rotation, adjustedPLoc.getX(), adjustedPLoc.getZ());
                                float newYaw = rotation == Rotation.CLOCKWISE ? 90F : -90F;
                                EntityUpdateCommand eUp = new EntityUpdateCommand(entity,
                                        new Vector3d(rotatedCoords[0] + tOP.getX(), entity.getLocation().getY(), rotatedCoords[1] + tOP.getZ()),
                                        newYaw);
                                updates.add(eUp);

                                if (Settings.Debug) {
                                    Movecraft.getInstance().getLogger().info("Submitting Entity Update: " + entity.getType().getName());
                                    if (entity instanceof Item) {
                                        Movecraft.getInstance().getLogger().info("Item Type: " + ((Item) entity).getItemType().getName());
                                    }
                                }
                            }
                        }

                        processedEntities.set(true);
                    })
                    .submit(Movecraft.getInstance());


            synchronized (this) {
                while (!processedEntities.get()) {
                    this.wait(1);
                }
            }
        }
    }

    private void checkCraftObstructed() {
        for (Vector3i originalLocation : oldHitBox) {
            Vector3i newLocation = MathUtils.rotateVec(rotation, originalLocation.sub(originPoint)).add(originPoint);
            newHitBox.add(newLocation);

            if (oldHitBox.contains(newLocation)) {
                continue;
            }

            //prevent chests collision
            BlockType oldMaterial = world.getBlockType(originalLocation);
            if ((oldMaterial.equals(BlockTypes.CHEST) || oldMaterial.equals(BlockTypes.TRAPPED_CHEST)) && !checkChests(oldMaterial, newLocation)) {
                fail(String.format("Craft is obstructed by chest" + " @ %d,%d,%d", newLocation.getX(), newLocation.getY(), newLocation.getZ()));
                break;
            }


            BlockType newMaterial = world.getBlockType(newLocation);
            if (newMaterial != BlockTypes.AIR && !craft.getType().getPassthroughBlocks().contains(newMaterial)) {
                fail(String.format("Craft is obstructed" + " @ %d,%d,%d", newLocation.getX(), newLocation.getY(), newLocation.getZ()));
                break;
            }
        }
    }

    private void notifyPlayerOfNewDirection() {
        int farthestX = 0;
        int farthestZ = 0;
        for (Vector3i loc : newHitBox) {
            if (Math.abs(loc.getX() - originPoint.getX()) > Math.abs(farthestX)) {
                farthestX = loc.getX() - originPoint.getX();
            }
            if (Math.abs(loc.getZ() - originPoint.getZ()) > Math.abs(farthestZ)) {
                farthestZ = loc.getZ() - originPoint.getZ();
            }
        }

        Player pilot = getNotificationPlayer().orElse(null);

        if (pilot != null) {
            if (Math.abs(farthestX) > Math.abs(farthestZ)) {
                if (farthestX > 0) {
                    pilot.sendMessage(Text.of("The farthest extent now faces East"));
                } else {
                    pilot.sendMessage(Text.of("The farthest extent now faces West"));
                }
            } else if (Math.abs(farthestX) < Math.abs(farthestZ)) {
                if (farthestZ > 0) {
                    pilot.sendMessage(Text.of("The farthest extent now faces South"));
                } else {
                    pilot.sendMessage(Text.of("The farthest extent now faces North"));
                }
            }
        }
    }

    @Override
    public void postProcess() {
        if (failed()) {
            craft.setProcessing(false);

        } else {

            MapUpdateManager.getInstance().scheduleUpdates(updates);
        }
    }

    @Override
    protected Optional<Player> getNotificationPlayer() {
        return craft.getNotificationPlayer();
    }

    public Vector3i getOriginPoint() {
        return originPoint;
    }

    public Set<UpdateCommand> getUpdates() {
        return updates;
    }

    public Rotation getRotation() {
        return rotation;
    }

    private boolean checkChests(BlockType mBlock, Vector3i newLoc) {
        for (Vector3i shift : SHIFTS) {
            Vector3i aroundNewLoc = newLoc.add(shift);
            BlockType testMaterial = craft.getWorld().getBlockType(aroundNewLoc.getX(), aroundNewLoc.getY(), aroundNewLoc.getZ());
            if (testMaterial.equals(mBlock) && !oldHitBox.contains(aroundNewLoc)) {
                return false;
            }
        }

        return true;
    }

    private static final Vector3i[] SHIFTS = {
            new Vector3i(1, 0, 0),
            new Vector3i(-1, 0, 0),
            new Vector3i(0, 0, 1),
            new Vector3i(0, 0, -1)
    };

    public HashHitBox getNewHitBox() {
        return newHitBox;
    }
}