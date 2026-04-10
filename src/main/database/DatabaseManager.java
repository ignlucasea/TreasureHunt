package com.treasurehunt.database;

import com.treasurehunt.TreasureHuntPlugin;
import com.treasurehunt.models.Treasure;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private final TreasureHuntPlugin plugin;
    private String jdbcUrl;
    private String username;
    private String password;
    private Connection connection;

    public DatabaseManager(TreasureHuntPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean connect() {
        try {
            // Load MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC",
                    plugin.getConfig().getString("mysql.host", "localhost"),
                    plugin.getConfig().getInt("mysql.port", 3306),
                    plugin.getConfig().getString("mysql.database", "treasurehunt"));
            username = plugin.getConfig().getString("mysql.username", "root");
            password = plugin.getConfig().getString("mysql.password", "password");

            // Create connection
            connection = DriverManager.getConnection(jdbcUrl, username, password);

            // Test connection and create tables
            createTables();
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to MySQL database", e);
            return false;
        }
    }

    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to close database connection", e);
            }
        }
    }

    public Connection getConnection() throws SQLException {
        // Check if connection is closed or invalid, reconnect if necessary
        if (connection == null || connection.isClosed() || !isConnectionValid()) {
            connection = DriverManager.getConnection(jdbcUrl, username, password);
        }
        return connection;
    }

    private boolean isConnectionValid() {
        try {
            return connection.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    private void createTables() {
        String createTreasuresTable = """
            CREATE TABLE IF NOT EXISTS treasures (
                id VARCHAR(64) PRIMARY KEY,
                world VARCHAR(64) NOT NULL,
                x INT NOT NULL,
                y INT NOT NULL,
                z INT NOT NULL,
                command TEXT NOT NULL
            )
            """;

        String createCompletedTable = """
            CREATE TABLE IF NOT EXISTS treasure_completed (
                treasure_id VARCHAR(64) NOT NULL,
                player_uuid VARCHAR(36) NOT NULL,
                completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (treasure_id, player_uuid),
                FOREIGN KEY (treasure_id) REFERENCES treasures(id) ON DELETE CASCADE
            )
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTreasuresTable);
            stmt.execute(createCompletedTable);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create tables", e);
        }
    }

    public void saveTreasure(Treasure treasure) {
        String sql = "INSERT INTO treasures (id, world, x, y, z, command) VALUES (?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE world=?, x=?, y=?, z=?, command=?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, treasure.getId());
            stmt.setString(2, treasure.getWorld());
            stmt.setInt(3, treasure.getX());
            stmt.setInt(4, treasure.getY());
            stmt.setInt(5, treasure.getZ());
            stmt.setString(6, treasure.getCommand());
            stmt.setString(7, treasure.getWorld());
            stmt.setInt(8, treasure.getX());
            stmt.setInt(9, treasure.getY());
            stmt.setInt(10, treasure.getZ());
            stmt.setString(11, treasure.getCommand());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save treasure", e);
        }
    }

    public void deleteTreasure(String id) {
        String sql = "DELETE FROM treasures WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete treasure", e);
        }
    }

    public List<Treasure> loadAllTreasures() {
        List<Treasure> treasures = new ArrayList<>();
        String sql = "SELECT * FROM treasures";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Treasure treasure = new Treasure(
                        rs.getString("id"),
                        rs.getString("world"),
                        rs.getInt("x"),
                        rs.getInt("y"),
                        rs.getInt("z"),
                        rs.getString("command")
                );
                loadCompletedPlayers(treasure);
                treasures.add(treasure);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load treasures", e);
        }

        return treasures;
    }

    private void loadCompletedPlayers(Treasure treasure) {
        String sql = "SELECT player_uuid FROM treasure_completed WHERE treasure_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, treasure.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    treasure.setPlayerCompleted(UUID.fromString(rs.getString("player_uuid")));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load completed players for treasure: " + treasure.getId(), e);
        }
    }

    public void markPlayerCompleted(String treasureId, UUID playerId) {
        String sql = "INSERT IGNORE INTO treasure_completed (treasure_id, player_uuid) VALUES (?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, treasureId);
            stmt.setString(2, playerId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to mark player as completed", e);
        }
    }

    public List<UUID> getCompletedPlayers(String treasureId) {
        List<UUID> players = new ArrayList<>();
        String sql = "SELECT player_uuid FROM treasure_completed WHERE treasure_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, treasureId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    players.add(UUID.fromString(rs.getString("player_uuid")));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get completed players", e);
        }

        return players;
    }
}
