package com.pushkraj.eventmanager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.pushkraj.eventmanager.util.ErrorHandler;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.onarandombox.multiversecore.MultiverseCore;

public class EventManager extends JavaPlugin implements Listener {
    private static boolean chatMutedGlobally = false;
    private static boolean adminAutoVanishEnabled = false;
    private static boolean autoRestartTimerEnabled = false;
    private static int autoRestartTaskId = -1;
    private static long autoRestartIntervalMinutes = 180; // Default 3 hours, configurable later if needed
    private static boolean blockNether = false; // Default: Nether accessible, loaded from config
    private static boolean blockEnd = false;   // Default: End accessible, loaded from config
    private static com.onarandombox.multiversecore.MultiverseCore multiverseCore = null;
    private static boolean debugMode = false; // Default debug mode

    public static boolean isMultiverseEnabled() {
        return multiverseCore != null;
    }

    public static com.onarandombox.multiversecore.MultiverseCore getMultiverseCore() {
        return multiverseCore;
    }

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
    @Override
    public void onEnable() {
        try {
            ErrorHandler.init(this); // Initialize ErrorHandler
            EventManagerGUI.init(this); // Initialize EventManagerGUI
            getLogger().info("EventManager has been enabled!");
            getServer().getPluginManager().registerEvents(this, this);

            // Initialize Multiverse-Core integration
            if (getServer().getPluginManager().getPlugin("Multiverse-Core") != null) {
                multiverseCore = (com.onarandombox.multiversecore.MultiverseCore) getServer().getPluginManager().getPlugin("Multiverse-Core");
                ErrorHandler.info("Multiverse-Core integration enabled!");
            } else {
                ErrorHandler.info("Multiverse-Core not found. World management features will be limited.");
            }

            // Load configuration
            saveDefaultConfig(); // Creates config.yml if it doesn't exist with defaults from plugin.yml (if any, or just empty)
            loadConfigValues();
            
            // Load world-specific settings
            loadWorldSettings();
        } catch (Exception e) {
            getLogger().severe("[EventManager CRITICAL] Failed to enable EventManager plugin. See console for details.");
            e.printStackTrace(); // Print stack trace for critical failure during enable
            getServer().getPluginManager().disablePlugin(this); // Disable plugin if enable fails critically
        }
    }

    // Original onEnable content moved here, to be called within the try-catch block
    private void enablePlugin() {
        getLogger().info("EventManager has been enabled!");
        getServer().getPluginManager().registerEvents(this, this);

        // Initialize Multiverse-Core integration
        if (getServer().getPluginManager().getPlugin("Multiverse-Core") != null) {
            multiverseCore = (com.onarandombox.multiversecore.MultiverseCore) getServer().getPluginManager().getPlugin("Multiverse-Core");
            getLogger().info("Multiverse-Core integration enabled!");
        } else {
            getLogger().info("Multiverse-Core not found. World management features will be limited.");
        }

        // Load configuration
        saveDefaultConfig(); // Creates config.yml if it doesn't exist with defaults from plugin.yml (if any, or just empty)
        loadConfigValues();
        
        // Load world-specific settings
        loadWorldSettings();
    }

    private void loadConfigValues() {
        getConfig().addDefault("block-nether", false);
        getConfig().addDefault("block-end", false);
        getConfig().addDefault("chat-muted-globally", false);
        getConfig().addDefault("admin-auto-vanish", false);
        getConfig().addDefault("auto-restart-interval-minutes", 180);
        getConfig().addDefault("auto-restart-timer-enabled", false);
        getConfig().addDefault("debug-mode", false); // Add default for debug-mode
        blockNether = getConfig().getBoolean("block-nether", false);
        blockEnd = getConfig().getBoolean("block-end", false);
        chatMutedGlobally = getConfig().getBoolean("chat-muted-globally", false);
        adminAutoVanishEnabled = getConfig().getBoolean("admin-auto-vanish", false);
        autoRestartIntervalMinutes = getConfig().getLong("auto-restart-interval-minutes", 180);
        autoRestartTimerEnabled = getConfig().getBoolean("auto-restart-timer-enabled", false);
        debugMode = getConfig().getBoolean("debug-mode", false); // Load debug-mode
        ErrorHandler.info("Loaded configuration: blockNether=" + blockNether + ", blockEnd=" + blockEnd + ", chatMutedGlobally=" + chatMutedGlobally + ", adminAutoVanishEnabled=" + adminAutoVanishEnabled + ", autoRestartIntervalMinutes=" + autoRestartIntervalMinutes + ", autoRestartTimerEnabled=" + autoRestartTimerEnabled + ", debugMode=" + debugMode);
    }

    private void loadConfigValues() {
        try {
            getConfig().addDefault("block-nether", false);
            getConfig().addDefault("block-end", false);
            getConfig().addDefault("chat-muted-globally", false);
            getConfig().addDefault("admin-auto-vanish", false);
            getConfig().addDefault("auto-restart-interval-minutes", 180);
            getConfig().addDefault("auto-restart-timer-enabled", false);
            getConfig().addDefault("debug-mode", false); // Ensure debug-mode is added
            getConfig().options().copyDefaults(true);
            saveConfig();

            blockNether = getConfig().getBoolean("block-nether");
            blockEnd = getConfig().getBoolean("block-end");
            chatMutedGlobally = getConfig().getBoolean("chat-muted-globally");
            adminAutoVanishEnabled = getConfig().getBoolean("admin-auto-vanish");
            autoRestartIntervalMinutes = getConfig().getLong("auto-restart-interval-minutes");
            autoRestartTimerEnabled = getConfig().getBoolean("auto-restart-timer-enabled");
            debugMode = getConfig().getBoolean("debug-mode");
            ErrorHandler.info("Loaded configuration: blockNether=" + blockNether + ", blockEnd=" + blockEnd + ", chatMutedGlobally=" + chatMutedGlobally + ", adminAutoVanishEnabled=" + adminAutoVanishEnabled + ", autoRestartIntervalMinutes=" + autoRestartIntervalMinutes + ", autoRestartTimerEnabled=" + autoRestartTimerEnabled + ", debugMode=" + debugMode);
        } catch (Exception e) {
            ErrorHandler.log("loading plugin configuration", e);
        }
    }

    private void saveDimensionAccessConfig() {
        try {
            getConfig().set("block-nether", blockNether);
            getConfig().set("block-end", blockEnd);
            saveConfig();
            ErrorHandler.info("Saved dimension access configuration: blockNether=" + blockNether + ", blockEnd=" + blockEnd);
        } catch (Exception e) {
            ErrorHandler.log("saving dimension access configuration", e);
        }
    }

    private void saveGlobalSettingsConfig() {
        try {
            getConfig().set("chat-muted-globally", chatMutedGlobally);
            getConfig().set("admin-auto-vanish", adminAutoVanishEnabled);
            saveConfig();
            ErrorHandler.info("Saved global settings configuration.");
        } catch (Exception e) {
            ErrorHandler.log("saving global settings configuration", e);
        }
    }

    private void saveAutoRestartConfig() {
        try {
            getConfig().set("auto-restart-interval-minutes", autoRestartIntervalMinutes);
            getConfig().set("auto-restart-timer-enabled", autoRestartTimerEnabled);
            saveConfig();
            ErrorHandler.info("Saved auto-restart configuration.");
        } catch (Exception e) {
            ErrorHandler.log("saving auto-restart configuration", e);
        }
    }

    private void loadWorldSettings() {
        try {
            if (!getConfig().contains("world-settings")) {
                getConfig().createSection("world-settings");
                saveConfig();
            }

            for (World world : Bukkit.getWorlds()) {
                String worldName = world.getName();
                String path = "world-settings." + worldName;

                if (!getConfig().contains(path)) {
                    getConfig().createSection(path);
                    getConfig().set(path + ".pvp", world.getPVP());
                    getConfig().set(path + ".keep-inventory", world.getGameRuleValue(GameRule.KEEP_INVENTORY));
                    getConfig().set(path + ".difficulty", world.getDifficulty().toString());
                    saveConfig();
                }

                world.setPVP(getConfig().getBoolean(path + ".pvp"));
                world.setGameRule(GameRule.KEEP_INVENTORY, getConfig().getBoolean(path + ".keep-inventory"));
                try {
                    world.setDifficulty(Difficulty.valueOf(getConfig().getString(path + ".difficulty")));
                } catch (IllegalArgumentException e) {
                    ErrorHandler.warn("loading world settings for " + worldName, "Invalid difficulty value: " + getConfig().getString(path + ".difficulty"));
                }
            }
            ErrorHandler.info("World settings loaded/applied.");
        } catch (Exception e) {
            ErrorHandler.log("loading world settings", e);
        }
    }

    public void saveWorldSettings(World world) {
        if (world == null) {
            ErrorHandler.warn("saving world settings", "Attempted to save settings for a null world.");
            return;
        }
        try {
            String path = "world-settings." + world.getName();
            getConfig().set(path + ".pvp", world.getPVP());
            getConfig().set(path + ".keep-inventory", world.getGameRuleValue(GameRule.KEEP_INVENTORY));
            getConfig().set(path + ".difficulty", world.getDifficulty().toString());
            saveConfig();
            ErrorHandler.info("Saved settings for world: " + world.getName());
        } catch (Exception e) {
            ErrorHandler.log("saving settings for world " + world.getName(), e);
        }
    }

    // Method to start the restart timer
    private void startAutoRestartTimer() {
        try {
            if (autoRestartTaskId != -1) {
                Bukkit.getScheduler().cancelTask(autoRestartTaskId);
            }
            long ticks = autoRestartIntervalMinutes * 60 * 20; // minutes to ticks
            autoRestartTaskId = Bukkit.getScheduler().runTaskLater(this, () -> {
                try {
                    Bukkit.broadcastMessage(ChatColor.RED + "[EventManager] Server is restarting in 1 minute!");
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        try {
                            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "restart");
                        } catch (Exception e) {
                            ErrorHandler.log("executing scheduled server restart", e);
                        }
                    }, 20L * 60);
                } catch (Exception e) {
                    ErrorHandler.log("scheduling server restart notification", e);
                }
            }, ticks).getTaskId();
            autoRestartTimerEnabled = true;
            saveAutoRestartConfig();
            ErrorHandler.info("Auto restart timer enabled. Restart in " + autoRestartIntervalMinutes + " minutes.");
        } catch (Exception e) {
            ErrorHandler.log("starting auto restart timer", e);
        }
    }

    // Method to stop the restart timer
    private void stopAutoRestartTimer() {
        try {
            if (autoRestartTaskId != -1) {
                Bukkit.getScheduler().cancelTask(autoRestartTaskId);
                autoRestartTaskId = -1;
            }
            autoRestartTimerEnabled = false;
            saveAutoRestartConfig();
            ErrorHandler.info("Auto restart timer disabled.");
        } catch (Exception e) {
            ErrorHandler.log("stopping auto restart timer", e);
        }
    }

    @Override
    public void onDisable() {
        try {
            stopAutoRestartTimer(); // Ensure timer is stopped on plugin disable
            ErrorHandler.info("EventManager has been disabled!");
        } catch (Exception e) {
            ErrorHandler.log("disabling EventManager plugin", e);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (command.getName().equalsIgnoreCase("eventmanager") || command.getName().equalsIgnoreCase("em")) {
                if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                    if (!sender.hasPermission("eventmanager.reload")) {
                        sender.sendMessage(ChatColor.RED + "You do not have permission to reload the configuration.");
                        return true;
                    }
                    try {
                        reloadConfig();
                        loadConfigValues(); // Reload all config values including debugMode
                        sender.sendMessage(ChatColor.GREEN + "[EventManager] Configuration reloaded successfully!");
                    } catch (Exception e) {
                        ErrorHandler.log("reloading plugin configuration", e, sender);
                    }
                    return true;
                }
                
                if (!(sender instanceof Player)) {
                    sender.sendMessage("This command can only be run by a player.");
                    return true;
                }
                Player player = (Player) sender;
                if (!player.hasPermission("eventmanager.gui")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    return true;
                }
                // Open GUI logic
                EventManagerGUI.openWorldSelectionGUI(player);
                return true;
            }
        } catch (Exception e) {
            ErrorHandler.log("processing command '" + command.getName() + "'", e, sender);
        }
        return false;
    }

    // Event Handlers with Error Handling
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        try {
            if (chatMutedGlobally && !event.getPlayer().hasPermission("eventmanager.chat.bypass")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "Chat is currently muted globally.");
            }
        } catch (Exception e) {
            ErrorHandler.log("handling AsyncPlayerChatEvent", e, event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        try {
            Player player = event.getPlayer();
            if (adminAutoVanishEnabled && player.hasPermission("eventmanager.admin.autovanish")) {
                // Basic vanish - for a more robust solution, consider a proper Vanish plugin API
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (!onlinePlayer.hasPermission("eventmanager.admin.seevanished")) { // Add a perm to see vanished admins
                        onlinePlayer.hidePlayer(this, player);
                    }
                }
                player.sendMessage(ChatColor.YELLOW + "You have automatically vanished upon joining.");
                ErrorHandler.info("Admin " + player.getName() + " auto-vanished on join.");
            }
        } catch (Exception e) {
            ErrorHandler.log("handling PlayerJoinEvent for auto-vanish", e, event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPortal(PlayerPortalEvent event) {
        try {
            if (event.isCancelled() || event.getPlayer().hasPermission("eventmanager.bypass.dimensionblock")) {
                return;
            }

            PlayerTeleportEvent.TeleportCause cause = event.getCause();
            World fromWorld = event.getFrom().getWorld();
            World toWorld = event.getTo() != null ? event.getTo().getWorld() : null;

            if (fromWorld == null) return; // Should not happen, but good practice

            // Check if trying to enter Nether
            if (cause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
                if (toWorld != null && toWorld.getEnvironment() == World.Environment.NETHER && blockNether) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(ChatColor.RED + "Access to The Nether is currently disabled.");
                    ErrorHandler.info("Blocked Nether portal travel for " + event.getPlayer().getName());
                }
            }
            // Check if trying to enter End
            else if (cause == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
                if (toWorld != null && toWorld.getEnvironment() == World.Environment.THE_END && blockEnd) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(ChatColor.RED + "Access to The End is currently disabled.");
                    ErrorHandler.info("Blocked End portal travel for " + event.getPlayer().getName());
                }
            }
        } catch (Exception e) {
            ErrorHandler.log("handling PlayerPortalEvent for dimension blocking", e, event.getPlayer());
        }
    }

    // Static getters for settings, now they can be accessed from ErrorHandler if needed (though ErrorHandler uses getConfig directly)
    public static boolean isDebugMode() {
        return debugMode;
    }

    // Setter methods for toggles, now with ErrorHandler logging
    public static void setChatMutedGlobally(boolean muted, CommandSender sender) {
        try {
            chatMutedGlobally = muted;
            JavaPlugin plugin = EventManager.getPlugin(EventManager.class);
            plugin.getConfig().set("chat-muted-globally", chatMutedGlobally);
            plugin.saveConfig();
            String status = muted ? "muted" : "unmuted";
            Bukkit.broadcastMessage(ChatColor.GOLD + "[EventManager] Global chat has been " + status + ".");
            ErrorHandler.info("Global chat set to: " + status + " by " + (sender != null ? sender.getName() : "CONSOLE"));
        } catch (Exception e) {
            ErrorHandler.log("setting global chat mute to " + muted, e, sender);
        }
    }

    public static void setAdminAutoVanishEnabled(boolean enabled, CommandSender sender) {
        try {
            adminAutoVanishEnabled = enabled;
            JavaPlugin plugin = EventManager.getPlugin(EventManager.class);
            plugin.getConfig().set("admin-auto-vanish", adminAutoVanishEnabled);
            plugin.saveConfig();
            String status = enabled ? "enabled" : "disabled";
            Bukkit.broadcastMessage(ChatColor.GOLD + "[EventManager] Admin auto-vanish has been " + status + ".");
            ErrorHandler.info("Admin auto-vanish set to: " + status + " by " + (sender != null ? sender.getName() : "CONSOLE"));
        } catch (Exception e) {
            ErrorHandler.log("setting admin auto-vanish to " + enabled, e, sender);
        }
    }

    public void toggleAutoRestartTimer(CommandSender sender) {
        try {
            if (autoRestartTimerEnabled) {
                stopAutoRestartTimer();
                if (sender != null) sender.sendMessage(ChatColor.GREEN + "Auto-restart timer disabled.");
            } else {
                startAutoRestartTimer();
                if (sender != null) sender.sendMessage(ChatColor.GREEN + "Auto-restart timer enabled. Restart in " + autoRestartIntervalMinutes + " minutes.");
            }
        } catch (Exception e) {
            ErrorHandler.log("toggling auto-restart timer", e, sender);
        }
    }

    public void setAutoRestartInterval(long minutes, CommandSender sender) {
        try {
            if (minutes <= 0) {
                if (sender != null) sender.sendMessage(ChatColor.RED + "Restart interval must be greater than 0 minutes.");
                return;
            }
            autoRestartIntervalMinutes = minutes;
            saveAutoRestartConfig();
            if (sender != null) sender.sendMessage(ChatColor.GREEN + "Auto-restart interval set to " + minutes + " minutes.");
            ErrorHandler.info("Auto-restart interval set to " + minutes + " minutes by " + (sender != null ? sender.getName() : "CONSOLE"));
            if (autoRestartTimerEnabled) { // If timer is running, restart it with new interval
                stopAutoRestartTimer();
                startAutoRestartTimer();
                if (sender != null) sender.sendMessage(ChatColor.YELLOW + "Auto-restart timer restarted with new interval.");
            }
        } catch (Exception e) {
            ErrorHandler.log("setting auto-restart interval to " + minutes, e, sender);
        }
    }

    public static void setNetherBlocked(boolean blocked, CommandSender sender) {
        try {
            blockNether = blocked;
            JavaPlugin plugin = EventManager.getPlugin(EventManager.class);
            plugin.getConfig().set("block-nether", blockNether);
            plugin.saveConfig();
            String status = blocked ? "disabled" : "enabled";
            Bukkit.broadcastMessage(ChatColor.GOLD + "[EventManager] Access to The Nether is now " + status + ".");
            ErrorHandler.info("Nether access set to: " + status + " by " + (sender != null ? sender.getName() : "CONSOLE"));
        } catch (Exception e) {
            ErrorHandler.log("setting Nether access to " + blocked, e, sender);
        }
    }

    public static void setEndBlocked(boolean blocked, CommandSender sender) {
        try {
            blockEnd = blocked;
            JavaPlugin plugin = EventManager.getPlugin(EventManager.class);
            plugin.getConfig().set("block-end", blockEnd);
            plugin.saveConfig();
            String status = blocked ? "disabled" : "enabled";
            Bukkit.broadcastMessage(ChatColor.GOLD + "[EventManager] Access to The End is now " + status + ".");
            ErrorHandler.info("End access set to: " + status + " by " + (sender != null ? sender.getName() : "CONSOLE"));
        } catch (Exception e) {
            ErrorHandler.log("setting End access to " + blocked, e, sender);
        }
    }

    // Method to toggle PvP for a specific world
    public void togglePVP(World world, Player player) {
        if (world == null) {
            ErrorHandler.log("toggling PvP", new IllegalStateException("World cannot be null"), player);
            return;
        }
        try {
            boolean newPVPState = !world.getPVP();
            world.setPVP(newPVPState);
            saveWorldSettings(world);
            String status = newPVPState ? "enabled" : "disabled";
            player.sendMessage(ChatColor.GREEN + "PvP in world '" + world.getName() + "' has been " + status + ".");
            ErrorHandler.info("PvP in world '" + world.getName() + "' set to " + status + " by " + player.getName());
        } catch (Exception e) {
            ErrorHandler.log("toggling PvP in world '" + world.getName() + "'", e, player);
        }
    }

    // Method to toggle Keep Inventory for a specific world
    public void toggleKeepInventory(World world, Player player) {
        if (world == null) {
            ErrorHandler.log("toggling KeepInventory", new IllegalStateException("World cannot be null"), player);
            return;
        }
        try {
            Boolean currentKeepInv = world.getGameRuleValue(GameRule.KEEP_INVENTORY);
            boolean newKeepInvState = !(currentKeepInv != null && currentKeepInv); // if null or false, set to true. if true, set to false.
            world.setGameRule(GameRule.KEEP_INVENTORY, newKeepInvState);
            saveWorldSettings(world);
            String status = newKeepInvState ? "enabled" : "disabled";
            player.sendMessage(ChatColor.GREEN + "Keep Inventory in world '" + world.getName() + "' has been " + status + ".");
            ErrorHandler.info("Keep Inventory in world '" + world.getName() + "' set to " + status + " by " + player.getName());
        } catch (Exception e) {
            ErrorHandler.log("toggling Keep Inventory in world '" + world.getName() + "'", e, player);
        }
    }

    // Method to set difficulty for a specific world
    public void setWorldDifficulty(World world, Difficulty difficulty, Player player) {
        if (world == null) {
            ErrorHandler.log("setting world difficulty", new IllegalStateException("World cannot be null"), player);
            return;
        }
        if (difficulty == null) {
            ErrorHandler.log("setting world difficulty for '" + world.getName() + "'", new IllegalArgumentException("Difficulty cannot be null"), player);
            return;
        }
        try {
            world.setDifficulty(difficulty);
            saveWorldSettings(world);
            player.sendMessage(ChatColor.GREEN + "Difficulty in world '" + world.getName() + "' set to " + difficulty.toString() + ".");
            ErrorHandler.info("Difficulty in world '" + world.getName() + "' set to " + difficulty.toString() + " by " + player.getName());
        } catch (Exception e) {
            ErrorHandler.log("setting difficulty for world '" + world.getName() + "' to " + difficulty.toString(), e, player);
        }
    }

    // Method to teleport a player to a world's spawn
    public void teleportToWorldSpawn(Player player, String worldName) {
        try {
            World targetWorld = Bukkit.getWorld(worldName);
            if (targetWorld == null) {
                ErrorHandler.log("teleporting player " + player.getName() + " to world " + worldName, new IllegalStateException("World '" + worldName + "' not found."), player);
                return;
            }
            player.teleport(targetWorld.getSpawnLocation());
            player.sendMessage(ChatColor.GREEN + "Teleported to the spawn of world '" + worldName + "'.");
            ErrorHandler.info("Player " + player.getName() + " teleported to spawn of world '" + worldName + "'.");
        } catch (Exception e) {
            ErrorHandler.log("teleporting player " + player.getName() + " to world '" + worldName + "'", e, player);
        }
    }

    // Method to broadcast a message
    public void broadcastMessage(String message, Player sender) {
        if (message == null || message.trim().isEmpty()) {
            if (sender != null) sender.sendMessage(ChatColor.RED + "Cannot broadcast an empty message.");
            return;
        }
        try {
            String formattedMessage = ChatColor.translateAlternateColorCodes('&', message);
            Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "[Broadcast] " + ChatColor.WHITE + formattedMessage);
            ErrorHandler.info("Broadcast by " + (sender != null ? sender.getName() : "CONSOLE") + ": " + formattedMessage);
        } catch (Exception e) {
            ErrorHandler.log("broadcasting message", e, sender);
        }
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

    private void loadWorldSettings() {
        if (!getConfig().contains("world-settings")) {
            getConfig().createSection("world-settings");
            saveConfig();
        }

        // Load settings for each world
        for (World world : Bukkit.getWorlds()) {
            String worldName = world.getName();
            String path = "world-settings." + worldName;

            // Create section for world if it doesn't exist
            if (!getConfig().contains(path)) {
                getConfig().createSection(path);
                // Set default values
                getConfig().set(path + ".pvp", world.getPVP());
                getConfig().set(path + ".keep-inventory", world.getGameRuleValue(GameRule.KEEP_INVENTORY));
                getConfig().set(path + ".difficulty", world.getDifficulty().toString());
                saveConfig();
            }

            // Apply saved settings
            world.setPVP(getConfig().getBoolean(path + ".pvp"));
            world.setGameRule(GameRule.KEEP_INVENTORY, getConfig().getBoolean(path + ".keep-inventory"));
            try {
                world.setDifficulty(Difficulty.valueOf(getConfig().getString(path + ".difficulty")));
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid difficulty value for world " + worldName);
            }
        }
    }

    public void saveWorldSettings(World world) {
        String path = "world-settings." + world.getName();
        getConfig().set(path + ".pvp", world.getPVP());
        getConfig().set(path + ".keep-inventory", world.getGameRuleValue(GameRule.KEEP_INVENTORY));
        getConfig().set(path + ".difficulty", world.getDifficulty().toString());
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
        ItemStack clickedItem = event.getCurrentItem();
        String viewTitle = view.getTitle();

        try {
            if (clickedItem == null || clickedItem.getType() == Material.AIR || clickedItem.getItemMeta() == null) return;

            // Prevent taking items from GUI
            if (viewTitle.startsWith(EventManagerGUI.SETTINGS_GUI_TITLE_PREFIX) || 
                viewTitle.equals(EventManagerGUI.WORLD_SELECTION_GUI_TITLE) ||
                viewTitle.equals(EventManagerGUI.MULTIVERSE_WORLDS_GUI_TITLE)) {
                event.setCancelled(true);
            }

            // World Selection GUI
            if (viewTitle.equals(EventManagerGUI.WORLD_SELECTION_GUI_TITLE)) {
                handleWorldSelectionGUIClick(player, clickedItem); // Refactored logic with its own try-catch
                return;
            }

            // Multiverse Worlds GUI
            if (viewTitle.equals(EventManagerGUI.MULTIVERSE_WORLDS_GUI_TITLE)) {
                handleMultiverseWorldsGUIClick(player, clickedItem, event.getSlot(), event.getInventory().getSize()); // Refactored logic
                return;
            }

            // Settings GUI (extract world name from title)
            if (viewTitle.startsWith(EventManagerGUI.SETTINGS_GUI_TITLE_PREFIX)) {
                String worldNameFromTitleRaw = viewTitle.substring(EventManagerGUI.SETTINGS_GUI_TITLE_PREFIX.length());
                String worldNameFromTitle = ChatColor.stripColor(worldNameFromTitleRaw);
                World world = Bukkit.getWorld(worldNameFromTitle);

                if (world == null) {
                    ErrorHandler.log("processing settings GUI click for world '" + worldNameFromTitle + "'", new IllegalStateException("World not found from GUI title."), player);
                    player.closeInventory();
                    return;
                }
                handleSettingsGUIClick(player, world, clickedItem, view); // Refactored logic
            }
        } catch (Exception e) {
            ErrorHandler.log("handling InventoryClickEvent in GUI: " + viewTitle, e, player);
            // player.closeInventory(); // Optional: close inventory on any major error
        }
    }

    // Extracted logic for world selection GUI click
    private void handleWorldSelectionGUIClick(Player player, ItemStack clickedItem) {
        try {
            if (!clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) return;
            String itemName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

            if (itemName.contains("Multiverse Worlds")) {
                if (EventManager.isMultiverseEnabled()) {
                    EventManagerGUI.openMultiverseWorldsGUI(player);
                } else {
                    player.sendMessage(ChatColor.RED + "Multiverse-Core is not available.");
                    ErrorHandler.warn("World Selection GUI click by " + player.getName(), "Multiverse Worlds clicked but Multiverse-Core not enabled.");
                }
                return;
            }

            World targetWorld = Bukkit.getWorlds().stream()
                .filter(w -> itemName.equalsIgnoreCase(w.getName()))
                .findFirst().orElse(null);

            if (targetWorld == null) {
                // Fallback for default world names if direct name match fails
                if (clickedItem.getType() == Material.GRASS_BLOCK && itemName.contains(Bukkit.getWorlds().get(0).getName())) targetWorld = Bukkit.getWorlds().get(0); // Assuming first world is overworld
                else if (clickedItem.getType() == Material.NETHERRACK) targetWorld = Bukkit.getWorlds().stream().filter(w -> w.getEnvironment() == World.Environment.NETHER).findFirst().orElse(null);
                else if (clickedItem.getType() == Material.END_STONE) targetWorld = Bukkit.getWorlds().stream().filter(w -> w.getEnvironment() == World.Environment.THE_END).findFirst().orElse(null);
            }

            if (targetWorld != null) {
                EventManagerGUI.openSettingsGUI(player, targetWorld);
            } else if (!clickedItem.getType().name().endsWith("_STAINED_GLASS_PANE")) { // Ignore filler items
                 ErrorHandler.warn("World Selection GUI click by " + player.getName(), "No world matched for item: " + itemName + " (Material: " + clickedItem.getType() + ")");
                 player.sendMessage(ChatColor.RED + "Could not determine the world for: " + itemName);
            }
        } catch (Exception e) {
            String clickedItemName = (clickedItem != null && clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName()) ? clickedItem.getItemMeta().getDisplayName() : (clickedItem != null ? clickedItem.getType().toString() : "null");
            ErrorHandler.log("handling World Selection GUI click for item '" + clickedItemName + "'", e, player);
        }
    }

    // Extracted logic for Multiverse worlds GUI click
    private void handleMultiverseWorldsGUIClick(Player player, ItemStack clickedItem, int slot, int invSize) {
        try {
            if (!clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) return;
            String itemName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

            if (clickedItem.getType() == Material.ARROW && itemName.contains("Back")) { // More robust back button check
                EventManagerGUI.openWorldSelectionGUI(player);
                return;
            }
            if (clickedItem.getType().name().endsWith("_STAINED_GLASS_PANE")) return; // Ignore filler

            World targetWorld = Bukkit.getWorld(itemName); // MVWorld names are usually direct world names
            if (targetWorld != null) {
                EventManagerGUI.openSettingsGUI(player, targetWorld);
            } else {
                ErrorHandler.warn("Multiverse Worlds GUI click by " + player.getName(), "Multiverse world not found: " + itemName);
                player.sendMessage(ChatColor.RED + "Could not find Multiverse world: " + itemName);
            }
        } catch (Exception e) {
            String clickedItemName = (clickedItem != null && clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName()) ? clickedItem.getItemMeta().getDisplayName() : (clickedItem != null ? clickedItem.getType().toString() : "null");
            ErrorHandler.log("handling Multiverse Worlds GUI click for item '" + clickedItemName + "'", e, player);
        }
    }

    // Extracted logic for settings GUI click
    private void handleSettingsGUIClick(Player player, World world, ItemStack clickedItem, InventoryView view) {
        try {
            if (!clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) return;
            String itemName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
            String worldDisplayName = ChatColor.stripColor(view.getTitle().substring(EventManagerGUI.SETTINGS_GUI_TITLE_PREFIX.length()));

            if (clickedItem.getType() == Material.ARROW && itemName.contains("Back to World Selection")) {
                EventManagerGUI.openWorldSelectionGUI(player);
                return;
            }
            if (clickedItem.getType().name().endsWith("_STAINED_GLASS_PANE")) return;

            // Permission checks should be done before attempting actions
            // Example for PvP:
            if (itemName.contains("Toggle PvP")) {
                if (!player.hasPermission("eventmanager.toggle.pvp")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to toggle PvP.");
                    return;
                }
                togglePVP(world, player); // This method already has its own try-catch
            } else if (itemName.contains("Toggle Hostile Mob Spawning")) {
                if (!player.hasPermission("eventmanager.toggle.mobspawning")) { // Assuming this permission
                    player.sendMessage(ChatColor.RED + "You don't have permission to toggle hostile mob spawning.");
                    return;
                }
                try {
                    Boolean currentMobSpawning = world.getGameRuleValue(GameRule.DO_MOB_SPAWNING);
                    boolean newMobSpawningState = !(currentMobSpawning != null && currentMobSpawning);
                    world.setGameRule(GameRule.DO_MOB_SPAWNING, newMobSpawningState);
                    saveWorldSettings(world); // Has error handling
                    player.sendMessage(ChatColor.GREEN + "Hostile Mob Spawning in '" + worldDisplayName + "' set to " + (newMobSpawningState ? "Enabled" : "Disabled") + ".");
                    ErrorHandler.info("Hostile Mob Spawning in '" + worldDisplayName + "' set to " + (newMobSpawningState ? "Enabled" : "Disabled") + " by " + player.getName());
                } catch (Exception e) {
                    ErrorHandler.log("toggling Hostile Mob Spawning in '" + worldDisplayName + "'", e, player);
                }
            } else if (itemName.contains("Toggle Keep Inventory")) {
                 if (!player.hasPermission("eventmanager.toggle.keepinventory")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to toggle Keep Inventory.");
                    return;
                }
                toggleKeepInventory(world, player); // Has error handling
            } else if (itemName.contains("Change Difficulty")) {
                if (!player.hasPermission("eventmanager.changedifficulty")) { // Assuming this permission
                    player.sendMessage(ChatColor.RED + "You don't have permission to change difficulty.");
                    return;
                }
                try {
                    Difficulty currentDiff = world.getDifficulty();
                    Difficulty nextDiff;
                    switch (currentDiff) {
                        case PEACEFUL: nextDiff = Difficulty.EASY; break;
                        case EASY: nextDiff = Difficulty.NORMAL; break;
                        case NORMAL: nextDiff = Difficulty.HARD; break;
                        case HARD: nextDiff = Difficulty.PEACEFUL; break;
                        default: 
                            nextDiff = Difficulty.NORMAL; 
                            ErrorHandler.warn("Settings GUI difficulty change for '" + worldDisplayName + "' by " + player.getName(), "Unexpected current difficulty: " + currentDiff + ", defaulting to NORMAL for next."); 
                            break;
                    }
                    setWorldDifficulty(world, nextDiff, player); // Has error handling
                } catch (Exception e) {
                    ErrorHandler.log("changing difficulty in '" + worldDisplayName + "'", e, player);
                }
            } else if (itemName.contains("Toggle Chat Mute")) {
                if (!player.hasPermission("eventmanager.toggle.chatmute")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to toggle chat mute.");
                    return;
                }
                setChatMutedGlobally(!EventManager.isChatMutedGlobally(), player); // Has error handling
            } else if (itemName.contains("Broadcast Message")) {
                // This is informational in the GUI
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "To broadcast, please use the command: " + ChatColor.GOLD + "/em broadcast <message>");
                ErrorHandler.info("Player " + player.getName() + " clicked Broadcast Message item in GUI for world '" + worldDisplayName + "'. Instructed to use command.");
            } else if (itemName.contains("Toggle Admin Auto Vanish")) {
                if (!player.hasPermission("eventmanager.toggle.adminautovanish")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to toggle admin auto vanish.");
                    return;
                }
                setAdminAutoVanishEnabled(!EventManager.isAdminAutoVanishEnabled(), player); // Has error handling
            } else if (itemName.contains("Toggle Auto Restart Timer")) {
                if (!player.hasPermission("eventmanager.toggle.autorestart")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to toggle the auto restart timer.");
                    return;
                }
                toggleAutoRestartTimer(player); // Has error handling
            } else if (itemName.contains("Nether: BLOCKED") || itemName.contains("Nether: ENABLED") || itemName.contains("Nether:")) {
                 if (!player.hasPermission("eventmanager.toggle.netheraccess")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to toggle Nether access.");
                    return;
                }
                setNetherBlocked(!EventManager.isNetherBlocked(), player); // Has error handling
            } else if (itemName.contains("The End: BLOCKED") || itemName.contains("The End: ENABLED") || itemName.contains("The End:")) {
                if (!player.hasPermission("eventmanager.toggle.endaccess")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to toggle End access.");
                    return;
                }
                setEndBlocked(!EventManager.isEndBlocked(), player); // Has error handling
            } else {
                if (!clickedItem.getType().name().endsWith("_STAINED_GLASS_PANE")) {
                    ErrorHandler.warn("Settings GUI click for '" + worldDisplayName + "' by " + player.getName(), "Unhandled item click: " + itemName + " (Material: " + clickedItem.getType() + ")");
                }
            }

            // Refresh GUI only if it's still the same one
            if (player.getOpenInventory().getTitle().equals(view.getTitle())) {
                 Bukkit.getScheduler().runTaskLater(this, () -> EventManagerGUI.openSettingsGUI(player, world), 1L); 
            }

        } catch (Exception e) {
            String clickedItemName = (clickedItem != null && clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName()) ? clickedItem.getItemMeta().getDisplayName() : (clickedItem != null ? clickedItem.getType().toString() : "null");
            ErrorHandler.log("handling Settings GUI click for world '" + (world != null ? world.getName() : "unknown") + "', item '" + clickedItemName + "'", e, player);
        }
    }

    // The original onInventoryClick method content from the file is now replaced by the refactored version above.
    // Keeping a comment here to denote the end of the new onInventoryClick and its helper methods.
    // End of onInventoryClick and its helper methods

    @org.bukkit.event.EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPortal(PlayerPortalEvent event) {
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