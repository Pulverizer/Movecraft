package io.github.pulverizer.movecraft.sign;

import static org.spongepowered.api.event.Order.FIRST;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import io.github.pulverizer.movecraft.Movecraft;
import io.github.pulverizer.movecraft.config.Settings;
import io.github.pulverizer.movecraft.utils.BlockSnapshotSignDataUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.tileentity.ChangeSignEvent;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Permissions checked
 * Code to be reviewed
 *
 * @author BernardisGood
 * @version 1.3 - 17 Apr 2020
 */
public class CrewSign {

    private static final String HEADER = "Crew:";
    private static final String MAIN_TABLE = "CrewSigns";

    public static void onSignChange(ChangeSignEvent event, Player player) {

        if (!Settings.EnableCrewSigns) {
            player.sendMessage(Text.of("Crew beds are not enabled"));
            event.setCancelled(true);
            return;
        }

        if (Settings.RequireCreateSignPerm && !player.hasPermission("movecraft.createsign.crew")) {
            player.sendMessage(Text.of("Insufficient Permissions"));
            event.setCancelled(true);
            return;
        }

        World world = event.getTargetTile().getLocation().getExtent();
        Vector3i location = event.getTargetTile().getLocation().getBlockPosition();
        ListValue<Text> lines = event.getText().lines();

        if (!world.getBlockType(location.sub(0, 1, 0)).equals(BlockTypes.BED)) {
            lines.set(1, Text.of("ERROR"));
            lines.set(2, Text.of("No Bed!"));

            event.getText().set(lines);

            return;
        }

        Connection sqlConnection = Movecraft.getInstance().connectToSQL();

        if (sqlConnection == null) {
            fail(event, player);
            return;
        }

        try {

            PreparedStatement statement = sqlConnection.prepareStatement("SELECT ID FROM " + MAIN_TABLE + " WHERE Username = ?");
            statement.setString(1, player.getName());

            ResultSet resultSet = statement.executeQuery();

            List<Integer> usedNumIDs = new ArrayList<>();
            int availableID = -1;
            int lastCheckedID = -1;

            while (resultSet.next()) {

                usedNumIDs.add(resultSet.getInt("ID"));
            }


            if (usedNumIDs.size() == Integer.MAX_VALUE) {
                fail(event, player);
                return;
            }
            Collections.sort(usedNumIDs);

            for (int id : usedNumIDs) {

                if (id == lastCheckedID + 1) {
                    lastCheckedID = id;
                    continue;
                }

                if (id > lastCheckedID + 1) {
                    availableID = lastCheckedID + 1;
                    break;
                }
            }

            if (availableID == -1 && availableID < lastCheckedID || lastCheckedID == -1) {
                availableID = lastCheckedID + 1;
            }


            if (availableID == -1) {
                sqlConnection.close();
                fail(event, player);
                return;
            }


            statement = sqlConnection.prepareStatement("INSERT INTO " + MAIN_TABLE + " (Username, ID, World, X, Y, Z) VALUES (?, ?, ?, ?, ?, ?)");

            statement.setString(1, player.getName());
            statement.setInt(2, availableID);
            statement.setString(3, world.getUniqueId().toString());
            statement.setInt(4, location.getX());
            statement.setInt(5, location.getY());
            statement.setInt(6, location.getZ());

            statement.execute();

            statement.close();

            lines.set(1, Text.of(player.getName()));
            lines.set(2, Text.of(availableID));

            event.getText().set(lines);

            sqlConnection.close();

            return;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        player.sendMessage(Text.of("There was an error. Please try placing the sign again. If this continues, please contact a Server Admin."));
    }

    private static void fail(ChangeSignEvent event, Player player) {

        player.sendMessage(Text.of("Please contact a Server Admin! There is something seriously wrong!"));

        ListValue<Text> lines = event.getText().lines();

        lines.set(1, Text.of("ERROR:"));
        lines.set(2, Text.of("Invalid SQL"));

        event.getText().set(lines);
    }

    public static void onSignTranslate(World world, Vector3i location, Sign sign) {

        ListValue<Text> lines = sign.lines();

        String username = lines.get(1).toPlain();
        int id = Integer.parseInt(lines.get(2).toPlain());

        updateSignLocation(username, id, world.getUniqueId(), location);
    }

    public static void onSignBreak(ChangeBlockEvent.Break event, Transaction<BlockSnapshot> transaction) {

        BlockSnapshot blockSnapshot = transaction.getOriginal();

        if (blockSnapshot.getState().getType() != BlockTypes.STANDING_SIGN && blockSnapshot.getState().getType() != BlockTypes.WALL_SIGN) {
            return;
        }

        if (!blockSnapshot.getLocation().isPresent()) {
            return;
        }

        if (!BlockSnapshotSignDataUtil.getTextLine(blockSnapshot, 1).get().equalsIgnoreCase(HEADER)) {
            return;
        }

        String username = BlockSnapshotSignDataUtil.getTextLine(blockSnapshot, 2).get();
        int id = Integer.parseInt(BlockSnapshotSignDataUtil.getTextLine(blockSnapshot, 3).get());

        if (!removeFromDatabase(username, id)) {
            transaction.setValid(false);
        }
    }

    private static boolean removeFromDatabase(String username, int id) {

        Connection sqlConnection = Movecraft.getInstance().connectToSQL();

        if (sqlConnection == null) {
            return false;
        }

        try {

            PreparedStatement statement = sqlConnection.prepareStatement("DELETE FROM " + MAIN_TABLE + " WHERE Username = ? AND ID = ?");
            statement.setString(1, username);
            statement.setInt(2, id);

            statement.execute();
            statement.close();

            sqlConnection.close();

            return true;

        } catch (SQLException e) {
            e.printStackTrace();

            try {
                sqlConnection.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            return false;
        }
    }

    private static void updateSignLocation(String username, int id, UUID world, Vector3i location) {

        Connection sqlConnection = Movecraft.getInstance().connectToSQL();

        try {

            PreparedStatement statement =
                    sqlConnection.prepareStatement("UPDATE " + MAIN_TABLE + " SET World = ?, X = ?, Y = ?, Z = ? WHERE Username = ? AND ID = ?");

            //New Variables
            statement.setString(1, world.toString());
            statement.setInt(2, location.getX());
            statement.setInt(3, location.getY());
            statement.setInt(4, location.getZ());

            //Primary Key
            statement.setString(5, username);
            statement.setInt(6, id);

            statement.execute();
            statement.close();

            sqlConnection.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void initDatabase() {

        Connection sqlConnection = Movecraft.getInstance().connectToSQL();

        try {

            PreparedStatement statement = sqlConnection.prepareStatement("CREATE TABLE IF NOT EXISTS " + MAIN_TABLE + " (" +
                    "Username CHAR(16) NOT NULL, " +
                    "ID INT(16) NOT NULL, " +
                    "World CHAR(36) NOT NULL," +
                    "X INT(12) NOT NULL," +
                    "Y INT(12) NOT NULL," +
                    "Z INT(12) NOT NULL," +
                    "CONSTRAINT " + MAIN_TABLE + "_PKey PRIMARY KEY (Username, ID)" +
                    ")");

            statement.execute();
            statement.close();

            sqlConnection.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Listener(order = FIRST)
    public final void onPlayerRespawn(RespawnPlayerEvent event) {

        if (!Settings.EnableCrewSigns) {
            return;
        }

        if (!event.isDeath()) {
            return;
        }

        Player player = event.getTargetEntity();
        HashMap<UUID, ArrayList<Vector3i>> respawnOptions = new HashMap<>();

        Connection sqlConnection = Movecraft.getInstance().connectToSQL();

        try {

            PreparedStatement statement = sqlConnection.prepareStatement("SELECT World, X, Y, Z FROM " + MAIN_TABLE + " WHERE Username = ?");
            statement.setString(1, player.getName());

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {

                UUID world = UUID.fromString(resultSet.getString("World"));
                Vector3i location = new Vector3i(resultSet.getInt("X"), resultSet.getInt("Y"), resultSet.getInt("Z"));

                if (!respawnOptions.containsKey(world)) {
                    ArrayList<Vector3i> locations = new ArrayList<>();

                    locations.add(location);
                    respawnOptions.put(world, locations);
                } else {
                    respawnOptions.get(world).add(location);
                }
            }

            statement.close();
            sqlConnection.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (respawnOptions.isEmpty()) {
            return;
        }

        World world = event.getToTransform().getExtent();

        ArrayList<Vector3i> locations = respawnOptions.get(world.getUniqueId());

        Vector3d deathLocation = event.getFromTransform().getPosition();
        Vector3i respawnLocation = null;
        double distance = 0;

        for (Vector3i location : locations) {

            if (!world.getBlockType(location.sub(0, 1, 0)).equals(BlockTypes.BED)) {
                continue;
            }

            if (respawnLocation == null) {

                respawnLocation = location;
                distance = deathLocation.distance(location.toDouble());

            } else if (deathLocation.distance(location.toDouble()) < distance) {

                respawnLocation = location;
                distance = deathLocation.distance(location.toDouble());

            }
        }

        if (respawnLocation != null) {


            Location<World> finalLocation = Sponge.getGame().getTeleportHelper().getSafeLocation(new Location<>(world, respawnLocation))
                    .orElse(new Location<>(world, respawnLocation));

            player.sendMessage(Text.of("Respawning at crew bed!"));
            Transform<World> respawnTransform = event.getToTransform();
            respawnTransform = respawnTransform.setLocation(finalLocation);
            event.setToTransform(respawnTransform);

            //TODO: Waiting on SpongeCommon issue #1824 - https://github.com/SpongePowered/SpongeCommon/issues/1824
            /*
            if (player.get(Keys.RESPAWN_LOCATIONS).isPresent()) {

                Map<UUID, RespawnLocation> respawnLocations = player.get(Keys.RESPAWN_LOCATIONS).get();

                RespawnLocation respawnBed = respawnLocations.get(world.getUniqueId());

                respawnLocations.put(world.getUniqueId(), RespawnLocation.builder().from(respawnBed).position(finalLocation.getPosition())
                .forceSpawn(false).build());

                player.offer(Keys.RESPAWN_LOCATIONS, respawnLocations);
            }
            */
        }
    }
}