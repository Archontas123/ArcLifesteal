package org.mythofy.mythofylifestealplugin;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDeathListener implements Listener {

    private final MythofyLifestealPlugin plugin;
    private final HeartManager heartManager;
    private final ItemManager itemManager;
    private final ConcurrentHashMap<UUID, Long> lastDeathTime = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> lastHeartGain = new ConcurrentHashMap<>();

    public PlayerDeathListener(MythofyLifestealPlugin plugin) {
        this.plugin = plugin;
        this.heartManager = plugin.getHeartManager();
        this.itemManager = plugin.getItemManager();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Player killer = player.getKiller();
        World world = player.getWorld();
        UUID playerUUID = player.getUniqueId();

        // Cooldown to prevent multiple processing of the same death event
        long currentTime = System.currentTimeMillis();
        long lastProcessedTime = lastDeathTime.getOrDefault(playerUUID, 0L);
        if (currentTime - lastProcessedTime < 1000) {
            return;
        }
        lastDeathTime.put(playerUUID, currentTime);

        try {
            plugin.getLogger().info("Handling death event for player: " + player.getName());
            boolean heartRewardOnElimination = plugin.getConfig().getBoolean("heartRewardOnElimination");
            boolean disableBanOnElimination = plugin.getConfig().getBoolean("disablePlayerBanOnElimination");
            boolean announceElimination = plugin.getConfig().getBoolean("announceElimination");
            boolean dropHeartsOnDeath = plugin.getConfig().getBoolean("dropHearts");
            boolean dropHeartsIfMax = plugin.getConfig().getBoolean("dropHeartsIfMax");
            int maxHearts = plugin.getConfig().getInt("maxHearts") * 2;
            boolean heartGainCooldownEnabled = plugin.getConfig().getBoolean("heartGainCooldown.enabled");
            long heartGainCooldown = plugin.getConfig().getLong("heartGainCooldown.cooldown");
            boolean heartGainCooldowndropOnCooldown = plugin.getConfig().getBoolean("heartGainCooldown.dropOnCooldown");

            if (!plugin.getConfig().getStringList("worlds").contains(world.getName())) {
                plugin.getLogger().info("World " + world.getName() + " is not in the configured worlds list.");
                return;
            }

            boolean isDeathByPlayer = killer != null && !killer.getUniqueId().equals(playerUUID);
            plugin.getLogger().info("Player " + player.getName() + " died. Is death by player: " + isDeathByPlayer);

            if (!isDeathByPlayer && plugin.getConfig().getBoolean("looseHeartsToNature")) {
                handleNaturalDeath(player, world, dropHeartsOnDeath, announceElimination, disableBanOnElimination);
                return;
            }

            if (isDeathByPlayer && plugin.getConfig().getBoolean("looseHeartsToPlayer")) {
                handlePlayerKill(player, killer, world, dropHeartsOnDeath, dropHeartsIfMax, heartRewardOnElimination, maxHearts, heartGainCooldownEnabled, heartGainCooldown, heartGainCooldowndropOnCooldown, announceElimination, disableBanOnElimination);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error handling player death: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleNaturalDeath(Player player, World world, boolean dropHeartsOnDeath, boolean announceElimination, boolean disableBanOnElimination) {
        try {
            plugin.getLogger().info("Handling natural death for player: " + player.getName());
            int playerHearts = heartManager.getHearts(player);
            plugin.getLogger().info("Player " + player.getName() + " has " + playerHearts + " hearts before natural death handling.");

            boolean heartDecreased = heartManager.decreaseHearts(player);
            if (heartDecreased) {
                updatePlayerHealth(player, heartManager.getHearts(player));
                plugin.getLogger().info("Player " + player.getName() + " lost a heart. Now has " + heartManager.getHearts(player) + " hearts.");
            } else {
                plugin.getLogger().warning("Player " + player.getName() + " could not lose a heart.");
            }

            if (dropHeartsOnDeath) {
                world.dropItemNaturally(player.getLocation(), itemManager.createHeartItem());
            }

            if (heartManager.getHearts(player) <= 0) {
                if (announceElimination) {
                    Bukkit.broadcastMessage("§c" + player.getName() + " has been eliminated!");
                }
                if (disableBanOnElimination) {
                    heartManager.setHearts(player, 0);
                    return;
                }
                eliminatePlayer(player);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error handling natural death: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handlePlayerKill(Player player, Player killer, World world, boolean dropHeartsOnDeath, boolean dropHeartsIfMax, boolean heartRewardOnElimination, int maxHearts, boolean heartGainCooldownEnabled, long heartGainCooldown, boolean heartGainCooldowndropOnCooldown, boolean announceElimination, boolean disableBanOnElimination) {
        try {
            plugin.getLogger().info("Handling player kill for player: " + player.getName() + " killed by " + killer.getName());

            if (heartGainCooldownEnabled && lastHeartGain.containsKey(killer.getUniqueId()) && lastHeartGain.get(killer.getUniqueId()) + heartGainCooldown > System.currentTimeMillis()) {
                if (heartGainCooldowndropOnCooldown) {
                    world.dropItemNaturally(player.getLocation(), itemManager.createHeartItem());
                }
                killer.sendMessage("§cYou have to wait before gaining another heart!");
                plugin.getLogger().info("Killer " + killer.getName() + " is on heart gain cooldown.");
            } else {
                if (dropHeartsOnDeath) {
                    world.dropItemNaturally(player.getLocation(), itemManager.createHeartItem());
                } else {
                    int killerHearts = heartManager.getHearts(killer);
                    plugin.getLogger().info("Killer " + killer.getName() + " has " + killerHearts + " hearts before gain.");

                    if (heartManager.getHearts(player) > 0 || (heartManager.getHearts(player) == 0 && heartRewardOnElimination)) {
                        if (killerHearts + 1 > maxHearts) {
                            if (dropHeartsIfMax) {
                                world.dropItemNaturally(killer.getLocation(), itemManager.createHeartItem());
                            } else {
                                killer.sendMessage("§cYou already reached the limit of " + maxHearts / 2 + " hearts!");
                            }
                            plugin.getLogger().info("Killer " + killer.getName() + " cannot gain more hearts because they reached the max limit.");
                        } else {
                            heartManager.increaseHearts(killer);
                            updatePlayerHealth(killer, heartManager.getHearts(killer));
                            lastHeartGain.put(killer.getUniqueId(), System.currentTimeMillis());
                            plugin.getLogger().info("Killer " + killer.getName() + " gained a heart. Now has " + heartManager.getHearts(killer) + " hearts.");
                        }
                    }
                }
            }

            boolean heartDecreased = heartManager.decreaseHearts(player);
            if (heartDecreased) {
                updatePlayerHealth(player, heartManager.getHearts(player));
                plugin.getLogger().info("Player " + player.getName() + " lost a heart from being killed by " + killer.getName() + ". Now has " + heartManager.getHearts(player) + " hearts.");
            } else {
                plugin.getLogger().warning("Player " + player.getName() + " could not lose a heart.");
            }

            if (heartManager.getHearts(player) <= 0) {
                if (announceElimination) {
                    Bukkit.broadcastMessage("§c" + player.getName() + " has been eliminated by " + killer.getName() + "!");
                }
                if (disableBanOnElimination) {
                    heartManager.setHearts(player, 0);
                    return;
                }
                eliminatePlayer(player);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error handling player kill: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updatePlayerHealth(Player player, int hearts) {
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attribute != null) {
            attribute.setBaseValue(hearts * 2.0);
        }
        player.setHealth(Math.min(player.getHealth(), hearts * 2.0));
    }

    private void eliminatePlayer(Player player) {
        try {
            plugin.getLogger().info("Eliminating player: " + player.getName());
            int banDuration = plugin.getConfig().getInt("ban-duration", -1);
            String banMessage = plugin.getConfig().getString("ban-message", "§c[Lifesteal] §7You have been eliminated!");
            if (banDuration < 0) {
                Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(), banMessage, null, null);
            } else {
                Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(), banMessage, new Date(System.currentTimeMillis() + (banDuration * 86400000L)), null);
            }
            player.kickPlayer(banMessage);
        } catch (Exception e) {
            plugin.getLogger().severe("Error eliminating player: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
