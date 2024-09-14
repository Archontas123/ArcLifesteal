package org.mythofy.mythofylifestealplugin;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class PlayerDeathListener implements Listener {

    private final MythofyLifestealPlugin plugin;
    private final HeartManager heartManager;
    private final ItemManager itemManager;
    private final ConcurrentHashMap<UUID, Long> lastDeathTime = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private static final long DEATH_COOLDOWN = 5000; // 5 seconds cooldown

    public PlayerDeathListener(MythofyLifestealPlugin plugin) {
        this.plugin = plugin;
        this.heartManager = plugin.getHeartManager();
        this.itemManager = plugin.getItemManager();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerUUID = player.getUniqueId();

        // Lock to prevent multiple processing of the same death event
        lock.lock();
        try {
            long currentTime = System.currentTimeMillis();
            long lastProcessedTime = lastDeathTime.getOrDefault(playerUUID, 0L);

            if (currentTime - lastProcessedTime < DEATH_COOLDOWN) {
                plugin.getLogger().info("Player " + player.getName() + " is in death cooldown period. Ignoring event.");
                return;
            }
            lastDeathTime.put(playerUUID, currentTime);
        } finally {
            lock.unlock();
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                handleDeathEvent(event);
            } catch (Exception e) {
                plugin.getLogger().severe("Error handling player death: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void handleDeathEvent(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Player killer = player.getKiller();
        World world = player.getWorld();
        UUID playerUUID = player.getUniqueId();

        plugin.getLogger().info("Handling death event for player: " + player.getName());
        boolean dropHeartsOnDeath = plugin.getConfig().getBoolean("dropHearts");
        boolean dropHeartsIfMax = plugin.getConfig().getBoolean("dropHeartsIfMax");
        boolean announceElimination = plugin.getConfig().getBoolean("announceElimination");
        boolean disableBanOnElimination = plugin.getConfig().getBoolean("disablePlayerBanOnElimination");
        int maxHearts = heartManager.getMaxHearts(killer);
        if (!plugin.getConfig().getStringList("worlds").contains(world.getName())) {
            plugin.getLogger().info("World " + world.getName() + " is not in the configured worlds list.");
            return;
        }

        boolean isDeathByPlayer = killer != null && !killer.getUniqueId().equals(playerUUID);
        plugin.getLogger().info("Player " + player.getName() + " died. Is death by player: " + isDeathByPlayer);

        if (!isDeathByPlayer) {
            handleNaturalDeath(player, world, dropHeartsOnDeath, announceElimination, disableBanOnElimination);
        } else {
            handlePlayerKill(player, killer, world, dropHeartsOnDeath, dropHeartsIfMax, maxHearts, announceElimination, disableBanOnElimination);
        }
    }

    private void handleNaturalDeath(Player player, World world, boolean dropHeartsOnDeath, boolean announceElimination, boolean disableBanOnElimination) {
        try {
            plugin.getLogger().info("Handling natural death for player: " + player.getName());
            int playerHearts = heartManager.getHearts(player);
            plugin.getLogger().info("Player " + player.getName() + " has " + playerHearts + " hearts before natural death handling.");

            if (heartManager.decreaseHearts(player)) {
                plugin.getLogger().info("Player " + player.getName() + " lost a heart. Now has " + heartManager.getHearts(player) + " hearts.");
            }

            if (dropHeartsOnDeath) {
                world.dropItemNaturally(player.getLocation(), itemManager.createHeartItem());
            }

            if (heartManager.getHearts(player) <= 0) {
                if (announceElimination) {
                    Bukkit.broadcastMessage("§c" + player.getName() + " has been eliminated!");
                }
                if (!disableBanOnElimination) {
                    heartManager.eliminatePlayer(player);
                } else {
                    heartManager.setHearts(player, 0);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error handling natural death: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handlePlayerKill(Player player, Player killer, World world, boolean dropHeartsOnDeath, boolean dropHeartsIfMax, int maxHearts, boolean announceElimination, boolean disableBanOnElimination) {

        try {

            plugin.getLogger().info("Handling player kill for player: " + player.getName() + " killed by " + killer.getName());
            plugin.getLogger().info("Max Hearts allowed: " + maxHearts);

            int killerHearts = heartManager.getHearts(killer);
            plugin.getLogger().info("Killer " + killer.getName() + " has " + killerHearts + " hearts before gain.");

            if (heartManager.decreaseHearts(player)) {
                plugin.getLogger().info("Player " + player.getName() + " lost a heart from being killed by " + killer.getName() + ". Now has " + heartManager.getHearts(player) + " hearts.");
            }

            if (heartManager.getHearts(player) <= 0) {
                if (announceElimination) {
                    Bukkit.broadcastMessage("§c" + player.getName() + " has been eliminated by " + killer.getName() + "!");
                }
                if (!disableBanOnElimination) {
                    heartManager.eliminatePlayer(player);
                } else {
                    heartManager.setHearts(player, 0);
                }
            }

            if (killerHearts + 1 <= maxHearts) {
                heartManager.increaseHearts(killer); // Increase the killer's hearts directly
                plugin.getLogger().info("Killer " + killer.getName() + " gained a heart. Now has " + heartManager.getHearts(killer) + " hearts.");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error handling player kill: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
