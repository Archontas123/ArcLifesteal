package org.mythofy.mythofylifestealplugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
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
    }

    private ItemStack createRevivalBeacon() {
        ItemStack beacon = new ItemStack(Material.BEACON, 1);
        ItemMeta meta = beacon.getItemMeta();
        meta.setDisplayName("§6Revival Beacon");
        List<String> lore = Arrays.asList("§7Use this beacon to revive", "§7an eliminated player");
        meta.setLore(lore);
        beacon.setItemMeta(meta);
        return beacon;
    }

    public boolean revivePlayer(String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            // Check if the player is in the eliminated players list
            // For simplicity, we're assuming a player is eliminated if they're offline
            // In a full implementation, you'd check a database or file of eliminated players
            target = Bukkit.getOfflinePlayer(playerName).getPlayer();
            if (target == null) {
                return false;
            }
        }

        // Revive the player
        plugin.getHeartManager().setHearts(target, 10); // Reset to base hearts
        // If the player was banned, unban them here
        // Bukkit.getBanList(BanList.Type.NAME).pardon(playerName);

        target.sendMessage("You have been revived!");
        return true;
    }

    public boolean isRevivalBeacon(ItemStack item) {
        return item != null && item.getType() == Material.BEACON && item.hasItemMeta() &&
                item.getItemMeta().getDisplayName().equals("§6Revival Beacon");
    }
}