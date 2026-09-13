package com.fortunetowers.managers;

import com.fortunetowers.FortuneTowers;
import com.fortunetowers.objects.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ArenaManager {

    private final FortuneTowers plugin;
    private final Map<String, Arena> arenas = new HashMap<>();

    public ArenaManager(FortuneTowers plugin) {
        this.plugin = plugin;
    }

    public Arena getArena(String name) {
        return arenas.get(name.toLowerCase());
    }

    public Collection<Arena> getArenas() {
        return arenas.values();
    }

    public void createArena(String name) {
        arenas.put(name.toLowerCase(), new Arena(name));
        saveArena(getArena(name));
    }

    public void removeArena(String name) {
        Arena a = arenas.remove(name.toLowerCase());
        if (a != null) {
            File file = new File(plugin.getDataFolder() + "/arenas/" + name + ".yml");
            if (file.exists()) file.delete();
        }
    }

    public void stopAllGames() {
        for (Arena a : arenas.values()) {
            if (a.isRunning()) a.stop();
        }
    }

    public void loadArenas() {
        File dir = new File(plugin.getDataFolder(), "arenas");
        if (!dir.exists()) dir.mkdirs();

        for (File f : dir.listFiles((d, name) -> name.endsWith(".yml"))) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(f);
            String name = f.getName().replace(".yml", "");
            Arena arena = new Arena(name);

            if (config.contains("pos1")) arena.setPos1(config.getLocation("pos1"));
            if (config.contains("pos2")) arena.setPos2(config.getLocation("pos2"));
            if (config.contains("lobby")) arena.setLobby(config.getLocation("lobby"));
            arena.setIntervalSeconds(config.getInt("interval", 3));

            List<Location> spawns = (List<Location>) config.getList("spawns");
            if (spawns != null) arena.getSpawns().addAll(spawns);

            arenas.put(name.toLowerCase(), arena);
        }
    }

    public void saveArena(Arena arena) {
        File file = new File(plugin.getDataFolder() + "/arenas", arena.getName() + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        config.set("pos1", arena.getPos1());
        config.set("pos2", arena.getPos2());
        config.set("lobby", arena.getLobby());
        config.set("interval", arena.getIntervalSeconds());
        config.set("spawns", arena.getSpawns());

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
