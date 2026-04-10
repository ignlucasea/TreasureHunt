package com.spaleforce.treasurehunt.commands;

import com.spaleforce.treasurehunt.TreasureHunt;
import com.spaleforce.treasurehunt.database.DatabaseManager;
import com.spaleforce.treasurehunt.gui.TreasureGUI;
import com.spaleforce.treasurehunt.managers.SelectionManager;
import com.spaleforce.treasurehunt.models.Treasure;
import com.spaleforce.treasurehunt.models.TreasureCompletion;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class TreasureCommand implements CommandExecutor, TabCompleter {
    
    private final TreasureHunt plugin;
    private final DatabaseManager databaseManager;
    private final SelectionManager selectionManager;
    private final TreasureGUI treasureGUI;
    private final SimpleDateFormat dateFormat;
    
    public TreasureCommand(TreasureHunt plugin, DatabaseManager databaseManager, 
                          SelectionManager selectionManager, TreasureGUI treasureGUI) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.selectionManager = selectionManager;
        this.treasureGUI = treasureGUI;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    }
    
    @Override
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
