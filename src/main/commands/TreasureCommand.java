package com.treasurehunt.commands;

import com.treasurehunt.TreasureHuntPlugin;
import com.treasurehunt.gui.TreasureGUI;
import com.treasurehunt.managers.TreasureManager;
import com.treasurehunt.models.Treasure;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class TreasureCommand implements CommandExecutor {

    private final TreasureHuntPlugin plugin;
    private final TreasureManager treasureManager;
    private final TreasureGUI treasureGUI;

    public TreasureCommand(TreasureHuntPlugin plugin, TreasureManager treasureManager, TreasureGUI treasureGUI) {
        this.plugin = plugin;
        this.treasureManager = treasureManager;
        this.treasureGUI = treasureGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("treasurehunt.admin")) {
            sender.sendMessage(colorize(getPrefix() + getMessage("no-permission")));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(colorize(getPrefix() + getMessage("invalid-usage")));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                return handleCreate(sender, args);
            case "delete":
                return handleDelete(sender, args);
            case "completed":
                return handleCompleted(sender, args);
            case "list":
                return handleList(sender);
            case "gui":
                return handleGui(sender);
            default:
                sender.sendMessage(colorize(getPrefix() + getMessage("invalid-usage")));
                return true;
        }
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(colorize(getPrefix() + getMessage("player-only")));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(colorize(getPrefix() + "&cUsage: /treasure create <id> <command>"));
            return true;
        }

        String treasureId = args[1];
        // Join remaining args as the command
        StringBuilder commandBuilder = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            if (i > 2) commandBuilder.append(" ");
            commandBuilder.append(args[i]);
        }
        String cmd = commandBuilder.toString();

        // Check if treasure ID already exists
        if (treasureManager.getTreasure(treasureId) != null) {
            sender.sendMessage(colorize(getPrefix() + "&cA treasure with ID '" + treasureId + "' already exists!"));
            return true;
        }

        // Set pending creation
        treasureManager.setPendingCreation(player.getUniqueId(), treasureId, cmd);
        sender.sendMessage(colorize(getPrefix() + getMessage("select-block")));

        return true;
    }

    private boolean handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(colorize(getPrefix() + "&cUsage: /treasure delete <id>"));
            return true;
        }

        String treasureId = args[1];

        if (treasureManager.deleteTreasure(treasureId)) {
            sender.sendMessage(colorize(getPrefix() + getMessage("treasure-deleted").replace("%id%", treasureId)));
        } else {
            sender.sendMessage(colorize(getPrefix() + getMessage("treasure-not-found").replace("%id%", treasureId)));
        }

        return true;
    }

    private boolean handleCompleted(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(colorize(getPrefix() + "&cUsage: /treasure completed <id>"));
            return true;
        }

        String treasureId = args[1];
        Treasure treasure = treasureManager.getTreasure(treasureId);

        if (treasure == null) {
            sender.sendMessage(colorize(getPrefix() + getMessage("treasure-not-found").replace("%id%", treasureId)));
            return true;
        }

        List<UUID> completedPlayers = treasureManager.getCompletedPlayers(treasureId);

        sender.sendMessage(colorize(getPrefix() + "&6Players who found '" + treasureId + "':"));
        if (completedPlayers.isEmpty()) {
            sender.sendMessage(colorize("&7No one has found this treasure yet."));
        } else {
            String playerList = completedPlayers.stream()
                    .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("&7, &f"));
            sender.sendMessage(colorize("&f" + playerList));
            sender.sendMessage(colorize("&7Total: " + completedPlayers.size() + " players"));
        }

        return true;
    }

    private boolean handleList(CommandSender sender) {
        Collection<Treasure> treasures = treasureManager.getAllTreasures();

        if (treasures.isEmpty()) {
            sender.sendMessage(colorize(getPrefix() + getMessage("no-treasures")));
            return true;
        }

        sender.sendMessage(colorize(getPrefix() + "&6Treasure List:"));
        for (Treasure treasure : treasures) {
            int completedCount = treasureManager.getCompletedPlayers(treasure.getId()).size();
            sender.sendMessage(colorize("&7- &f" + treasure.getId() + " &7at &f" + treasure.getLocationString() +
                    " &7(" + completedCount + " found)"));
        }
        sender.sendMessage(colorize("&7Total: " + treasures.size() + " treasures"));

        return true;
    }

    private boolean handleGui(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(colorize(getPrefix() + getMessage("player-only")));
            return true;
        }

        treasureGUI.openTreasureListGUI(player, 0);
        return true;
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
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessage("prefix") + "§cOnly players can use this command.");
            return true;
        }
        
        if (!player.hasPermission("treasurehunt.admin")) {
            player.sendMessage(plugin.getMessage("prefix") + plugin.getMessage("no-permission"));
            return true;
        }
        
        if (args.length == 0) {
            // Open GUI by default
            treasureGUI.openGUI(player, 0);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "create" -> handleCreate(player, args);
            case "delete" -> handleDelete(player, args);
            case "completed" -> handleCompleted(player, args);
            case "list" -> handleList(player);
            case "gui" -> treasureGUI.openGUI(player, 0);
            default -> player.sendMessage(plugin.getMessage("prefix") + plugin.getMessage("invalid-usage"));
        }
        
        return true;
    }
    
    private void handleCreate(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(plugin.getMessage("prefix") + "§cUsage: /treasure create <id> <command>");
            return;
        }
        
        String id = args[1];
        
        // Validate ID (alphanumeric and underscores only)
        if (!id.matches("^[a-zA-Z0-9_]+$")) {
            player.sendMessage(plugin.getMessage("prefix") + "§cTreasure ID must only contain letters, numbers, and underscores.");
            return;
        }
        
        // Check if treasure already exists
        if (databaseManager.getTreasure(id) != null) {
            player.sendMessage(plugin.getMessage("prefix") + plugin.getMessage("treasure-already-exists", "%id%", id));
            return;
        }
        
        // Build command from remaining args
        StringBuilder commandBuilder = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            if (i > 2) commandBuilder.append(" ");
            commandBuilder.append(args[i]);
        }
        String cmd = commandBuilder.toString();
        
        // Start selection mode
        selectionManager.startSelection(player.getUniqueId(), id, cmd);
        player.sendMessage(plugin.getMessage("prefix") + plugin.getMessage("select-block"));
    }
    
    private void handleDelete(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getMessage("prefix") + "§cUsage: /treasure delete <id>");
            return;
        }
        
        String id = args[1];
        
        if (databaseManager.deleteTreasure(id)) {
            player.sendMessage(plugin.getMessage("prefix") + plugin.getMessage("treasure-deleted", "%id%", id));
        } else {
            player.sendMessage(plugin.getMessage("prefix") + plugin.getMessage("treasure-not-found", "%id%", id));
        }
    }
    
    private void handleCompleted(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getMessage("prefix") + "§cUsage: /treasure completed <id>");
            return;
        }
        
        String id = args[1];
        Treasure treasure = databaseManager.getTreasure(id);
        
        if (treasure == null) {
            player.sendMessage(plugin.getMessage("prefix") + plugin.getMessage("treasure-not-found", "%id%", id));
            return;
        }
        
        List<TreasureCompletion> completions = databaseManager.getCompletions(id);
        
        if (completions.isEmpty()) {
            player.sendMessage(plugin.getMessage("prefix") + plugin.getMessage("no-completions"));
            return;
        }
        
        player.sendMessage(plugin.getMessage("prefix") + plugin.getMessage("completed-header", "%id%", id));
        for (TreasureCompletion completion : completions) {
            String date = dateFormat.format(new Date(completion.getCompletedAt()));
            player.sendMessage("§e- §f" + completion.getPlayerName() + " §7(" + date + ")");
        }
        player.sendMessage("§7Total: " + completions.size() + " players");
    }
    
    private void handleList(Player player) {
        Collection<Treasure> treasures = databaseManager.getAllTreasures();
        
        if (treasures.isEmpty()) {
            player.sendMessage(plugin.getMessage("prefix") + plugin.getMessage("no-treasures"));
            return;
        }
        
        player.sendMessage(plugin.getMessage("prefix") + plugin.getMessage("treasure-list-header"));
        for (Treasure treasure : treasures) {
            int completions = databaseManager.getCompletionCount(treasure.getId());
            String msg = plugin.getMessage("treasure-list-item", 
                "%id%", treasure.getId(),
                "%world%", treasure.getWorld(),
                "%x%", String.valueOf(treasure.getX()),
                "%y%", String.valueOf(treasure.getY()),
                "%z%", String.valueOf(treasure.getZ()));
            player.sendMessage(msg + " §7[" + completions + " found]");
        }
        player.sendMessage("§7Total: " + treasures.size() + " treasures");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("treasurehunt.admin")) {
            return Collections.emptyList();
        }
        
        if (args.length == 1) {
            return Arrays.asList("create", "delete", "completed", "list", "gui").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("delete") || subCommand.equals("completed")) {
                return databaseManager.getAllTreasures().stream()
                        .map(Treasure::getId)
                        .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        
        return Collections.emptyList();
    }
}
