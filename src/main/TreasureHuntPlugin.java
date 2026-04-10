package com.treasurehunt;

import com.treasurehunt.commands.TreasureCommand;
import com.treasurehunt.database.DatabaseManager;
import com.treasurehunt.gui.TreasureGUI;
import com.treasurehunt.listeners.PlayerInteractListener;
import com.treasurehunt.managers.TreasureManager;
import org.bukkit.plugin.java.JavaPlugin;

public class TreasureHuntPlugin extends JavaPlugin {

    private static TreasureHuntPlugin instance;
    private DatabaseManager databaseManager;
    private TreasureManager treasureManager;
    private TreasureGUI treasureGUI;

    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config
        saveDefaultConfig();
        
        // Initialize database manager
        databaseManager = new DatabaseManager(this);
        if (!databaseManager.connect()) {
            getLogger().severe("Failed to connect to database! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize managers
        treasureManager = new TreasureManager(this, databaseManager);
        treasureGUI = new TreasureGUI(this, treasureManager);
        
        // Load treasures from database
        treasureManager.loadTreasures();
        
        // Register commands
        getCommand("treasure").setExecutor(new TreasureCommand(this, treasureManager, treasureGUI));
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this, treasureManager), this);
        getServer().getPluginManager().registerEvents(treasureGUI, this);
        
        getLogger().info("TreasureHunt has been enabled!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        getLogger().info("TreasureHunt has been disabled!");
    }

    public static TreasureHuntPlugin getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public TreasureManager getTreasureManager() {
        return treasureManager;
    }

    public TreasureGUI getTreasureGUI() {
        return treasureGUI;
    }
}
