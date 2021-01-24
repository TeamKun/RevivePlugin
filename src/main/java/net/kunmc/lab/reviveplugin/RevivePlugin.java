package net.kunmc.lab.reviveplugin;

import com.comphenix.protocol.ProtocolLibrary;
import org.bukkit.plugin.java.JavaPlugin;

public final class RevivePlugin extends JavaPlugin {
    private static RevivePlugin instance;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        instance = this;
        configManager = new ConfigManager();
        RespawnListener respawnListener = new RespawnListener();
        ProtocolLibrary.getProtocolManager().addPacketListener(respawnListener);
        getServer().getPluginManager().registerEvents(respawnListener, this);
        getServer().getPluginManager().registerEvents(new EventListener(), this);
    }

    public static RevivePlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    @Override
    public void onDisable() {
    }
}
