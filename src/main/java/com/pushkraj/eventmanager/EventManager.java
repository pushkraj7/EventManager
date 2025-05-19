package com.pushkraj.eventmanager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.inventory.meta.ItemMeta;

public class EventManager extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        getLogger().info("EventManager+ has been enabled!");
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("EventManager+ has been disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("eventmanager") || command.getName().equalsIgnoreCase("em")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be run by a player.");
                return true;
            }
            Player player = (Player) sender;
            if (!player.hasPermission("eventmanager.gui")) {
                player.sendMessage("You do not have permission to use this command.");
                return true;
            }
            // Open GUI logic
            EventManagerGUI.open(player);
            return true;
        }
        return false;
    }

    @org.bukkit.event.EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        InventoryView view = event.getView();

        if (view.getTitle().equals("Event Manager")) {
            event.setCancelled(true); // Prevent item moving
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            World world = player.getWorld();

            // Handle PvP Toggle
            if (clickedItem.getType() == Material.IRON_SWORD && clickedItem.hasItemMeta() && clickedItem.getItemMeta().getDisplayName().contains("Toggle PvP")) {
                if (!player.hasPermission("eventmanager.toggle.pvp")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to toggle PvP.");
                    return;
                }
                boolean currentPvp = world.getPVP();
                world.setPVP(!currentPvp);
                player.sendMessage(ChatColor.GREEN + "PvP has been " + (!currentPvp ? "enabled." : "disabled."));
                // Update item in GUI (optional, for visual feedback)
                ItemMeta meta = clickedItem.getItemMeta();
                meta.setDisplayName(ChatColor.RESET + "Toggle PvP (" + (world.getPVP() ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled") + ChatColor.RESET + ")");
                clickedItem.setItemMeta(meta);
                player.updateInventory(); // Refresh the GUI for the player
            }
            // Handle Keep Inventory Toggle
            else if (clickedItem.getType() == Material.TOTEM_OF_UNDYING && clickedItem.hasItemMeta() && clickedItem.getItemMeta().getDisplayName().contains("Toggle Keep Inventory")) {
                if (!player.hasPermission("eventmanager.toggle.keepinventory")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to toggle Keep Inventory.");
                    return;
                }
                Boolean currentKeepInventory = world.getGameRuleValue(GameRule.KEEP_INVENTORY);
                if (currentKeepInventory == null) currentKeepInventory = false; // Default if null
                world.setGameRule(GameRule.KEEP_INVENTORY, !currentKeepInventory);
                player.sendMessage(ChatColor.GREEN + "Keep Inventory has been " + (!currentKeepInventory ? "enabled." : "disabled."));
                // Update item in GUI
                ItemMeta meta = clickedItem.getItemMeta();
                meta.setDisplayName(ChatColor.RESET + "Toggle Keep Inventory (" + (world.getGameRuleValue(GameRule.KEEP_INVENTORY) ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled") + ChatColor.RESET + ")");
                clickedItem.setItemMeta(meta);
                player.updateInventory();
            }
            // Handle Mob Spawning Toggle
            else if (clickedItem.getType() == Material.ZOMBIE_HEAD && clickedItem.hasItemMeta() && clickedItem.getItemMeta().getDisplayName().contains("Toggle Mob Spawning")) {
                if (!player.hasPermission("eventmanager.toggle.mobspawning")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to toggle Mob Spawning.");
                    return;
                }
                Boolean currentMobSpawning = world.getGameRuleValue(GameRule.DO_MOB_SPAWNING);
                if (currentMobSpawning == null) currentMobSpawning = true; // Default if null (usually true)
                world.setGameRule(GameRule.DO_MOB_SPAWNING, !currentMobSpawning);
                player.sendMessage(ChatColor.GREEN + "Hostile Mob Spawning has been " + (!currentMobSpawning ? "enabled." : "disabled."));
                ItemMeta meta = clickedItem.getItemMeta();
                meta.setDisplayName(ChatColor.RESET + "Toggle Mob Spawning (" + (world.getGameRuleValue(GameRule.DO_MOB_SPAWNING) ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled") + ChatColor.RESET + ")");
                clickedItem.setItemMeta(meta);
                player.updateInventory();
            }
            // Handle Difficulty Change
            else if (clickedItem.getType() == Material.DIAMOND && clickedItem.hasItemMeta() && clickedItem.getItemMeta().getDisplayName().contains("Change Difficulty")) {
                if (!player.hasPermission("eventmanager.changedifficulty")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to change difficulty.");
                    return;
                }
                org.bukkit.Difficulty currentDifficulty = world.getDifficulty();
                org.bukkit.Difficulty nextDifficulty;
                switch (currentDifficulty) {
                    case PEACEFUL:
                        nextDifficulty = org.bukkit.Difficulty.EASY; 
                        break;
                    case EASY:
                        nextDifficulty = org.bukkit.Difficulty.NORMAL;
                        break;
                    case NORMAL:
                        nextDifficulty = org.bukkit.Difficulty.HARD;
                        break;
                    case HARD:
                        nextDifficulty = org.bukkit.Difficulty.PEACEFUL;
                        break;
                    default:
                        nextDifficulty = org.bukkit.Difficulty.NORMAL; // Should not happen
                        break;
                }
                world.setDifficulty(nextDifficulty);
                player.sendMessage(ChatColor.GREEN + "Difficulty changed to " + nextDifficulty.toString() + ".");
                ItemMeta meta = clickedItem.getItemMeta();
                meta.setDisplayName(ChatColor.RESET + "Change Difficulty (" + ChatColor.YELLOW + nextDifficulty.toString() + ChatColor.RESET + ")");
                clickedItem.setItemMeta(meta);
                player.updateInventory();
            }
            // Handle Friendly Mob Spawning Toggle
            else if (clickedItem.getType() == Material.PLAYER_HEAD && clickedItem.hasItemMeta() && clickedItem.getItemMeta().getDisplayName().contains("Toggle Friendly Mob Spawning")) {
                if (!player.hasPermission("eventmanager.toggle.friendlymobspawning")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to toggle Friendly Mob Spawning.");
                    return;
                }
                // NOTE: Bukkit/Spigot does not have a simple GameRule for *only* friendly mobs.
                // DO_MOB_SPAWNING affects all mobs. Implementing this feature accurately
                // would require listening to mob spawn events and cancelling them for passive mobs,
                // or using server-specific APIs if available (e.g., PaperMC per-world settings).
                // For now, this is a placeholder.
                boolean isCurrentlyEnabled = true; // Placeholder: assume enabled, or load from a config
                // world.setGameRule(GameRule.????, !isCurrentlyEnabled); // No direct GameRule
                player.sendMessage(ChatColor.YELLOW + "Friendly Mob Spawning toggle is complex and not fully implemented here.");
                player.sendMessage(ChatColor.GREEN + "Friendly Mob Spawning is currently " + (isCurrentlyEnabled ? "conceptually enabled." : "conceptually disabled."));
                ItemMeta meta = clickedItem.getItemMeta();
                // Update display based on a conceptual state or a custom config value if implemented
                meta.setDisplayName(ChatColor.RESET + "Toggle Friendly Mob Spawning (" + (isCurrentlyEnabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled") + ChatColor.RESET + ")");
                clickedItem.setItemMeta(meta);
                player.updateInventory();
            }
            // Handle End Dimension Toggle
            else if (clickedItem.getType() == Material.END_STONE && clickedItem.hasItemMeta() && clickedItem.getItemMeta().getDisplayName().contains("Toggle End Dimension")) {
                if (!player.hasPermission("eventmanager.toggle.end")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to toggle The End dimension.");
                    return;
                }
                // NOTE: Toggling dimensions typically requires server configuration (server.properties 'allow-end')
                // and often a server restart. Doing this dynamically via plugin is non-trivial and may not be fully supported.
                // This is a placeholder action.
                boolean isEndEnabled = true; // Placeholder: Assume enabled or check server.properties if possible (read-only)
                player.sendMessage(ChatColor.YELLOW + "Toggling The End dimension usually requires a server restart and config changes.");
                player.sendMessage(ChatColor.GREEN + "The End dimension is currently " + (isEndEnabled ? "conceptually enabled." : "conceptually disabled."));
                ItemMeta meta = clickedItem.getItemMeta();
                meta.setDisplayName(ChatColor.RESET + "Toggle End Dimension (" + (isEndEnabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled") + ChatColor.RESET + ")");
                clickedItem.setItemMeta(meta);
                player.updateInventory();
            }
            // Handle Nether Dimension Toggle
            else if (clickedItem.getType() == Material.NETHERRACK && clickedItem.hasItemMeta() && clickedItem.getItemMeta().getDisplayName().contains("Toggle Nether Dimension")) {
                if (!player.hasPermission("eventmanager.toggle.nether")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to toggle The Nether dimension.");
                    return;
                }
                // NOTE: Toggling dimensions typically requires server configuration (server.properties 'allow-nether')
                // and often a server restart.
                boolean isNetherEnabled = true; // Placeholder: Assume enabled or check server.properties if possible (read-only)
                player.sendMessage(ChatColor.YELLOW + "Toggling The Nether dimension usually requires a server restart and config changes.");
                player.sendMessage(ChatColor.GREEN + "The Nether dimension is currently " + (isNetherEnabled ? "conceptually enabled." : "conceptually disabled."));
                ItemMeta meta = clickedItem.getItemMeta();
                meta.setDisplayName(ChatColor.RESET + "Toggle Nether Dimension (" + (isNetherEnabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled") + ChatColor.RESET + ")");
                clickedItem.setItemMeta(meta);
                player.updateInventory();
            }
            // Add more else if blocks here for other items
        }
    }
}