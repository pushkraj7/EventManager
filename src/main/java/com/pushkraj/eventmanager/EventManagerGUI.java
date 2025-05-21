package com.pushkraj.eventmanager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.World;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import java.util.List;

import com.onarandombox.multiversecore.MultiverseCore;
import com.onarandombox.multiversecore.api.MVWorld;
import com.onarandombox.multiversecore.api.MVWorldManager;

public class EventManagerGUI {

    public static final String WORLD_SELECTION_GUI_TITLE = "§8§lSelect World to Manage";
    public static final String SETTINGS_GUI_TITLE_PREFIX = "§6§lEvent Manager §8» §7";
    public static final String MULTIVERSE_WORLDS_GUI_TITLE = "§8§lMultiverse Worlds";

    public static void openMultiverseWorldsGUI(Player player) {
        if (!player.hasPermission("eventmanager.gui.multiverse")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to access Multiverse world settings.");
            return;
        }

        com.onarandombox.multiversecore.MultiverseCore mvCore = com.pushkraj.eventmanager.EventManager.getMultiverseCore();
        if (mvCore == null) {
            player.sendMessage(ChatColor.RED + "Multiverse-Core is not installed or enabled.");
            return;
        }

        java.util.List<com.onarandombox.multiversecore.api.MVWorld> mvWorlds = mvCore.getMVWorldManager().getMVWorlds();
        int size = Math.min(54, ((mvWorlds.size() + 8) / 9) * 9); // Round up to nearest multiple of 9, max 54

        Inventory gui = Bukkit.createInventory(null, size, MULTIVERSE_WORLDS_GUI_TITLE);

        for (int i = 0; i < mvWorlds.size(); i++) {
            com.onarandombox.multiversecore.api.MVWorld mvWorld = mvWorlds.get(i);
            World world = mvWorld.getCBWorld();
            if (world != null) {
                Material icon = world.getEnvironment() == World.Environment.NORMAL ? Material.GRASS_BLOCK
                             : world.getEnvironment() == World.Environment.NETHER ? Material.NETHERRACK
                             : world.getEnvironment() == World.Environment.THE_END ? Material.END_STONE
                             : Material.STONE;

                ItemStack worldItem = createGuiItem(icon,
                    "§b" + world.getName(),
                    "§7Environment: §e" + world.getEnvironment().name(),
                    "§7PvP: " + (world.getPVP() ? "§aEnabled" : "§cDisabled"),
                    "§7Difficulty: §e" + world.getDifficulty().name(),
                    "",
                    "§7Click to manage world settings");

                gui.setItem(i, worldItem);
            }
        }

        // Add back button
        ItemStack backButton = createGuiItem(Material.ARROW, "§c« Back", "§7Return to main menu");
        gui.setItem(size - 1, backButton);

        // Fill empty slots with glass panes
        ItemStack filler = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }

        player.openInventory(gui);
    }

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

        // Add Multiverse Worlds button if Multiverse-Core is enabled
        if (com.pushkraj.eventmanager.EventManager.isMultiverseEnabled()) {
            gui.setItem(26, createGuiItem(Material.ENDER_PEARL, "§b🌍 Multiverse Worlds", 
                "§7Click to view and manage", 
                "§7all Multiverse worlds."));
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

        // Functional toggles for dimension access
        boolean isEndBlocked = com.pushkraj.eventmanager.EventManager.isEndBlocked();
        ItemStack endToggle = createGuiItem(Material.END_STONE, // Material changed to match EventManager logic
            isEndBlocked ? "§c§lThe End: §4BLOCKED" : "§5§lThe End: §aENABLED",
            "§7Player access to The End dimension.",
            "§7Current: " + (isEndBlocked ? "§cBlocked" : "§aEnabled"),
            "",
            "§7Click to " + (isEndBlocked ? "§aenable" : "§cblock") + " player access."
        );

        boolean isNetherBlocked = com.pushkraj.eventmanager.EventManager.isNetherBlocked();
        ItemStack netherToggle = createGuiItem(Material.NETHERRACK, // Material changed to match EventManager logic
            isNetherBlocked ? "§c§lNether: §4BLOCKED" : "§c§lNether: §aENABLED", // Using §c for Nether as per world selection
            "§7Player access to The Nether dimension.",
            "§7Current: " + (isNetherBlocked ? "§cBlocked" : "§aEnabled"),
            "",
            "§7Click to " + (isNetherBlocked ? "§aenable" : "§cblock") + " player access."
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
        // Place Nether and End toggles
        // Using slots 30 (Nether) and 32 (End) from previous attempt, ensure they are distinct.
        gui.setItem(30, netherToggle); 
        gui.setItem(32, endToggle);   

        // Fill the rest of the row with separators if desired, or leave empty
        // for(int i = 27; i < 36; i++) {
        //     if (gui.getItem(i) == null && i != 30 && i != 32) { // Avoid overwriting our toggles
        //         gui.setItem(i, separator);
        //     }
        // }
        
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