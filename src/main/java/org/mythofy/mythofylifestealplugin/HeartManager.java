package org.mythofy.mythofylifestealplugin;

import net.kyori.adventure.title.Title;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.time.Duration;
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
        int maxHearts = plugin.getPrestigeManager().getHeartCap(player);
        hearts = Math.min(hearts, maxHearts);
        playerHearts.put(player.getUniqueId(), hearts);

        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attribute != null) {
            attribute.setBaseValue(hearts * 2);
        }

        double currentHealth = player.getHealth();
        double maxHealth = hearts * 2;

        if (currentHealth > maxHealth) {
            player.setHealth(maxHealth); // Reduce current health to new max if necessary
        }

        saveHearts();
    }

    public int getHearts(Player player) {
        return playerHearts.getOrDefault(player.getUniqueId(), plugin.getConfig().getInt("base-heart-count", 10));
    }

    public boolean decreaseHearts(Player player) {
        int currentHearts = getHearts(player);
        if (currentHearts > 1) {
            setHearts(player, currentHearts - 1);
            // Create components using Adventure API
            Component titleComponent = Component.text("Heart Lost!").color(TextColor.fromHexString("#CB2D3E"));
            Component subtitleComponent = Component.text("You now have " + getHearts(player) + " hearts.").color(TextColor.fromHexString("#FFFFFF"));

            // Prepare the title times
            Duration fadeIn = Duration.ofMillis(500);   // 0.5 seconds
            Duration stay = Duration.ofMillis(3500);    // 3.5 seconds
            Duration fadeOut = Duration.ofMillis(1000); // 1 second

            // Create and show the title using the builder pattern if 'of()' is deprecated
            Title title = Title.title(titleComponent, subtitleComponent, Title.Times.times(fadeIn, stay, fadeOut));
            player.showTitle(title);
            return true;

        } else if (currentHearts == 1) {
            setHearts(player, 0);
            if (plugin.getConfig().getBoolean("ban-on-zero-hearts", true)) {
                eliminatePlayer(player);
            }
        }
        return false;
    }

    public int getMaxHearts(Player player) {
        // Fetch the base max hearts from the config, defaulting to 20 if not set
        int baseMaxHearts = plugin.getConfig().getInt("base-max-hearts", 20);
        plugin.getLogger().info("[HeartManager] Base max hearts (from config): " + baseMaxHearts);

        // Fetch additional hearts per prestige level from the config, defaulting to 2 if not set
        int heartsPerPrestigeLevel = plugin.getConfig().getInt("hearts-per-prestige-level", 2);
        plugin.getLogger().info("[HeartManager] Additional hearts per prestige level (from config): " + heartsPerPrestigeLevel);

        // Get the player's current prestige level from the PrestigeManager
        int prestigeLevel = plugin.getPrestigeManager().getPrestigeLevel(player);
        plugin.getLogger().info("[HeartManager] Player's prestige level: " + prestigeLevel);

        // Calculate the total maximum hearts
        int totalMaxHearts = baseMaxHearts + (heartsPerPrestigeLevel * prestigeLevel);
        plugin.getLogger().info("[HeartManager] Total max hearts calculated for player " + player.getName() + ": " + totalMaxHearts);

        return totalMaxHearts;
    }


    public boolean increaseHearts(Player player) {
        int currentHearts = getHearts(player);
        int heartCap = plugin.getPrestigeManager().getHeartCap(player);
        if (currentHearts < heartCap) {
            setHearts(player, currentHearts + 1);



            // Create components using Adventure API
            Component titleComponent = Component.text("Heart Gained!").color(TextColor.fromHexString("#CB2D3E"));
            Component subtitleComponent = Component.text("You now have " + getHearts(player) + " hearts.").color(TextColor.fromHexString("#FFFFFF"));

            // Prepare the title times
            Duration fadeIn = Duration.ofMillis(500);   // 0.5 seconds
            Duration stay = Duration.ofMillis(3500);    // 3.5 seconds
            Duration fadeOut = Duration.ofMillis(1000); // 1 second

            // Create and show the title using the builder pattern if 'of()' is deprecated
            Title title = Title.title(titleComponent, subtitleComponent, Title.Times.times(fadeIn, stay, fadeOut));
            player.showTitle(title);
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

    public void setOfflineHearts(UUID playerUUID, int hearts) {
        int maxHearts = plugin.getConfig().getInt("max-hearts", 20);
        hearts = Math.min(hearts, maxHearts);
        playerHearts.put(playerUUID, hearts);
        saveHearts();
    }

    public void eliminatePlayer(Player player) {
        int banDuration = plugin.getConfig().getInt("ban-duration", -1);
        String banMessage = plugin.getConfig().getString("ban-message", "§c[Lifesteal] §7You have been eliminated!");

        // Broadcast elimination to all players using Adventure API for color support with hex codes
        Component broadcastMessage = Component.text(player.getName() + " has been eliminated!", TextColor.fromHexString("#CB2D3E"));
        Bukkit.getServer().sendMessage(broadcastMessage);

        // Apply the ban based on duration specified in the config
        if (banDuration < 0) {
            Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(), banMessage, null, null);
        } else {
            Date expirationDate = new Date(System.currentTimeMillis() + (banDuration * 86400000L)); // 86400000L is the number of milliseconds in a day
            Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(), banMessage, expirationDate, null);
        }

        // Kick the player with the ban message
        player.kickPlayer(banMessage);
    }
}
