package org.mythofy.mythofylifestealplugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class MythofyLifestealPlugin extends JavaPlugin implements Listener {

    private HeartManager heartManager;
    private ItemManager itemManager;
    private CraftingManager craftingManager;
    private PrestigeManager prestigeManager;
    private RevivalManager revivalManager;
    private String resourcePackURL;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Initialize managers
        heartManager = new HeartManager(this);
        itemManager = new ItemManager(this);
        craftingManager = new CraftingManager(this, itemManager);
        prestigeManager = new PrestigeManager(this);
        revivalManager = new RevivalManager(this);

        // Register events
        getServer().getPluginManager().registerEvents(this, this);

        // Register commands
        if (getCommand("withdraw") != null) {
            getCommand("withdraw").setExecutor(this);
        }
        if (getCommand("hearts") != null) {
            getCommand("hearts").setExecutor(this);
        }
        if (getCommand("lsrevive") != null) {
            getCommand("lsrevive").setExecutor(this);
        }
        if (getCommand("giveheart") != null) {
            getCommand("giveheart").setExecutor(this);
        }
        if (getCommand("prestige") != null) {
            getCommand("prestige").setExecutor(this);
        }

        // Register recipes
        if (getConfig().getBoolean("allow-heart-crafting", true)) {
            craftingManager.registerHeartRecipe();
            craftingManager.registerHeartFragmentRecipe();
        }
        if (getConfig().getBoolean("allow-revival-beacon-crafting", true)) {
            revivalManager.registerRevivalBeaconRecipe();
        }

        // Register PlaceholderAPI expansion
        new PrestigePlaceholder(this).register();

        // Get resource pack URL from config
        resourcePackURL = getConfig().getString("resource-pack-url");
    }

    @Override
    public void onDisable() {
        if (heartManager != null) {
            heartManager.saveHearts();
        }
        if (prestigeManager != null) {
            prestigeManager.savePrestigeLevels();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        int hearts = heartManager.getHearts(player);
        heartManager.setHearts(player, hearts);

        // Send resource pack to player
        if (resourcePackURL != null && !resourcePackURL.isEmpty()) {
            player.setResourcePack(resourcePackURL);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null) {
            handlePlayerKill(victim, killer);
        } else {
            handlePlayerDeath(victim);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (itemManager.isHeartItem(item)) {
            event.setCancelled(true);
            if (heartManager.increaseHearts(player)) {
                item.setAmount(item.getAmount() - 1);
                player.sendMessage("You have gained an extra heart!");
            } else {
                player.sendMessage("You already have the maximum number of hearts.");
            }
        }
    }

    private void handlePlayerKill(Player victim, Player killer) {
        heartManager.decreaseHearts(victim);
        heartManager.increaseHearts(killer);

        if (prestigeManager.getPrestigeLevel(killer) >= getConfig().getInt("max-prestige", 10) &&
                Math.random() < getConfig().getDouble("heart-fragment-chance", 0.25)) {
            itemManager.giveHeartFragmentItem(killer);
        }
    }

    private void handlePlayerDeath(Player victim) {
        heartManager.decreaseHearts(victim);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        int amount = 1;

        if (args.length > 0) {
            try {
                amount = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage("Invalid number.");
                return true;
            }
        }

        switch (label.toLowerCase()) {
            case "withdraw":
                return handleWithdrawCommand(player, amount);
            case "hearts":
                return handleHeartsCommand(player);
            case "lsrevive":
                return handleReviveCommand(player, args);
            case "giveheart":
                return handleGiveHeartCommand(player, amount);
            case "prestige":
                return handlePrestigeCommand(player);
        }

        return false;
    }

    private boolean handleWithdrawCommand(Player player, int amount) {
        if (!player.hasPermission("lifesteal.withdraw")) {
            player.sendMessage("You don't have permission to use this command.");
            return true;
        }
        int withdrawn = heartManager.withdrawHearts(player, amount);
        if (withdrawn > 0) {
            itemManager.giveHeartItem(player, withdrawn);
            player.sendMessage("You have withdrawn " + withdrawn + " heart(s)!");
        } else {
            player.sendMessage("You cannot withdraw hearts at this time.");
        }
        return true;
    }

    private boolean handleHeartsCommand(Player player) {
        int hearts = heartManager.getHearts(player);
        player.sendMessage("You currently have " + hearts + " hearts.");
        return true;
    }

    private boolean handleReviveCommand(Player player, String[] args) {
        if (!player.hasPermission("lifesteal.revive")) {
            player.sendMessage("You don't have permission to use this command.");
            return true;
        }
        if (args.length != 1) {
            player.sendMessage("Usage: /lsrevive <player>");
            return false;
        }
        String targetName = args[0];
        if (revivalManager.revivePlayer(targetName)) {
            player.sendMessage("Successfully revived " + targetName);
        } else {
            player.sendMessage("Failed to revive " + targetName);
        }
        return true;
    }

    private boolean handleGiveHeartCommand(Player player, int amount) {
        if (!player.hasPermission("lifesteal.giveheart")) {
            player.sendMessage("You don't have permission to use this command.");
            return true;
        }
        itemManager.giveHeartItem(player, amount);
        player.sendMessage("You have been given " + amount + " heart item(s).");
        return true;
    }

    private boolean handlePrestigeCommand(Player player) {
        if (!player.hasPermission("lifesteal.prestige")) {
            player.sendMessage("You don't have permission to use this command.");
            return true;
        }
        if (prestigeManager.canPrestige(player)) {
            prestigeManager.prestige(player);
        } else {
            player.sendMessage("You cannot prestige at this time.");
        }
        return true;
    }

    public HeartManager getHeartManager() {
        return heartManager;
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public PrestigeManager getPrestigeManager() {
        return prestigeManager;
    }

    private class PrestigePlaceholder extends PlaceholderExpansion {
        private final MythofyLifestealPlugin plugin;

        public PrestigePlaceholder(MythofyLifestealPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public String getIdentifier() {
            return "lifesteal";
        }

        @Override
        public String getAuthor() {
            return plugin.getDescription().getAuthors().toString();
        }

        @Override
        public String getVersion() {
            return plugin.getDescription().getVersion();
        }

        @Override
        public String onPlaceholderRequest(Player player, String identifier) {
            if (player == null) {
                return "";
            }
            if (identifier.equals("prestige")) {
                return String.valueOf(plugin.getPrestigeManager().getPrestigeLevel(player));
            }
            if (identifier.equals("hearts")) {
                return String.valueOf(plugin.getHeartManager().getHearts(player));
            }
            return null;
        }
    }
}
