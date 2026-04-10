package com.treasurehunt.listeners;

import com.treasurehunt.TreasureHuntPlugin;
import com.treasurehunt.managers.TreasureManager;
import com.treasurehunt.models.Treasure;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class PlayerInteractListener implements Listener {

    private final TreasureHuntPlugin plugin;
    private final TreasureManager treasureManager;

    public PlayerInteractListener(TreasureHuntPlugin plugin, TreasureManager treasureManager) {
        this.plugin = plugin;
        this.treasureManager = treasureManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (block == null) return;

        // Handle pending treasure creation (only for right-click)
        if (event.getAction().toString().contains("RIGHT_CLICK")) {
            TreasureManager.PendingCreation pending = treasureManager.getPendingCreation(player.getUniqueId());
            if (pending != null) {
                // Only handle main hand to prevent double firing
                if (event.getHand() != EquipmentSlot.HAND) return;

                event.setCancelled(true);

                Location location = block.getLocation();

                // Check if there's already a treasure at this location
                Treasure existingTreasure = treasureManager.getTreasureAt(location);
                if (existingTreasure != null) {
                    player.sendMessage(colorize(getPrefix() + "&cThere is already a treasure at this location!"));
                    return;
                }

                // Create the treasure
                if (treasureManager.createTreasure(pending.getTreasureId(), location, pending.getCommand())) {
                    player.sendMessage(colorize(getPrefix() + getMessage("treasure-created").replace("%id%", pending.getTreasureId())));
                    treasureManager.removePendingCreation(player.getUniqueId());
                } else {
                    player.sendMessage(colorize(getPrefix() + "&cFailed to create treasure. ID may already exist."));
                }

                return;
            }
        }

        // Handle treasure claiming (only for right-click)
        if (event.getAction().toString().contains("RIGHT_CLICK")) {
            // Only handle main hand to prevent double firing
            if (event.getHand() != EquipmentSlot.HAND) return;

            Location location = block.getLocation();
            Treasure treasure = treasureManager.getTreasureAt(location);

            if (treasure == null) return;

            event.setCancelled(true);

            // Check if player has already claimed this treasure
            if (treasureManager.hasPlayerCompleted(treasure, player.getUniqueId())) {
                player.sendMessage(colorize(getPrefix() + getMessage("already-claimed")));
                return;
            }

            // Mark as completed in database
            treasureManager.markPlayerCompleted(treasure, player.getUniqueId());

            // Execute the command from console
            String command = treasure.getCommand().replace("%player%", player.getName());
            Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));

            // Send message to player
            player.sendMessage(colorize(getPrefix() + getMessage("treasure-found")));
        }
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private String getPrefix() {
        return plugin.getConfig().getString("messages.prefix", "&6[TreasureHunt] &r");
    }

    private String getMessage(String key) {
        return plugin.getConfig().getString("messages." + key, "");
    }
}
