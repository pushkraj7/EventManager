package org.pushkraj.eventmanager.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.pushkraj.eventmanager.EventManager;

import java.util.logging.Level;

public class ErrorHandler {

    private static JavaPlugin plugin;

    public static void init(JavaPlugin pluginInstance) {
        plugin = pluginInstance;
    }

    /**
     * Logs an error to the console and optionally informs the player.
     *
     * @param context A description of what action was being performed when the error occurred.
     * @param e       The exception that was caught.
     * @param player  The player to send a feedback message to (can be null).
     */
    public static void log(String context, Exception e, CommandSender player) {
        String errorMessage = "[EventManager ERROR] Action: " + context;
        plugin.getLogger().severe(errorMessage);

        boolean debugMode = plugin.getConfig().getBoolean("debug-mode", false);

        if (debugMode) {
            plugin.getLogger().log(Level.SEVERE, "Reason: " + e.getMessage(), e);
        } else {
            plugin.getLogger().severe("Reason: " + e.getMessage());
        }

        if (player != null) {
            player.sendMessage(ChatColor.RED + "⚠️ Failed to apply change. Check console for details.");
        }
    }

    /**
     * Logs an error to the console.
     *
     * @param context A description of what action was being performed when the error occurred.
     * @param e       The exception that was caught.
     */
    public static void log(String context, Exception e) {
        log(context, e, null);
    }

    /**
     * Logs a warning message.
     *
     * @param context The context of the warning.
     * @param message The warning message.
     */
    public static void warn(String context, String message) {
        plugin.getLogger().warning("[EventManager WARNING] Context: " + context + " - Message: " + message);
    }

     /**
     * Logs an informational message, typically for successful operations or status updates.
     *
     * @param message The informational message.
     */
    public static void info(String message) {
        plugin.getLogger().info(message);
    }
}