package com.fortunetowers.listeners;

import com.fortunetowers.FortuneTowers;
import com.fortunetowers.objects.Arena;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

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
                arena.getActivePlayers().remove(p.getUniqueId());
                arena.getSpectators().add(p.getUniqueId());

                p.setGameMode(GameMode.SPECTATOR);

                if (!arena.getSpawns().isEmpty()) {
                    p.teleport(arena.getSpawns().get(0));
                }
                break;
            }
        }
    }
}
