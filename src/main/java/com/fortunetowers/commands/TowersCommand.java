package com.fortunetowers.commands;

import com.fortunetowers.FortuneTowers;
import com.fortunetowers.objects.Arena;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class TowersCommand implements CommandExecutor, TabCompleter {

    private final FortuneTowers plugin;
    private final Map<String, Long> removeConfirmations = new HashMap<>();

    public TowersCommand(FortuneTowers plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) return true;

        String sub = args[0].toLowerCase();

        if (sub.equals("reload")) {
            if (!sender.hasPermission("towers.permission.reload")) return noPerm(sender);
            plugin.reloadConfig();
            sender.sendMessage(plugin.getMsg("reloaded"));
            return true;
        }

        if (sub.equals("setmainlobby") && sender instanceof Player p) {
            if (!p.hasPermission("towers.permission.setmainlobby")) return noPerm(p);
            plugin.getConfig().set("main-lobby", p.getLocation());
            plugin.saveConfig();
            p.sendMessage(plugin.getMsg("mainlobby-set"));
            return true;
        }

        if (sub.equals("removemainlobby")) {
            if (!sender.hasPermission("towers.permission.removemainlobby")) return noPerm(sender);
            plugin.getConfig().set("main-lobby", null);
            plugin.saveConfig();
            sender.sendMessage(plugin.getMsg("mainlobby-removed"));
            return true;
        }

        if (sub.equals("arena") && args.length >= 2) {
            String action = args[1].toLowerCase();

            if (action.equals("create") && args.length >= 3) {
                if (!sender.hasPermission("towers.permission.arena.create")) return noPerm(sender);
                String name = args[2];
                Arena arena = plugin.getArenaManager().getArena(name);

                if (arena == null) {
                    plugin.getArenaManager().createArena(name);
                    sender.sendMessage(plugin.getMsg("arena-created-init"));
                } else {
                    if (arena.getPos1() != null && arena.getPos2() != null) {
                        plugin.getArenaManager().saveArena(arena);
                        sender.sendMessage(plugin.getMsg("arena-created-success").replace("%arena%", name));
                    } else {
                        sender.sendMessage(plugin.getMsg("arena-created-init"));
                    }
                }
                return true;
            }

            if ((action.equals("pos1") || action.equals("pos2")) && args.length >= 3 && sender instanceof Player p) {
                if (!p.hasPermission("towers.permission.arena.pos")) return noPerm(p);
                Arena arena = plugin.getArenaManager().getArena(args[2]);
                if (arena == null) return true;

                if (action.equals("pos1")) {
                    arena.setPos1(p.getLocation());
                    p.sendMessage(plugin.getMsg("pos1-set").replace("%loc%", p.getLocation().toVector().toString()));
                } else {
                    arena.setPos2(p.getLocation());
                    p.sendMessage(plugin.getMsg("pos2-set").replace("%loc%", p.getLocation().toVector().toString()));
                }
                return true;
            }

            if (action.equals("addspawn") && args.length >= 3 && sender instanceof Player p) {
                if (!p.hasPermission("towers.permission.arena.addspawn")) return noPerm(p);
                Arena arena = plugin.getArenaManager().getArena(args[2]);
                if (arena != null) {
                    arena.getSpawns().add(p.getLocation());
                    int id = arena.getSpawns().size() - 1;
                    plugin.getArenaManager().saveArena(arena);
                    p.sendMessage(plugin.getMsg("spawn-added").replace("%id%", String.valueOf(id)).replace("%arena%", arena.getName()));
                }
                return true;
            }

            if (action.equals("removespawn") && args.length >= 4) {
                if (!sender.hasPermission("towers.permission.arena.removespawn")) return noPerm(sender);
                Arena arena = plugin.getArenaManager().getArena(args[2]);
                if (arena != null) {
                    int id = Integer.parseInt(args[3]);
                    if (id >= 0 && id < arena.getSpawns().size()) {
                        arena.getSpawns().remove(id);
                        plugin.getArenaManager().saveArena(arena);
                        sender.sendMessage(plugin.getMsg("spawn-removed").replace("%id%", String.valueOf(id)));
                    }
                }
                return true;
            }

            if (action.equals("setlobby") && args.length >= 3 && sender instanceof Player p) {
                if (!p.hasPermission("towers.permission.arena.setlobby")) return noPerm(p);
                Arena arena = plugin.getArenaManager().getArena(args[2]);
                if (arena != null) {
                    arena.setLobby(p.getLocation());
                    plugin.getArenaManager().saveArena(arena);
                    p.sendMessage(plugin.getMsg("lobby-set").replace("%arena%", arena.getName()));
                }
                return true;
            }

            if (action.equals("removelobby") && args.length >= 3) {
                if (!sender.hasPermission("towers.permission.arena.removelobby")) return noPerm(sender);
                Arena arena = plugin.getArenaManager().getArena(args[2]);
                if (arena != null) {
                    arena.setLobby(null);
                    plugin.getArenaManager().saveArena(arena);
                    sender.sendMessage(plugin.getMsg("lobby-removed").replace("%arena%", arena.getName()));
                }
                return true;
            }

            if (action.equals("interval") && args.length >= 4) {
                if (!sender.hasPermission("towers.permission.arena.interval")) return noPerm(sender);
                Arena arena = plugin.getArenaManager().getArena(args[2]);
                if (arena != null) {
                    int sec = Integer.parseInt(args[3]);
                    arena.setIntervalSeconds(sec);
                    plugin.getArenaManager().saveArena(arena);
                    sender.sendMessage(plugin.getMsg("interval-set").replace("%arena%", arena.getName()).replace("%sec%", String.valueOf(sec)));
                }
                return true;
            }

            if (action.equals("remove") && args.length >= 3) {
                if (!sender.hasPermission("towers.permission.arena.remove")) return noPerm(sender);
                String name = args[2];
                long now = System.currentTimeMillis();

                if (removeConfirmations.containsKey(name) && (now - removeConfirmations.get(name) <= 5000)) {
                    plugin.getArenaManager().removeArena(name);
                    removeConfirmations.remove(name);
                    sender.sendMessage(plugin.getMsg("arena-removed").replace("%arena%", name));
                } else {
                    removeConfirmations.put(name, now);
                    sender.sendMessage(plugin.getMsg("arena-remove-confirm").replace("%arena%", name));
                }
                return true;
            }
        }

        if (sub.equals("join") && args.length >= 3) {
            if (!sender.hasPermission("towers.permission.join")) return noPerm(sender);
            Arena arena = plugin.getArenaManager().getArena(args[1]);
            Player target = Bukkit.getPlayer(args[2]);
            if (arena != null && target != null) {
                plugin.getPlayerDataManager().saveAndClearPlayer(target);
                arena.getActivePlayers().add(target.getUniqueId());
                if (arena.getLobby() != null) target.teleport(arena.getLobby());
                sender.sendMessage(plugin.getMsg("player-joined").replace("%player%", target.getName()).replace("%arena%", arena.getName()));
            }
            return true;
        }

        if (sub.equals("leave") && args.length >= 3) {
            if (!sender.hasPermission("towers.permission.leave")) return noPerm(sender);
            Arena arena = plugin.getArenaManager().getArena(args[1]);
            Player target = Bukkit.getPlayer(args[2]);
            if (arena != null && target != null) {
                arena.getActivePlayers().remove(target.getUniqueId());
                arena.getSpectators().remove(target.getUniqueId());
                plugin.getPlayerDataManager().restorePlayer(target);
                sender.sendMessage(plugin.getMsg("player-left").replace("%player%", target.getName()).replace("%arena%", arena.getName()));
            }
            return true;
        }

        if (sub.equals("start") && args.length >= 2) {
            if (!sender.hasPermission("towers.permission.start")) return noPerm(sender);
            Arena arena = plugin.getArenaManager().getArena(args[1]);
            if (arena != null && !arena.isRunning()) {
                arena.start();
                sender.sendMessage(plugin.getMsg("game-started").replace("%arena%", arena.getName()));
            }
            return true;
        }

        if (sub.equals("stop") && args.length >= 2) {
            if (!sender.hasPermission("towers.permission.stop")) return noPerm(sender);
            Arena arena = plugin.getArenaManager().getArena(args[1]);
            if (arena != null && arena.isRunning()) {
                arena.stop();
                sender.sendMessage(plugin.getMsg("game-stopped").replace("%arena%", arena.getName()));
            }
            return true;
        }

        return true;
    }

    private boolean noPerm(CommandSender sender) {
        sender.sendMessage(plugin.getMsg("no-permission"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            return Arrays.asList("arena", "join", "leave", "start", "stop", "setmainlobby", "removemainlobby", "reload");
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("arena")) {
                return Arrays.asList("create", "pos1", "pos2", "addspawn", "removespawn", "setlobby", "removelobby", "interval", "remove");
            }
            if (args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase("leave") || args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("stop")) {
                for (Arena a : plugin.getArenaManager().getArenas()) {
                    suggestions.add(a.getName());
                }
                return suggestions;
            }
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("arena")) {
                for (Arena a : plugin.getArenaManager().getArenas()) {
                    suggestions.add(a.getName());
                }
                return suggestions;
            }
            if (args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase("leave")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    suggestions.add(p.getName());
                }
                return suggestions;
            }
        }

        return Collections.emptyList();
    }
}
