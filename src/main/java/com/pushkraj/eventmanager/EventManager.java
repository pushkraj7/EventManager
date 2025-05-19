package com.pushkraj.eventmanager;

import org.bukkit.plugin.java.JavaPlugin;

public class EventManager extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info("MyPlugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("MyPlugin has been disabled!");
    }
}