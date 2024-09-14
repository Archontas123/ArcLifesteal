package org.mythofy.mythofylifestealplugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
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
        return baseHeartCap + Math.min(prestigeLevel, maxPrestige);
    }

    public int getMaxPrestige() {
        return maxPrestige;
    }

    public boolean canPrestige(Player player) {
        int heartCap = getHeartCap(player);
        int currentHearts = plugin.getHeartManager().getHearts(player);
        return currentHearts >= heartCap;
    }

    public void prestige(Player player) {
        int currentLevel = getPrestigeLevel(player);
        if (currentLevel < maxPrestige) {
            int newLevel = currentLevel + 1;
            prestigeLevels.put(player.getUniqueId(), newLevel);
            plugin.getHeartManager().setHearts(player, 10); // Reset to 10 hearts
            savePrestigeLevels();
            Component title = Component.text("Prestige Advanced!", TextColor.fromHexString("#CB2D3E"));
            Component subtitle = Component.text("You are now at Prestige Level " + plugin.getPrestigeManager().getPrestigeLevel(player) + ".", TextColor.fromHexString("#FFFFFF"));
            player.showTitle(Title.title(title, subtitle));
            player.sendMessage("§c[Lifesteal] §7You have prestiged to level " + newLevel + "!");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "prewards " + player.getName());
        } else {
            player.sendMessage("§c[Lifesteal] §7You have already prestiged the maximum number of times.");
        }
    }

    public void extraPrestige(Player player) {
        int currentLevel = getPrestigeLevel(player);
        prestigeLevels.put(player.getUniqueId(), currentLevel + 1);
        plugin.getHeartManager().setHearts(player, 10); // Reset to 10 hearts
        savePrestigeLevels();
        player.sendMessage("§c[Lifesteal] §7You have prestiged to level " + (currentLevel + 1) + ", but your heart cap remains the same.");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "prewards2 " + player.getName());
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
