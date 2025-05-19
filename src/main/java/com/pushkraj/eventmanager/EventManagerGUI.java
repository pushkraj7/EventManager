package com.pushkraj.eventmanager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class EventManagerGUI {

    public static void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "Event Manager"); // 54 slots, 6 rows

        // Placeholder items for new features
        ItemStack pvpToggle = createGuiItem(Material.IRON_SWORD, "Toggle PvP", "Click to toggle PvP status.");
        ItemStack mobSpawningToggle = createGuiItem(Material.ZOMBIE_HEAD, "Toggle Mob Spawning", "Click to toggle hostile mob spawning.");
        ItemStack friendlyMobSpawningToggle = createGuiItem(Material.PLAYER_HEAD, "Toggle Friendly Mob Spawning", "Click to toggle friendly mob spawning.");
        ItemStack keepInventoryToggle = createGuiItem(Material.TOTEM_OF_UNDYING, "Toggle Keep Inventory", "Click to toggle keep inventory.");
        ItemStack difficultySelector = createGuiItem(Material.DIAMOND, "Change Difficulty", "Click to change game difficulty.");
        ItemStack endToggle = createGuiItem(Material.END_STONE, "Toggle End Dimension", "Click to enable/disable The End.");
        ItemStack netherToggle = createGuiItem(Material.NETHERRACK, "Toggle Nether Dimension", "Click to enable/disable The Nether.");

        // Add items to the GUI - positions can be adjusted
        gui.setItem(10, pvpToggle);
        gui.setItem(12, mobSpawningToggle);
        gui.setItem(14, friendlyMobSpawningToggle);
        gui.setItem(16, keepInventoryToggle);
        gui.setItem(28, difficultySelector);
        gui.setItem(30, endToggle);
        gui.setItem(32, netherToggle);

        player.openInventory(gui);
    }

    private static ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§r" + name);
            meta.setLore(java.util.Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}