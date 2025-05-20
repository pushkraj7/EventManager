package com.pushkraj.eventmanager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import org.bukkit.World;
import org.bukkit.Bukkit;
import java.util.List;

public class EventManagerGUI {

    public static final String WORLD_SELECTION_GUI_TITLE = "§8§lSelect World to Manage";
    public static final String SETTINGS_GUI_TITLE_PREFIX = "§6§lEvent Manager §8» §7";

    public static void openWorldSelectionGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, WORLD_SELECTION_GUI_TITLE); // 27 slots, 3 rows for better spacing

        List<World> worlds = Bukkit.getWorlds();
        // Typically, Overworld, Nether, End are the first three, but this can vary.
        // We'll add them explicitly if they exist.

        World overworld = Bukkit.getWorlds().stream().filter(w -> w.getEnvironment() == World.Environment.NORMAL).findFirst().orElse(null);
        World nether = Bukkit.getWorlds().stream().filter(w -> w.getEnvironment() == World.Environment.NETHER).findFirst().orElse(null);
        World end = Bukkit.getWorlds().stream().filter(w -> w.getEnvironment() == World.Environment.THE_END).findFirst().orElse(null);

        int slot = 10; // Start from the second row, more centered
        if (overworld != null) {
            gui.setItem(slot++, createGuiItem(Material.GRASS_BLOCK, "§a" + overworld.getName(), "§7Click to manage game rules and settings", "§7for the §aOverworld§7."));
        }
        if (nether != null) {
            // Add a spacer if previous item was added
            if(gui.getItem(slot-1) != null) slot++; 
            gui.setItem(slot++, createGuiItem(Material.NETHERRACK, "§c" + nether.getName(), "§7Click to manage game rules and settings", "§7for §cThe Nether§7."));
        }
        if (end != null) {
            // Add a spacer if previous item was added
            if(gui.getItem(slot-1) != null && gui.getItem(slot-1).getType() != Material.AIR) slot++; 
            gui.setItem(slot++, createGuiItem(Material.END_STONE, "§5" + end.getName(), "§7Click to manage game rules and settings", "§7for §5The End§7."));
        }

        // Fill empty slots with glass panes for aesthetics
        ItemStack filler = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }
        // Add more worlds if needed, or make it dynamic for all worlds

        player.openInventory(gui);
    }

    public static void openSettingsGUI(Player player, World world) {
        String worldDisplayName = world.getEnvironment() == World.Environment.NORMAL ? "§a" + world.getName()
                            : world.getEnvironment() == World.Environment.NETHER ? "§c" + world.getName()
                            : world.getEnvironment() == World.Environment.THE_END ? "§5" + world.getName()
                            : "§7" + world.getName();
        Inventory gui = Bukkit.createInventory(null, 54, SETTINGS_GUI_TITLE_PREFIX + worldDisplayName + "§r"); // 54 slots, 6 rows

        // Items need to reflect the state of the *selected world*
        boolean pvpState = world.getPVP();
        ItemStack pvpToggle = createGuiItem(Material.IRON_SWORD, 
            "§eToggle PvP", 
            "§7Current: " + (pvpState ? "§aEnabled" : "§cDisabled"),
            "",
            "§7Click to " + (pvpState ? "§cdisable" : "§aenable") + " PvP",
            "§7in " + worldDisplayName + "§7."
        );

        Boolean mobSpawningState = world.getGameRuleValue(org.bukkit.GameRule.DO_MOB_SPAWNING);
        if (mobSpawningState == null) mobSpawningState = true; // Default to true
        ItemStack mobSpawningToggle = createGuiItem(Material.ZOMBIE_HEAD, 
            "§eToggle Hostile Mob Spawning", 
            "§7Current: " + (mobSpawningState ? "§aEnabled" : "§cDisabled"),
            "",
            "§7Click to " + (mobSpawningState ? "§cdisable" : "§aenable") + " hostile mob spawning",
            "§7in " + worldDisplayName + "§7."
        );

        // Friendly Mob Spawning - Conceptual
        ItemStack friendlyMobSpawningToggle = createGuiItem(Material.PLAYER_HEAD, 
            "§eToggle Friendly Mob Spawning", 
            "§c§oConceptual - Not fully implemented",
            "§7Bukkit API lacks a direct rule for this.",
            "§7Requires custom spawn event handling."
        );

        Boolean keepInventoryState = world.getGameRuleValue(org.bukkit.GameRule.KEEP_INVENTORY);
        if (keepInventoryState == null) keepInventoryState = false; // Default to false
        ItemStack keepInventoryToggle = createGuiItem(Material.TOTEM_OF_UNDYING, 
            "§eToggle Keep Inventory", 
            "§7Current: " + (keepInventoryState ? "§aEnabled" : "§cDisabled"),
            "",
            "§7Click to " + (keepInventoryState ? "§cdisable" : "§aenable") + " keep inventory",
            "§7on death in " + worldDisplayName + "§7."
        );

        org.bukkit.Difficulty currentDifficulty = world.getDifficulty();
        ItemStack difficultySelector = createGuiItem(Material.DIAMOND_SWORD, // Changed from DIAMOND for better visual
            "§eChange Difficulty", 
            "§7Current: §b" + currentDifficulty.toString(),
            "",
            "§7Click to cycle through difficulties:",
            "§7Peaceful -> Easy -> Normal -> Hard",
            "§7for " + worldDisplayName + "§7."
        );

        // Conceptual toggles for dimensions remain
        ItemStack endToggle = createGuiItem(Material.END_STONE_BRICKS, // Changed for visual variety
            "§eToggle End Dimension Access", 
            "§c§oConceptual - Server Setting",
            "§7Typically requires 'allow-end' in server.properties",
            "§7and a server restart."
        );

        ItemStack netherToggle = createGuiItem(Material.NETHER_BRICKS, // Changed for visual variety
            "§eToggle Nether Dimension Access", 
            "§c§oConceptual - Server Setting",
            "§7Typically requires 'allow-nether' in server.properties",
            "§7and a server restart."
        );

        // New items for added features
        ItemStack chatMuteToggle = createGuiItem(Material.WRITABLE_BOOK, 
            "§eToggle Chat Mute", 
            "§7Current: " + (com.pushkraj.eventmanager.EventManager.isChatMutedGlobally() ? "§cMuted" : "§aUnmuted"), // Requires access to a static boolean in EventManager
            "",
            "§7Click to " + (com.pushkraj.eventmanager.EventManager.isChatMutedGlobally() ? "§aunmute" : "§cmute") + " global chat."
        );

        ItemStack broadcastMessageItem = createGuiItem(Material.PAPER, 
            "§bBroadcast Message", 
            "§7Click to send a server-wide message.",
            "§c§o(Placeholder - Requires input)"
        );

        ItemStack adminAutoVanishToggle = createGuiItem(Material.FEATHER, // Or Material.ENDER_PEARL
            "§dToggle Admin Auto Vanish",
            "§7Current: " + (com.pushkraj.eventmanager.EventManager.isAdminAutoVanishEnabled() ? "§aEnabled" : "§cDisabled"), // Requires access to a static boolean in EventManager
            "",
            "§7Click to " + (com.pushkraj.eventmanager.EventManager.isAdminAutoVanishEnabled() ? "§cdisable" : "§aenable") + " auto vanish for admins on join."
        );

        // New item for Auto Restart Timer
        ItemStack autoRestartToggle = createGuiItem(Material.CLOCK,
            "§6Toggle Auto Restart Timer",
            "§7Current: " + (com.pushkraj.eventmanager.EventManager.isAutoRestartTimerEnabled() ? "§aEnabled" : "§cDisabled"), // Requires access to EventManager
            "§7Interval: " + com.pushkraj.eventmanager.EventManager.getAutoRestartIntervalMinutes() + " minutes (approx)", // Requires access to EventManager
            "",
            "§7Click to " + (com.pushkraj.eventmanager.EventManager.isAutoRestartTimerEnabled() ? "§cdisable" : "§aenable") + " the auto restart timer.",
            "§c§oNote: Restart command runs as console."
        );
        
        ItemStack backButton = createGuiItem(Material.ARROW, "§c« Back to World Selection", "§7Return to the world list.");

        // Layout items
        gui.setItem(10, pvpToggle);
        gui.setItem(12, mobSpawningToggle);
        gui.setItem(14, friendlyMobSpawningToggle);
        gui.setItem(16, keepInventoryToggle);

        gui.setItem(22, chatMuteToggle); // New item slot
        gui.setItem(24, broadcastMessageItem); // New item slot
        gui.setItem(26, adminAutoVanishToggle); // New item slot for admin auto vanish
        gui.setItem(28, autoRestartToggle); // New item slot for auto restart timer

        gui.setItem(20, difficultySelector);
        // Separator line idea
        ItemStack separator = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for(int i = 27; i < 36; i++) { // Row for conceptual/server settings
            if (i == 29) gui.setItem(i, endToggle); // Adjusted slot
            else if (i == 31) gui.setItem(i, netherToggle); // Adjusted slot
            // else gui.setItem(i, separator); // Optional: fill with separators
        }
        
        gui.setItem(49, backButton); // Center bottom row for back button

        // Fill empty slots with glass panes for aesthetics
        ItemStack filler = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }

        player.openInventory(gui);
    }

    // Original open method is now replaced by openSettingsGUI and openWorldSelectionGUI
    // public static void open(Player player) { ... }

    private static ItemStack createGuiItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name); // Name now includes color codes directly
            java.util.List<String> lore = new java.util.ArrayList<>();
            for (String line : loreLines) {
                lore.add("§7" + line); // Default lore color, can be overridden by codes in line
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}