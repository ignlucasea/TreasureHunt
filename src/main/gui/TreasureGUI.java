package com.treasurehunt.gui;

import com.treasurehunt.TreasureHuntPlugin;
import com.treasurehunt.managers.TreasureManager;
import com.treasurehunt.models.Treasure;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class TreasureGUI implements Listener {

    private final TreasureHuntPlugin plugin;
    private final TreasureManager treasureManager;
    private final Map<UUID, Integer> playerPages;
    private final Map<UUID, String> confirmDeletion;

    private static final int ITEMS_PER_PAGE = 45;
    private static final int INVENTORY_SIZE = 54;

    public TreasureGUI(TreasureHuntPlugin plugin, TreasureManager treasureManager) {
        this.plugin = plugin;
        this.treasureManager = treasureManager;
        this.playerPages = new HashMap<>();
        this.confirmDeletion = new HashMap<>();
    }

    public void openTreasureListGUI(Player player, int page) {
        List<Treasure> treasures = new ArrayList<>(treasureManager.getAllTreasures());
        int totalPages = (int) Math.ceil(treasures.size() / (double) ITEMS_PER_PAGE);

        if (page < 0) page = 0;
        if (totalPages == 0) totalPages = 1;
        if (page >= totalPages) page = totalPages - 1;

        playerPages.put(player.getUniqueId(), page);

        String title = plugin.getConfig().getString("messages.gui-title", "&8Treasure Management");
        Inventory gui = Bukkit.createInventory(null, INVENTORY_SIZE, colorize(title + " &7(Page " + (page + 1) + "/" + totalPages + ")"));

        // Add treasure items
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, treasures.size());

        for (int i = startIndex; i < endIndex; i++) {
            Treasure treasure = treasures.get(i);
            int completedCount = treasureManager.getCompletedPlayers(treasure.getId()).size();
            gui.setItem(i - startIndex, createTreasureItem(treasure, completedCount));
        }

        // Fill empty slots with filler
        Material fillerMaterial = Material.valueOf(plugin.getConfig().getString("gui.filler-material", "BLACK_STAINED_GLASS_PANE"));
        ItemStack filler = createItem(fillerMaterial, " ");
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (gui.getItem(i) == null && i < 45) {
                gui.setItem(i, filler);
            }
        }

        // Navigation and control items (bottom row)
        for (int i = 45; i < 54; i++) {
            gui.setItem(i, filler);
        }

        // Previous page button
        if (page > 0) {
            Material prevMaterial = Material.valueOf(plugin.getConfig().getString("gui.previous-page-material", "ARROW"));
            gui.setItem(45, createItem(prevMaterial, "&e&lPrevious Page", "&7Click to go to page " + page));
        }

        // Next page button
        if (page < totalPages - 1) {
            Material nextMaterial = Material.valueOf(plugin.getConfig().getString("gui.next-page-material", "ARROW"));
            gui.setItem(53, createItem(nextMaterial, "&e&lNext Page", "&7Click to go to page " + (page + 2)));
        }

        // Info item
        gui.setItem(49, createItem(Material.BOOK, "&6&lTreasure Info", 
                "&7Total: &f" + treasures.size() + " treasures",
                "&7Click a treasure to delete it"));

        player.openInventory(gui);
    }

    private void openConfirmDeleteGUI(Player player, String treasureId) {
        confirmDeletion.put(player.getUniqueId(), treasureId);

        Inventory gui = Bukkit.createInventory(null, 27, colorize("&c&lConfirm Deletion"));

        // Fill with glass
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, filler);
        }

        Treasure treasure = treasureManager.getTreasure(treasureId);
        if (treasure != null) {
            gui.setItem(11, createItem(Material.CHEST, "&eTreasure: &f" + treasureId,
                    "&7Location: &f" + treasure.getLocationString()));
        }

        // Cancel button
        gui.setItem(15, createItem(Material.RED_WOOL, "&c&lCancel", "&7Click to cancel"));

        // Confirm button
        Material deleteMaterial = Material.valueOf(plugin.getConfig().getString("gui.delete-material", "BARRIER"));
        gui.setItem(13, createItem(deleteMaterial, "&4&lConfirm Delete", 
                "&c&lWARNING: This cannot be undone!",
                "&7Click to delete this treasure"));

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();

        // Check if this is our GUI
        if (!title.contains("Treasure Management") && !title.contains("Confirm Deletion")) return;

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        if (title.contains("Confirm Deletion")) {
            handleConfirmDeleteClick(player, clickedItem);
            return;
        }

        int slot = event.getRawSlot();
        int page = playerPages.getOrDefault(player.getUniqueId(), 0);

        // Handle navigation
        if (slot == 45 && clickedItem.getType() == Material.ARROW) {
            openTreasureListGUI(player, page - 1);
            return;
        }

        if (slot == 53 && clickedItem.getType() == Material.ARROW) {
            openTreasureListGUI(player, page + 1);
            return;
        }

        // Handle treasure selection (delete)
        if (slot < 45 && clickedItem.getType() != Material.BLACK_STAINED_GLASS_PANE) {
            String treasureId = getTreasureIdFromItem(clickedItem);
            if (treasureId != null) {
                openConfirmDeleteGUI(player, treasureId);
            }
        }
    }

    private void handleConfirmDeleteClick(Player player, ItemStack clickedItem) {
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;

        String displayName = meta.getDisplayName();

        if (displayName.contains("Cancel")) {
            // Go back to list
            int page = playerPages.getOrDefault(player.getUniqueId(), 0);
            openTreasureListGUI(player, page);
            return;
        }

        if (displayName.contains("Confirm Delete")) {
            String treasureId = confirmDeletion.get(player.getUniqueId());
            if (treasureId != null) {
                if (treasureManager.deleteTreasure(treasureId)) {
                    player.sendMessage(colorize(getPrefix() + getMessage("treasure-deleted").replace("%id%", treasureId)));
                } else {
                    player.sendMessage(colorize(getPrefix() + getMessage("treasure-not-found").replace("%id%", treasureId)));
                }
                confirmDeletion.remove(player.getUniqueId());
            }
            player.closeInventory();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            String title = event.getView().getTitle();
            if (title.contains("Confirm Deletion")) {
                confirmDeletion.remove(player.getUniqueId());
            }
        }
    }

    private ItemStack createTreasureItem(Treasure treasure, int completedCount) {
        Material material = Material.valueOf(plugin.getConfig().getString("gui.treasure-material", "CHEST"));
        String locationStr = treasure.getLocationString();
        String command = treasure.getCommand();
        if (command.length() > 30) {
            command = command.substring(0, 30) + "...";
        }

        return createItem(material, "&6&l" + treasure.getId(),
                "&7Location: &f" + locationStr,
                "&7Command: &f" + command,
                "&7Found by: &f" + completedCount + " players",
                "",
                "&c&lClick to delete");
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(colorize(name));
            if (lore.length > 0) {
                List<String> loreList = new ArrayList<>();
                for (String line : lore) {
                    loreList.add(colorize(line));
                }
                meta.setLore(loreList);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private String getTreasureIdFromItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        String name = meta.getDisplayName();
        // Remove color codes and get the ID
        String plainName = name.replaceAll("§.", "").replaceAll("&.", "").replace("[", "").replace("]", "").trim();
        if (plainName.startsWith("Treasure: ")) {
            return plainName.substring(10);
        }
        return plainName;
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
