package com.fortunetowers.objects;

import com.fortunetowers.FortuneTowers;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class Arena {

    private final String name;
    private Location pos1;
    private Location pos2;
    private Location lobby;
    private Location specSpawn;
    private final List<Location> spawns = new ArrayList<>();
    private int intervalSeconds = 3;

    private boolean running = false;
    private final Set<UUID> activePlayers = new HashSet<>();
    private final Set<UUID> spectators = new HashSet<>();
    private final List<BlockState> originalBlocks = new ArrayList<>();

    private BukkitTask itemTask;
    private BukkitTask borderTask;

    public Arena(String name) {
        this.name = name;
    }

    public void broadcast(String message) {
        Set<UUID> all = new HashSet<>(activePlayers);
        all.addAll(spectators);
        for (UUID uuid : all) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(message);
        }
    }

    public Location getSpectatorSpawnLocation() {
        if (specSpawn != null) return specSpawn;
        if (!spawns.isEmpty()) return spawns.get(0);
        return lobby;
    }

    public void saveSnapshot() {
        originalBlocks.clear();
        if (pos1 == null || pos2 == null) return;
        World world = pos1.getWorld();
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    originalBlocks.add(world.getBlockAt(x, y, z).getState());
                }
            }
        }
    }

    public void rollback() {
        for (BlockState state : originalBlocks) {
            state.update(true, false);
        }
    }

    public void start() {
        this.running = true;
        saveSnapshot();

        int spawnIndex = 0;
        for (UUID uuid : activePlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && !spawns.isEmpty()) {
                p.teleport(spawns.get(spawnIndex % spawns.size()));
                spawnIndex++;
            }
        }

        for (UUID uuid : spectators) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                makeSpectator(p);
                Location loc = getSpectatorSpawnLocation();
                if (loc != null) p.teleport(loc);
            }
        }

        updateVisibility();

        itemTask = new BukkitRunnable() {
            @Override
            public void run() {
                giveRandomItems();
            }
        }.runTaskTimer(FortuneTowers.getInstance(), 20L * intervalSeconds, 20L * intervalSeconds);

        borderTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkBorders();
            }
        }.runTaskTimer(FortuneTowers.getInstance(), 10L, 10L);
    }

    public void makeSpectator(Player p) {
        p.setGameMode(GameMode.SURVIVAL);
        p.setAllowFlight(true);
        p.setFlying(true);
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
    }

    public void updateVisibility() {
        for (UUID specUUID : spectators) {
            Player spec = Bukkit.getPlayer(specUUID);
            if (spec == null) continue;

            for (UUID activeUUID : activePlayers) {
                Player active = Bukkit.getPlayer(activeUUID);
                if (active != null) active.hidePlayer(spec);
            }

            for (UUID otherSpecUUID : spectators) {
                Player otherSpec = Bukkit.getPlayer(otherSpecUUID);
                if (otherSpec != null) spec.showPlayer(otherSpec);
            }
        }
    }

    public void checkWinner() {
        if (!running) return;

        if (activePlayers.size() == 1) {
            UUID winnerUUID = activePlayers.iterator().next();
            Player winner = Bukkit.getPlayer(winnerUUID);
            if (winner != null) {
                broadcast(FortuneTowers.getInstance().getMsg("game-winner-broadcast").replace("%player%", winner.getName()));
            }
            stop();
        } else if (activePlayers.isEmpty()) {
            stop();
        }
    }

    public void stop() {
        this.running = false;
        if (itemTask != null) itemTask.cancel();
        if (borderTask != null) borderTask.cancel();

        Set<UUID> all = new HashSet<>(activePlayers);
        all.addAll(spectators);

        for (UUID uuid : all) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                FortuneTowers.getInstance().getPlayerDataManager().restorePlayer(p);
            }
        }
        activePlayers.clear();
        spectators.clear();
        rollback();
    }

    private void giveRandomItems() {
        Material[] materials = Material.values();
        List<String> blacklist = FortuneTowers.getInstance().getConfig().getStringList("item-blacklist");
        Random r = new Random();

        for (UUID uuid : activePlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) continue;

            Material mat;
            do {
                mat = materials[r.nextInt(materials.length)];
            } while (!mat.isItem() || mat.isAir() || blacklist.contains(mat.name()));

            ItemStack item = new ItemStack(mat, 1);
            HashMap<Integer, ItemStack> left = p.getInventory().addItem(item);
            if (!left.isEmpty()) {
                p.getWorld().dropItem(p.getLocation(), item);
            }
        }
    }

    private void checkBorders() {
        if (pos1 == null || pos2 == null) return;

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        String title = ChatColor.translateAlternateColorCodes('&', FortuneTowers.getInstance().getConfig().getString("messages.border-warning-title"));
        String sub = ChatColor.translateAlternateColorCodes('&', FortuneTowers.getInstance().getConfig().getString("messages.border-warning-subtitle"));

        for (UUID uuid : new HashSet<>(activePlayers)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;

            Location loc = p.getLocation();
            int pX = loc.getBlockX();
            int pZ = loc.getBlockZ();

            if (pX < minX || pX > maxX || pZ < minZ || pZ > maxZ) {
                p.setHealth(0.0);
                continue;
            }

            int distToEdge = Math.min(
                Math.min(pX - minX, maxX - pX),
                Math.min(pZ - minZ, maxZ - pZ)
            );

            if (distToEdge <= 5) {
                p.sendTitle(title, sub, 0, 20, 10);
                double damage = (6 - distToEdge) * 1.5;
                p.damage(damage);
            }
        }

        for (UUID uuid : spectators) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            Location loc = p.getLocation();
            if (loc.getBlockX() < minX || loc.getBlockX() > maxX || loc.getBlockZ() < minZ || loc.getBlockZ() > maxZ) {
                Location specLoc = getSpectatorSpawnLocation();
                if (specLoc != null) p.teleport(specLoc);
            }
        }
    }

    public String getName() { return name; }
    public Location getPos1() { return pos1; }
    public void setPos1(Location pos1) { this.pos1 = pos1; }
    public Location getPos2() { return pos2; }
    public void setPos2(Location pos2) { this.pos2 = pos2; }
    public Location getLobby() { return lobby; }
    public void setLobby(Location lobby) { this.lobby = lobby; }
    public Location getSpecSpawn() { return specSpawn; }
    public void setSpecSpawn(Location specSpawn) { this.specSpawn = specSpawn; }
    public List<Location> getSpawns() { return spawns; }
    public int getIntervalSeconds() { return intervalSeconds; }
    public void setIntervalSeconds(int intervalSeconds) { this.intervalSeconds = intervalSeconds; }
    public boolean isRunning() { return running; }
    public Set<UUID> getActivePlayers() { return activePlayers; }
    public Set<UUID> getSpectators() { return spectators; }
}
