package net.kunmc.lab.reviveplugin;

import com.comphenix.protocol.ProtocolLibrary;
import net.kunmc.lab.reviveplugin.config.ConfigCommand;
import net.kunmc.lab.reviveplugin.config.ConfigManager;
import net.kunmc.lab.reviveplugin.listener.EventListener;
import net.kunmc.lab.reviveplugin.listener.RespawnListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class RevivePlugin extends JavaPlugin {
    private static RevivePlugin instance;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        instance = this;
        configManager = new ConfigManager();
        configManager.load();
        RespawnListener respawnListener = new RespawnListener();
        ProtocolLibrary.getProtocolManager().addPacketListener(respawnListener);
        getServer().getPluginManager().registerEvents(respawnListener, this);
        getServer().getPluginManager().registerEvents(new EventListener(), this);
        ConfigCommand configCommand = new ConfigCommand();
        Objects.requireNonNull(getCommand("reviveconfig")).setExecutor(configCommand);
        Objects.requireNonNull(getCommand("reviveconfig")).setTabCompleter(configCommand);
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
