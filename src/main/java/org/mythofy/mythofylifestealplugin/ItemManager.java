package org.mythofy.mythofylifestealplugin;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class ItemManager {
    private final MythofyLifestealPlugin plugin;

    public ItemManager(MythofyLifestealPlugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack createHeartItem() {
        ItemStack heartItem = new ItemStack(Material.NETHER_STAR, 1);
        ItemMeta meta = heartItem.getItemMeta();
        meta.setDisplayName("§c❤ Heart");
        meta.setLore(Arrays.asList("§7A precious heart", "§7Use to gain an extra heart"));
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        heartItem.setItemMeta(meta);
        return heartItem;
    }

    public ItemStack createHeartFragmentItem() {
        ItemStack fragmentItem = new ItemStack(Material.PRISMARINE_CRYSTALS, 1);
        ItemMeta meta = fragmentItem.getItemMeta();
        meta.setDisplayName("§d❤ Heart Fragment");
        meta.setLore(Arrays.asList("§7A piece of a heart", "§7Craft 9 to create a full heart"));
        fragmentItem.setItemMeta(meta);
        return fragmentItem;
    }

    public void giveHeartItem(Player player) {
        player.getInventory().addItem(createHeartItem());
    }

    public void giveHeartItem(Player player, int amount) {
        ItemStack heartItem = createHeartItem();
        heartItem.setAmount(amount);
        player.getInventory().addItem(heartItem);
    }

    public void giveHeartFragmentItem(Player player) {
        player.getInventory().addItem(createHeartFragmentItem());
    }

    public boolean isHeartItem(ItemStack item) {
        return item != null && item.getType() == Material.NETHER_STAR && item.hasItemMeta() &&
                item.getItemMeta().getDisplayName().equals("§c❤ Heart");
    }

    public boolean isHeartFragmentItem(ItemStack item) {
        return item != null && item.getType() == Material.PRISMARINE_CRYSTALS && item.hasItemMeta() &&
                item.getItemMeta().getDisplayName().equals("§d❤ Heart Fragment");
    }
}
