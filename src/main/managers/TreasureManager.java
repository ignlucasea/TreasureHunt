package com.treasurehunt.managers;

import com.treasurehunt.TreasureHuntPlugin;
import com.treasurehunt.database.DatabaseManager;
import com.treasurehunt.models.Treasure;
import org.bukkit.Location;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TreasureManager {

    private final TreasureHuntPlugin plugin;
    private final DatabaseManager databaseManager;
    private final ConcurrentHashMap<String, Treasure> treasures;
    private final ConcurrentHashMap<UUID, PendingCreation> pendingCreations;

    public TreasureManager(TreasureHuntPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.treasures = new ConcurrentHashMap<>();
        this.pendingCreations = new ConcurrentHashMap<>();
    }

    public void loadTreasures() {
        treasures.clear();
        List<Treasure> loadedTreasures = databaseManager.loadAllTreasures();
        for (Treasure treasure : loadedTreasures) {
            treasures.put(treasure.getId().toLowerCase(), treasure);
        }
        plugin.getLogger().info("Loaded " + treasures.size() + " treasures from database.");
    }

    public Treasure getTreasure(String id) {
        return treasures.get(id.toLowerCase());
    }

    public Treasure getTreasureAt(Location location) {
        for (Treasure treasure : treasures.values()) {
            if (treasure.getX() == location.getBlockX() &&
                treasure.getY() == location.getBlockY() &&
                treasure.getZ() == location.getBlockZ() &&
                treasure.getWorld().equals(location.getWorld().getName())) {
                return treasure;
            }
        }
        return null;
    }

    public boolean createTreasure(String id, Location location, String command) {
        if (treasures.containsKey(id.toLowerCase())) {
            return false;
        }

        Treasure treasure = new Treasure(
                id.toLowerCase(),
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                command
        );

        treasures.put(id.toLowerCase(), treasure);
        databaseManager.saveTreasure(treasure);
        return true;
    }

    public boolean deleteTreasure(String id) {
        Treasure removed = treasures.remove(id.toLowerCase());
        if (removed != null) {
            databaseManager.deleteTreasure(id.toLowerCase());
            return true;
        }
        return false;
    }

    public Collection<Treasure> getAllTreasures() {
        return treasures.values();
    }

    public boolean hasPlayerCompleted(Treasure treasure, UUID playerId) {
        // Always check database for multi-server sync
        List<UUID> completed = databaseManager.getCompletedPlayers(treasure.getId());
        return completed.contains(playerId);
    }

    public void markPlayerCompleted(Treasure treasure, UUID playerId) {
        treasure.setPlayerCompleted(playerId);
        databaseManager.markPlayerCompleted(treasure.getId(), playerId);
    }

    public List<UUID> getCompletedPlayers(String treasureId) {
        Treasure treasure = getTreasure(treasureId);
        if (treasure != null) {
            return databaseManager.getCompletedPlayers(treasureId);
        }
        return new ArrayList<>();
    }

    public void setPendingCreation(UUID playerId, String treasureId, String command) {
        pendingCreations.put(playerId, new PendingCreation(treasureId, command));
    }

    public PendingCreation getPendingCreation(UUID playerId) {
        return pendingCreations.get(playerId);
    }

    public void removePendingCreation(UUID playerId) {
        pendingCreations.remove(playerId);
    }

    public static class PendingCreation {
        private final String treasureId;
        private final String command;

        public PendingCreation(String treasureId, String command) {
            this.treasureId = treasureId;
            this.command = command;
        }

        public String getTreasureId() {
            return treasureId;
        }

        public String getCommand() {
            return command;
        }
    }
}
