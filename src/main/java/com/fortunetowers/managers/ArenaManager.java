package com.fortunetowers.managers;

import com.fortunetowers.FortuneTowers;
import com.fortunetowers.objects.Arena;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ArenaManager {

    private final FortuneTowers plugin;
    private final Map<String, Arena> arenas = new HashMap<>();
    private File file;
    private FileConfiguration config;

    public ArenaManager(FortuneTowers plugin) {
        this.plugin = plugin;
        loadArenas();
    }

    public void stopAllGames() {
        for (Arena arena : arenas.values()) {
            if (arena.isRunning()) {
                arena.stop();
            }
        }
    }

    public void loadArenas() {
        file = new File(plugin.getDataFolder(), "arenas.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(file);

        for (String key : config.getKeys(false)) {
            Arena arena = new Arena(key);
            arena.setPos1(config.getLocation(key + ".pos1"));
            arena.setPos2(config.getLocation(key + ".pos2"));
            arena.setLobby(config.getLocation(key + ".lobby"));
            arena.setSpecSpawn(config.getLocation(key + ".specspawn"));
            arena.setIntervalSeconds(config.getInt(key + ".interval", 3));

            List<?> list = config.getList(key + ".spawns");
            if (list != null) {
                for (Object item : list) {
                    if (item instanceof Location loc) {
                        arena.getSpawns().add(loc);
                    }
                }
            }

            arenas.put(key.toLowerCase(), arena);
        }
    }

    public void saveArena(Arena arena) {
        String key = arena.getName();
        config.set(key + ".pos1", arena.getPos1());
        config.set(key + ".pos2", arena.getPos2());
        config.set(key + ".lobby", arena.getLobby());
        config.set(key + ".specspawn", arena.getSpecSpawn());
        config.set(key + ".interval", arena.getIntervalSeconds());
        config.set(key + ".spawns", arena.getSpawns());

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createArena(String name) {
        Arena arena = new Arena(name);
        arenas.put(name.toLowerCase(), arena);
        saveArena(arena);
    }

    public void removeArena(String name) {
        Arena arena = arenas.remove(name.toLowerCase());
        if (arena != null) {
            if (arena.isRunning()) arena.stop();
            config.set(name, null);
            try {
                config.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Arena getArena(String name) {
        return arenas.get(name.toLowerCase());
    }

    public Collection<Arena> getArenas() {
        return arenas.values();
    }
}
