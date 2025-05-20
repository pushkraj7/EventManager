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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.EventPriority;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class EventManager extends JavaPlugin implements Listener {
    private static boolean chatMutedGlobally = false;
    private static boolean adminAutoVanishEnabled = false;
    private static boolean autoRestartTimerEnabled = false;
    private static int autoRestartTaskId = -1;
    private static long autoRestartIntervalMinutes = 180; // Default 3 hours, configurable later if needed

    public static boolean isChatMutedGlobally() {
        return chatMutedGlobally;
    }

    public static boolean isAdminAutoVanishEnabled() {
        return adminAutoVanishEnabled;
    }

    public static boolean isAutoRestartTimerEnabled() {
        return autoRestartTimerEnabled;
    }

    public static long getAutoRestartIntervalMinutes() {
        return autoRestartIntervalMinutes;
    }

    // Method to start the restart timer
    private void startAutoRestartTimer() {
        if (autoRestartTaskId != -1) {
            Bukkit.getScheduler().cancelTask(autoRestartTaskId);
        }
        long ticks = autoRestartIntervalMinutes * 60 * 20; // minutes to ticks
        autoRestartTaskId = Bukkit.getScheduler().runTaskLater(this, () -> {
            Bukkit.broadcastMessage(ChatColor.RED + "[EventManager] Server is restarting in 1 minute!");
            // Schedule actual restart command after a short delay (e.g., 1 minute)
            Bukkit.getScheduler().runTaskLater(this, () -> {
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "restart"); // Or "stop" if preferred
            }, 20L * 60); // 1 minute in ticks
        }, ticks).getTaskId();
        autoRestartTimerEnabled = true;
        getLogger().info("Auto restart timer enabled. Restart in " + autoRestartIntervalMinutes + " minutes.");
    }

    // Method to stop the restart timer
    private void stopAutoRestartTimer() {
        if (autoRestartTaskId != -1) {
            Bukkit.getScheduler().cancelTask(autoRestartTaskId);
            autoRestartTaskId = -1;
        }
        autoRestartTimerEnabled = false;
        getLogger().info("Auto restart timer disabled.");
    }
    @Override
    public void onEnable() {
        getLogger().info("EventManager has been enabled!");
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        stopAutoRestartTimer(); // Ensure timer is stopped on plugin disable
        getLogger().info("EventManager has been disabled!");
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
            EventManagerGUI.openWorldSelectionGUI(player);
            return true;
        }
        return false;
    }

    @org.bukkit.event.EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        InventoryView view = event.getView();

        // Handle clicks in the World Selection GUI
        if (view.getTitle().equals(EventManagerGUI.WORLD_SELECTION_GUI_TITLE)) {
            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR || !clickedItem.hasItemMeta()) return;

            String worldName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName()); // Get world name from item
            World selectedWorld = org.bukkit.Bukkit.getWorld(worldName);

            if (selectedWorld != null) {
                EventManagerGUI.openSettingsGUI(player, selectedWorld);
            } else {
                player.sendMessage(ChatColor.RED + "Selected world '" + worldName + "' not found!");
            }
            return;
        }

        // Handle clicks in the Settings GUI (now world-specific)
        if (view.getTitle().startsWith(EventManagerGUI.SETTINGS_GUI_TITLE_PREFIX)) {
            event.setCancelled(true); // Prevent item moving
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            String title = view.getTitle();
            String worldNameFromTitleRaw = title.substring(EventManagerGUI.SETTINGS_GUI_TITLE_PREFIX.length());
            String worldNameFromTitle = ChatColor.stripColor(worldNameFromTitleRaw); // Strip color codes
            World world = org.bukkit.Bukkit.getWorld(worldNameFromTitle);

            if (world == null) {
                player.sendMessage(ChatColor.RED + "Could not determine the world for this GUI. Please re-open.");
                player.closeInventory();
                return;
            }

            // Handle Back Button
            if (clickedItem.getType() == Material.ARROW && clickedItem.hasItemMeta() && clickedItem.getItemMeta().getDisplayName().contains("Back to World Selection")) {
                EventManagerGUI.openWorldSelectionGUI(player);
                return;
            }
            // Handle PvP Toggle
            else if (clickedItem.getType() == Material.IRON_SWORD && clickedItem.hasItemMeta() && clickedItem.getItemMeta().getDisplayName().contains("Toggle PvP")) {
                if (!player.hasPermission("eventmanager.toggle.pvp")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to toggle PvP.");
                    return;
                }
                boolean currentPvp = world.getPVP();
                world.setPVP(!currentPvp);
                player.sendMessage(ChatColor.GOLD + "[EventManager] " + ChatColor.GREEN + "PvP in " + ChatColor.YELLOW + world.getName() + ChatColor.GREEN + " has been " + (!currentPvp ? ChatColor.AQUA + "ENABLED" : ChatColor.RED + "DISABLED") + ChatColor.GREEN + ".");
                // Update item in GUI (optional, for visual feedback)
                // Re-opening the GUI will show the updated state, or update item directly if preferred
                EventManagerGUI.openSettingsGUI(player, world); // Re-open to refresh with current world state
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
                player.sendMessage(ChatColor.GOLD + "[EventManager] " + ChatColor.GREEN + "Keep Inventory in " + ChatColor.YELLOW + world.getName() + ChatColor.GREEN + " has been " + (!currentKeepInventory ? ChatColor.AQUA + "ENABLED" : ChatColor.RED + "DISABLED") + ChatColor.GREEN + ".");
                EventManagerGUI.openSettingsGUI(player, world);
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
                player.sendMessage(ChatColor.GOLD + "[EventManager] " + ChatColor.GREEN + "Hostile Mob Spawning in " + ChatColor.YELLOW + world.getName() + ChatColor.GREEN + " has been " + (!currentMobSpawning ? ChatColor.AQUA + "ENABLED" : ChatColor.RED + "DISABLED") + ChatColor.GREEN + ".");
                EventManagerGUI.openSettingsGUI(player, world);
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
                player.sendMessage(ChatColor.GOLD + "[EventManager] " + ChatColor.GREEN + "Difficulty in " + ChatColor.YELLOW + world.getName() + ChatColor.GREEN + " changed to " + ChatColor.AQUA + nextDifficulty.toString() + ChatColor.GREEN + ".");
                EventManagerGUI.openSettingsGUI(player, world);
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
                player.sendMessage(ChatColor.GOLD + "[EventManager] " + ChatColor.YELLOW + "Friendly Mob Spawning toggle is complex and not fully implemented in Bukkit/Spigot.");
                // player.sendMessage(ChatColor.GREEN + "Friendly Mob Spawning is currently " + (isCurrentlyEnabled ? "conceptually enabled." : "conceptually disabled."));
                ItemMeta meta = clickedItem.getItemMeta();
                // Update display based on a conceptual state or a custom config value if implemented
                // meta.setDisplayName(ChatColor.RESET + "Toggle Friendly Mob Spawning (" + (isCurrentlyEnabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled") + ChatColor.RESET + ")");
                // clickedItem.setItemMeta(meta);
                EventManagerGUI.openSettingsGUI(player, world); // Re-open to refresh conceptual state if needed
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
                player.sendMessage(ChatColor.GOLD + "[EventManager] " + ChatColor.YELLOW + "Toggling The End dimension access is a server-level configuration ('allow-end' in server.properties) and typically requires a restart.");
                // player.sendMessage(ChatColor.GREEN + "The End dimension is currently " + (isEndEnabled ? "conceptually enabled." : "conceptually disabled."));
                // meta.setDisplayName(ChatColor.RESET + "Toggle End Dimension (" + (isEndEnabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled") + ChatColor.RESET + ")");
                // clickedItem.setItemMeta(meta);
                EventManagerGUI.openSettingsGUI(player, world);
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
                player.sendMessage(ChatColor.GOLD + "[EventManager] " + ChatColor.YELLOW + "Toggling The Nether dimension access is a server-level configuration ('allow-nether' in server.properties) and typically requires a restart.");
                // player.sendMessage(ChatColor.GREEN + "The Nether dimension is currently " + (isNetherEnabled ? "conceptually enabled." : "conceptually disabled."));
                // meta.setDisplayName(ChatColor.RESET + "Toggle Nether Dimension (" + (isNetherEnabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled") + ChatColor.RESET + ")");
                // clickedItem.setItemMeta(meta);
                EventManagerGUI.openSettingsGUI(player, world);
            }
            // Handle Chat Mute Toggle
            else if (clickedItem.getType() == Material.WRITABLE_BOOK && clickedItem.hasItemMeta() && clickedItem.getItemMeta().getDisplayName().contains("Toggle Chat Mute")) {
                if (!player.hasPermission("eventmanager.toggle.chatmute")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to toggle chat mute.");
                    return;
                }
                chatMutedGlobally = !chatMutedGlobally;
                String status = chatMutedGlobally ? ChatColor.RED + "MUTED" : ChatColor.AQUA + "UNMUTED";
                player.sendMessage(ChatColor.GOLD + "[EventManager] " + ChatColor.GREEN + "Global chat has been " + status + ChatColor.GREEN + ".");
                Bukkit.broadcastMessage(ChatColor.GOLD + "[EventManager] " + ChatColor.YELLOW + player.getName() + " has " + status + ChatColor.YELLOW + " global chat.");
                EventManagerGUI.openSettingsGUI(player, world); // Re-open to refresh GUI
            }
            // Handle Broadcast Message (Placeholder)
            else if (clickedItem.getType() == Material.PAPER && clickedItem.hasItemMeta() && clickedItem.getItemMeta().getDisplayName().contains("Broadcast Message")) {
                if (!player.hasPermission("eventmanager.broadcast")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to broadcast messages.");
                    return;
                }
                // This is a placeholder. Actual implementation would require input from the player.
                player.sendMessage(ChatColor.GOLD + "[EventManager] " + ChatColor.YELLOW + "Broadcast feature placeholder. Input mechanism needed.");
                // For now, just close inventory or re-open
                // player.closeInventory(); 
                EventManagerGUI.openSettingsGUI(player, world); // Re-open to refresh GUI or keep it open
            }
            // Handle Admin Auto Vanish Toggle
            else if (clickedItem.getType() == Material.FEATHER && clickedItem.hasItemMeta() && clickedItem.getItemMeta().getDisplayName().contains("Toggle Admin Auto Vanish")) {
                if (!player.hasPermission("eventmanager.toggle.adminautovanish")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to toggle Admin Auto Vanish.");
                    return;
                }
                adminAutoVanishEnabled = !adminAutoVanishEnabled;
                String vanishStatus = adminAutoVanishEnabled ? ChatColor.AQUA + "ENABLED" : ChatColor.RED + "DISABLED";
                player.sendMessage(ChatColor.GOLD + "[EventManager] " + ChatColor.GREEN + "Admin Auto Vanish on join has been " + vanishStatus + ChatColor.GREEN + ".");
                Bukkit.broadcastMessage(ChatColor.GOLD + "[EventManager] " + ChatColor.YELLOW + player.getName() + " has " + vanishStatus + ChatColor.YELLOW + " Admin Auto Vanish on join.");
                EventManagerGUI.openSettingsGUI(player, world); // Re-open to refresh GUI
            }
            // Handle Auto Restart Timer Toggle
            else if (clickedItem.getType() == Material.CLOCK && clickedItem.hasItemMeta() && clickedItem.getItemMeta().getDisplayName().contains("Toggle Auto Restart Timer")) {
                if (!player.hasPermission("eventmanager.toggle.autorestart")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to toggle the Auto Restart Timer.");
                    return;
                }
                if (autoRestartTimerEnabled) {
                    stopAutoRestartTimer();
                    player.sendMessage(ChatColor.GOLD + "[EventManager] " + ChatColor.RED + "Auto Restart Timer DISABLED.");
                    Bukkit.broadcastMessage(ChatColor.GOLD + "[EventManager] " + ChatColor.YELLOW + player.getName() + ChatColor.RED + " DISABLED" + ChatColor.YELLOW + " the Auto Restart Timer.");
                } else {
                    startAutoRestartTimer();
                    player.sendMessage(ChatColor.GOLD + "[EventManager] " + ChatColor.AQUA + "Auto Restart Timer ENABLED. Server will restart in approx. " + autoRestartIntervalMinutes + " minutes.");
                    Bukkit.broadcastMessage(ChatColor.GOLD + "[EventManager] " + ChatColor.YELLOW + player.getName() + ChatColor.AQUA + " ENABLED" + ChatColor.YELLOW + " the Auto Restart Timer. Next restart in approx. " + autoRestartIntervalMinutes + " minutes.");
                }
                EventManagerGUI.openSettingsGUI(player, world); // Re-open to refresh GUI
            }
            // Add more else if blocks here for other items
        }
    }

    @org.bukkit.event.EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (chatMutedGlobally) {
            Player player = event.getPlayer();
            // Allow players with bypass permission to chat
            if (!player.hasPermission("eventmanager.chat.bypass")) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Chat is currently muted by an administrator.");
            }
        }
    }

    @org.bukkit.event.EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (adminAutoVanishEnabled && player.hasPermission("eventmanager.admin.autovanish")) {
            // Apply invisibility - basic potion effect. For true vanish, a dedicated vanish plugin API is better.
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false, false));
            player.sendMessage(ChatColor.GOLD + "[EventManager] " + ChatColor.GRAY + "You have joined silently (vanished).");
            // Optionally, hide join message if not already handled by another plugin
            // event.setJoinMessage(null); // This might be too intrusive or conflict
        }
    }
}