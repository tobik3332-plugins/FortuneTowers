package com.fortunetowers;

import com.fortunetowers.commands.TowersCommand;
import com.fortunetowers.listeners.GameEventListener;
import com.fortunetowers.managers.ArenaManager;
import com.fortunetowers.managers.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.List;

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

        // Dynamicka registrace prikazu pres CommandMap (kompatibilni s modernim Paperem)
        registerCustomCommand();

        getServer().getPluginManager().registerEvents(new GameEventListener(this), this);

        getLogger().info("FortuneTowers plugin byl uspesne zapnut!");
    }

    private void registerCustomCommand() {
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());

            TowersCommand handler = new TowersCommand(this);

            BukkitCommand customCmd = new BukkitCommand("towers") {
                @Override
                public boolean execute(org.bukkit.command.CommandSender sender, String commandLabel, String[] args) {
                    return handler.onCommand(sender, this, commandLabel, args);
                }

                @Override
                public List<String> tabComplete(org.bukkit.command.CommandSender sender, String alias, String[] args) {
                    return handler.onTabComplete(sender, this, alias, args);
                }
            };

            customCmd.setDescription("Hlavni prikaz pro FortuneTowers");
            customCmd.setPermission("towers.use");
            customCmd.setUsage("/towers");

            commandMap.register("fortunetowers", customCmd);
        } catch (Exception e) {
            getLogger().severe("Chyba pri registraci prikazu /towers: " + e.getMessage());
            e.printStackTrace();
        }
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
