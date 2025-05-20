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
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public class EventManager extends JavaPlugin implements Listener {
    private static boolean chatMutedGlobally = false;
    private static boolean adminAutoVanishEnabled = false;
    private static boolean autoRestartTimerEnabled = false;
    private static int autoRestartTaskId = -1;
    private static long autoRestartIntervalMinutes = 180; // Default 3 hours, configurable later if needed
    private static boolean blockNether = false; // Default: Nether accessible, loaded from config
    private static boolean blockEnd = false;   // Default: End accessible, loaded from config

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

    public static boolean isNetherBlocked() {
        return blockNether;
    }

    public static boolean isEndBlocked() {
        return blockEnd;
    }

    // Method to start the restart timer
    private void startAutoRestartTimer() {
        if (autoRestartTaskId != -1) {
            Bukkit.getScheduler().cancelTask(autoRestartTaskId);
        }
        long ticks = autoRestartIntervalMinutes * 60 * 20; // minutes to ticks
        autoRestartTaskId = Bukkit.getScheduler().runTaskLater(this, () -> {
            Bukkit.broadcastMessage(ChatColor.RED + "[EventManager] Server is restarting in 1 minute!");
            Bukkit.getScheduler().runTaskLater(this, () -> {
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "restart");
            }, 20L * 60);
        }, ticks).getTaskId();
        autoRestartTimerEnabled = true;
        saveAutoRestartConfig();
        getLogger().info("Auto restart timer enabled. Restart in " + autoRestartIntervalMinutes + " minutes.");
    }

    // Method to stop the restart timer
    private void stopAutoRestartTimer() {
        if (autoRestartTaskId != -1) {
            Bukkit.getScheduler().cancelTask(autoRestartTaskId);
            autoRestartTaskId = -1;
        }
        autoRestartTimerEnabled = false;
        saveAutoRestartConfig();
        getLogger().info("Auto restart timer disabled.");
    }
    @Override
    public void onEnable() {
        getLogger().info("EventManager has been enabled!");
        getServer().getPluginManager().registerEvents(this, this);
        // Load configuration
        saveDefaultConfig(); // Creates config.yml if it doesn't exist with defaults from plugin.yml (if any, or just empty)
        loadConfigValues();
    }

    private void loadConfigValues() {
        getConfig().addDefault("block-nether", false);
        getConfig().addDefault("block-end", false);
        getConfig().addDefault("chat-muted-globally", false);
        getConfig().addDefault("admin-auto-vanish", false);
        getConfig().addDefault("auto-restart-interval-minutes", 180);
        getConfig().addDefault("auto-restart-timer-enabled", false);
        blockNether = getConfig().getBoolean("block-nether", false);
        blockEnd = getConfig().getBoolean("block-end", false);
        chatMutedGlobally = getConfig().getBoolean("chat-muted-globally", false);
        adminAutoVanishEnabled = getConfig().getBoolean("admin-auto-vanish", false);
        autoRestartIntervalMinutes = getConfig().getLong("auto-restart-interval-minutes", 180);
        autoRestartTimerEnabled = getConfig().getBoolean("auto-restart-timer-enabled", false);
        getLogger().info("Loaded configuration: blockNether=" + blockNether + ", blockEnd=" + blockEnd + ", chatMutedGlobally=" + chatMutedGlobally + ", adminAutoVanishEnabled=" + adminAutoVanishEnabled + ", autoRestartIntervalMinutes=" + autoRestartIntervalMinutes + ", autoRestartTimerEnabled=" + autoRestartTimerEnabled);
    }

    private void saveDimensionAccessConfig() {
        getConfig().set("block-nether", blockNether);
        getConfig().set("block-end", blockEnd);
        saveConfig();
        getLogger().info("Saved dimension access configuration: blockNether=" + blockNether + ", blockEnd=" + blockEnd);
    }

    private void saveGlobalSettingsConfig() {
        getConfig().set("chat-muted-globally", chatMutedGlobally);
        getConfig().set("admin-auto-vanish", adminAutoVanishEnabled);
        saveConfig();
    }

    private void saveAutoRestartConfig() {
        getConfig().set("auto-restart-interval-minutes", autoRestartIntervalMinutes);
        getConfig().set("auto-restart-timer-enabled", autoRestartTimerEnabled);
        saveConfig();
    }
    @Override
    public void onDisable() {
        stopAutoRestartTimer(); // Ensure timer is stopped on plugin disable
        getLogger().info("EventManager has been disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("eventmanager") || command.getName().equalsIgnoreCase("em")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("eventmanager.reload")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to reload the configuration.");
                    return true;
                }
                reloadConfig();
                loadConfigValues();
                sender.sendMessage(ChatColor.GREEN + "[EventManager] Configuration reloaded successfully!");
                return true;
            }
            
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
            else if (clickedItem.getType() == Material.END_STONE && clickedItem.hasItemMeta() && clickedItem.getItemMeta().getDisplayName().contains("The End:")) { // Display name check updated
                if (!player.hasPermission("eventmanager.toggle.endaccess")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to toggle The End dimension access.");
                    return;
                }
                blockEnd = !blockEnd; // Toggle the state
                saveDimensionAccessConfig(); // Save to config
                String endStatus = blockEnd ? ChatColor.RED + "BLOCKED" : ChatColor.AQUA + "ENABLED";
                String message = ChatColor.GOLD + "[EventManager] " + ChatColor.GREEN + "Player access to The End dimension has been " + endStatus + ChatColor.GREEN + ".";
                player.sendMessage(message);
                Bukkit.broadcastMessage(ChatColor.GOLD + "[EventManager] " + ChatColor.YELLOW + player.getName() + " has set player access to The End dimension to " + endStatus + ChatColor.YELLOW + ".");
                EventManagerGUI.openSettingsGUI(player, world); // Refresh GUI
            }
            // Handle Nether Dimension Toggle
            else if (clickedItem.getType() == Material.NETHERRACK && clickedItem.hasItemMeta() && clickedItem.getItemMeta().getDisplayName().contains("Nether:")) { // Display name check updated
                if (!player.hasPermission("eventmanager.toggle.netheraccess")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to toggle The Nether dimension access.");
                    return;
                }
                blockNether = !blockNether; // Toggle the state
                saveDimensionAccessConfig(); // Save to config
                String netherStatus = blockNether ? ChatColor.RED + "BLOCKED" : ChatColor.AQUA + "ENABLED";
                String message = ChatColor.GOLD + "[EventManager] " + ChatColor.GREEN + "Player access to The Nether dimension has been " + netherStatus + ChatColor.GREEN + ".";
                player.sendMessage(message);
                Bukkit.broadcastMessage(ChatColor.GOLD + "[EventManager] " + ChatColor.YELLOW + player.getName() + " has set player access to The Nether dimension to " + netherStatus + ChatColor.YELLOW + ".");
                EventManagerGUI.openSettingsGUI(player, world); // Refresh GUI
            }
            // Handle Chat Mute Toggle
            else if (clickedItem.getType() == Material.WRITABLE_BOOK && clickedItem.hasItemMeta() && clickedItem.getItemMeta().getDisplayName().contains("Toggle Chat Mute")) {
                if (!player.hasPermission("eventmanager.toggle.chatmute")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to toggle chat mute.");
                    return;
                }
                chatMutedGlobally = !chatMutedGlobally;
                saveGlobalSettingsConfig();
                Bukkit.broadcastMessage(ChatColor.GOLD + "[EventManager] " + ChatColor.GREEN + "Global chat has been " + (chatMutedGlobally ? ChatColor.RED + "MUTED" : ChatColor.AQUA + "UNMUTED") + ChatColor.GREEN + ".");
                EventManagerGUI.openSettingsGUI(player, world);
            }
            // Handle Admin Auto Vanish Toggle
            else if (clickedItem.getType() == Material.FEATHER && clickedItem.hasItemMeta() && clickedItem.getItemMeta().getDisplayName().contains("Toggle Admin Auto Vanish")) {
                if (!player.hasPermission("eventmanager.toggle.adminautovanish")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to toggle admin auto vanish.");
                    return;
                }
                adminAutoVanishEnabled = !adminAutoVanishEnabled;
                saveGlobalSettingsConfig();
                Bukkit.broadcastMessage(ChatColor.GOLD + "[EventManager] " + ChatColor.GREEN + "Admin auto vanish has been " + (adminAutoVanishEnabled ? ChatColor.AQUA + "ENABLED" : ChatColor.RED + "DISABLED") + ChatColor.GREEN + ".");
                EventManagerGUI.openSettingsGUI(player, world);
            }
            // Handle Auto Restart Timer Toggle
            else if (clickedItem.getType() == Material.CLOCK && clickedItem.hasItemMeta() && clickedItem.getItemMeta().getDisplayName().contains("Toggle Auto Restart Timer")) {
                if (!player.hasPermission("eventmanager.toggle.autorestart")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to toggle auto restart timer.");
                    return;
                }
                if (autoRestartTimerEnabled) {
                    stopAutoRestartTimer();
                    Bukkit.broadcastMessage(ChatColor.GOLD + "[EventManager] " + ChatColor.RED + "Auto restart timer has been DISABLED.");
                } else {
                    startAutoRestartTimer();
                    Bukkit.broadcastMessage(ChatColor.GOLD + "[EventManager] " + ChatColor.AQUA + "Auto restart timer has been ENABLED. Next restart in " + autoRestartIntervalMinutes + " minutes.");
                }
                EventManagerGUI.openSettingsGUI(player, world);
            }
            // Handle Nether Access Toggle
            else if (clickedItem.getType() == Material.NETHERRACK && clickedItem.hasItemMeta() && clickedItem.getItemMeta().getDisplayName().contains("Nether")) {
                if (!player.hasPermission("eventmanager.toggle.netheraccess")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to toggle Nether access.");
                    return;
                }
                blockNether = !blockNether;
                saveDimensionAccessConfig();
                Bukkit.broadcastMessage(ChatColor.GOLD + "[EventManager] " + ChatColor.GREEN + "Nether access has been " + (blockNether ? ChatColor.RED + "BLOCKED" : ChatColor.AQUA + "ENABLED") + ChatColor.GREEN + ".");
                EventManagerGUI.openSettingsGUI(player, world);
            }
            // Handle End Access Toggle
            else if (clickedItem.getType() == Material.END_STONE && clickedItem.hasItemMeta() && clickedItem.getItemMeta().getDisplayName().contains("End")) {
                if (!player.hasPermission("eventmanager.toggle.endaccess")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to toggle End access.");
                    return;
                }
                blockEnd = !blockEnd;
                saveDimensionAccessConfig();
                Bukkit.broadcastMessage(ChatColor.GOLD + "[EventManager] " + ChatColor.GREEN + "End access has been " + (blockEnd ? ChatColor.RED + "BLOCKED" : ChatColor.AQUA + "ENABLED") + ChatColor.GREEN + ".");
                EventManagerGUI.openSettingsGUI(player, world);
            }
            // Add more else if blocks here for other items
        }
    }

    @org.bukkit.event.EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        // Allow ops or players with specific permission to bypass
        if (player.isOp() || player.hasPermission("eventmanager.bypass.dimensionblock")) {
            return;
        }

        // Check destination world and environment
        if (event.getTo() == null || event.getTo().getWorld() == null) {
            return; // Should not happen with portals, but good practice
        }
        World.Environment toEnvironment = event.getTo().getWorld().getEnvironment();

        if (toEnvironment == World.Environment.NETHER && blockNether) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "⚠ Nether is currently disabled by server admin.");
        } else if (toEnvironment == World.Environment.THE_END && blockEnd) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "⚠ The End is currently disabled by server admin.");
        }
    }

    @org.bukkit.event.EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        // Allow ops or players with specific permission to bypass
        if (player.isOp() || player.hasPermission("eventmanager.bypass.dimensionblock")) {
            return;
        }

        // Check destination world and environment
        if (event.getTo() == null || event.getTo().getWorld() == null) {
            return; // Destination might be null if teleport is invalid
        }
        World.Environment toEnvironment = event.getTo().getWorld().getEnvironment();
        TeleportCause cause = event.getCause();

        boolean isPortalTeleport = (cause == TeleportCause.NETHER_PORTAL || cause == TeleportCause.END_PORTAL);

        if (toEnvironment == World.Environment.NETHER && blockNether) {
            event.setCancelled(true);
            if (!isPortalTeleport) { // Only send message if not a portal (portal event handles its own message)
                player.sendMessage(ChatColor.RED + "⚠ Nether is currently disabled by server admin.");
            }
        } else if (toEnvironment == World.Environment.THE_END && blockEnd) {
            event.setCancelled(true);
            if (!isPortalTeleport) { // Only send message if not a portal
                player.sendMessage(ChatColor.RED + "⚠ The End is currently disabled by server admin.");
            }
        }
    }

    // Existing event handlers like AsyncPlayerChatEvent, PlayerJoinEvent might be in the truncated part or follow here.

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