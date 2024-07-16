package org.mythofy.mythofylifestealplugin;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class RevivalManager {
    private final MythofyLifestealPlugin plugin;

    public RevivalManager(MythofyLifestealPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerRevivalBeaconRecipe() {
        ItemStack revivalBeacon = createRevivalBeacon();
        NamespacedKey key = new NamespacedKey(plugin, "revival_beacon");
        ShapedRecipe recipe = new ShapedRecipe(key, revivalBeacon);

        recipe.shape("NHN", "HBH", "NHN");
        recipe.setIngredient('N', Material.NETHERITE_BLOCK);
        recipe.setIngredient('H', Material.NETHER_STAR); // Representing heart item
        recipe.setIngredient('B', Material.BEACON);

        Bukkit.addRecipe(recipe);
        plugin.getLogger().info("[LifeStealPlugin] Revival beacon recipe registered.");
    }

    private ItemStack createRevivalBeacon() {
        ItemStack beacon = new ItemStack(Material.BEACON, 1);
        ItemMeta meta = beacon.getItemMeta();
        meta.setDisplayName("§6Revival Beacon");
        List<String> lore = Arrays.asList("§7Use this beacon to revive", "§7an eliminated player");
        meta.setLore(lore);
        beacon.setItemMeta(meta);
        plugin.getLogger().info("[LifeStealPlugin] Created revival beacon item.");
        return beacon;
    }

    public boolean revivePlayer(String playerName) {
        plugin.getLogger().info("[LifeStealPlugin] Attempting to revive player: " + playerName);
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);

        // Unban the player if they were banned
        Bukkit.getBanList(BanList.Type.NAME).pardon(playerName);
        plugin.getLogger().info("[LifeStealPlugin] Player " + playerName + " has been unbanned.");

        // If the player is online, set their hearts immediately
        if (target.isOnline()) {
            Player onlinePlayer = target.getPlayer();
            if (onlinePlayer != null) {
                plugin.getHeartManager().setHearts(onlinePlayer, 10);
                onlinePlayer.sendMessage("§c[Lifesteal] §7You have been revived!");
                plugin.getLogger().info("[LifeStealPlugin] Revived online player: " + playerName);
                return true;
            }
        } else {
            // If the player is offline, set their hearts when they join
            plugin.getHeartManager().setOfflineHearts(target.getUniqueId(), 10);
            plugin.getLogger().info("[LifeStealPlugin] Set offline hearts for player: " + playerName);
            return true;
        }

        plugin.getLogger().info("[LifeStealPlugin] Failed to revive player: " + playerName);
        return false;
    }

    public boolean isRevivalBeacon(ItemStack item) {
        boolean result = item != null && item.getType() == Material.BEACON && item.hasItemMeta() &&
                item.getItemMeta().getDisplayName().equals("§6Revival Beacon");
        plugin.getLogger().info("[LifeStealPlugin] Checking if item is revival beacon: " + result);
        return result;
    }
}
