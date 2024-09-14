package org.mythofy.mythofylifestealplugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.configuration.ConfigurationSection;

public class CraftingManager {
    private final MythofyLifestealPlugin plugin;
    private final ItemManager itemManager;

    public CraftingManager(MythofyLifestealPlugin plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
    }

    public void registerRecipes() {
        registerHeartRecipe();
        registerHeartFragmentRecipe();
    }

    public void registerHeartRecipe() {
        ItemStack heartItem = itemManager.createHeartItem();
        NamespacedKey key = new NamespacedKey(plugin, "heart_item");
        ShapedRecipe recipe = new ShapedRecipe(key, heartItem);

        ConfigurationSection heartRecipeConfig = plugin.getConfig().getConfigurationSection("heart-recipe");
        if (heartRecipeConfig != null) {
            recipe.shape(
                    heartRecipeConfig.getString("shape.line1"),
                    heartRecipeConfig.getString("shape.line2"),
                    heartRecipeConfig.getString("shape.line3")
            );
            for (String ingredientKey : heartRecipeConfig.getConfigurationSection("ingredients").getKeys(false)) {
                recipe.setIngredient(ingredientKey.charAt(0), Material.matchMaterial(heartRecipeConfig.getString("ingredients." + ingredientKey)));
            }
        } else {
            recipe.shape("DDD", "DFD", "DND");
            recipe.setIngredient('D', Material.DIAMOND_BLOCK);
            recipe.setIngredient('F', Material.PRISMARINE_CRYSTALS);
            recipe.setIngredient('N', Material.NETHERITE_INGOT);
        }

        Bukkit.addRecipe(recipe);
    }

    public void registerHeartFragmentRecipe() {
        ItemStack fragmentItem = itemManager.createHeartFragmentItem();
        NamespacedKey key = new NamespacedKey(plugin, "heart_fragment");
        ShapedRecipe recipe = new ShapedRecipe(key, fragmentItem);

        ConfigurationSection fragmentRecipeConfig = plugin.getConfig().getConfigurationSection("heart-fragment-recipe");
        if (fragmentRecipeConfig != null) {
            recipe.shape(
                    fragmentRecipeConfig.getString("shape.line1"),
                    fragmentRecipeConfig.getString("shape.line2"),
                    fragmentRecipeConfig.getString("shape.line3")
            );
            for (String ingredientKey : fragmentRecipeConfig.getConfigurationSection("ingredients").getKeys(false)) {
                recipe.setIngredient(ingredientKey.charAt(0), Material.matchMaterial(fragmentRecipeConfig.getString("ingredients." + ingredientKey)));
            }
        } else {
            recipe.shape("GRG", "RTR", "GRG");
            recipe.setIngredient('G', Material.GOLD_BLOCK);
            recipe.setIngredient('R', Material.REDSTONE_BLOCK);
            recipe.setIngredient('T', Material.TOTEM_OF_UNDYING);
        }

        Bukkit.addRecipe(recipe);
    }
}
