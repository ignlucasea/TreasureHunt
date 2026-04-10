package com.treasurehunt.models;

import org.bukkit.Location;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Treasure {

    private final String id;
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final String command;
    private final ConcurrentHashMap<UUID, Boolean> completedPlayers;

    public Treasure(String id, String world, int x, int y, int z, String command) {
        this.id = id;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.command = command;
        this.completedPlayers = new ConcurrentHashMap<>();
    }

    public String getId() {
        return id;
    }

    public String getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public String getCommand() {
        return command;
    }

    public Location getLocation() {
        return new Location(org.bukkit.Bukkit.getWorld(world), x, y, z);
    }

    public boolean hasPlayerCompleted(UUID playerId) {
        return completedPlayers.getOrDefault(playerId, false);
    }

    public void setPlayerCompleted(UUID playerId) {
        completedPlayers.put(playerId, true);
    }

    public void removePlayerCompletion(UUID playerId) {
        completedPlayers.remove(playerId);
    }

    public ConcurrentHashMap<UUID, Boolean> getCompletedPlayers() {
        return completedPlayers;
    }

    public String getLocationString() {
        return world + ", " + x + ", " + y + ", " + z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Treasure treasure = (Treasure) o;
        return x == treasure.x && y == treasure.y && z == treasure.z && Objects.equals(world, treasure.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, x, y, z);
    }
}
