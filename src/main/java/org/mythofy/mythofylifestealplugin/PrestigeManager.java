package org.mythofy.mythofylifestealplugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PrestigeManager {
    private final MythofyLifestealPlugin plugin;
    private final Map<UUID, Integer> prestigeLevels;
    private final File prestigeFile;
    private FileConfiguration prestigeConfig;
    private final int maxPrestige;

    public PrestigeManager(MythofyLifestealPlugin plugin) {
        this.plugin = plugin;
        this.prestigeLevels = new HashMap<>();
        this.prestigeFile = new File(plugin.getDataFolder(), "prestige.yml");
        this.maxPrestige = plugin.getConfig().getInt("max-prestige", 10);
        loadPrestigeLevels();
    }

    private void loadPrestigeLevels() {
        if (!prestigeFile.exists()) {
            plugin.saveResource("prestige.yml", false);
        }
        prestigeConfig = YamlConfiguration.loadConfiguration(prestigeFile);
        for (String uuidString : prestigeConfig.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidString);
            int level = prestigeConfig.getInt(uuidString);
            prestigeLevels.put(uuid, level);
        }
    }

    public int getPrestigeLevel(Player player) {
        return prestigeLevels.getOrDefault(player.getUniqueId(), 0);
    }

    public int getHeartCap(Player player) {
        int baseHeartCap = plugin.getConfig().getInt("base-heart-cap", 20);
        int prestigeLevel = getPrestigeLevel(player);
        return baseHeartCap + prestigeLevel;
    }

    public boolean canPrestige(Player player) {
        int currentLevel = getPrestigeLevel(player);
        return currentLevel < maxPrestige && plugin.getHeartManager().getHearts(player) >= getHeartCap(player);
    }

    public void prestige(Player player) {
        if (canPrestige(player)) {
            int newLevel = getPrestigeLevel(player) + 1;
            prestigeLevels.put(player.getUniqueId(), newLevel);
            plugin.getHeartManager().setHearts(player, 10); // Reset to base hearts
            savePrestigeLevels();
            player.sendMessage("You have prestiged to level " + newLevel + "!");
        } else {
            player.sendMessage("You cannot prestige at this time.");
        }
    }

    public void savePrestigeLevels() {
        for (Map.Entry<UUID, Integer> entry : prestigeLevels.entrySet()) {
            prestigeConfig.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            prestigeConfig.save(prestigeFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save prestige levels: " + e.getMessage());
        }
    }
}
