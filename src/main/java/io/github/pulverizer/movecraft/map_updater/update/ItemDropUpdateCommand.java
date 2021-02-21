package io.github.pulverizer.movecraft.map_updater.update;

import io.github.pulverizer.movecraft.Movecraft;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Objects;

/**   Class that stores the data about a item drops to the map in an unspecified world. The world is retrieved contextually from the submitting
 * craft.   */
public class ItemDropUpdateCommand extends UpdateCommand {

    private final Location<World> location;
    private final ItemStack itemStack;

    public ItemDropUpdateCommand(Location<World> location, ItemStack itemStack) {
        this.location = location;
        this.itemStack = itemStack;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public Location<World> getLocation() {
        return location;
    }

    @Override
    public void doUpdate() {
        if (itemStack != null) {
            final World world = location.getExtent();
            // drop Item
            Task.builder()
                    .delayTicks(20)
                    .execute(() -> {
                        Entity entity = world.createEntityNaturally(EntityTypes.ITEM, ItemDropUpdateCommand.this.location.getPosition());
                        entity.offer(Keys.REPRESENTED_ITEM, itemStack.createSnapshot());
                        world.spawnEntity(entity);
                    })
                    .submit(Movecraft.getInstance());
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, itemStack);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ItemDropUpdateCommand)) {
            return false;
        }
        ItemDropUpdateCommand other = (ItemDropUpdateCommand) obj;
        return this.location.equals(other.location) &&
                this.itemStack.equals(other.itemStack);
    }

}
