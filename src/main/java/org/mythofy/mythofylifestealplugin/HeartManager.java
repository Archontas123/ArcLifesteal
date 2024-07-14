package org.mythofy.mythofylifestealplugin;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HeartManager {
    private final MythofyLifestealPlugin plugin;
    private final Map<UUID, Integer> playerHearts;
    private final File heartsFile;
    private FileConfiguration heartsConfig;

    public HeartManager(MythofyLifestealPlugin plugin) {
        this.plugin = plugin;
        this.playerHearts = new HashMap<>();
        this.heartsFile = new File(plugin.getDataFolder(), "hearts.yml");
        loadHearts();
    }

    private void loadHearts() {
        if (!heartsFile.exists()) {
            plugin.saveResource("hearts.yml", false);
        }
        heartsConfig = YamlConfiguration.loadConfiguration(heartsFile);
        for (String uuidString : heartsConfig.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidString);
            int hearts = heartsConfig.getInt(uuidString);
            playerHearts.put(uuid, hearts);
        }
    }

    public void saveHearts() {
        for (Map.Entry<UUID, Integer> entry : playerHearts.entrySet()) {
            heartsConfig.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            heartsConfig.save(heartsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save player hearts: " + e.getMessage());
        }
    }

    public void setHearts(Player player, int hearts) {
        int maxHearts = plugin.getConfig().getInt("max-hearts", 20);
        hearts = Math.min(hearts, maxHearts);
        playerHearts.put(player.getUniqueId(), hearts);
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attribute != null) {
            attribute.setBaseValue(hearts * 2);
        }
        player.setHealth(player.getHealth()); // This will cap health at the new max
        saveHearts();
    }

    public int getHearts(Player player) {
        return playerHearts.getOrDefault(player.getUniqueId(), plugin.getConfig().getInt("base-heart-count", 10));
    }

    public boolean decreaseHearts(Player player) {
        int currentHearts = getHearts(player);
        if (currentHearts > 1) {
            setHearts(player, currentHearts - 1);
            return true;
        } else if (currentHearts == 1) {
            setHearts(player, 0);
            if (plugin.getConfig().getBoolean("ban-on-zero-hearts", true)) {
                eliminatePlayer(player);
            }
        }
        return false;
    }

    public boolean increaseHearts(Player player) {
        int currentHearts = getHearts(player);
        int heartCap = plugin.getPrestigeManager().getHeartCap(player);
        if (currentHearts < heartCap) {
            setHearts(player, currentHearts + 1);
            return true;
        }
        return false;
    }

    public boolean canWithdrawHeart(Player player) {
        return getHearts(player) > 1;
    }

    public int withdrawHearts(Player player, int amount) {
        int currentHearts = getHearts(player);
        int withdrawableHearts = Math.min(amount, currentHearts - 1); // Ensure at least 1 heart remains
        if (withdrawableHearts > 0) {
            setHearts(player, currentHearts - withdrawableHearts);
            return withdrawableHearts;
        }
        return 0;
    }

    private void eliminatePlayer(Player player) {
        int banDuration = plugin.getConfig().getInt("ban-duration", -1);
        String banMessage = plugin.getConfig().getString("ban-message", "You have been eliminated!");
        if (banDuration < 0) {
            Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(), banMessage, null, null);
        } else {
            Date expirationDate = new Date(System.currentTimeMillis() + (banDuration * 86400000L));
            Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(), banMessage, expirationDate, null);
        }
        player.kickPlayer(banMessage);
    }
}
