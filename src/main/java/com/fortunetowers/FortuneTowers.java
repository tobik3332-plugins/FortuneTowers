package com.fortunetowers;

import com.fortunetowers.commands.TowersCommand;
import com.fortunetowers.listeners.GameEventListener;
import com.fortunetowers.managers.ArenaManager;
import com.fortunetowers.managers.PlayerDataManager;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class FortuneTowers extends JavaPlugin {

    private static FortuneTowers instance;
    private ArenaManager arenaManager;
    private PlayerDataManager playerDataManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.playerDataManager = new PlayerDataManager();
        this.arenaManager = new ArenaManager(this);
        this.arenaManager.loadArenas();

        getCommand("towers").setExecutor(new TowersCommand(this));
        getCommand("towers").setTabCompleter(new TowersCommand(this));

        getServer().getPluginManager().registerEvents(new GameEventListener(this), this);

        getLogger().info("FortuneTowers plugin byl uspesne zapnut!");
    }

    @Override
    public void onDisable() {
        if (arenaManager != null) {
            arenaManager.stopAllGames();
        }
        getLogger().info("FortuneTowers plugin byl vypnut.");
    }

    public static FortuneTowers getInstance() {
        return instance;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public String getMsg(String path) {
        String msg = getConfig().getString("messages." + path, "");
        String prefix = getConfig().getString("messages.prefix", "");
        return ChatColor.translateAlternateColorCodes('&', prefix + msg);
    }
}
