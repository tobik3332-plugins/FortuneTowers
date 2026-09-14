package com.fortunetowers.listeners;

import com.fortunetowers.FortuneTowers;
import com.fortunetowers.objects.Arena;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.List;

public class GameEventListener implements Listener {

    private final FortuneTowers plugin;

    public GameEventListener(FortuneTowers plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();

        for (Arena arena : plugin.getArenaManager().getArenas()) {
            if (arena.getActivePlayers().contains(p.getUniqueId()) || arena.getSpectators().contains(p.getUniqueId())) {
                String msg = e.getMessage().toLowerCase();

                if (msg.startsWith("/towers") && p.hasPermission("towers.use")) {
                    return;
                }

                List<String> allowed = plugin.getConfig().getStringList("allowed-commands-in-game");
                boolean isAllowed = false;
                for (String cmd : allowed) {
                    if (msg.startsWith(cmd.toLowerCase())) {
                        isAllowed = true;
                        break;
                    }
                }

                if (!isAllowed) {
                    e.setCancelled(true);
                    p.sendMessage(plugin.getMsg("command-blocked"));
                }
                return;
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();

        for (Arena arena : plugin.getArenaManager().getArenas()) {
            if (arena.isRunning() && arena.getActivePlayers().contains(p.getUniqueId())) {
                e.getDrops().clear();
                e.setDroppedExp(0);

                arena.getActivePlayers().remove(p.getUniqueId());
                arena.getSpectators().add(p.getUniqueId());

                arena.broadcast(plugin.getMsg("player-died-broadcast").replace("%player%", p.getName()));

                String title = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.death-title"));
                String sub = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.death-subtitle"));
                p.sendTitle(title, sub, 10, 40, 10);

                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    p.spigot().respawn();
                }, 2L);

                arena.checkWinner();
                break;
            }
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();

        for (Arena arena : plugin.getArenaManager().getArenas()) {
            if (arena.getSpectators().contains(p.getUniqueId())) {
                if (arena.isEnding() || !arena.isRunning()) {
                    Location mainLobby = plugin.getConfig().getLocation("main-lobby");
                    if (mainLobby != null) {
                        e.setRespawnLocation(mainLobby);
                    }
                } else {
                    Location specLoc = arena.getSpectatorSpawnLocation();
                    if (specLoc != null) {
                        e.setRespawnLocation(specLoc);
                    }

                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        arena.makeSpectator(p);
                        arena.updateVisibility();
                    }, 2L);
                }
                break;
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        for (Arena arena : plugin.getArenaManager().getArenas()) {
            if (arena.getSpectators().contains(p.getUniqueId())) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p) {
            for (Arena arena : plugin.getArenaManager().getArenas()) {
                if (arena.getSpectators().contains(p.getUniqueId())) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        for (Arena arena : plugin.getArenaManager().getArenas()) {
            if (arena.getSpectators().contains(p.getUniqueId())) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        for (Arena arena : plugin.getArenaManager().getArenas()) {
            if (arena.getSpectators().contains(p.getUniqueId())) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player p) {
            for (Arena arena : plugin.getArenaManager().getArenas()) {
                if (arena.getSpectators().contains(p.getUniqueId())) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }
}
