package io.github.pulverizer.movecraft.utils;


import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Direction;

public abstract class ChatUtils {

    public static final String MOVECRAFT_COMMAND_PREFIX =
            TextColors.GOLD + "[" + TextColors.WHITE + "Movecraft" + TextColors.GOLD + "] " + TextColors.RESET;

    public static String abbreviateDirection(Direction direction) {
        switch (direction) {
            case NORTH:
                return "N";

            case NORTH_NORTHWEST:
                return "N-NW";

            case NORTHWEST:
                return "NW";

            case WEST_NORTHWEST:
                return "W-NW";

            case WEST:
                return "W";

            case WEST_SOUTHWEST:
                return "W-SW";

            case SOUTHWEST:
                return "SW";

            case SOUTH_SOUTHWEST:
                return "S-SW";

            case SOUTH:
                return "S";

            case SOUTH_SOUTHEAST:
                return "S-SE";

            case SOUTHEAST:
                return "SE";

            case EAST_SOUTHEAST:
                return "E-SE";

            case EAST:
                return "E";

            case EAST_NORTHEAST:
                return "E-NE";

            case NORTHEAST:
                return "NE";

            case NORTH_NORTHEAST:
                return "N-NE";

            case UP:
                return "UP";

            case DOWN:
                return "DOWN";

            default:
                return "N/A";
        }
    }
}