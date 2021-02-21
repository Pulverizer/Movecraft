package io.github.pulverizer.movecraft.sign;

import com.flowpowered.math.vector.Vector3i;
import io.github.pulverizer.movecraft.config.Settings;
import io.github.pulverizer.movecraft.config.craft_settings.Defaults;
import io.github.pulverizer.movecraft.craft.Craft;
import io.github.pulverizer.movecraft.event.CraftDetectEvent;
import io.github.pulverizer.movecraft.utils.HashHitBox;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.block.tileentity.ChangeSignEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Permissions checked
 * Code to be reviewed
 *
 * @author BernardisGood
 * @version 1.4 - 20 Apr 2020
 */
public final class StatusSign {

    public static void onSignChange(ChangeSignEvent event, Player player) {

        if (Settings.RequireCreateSignPerm && !player.hasPermission("movecraft.createsign.status")) {
            player.sendMessage(Text.of("Insufficient Permissions"));
            event.setCancelled(true);
        }
    }

    public static void onCraftDetect(CraftDetectEvent event, World world, HashHitBox hitBox) {

        for (Vector3i location : hitBox) {

            if (world.getBlockType(location) != BlockTypes.WALL_SIGN && world.getBlockType(location) != BlockTypes.STANDING_SIGN || !world
                    .getTileEntity(location).isPresent()) {
                continue;
            }

            Sign sign = (Sign) world.getTileEntity(location).get();
            ListValue<Text> lines = sign.lines();

            if (lines.get(0).toPlain().equalsIgnoreCase("Status:")) {
                lines.set(0, Text.of("Status:"));
                lines.set(1, Text.of(""));
                lines.set(2, Text.of(""));
                lines.set(3, Text.of(""));
                sign.offer(lines);
            }
        }
    }

    public static void onSignTranslate(Craft craft, Sign sign) {

        ListValue<Text> lines = sign.lines();

        double fuel = craft.checkFuelStored() + craft.getMovePoints();
        double totalBlocks = craft.getSize();

        Map<BlockType, Set<Vector3i>> blockMap = craft.getHitBox().map(craft.getWorld());
        Map<BlockType, Integer> foundBlocks = new HashMap<>();

        blockMap.forEach((blockType, locations) -> foundBlocks.put(blockType, locations.size()));

        //Fly Blocks
        if (!craft.getType().getSetting(Defaults.FlyBlocks.class).get().getValue().isEmpty()) {

            BlockType flyBlock = BlockTypes.AIR;
            double percentFB = 0d;
            double minimumFB = 0d;

            for (Map.Entry<List<BlockType>, List<Double>> flyBlockMapEntry :
                    craft.getType().getSetting(Defaults.FlyBlocks.class).get().getValue().entrySet()) {

                Double minimum = flyBlockMapEntry.getValue().get(0);

                if (minimum > 0) {
                    int amount = 0;

                    for (BlockType blockType : flyBlockMapEntry.getKey()) {
                        if (foundBlocks.containsKey(blockType)) {
                            amount += foundBlocks.get(blockType);
                        }
                    }

                    if (amount <= 0) {
                        continue;
                    }

                    Double percentPresent = amount * 100 / totalBlocks;

                    if (percentFB == 0 || percentPresent - minimum < percentFB - minimumFB) {
                        flyBlock = flyBlockMapEntry.getKey().get(0);
                        percentFB = percentPresent;
                        minimumFB = minimum;
                    }
                }
            }

            TextColor lineColor;

            if (percentFB > minimumFB * 1.05) {
                lineColor = TextColors.GREEN;
            } else if (percentFB > minimumFB * 1.025) {
                lineColor = TextColors.YELLOW;
            } else {
                lineColor = TextColors.RED;
            }

            String[] strings = flyBlock.getName().split(":");
            char name = strings[strings.length - 1].toUpperCase().charAt(0);

            if (percentFB > 10) {
                lines.set(1, Text.of(lineColor, name, ": ", (int) percentFB, "/", (int) minimumFB));

            } else {
                percentFB = (double) ((int) (percentFB * 10)) / 10;
                minimumFB = (double) ((int) (minimumFB * 10)) / 10;

                lines.set(2, Text.of(lineColor, name, ": ", percentFB, "/", minimumFB));
            }
        }

        //Move Blocks
        if (!craft.getType().getSetting(Defaults.MoveBlocks.class).get().getValue().isEmpty()) {
            BlockType moveBlock = BlockTypes.AIR;
            double percentMB = 0d;
            double minimumMB = 0d;

            for (Map.Entry<List<BlockType>, List<Double>> moveBlockMapEntry :
                    craft.getType().getSetting(Defaults.MoveBlocks.class).get().getValue().entrySet()) {

                Double minimum = moveBlockMapEntry.getValue().get(0);

                if (minimum > 0) {
                    double amount = 0;

                    for (BlockType blockType : moveBlockMapEntry.getKey()) {
                        if (foundBlocks.containsKey(blockType)) {
                            amount += foundBlocks.get(blockType);
                        }
                    }

                    if (amount <= 0) {
                        continue;
                    }

                    Double percentPresent = amount * 100 / totalBlocks;

                    if (percentMB == 0 || percentPresent - minimum < percentMB - minimumMB) {
                        moveBlock = moveBlockMapEntry.getKey().get(0);
                        percentMB = percentPresent;
                        minimumMB = minimum;
                    }
                }
            }

            TextColor lineColor;

            if (percentMB > minimumMB * 1.05) {
                lineColor = TextColors.GREEN;
            } else if (percentMB > minimumMB * 1.025) {
                lineColor = TextColors.YELLOW;
            } else {
                lineColor = TextColors.RED;
            }

            String[] strings = moveBlock.getName().split(":");
            char name = strings[strings.length - 1].toUpperCase().charAt(0);

            if (percentMB > 10) {
                lines.set(2, Text.of(lineColor, name, ": ", (int) percentMB, "/", (int) minimumMB));

            } else {
                percentMB = (double) ((int) (percentMB * 10)) / 10;
                minimumMB = (double) ((int) (minimumMB * 10)) / 10;

                lines.set(2, Text.of(lineColor, name, ": ", percentMB, "/", minimumMB));
            }
        }

        int fuelRange =
                (int) Math.floor((fuel * (1 + craft.getType().getSetting(Defaults.CruiseSkipBlocks.class).get().getValue())) / craft.getType().getSetting(Defaults.FuelBurnRate.class).get().getValue());
        TextColor fuelColor;

        //TODO: Add default to craft config
        //  Add new sign type "Fuel Range:" which allows the player to set it per craft
        if (fuelRange > 10000) {
            fuelColor = TextColors.GREEN;
        } else if (fuelRange > 2500) {
            fuelColor = TextColors.YELLOW;
        } else {
            fuelColor = TextColors.RED;
        }

        fuelRange = Math.min(fuelRange, 999999999);

        lines.set(3, Text.of(fuelColor, "Fuel: ", fuelRange, "m"));
        sign.offer(lines);
    }
}