package com.fortunetowers.managers;

import com.fortunetowers.FortuneTowers;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {

    private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();
    private final Map<UUID, ItemStack[]> savedArmor = new HashMap<>();
    private final Map<UUID, GameMode> savedGameModes = new HashMap<>();

    public void saveAndClearPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        savedInventories.put(uuid, player.getInventory().getContents());
        savedArmor.put(uuid, player.getInventory().getArmorContents());
        savedGameModes.put(uuid, player.getGameMode());

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setAllowFlight(false);
        player.setFlying(false);
    }

    public void restorePlayer(Player player) {
        UUID uuid = player.getUniqueId();

        // Zviditelnit hrace pro vsechny
        for (Player other : Bukkit.getOnlinePlayers()) {
            other.showPlayer(FortuneTowers.getInstance(), player);
            player.showPlayer(FortuneTowers.getInstance(), other);
        }

        player.setAllowFlight(false);
        player.setFlying(false);

        if (savedInventories.containsKey(uuid)) {
            player.getInventory().clear();
            player.getInventory().setContents(savedInventories.remove(uuid));
            player.getInventory().setArmorContents(savedArmor.remove(uuid));
            player.setGameMode(savedGameModes.getOrDefault(uuid, GameMode.SURVIVAL));
            savedGameModes.remove(uuid);
        }

        Location mainLobby = FortuneTowers.getInstance().getConfig().getLocation("main-lobby");
        if (mainLobby != null) {
            player.teleport(mainLobby);
        }
    }

    public boolean hasSavedData(UUID uuid) {
        return savedInventories.containsKey(uuid);
    }
}
