package net.kunmc.lab.reviveplugin.config;

import net.kunmc.lab.reviveplugin.RevivePlugin;
import net.kunmc.lab.reviveplugin.config.parser.BooleanParser;
import net.kunmc.lab.reviveplugin.config.parser.DoubleParser;
import net.kunmc.lab.reviveplugin.config.parser.IntParser;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.function.Function;

public class ConfigManager {
    private FileConfiguration config;
    private static final Map<String, Function<String, Object>> CONFIGS = new HashMap<String, Function<String, Object>>() {{
        put("respawnTime", new IntParser(0, Integer.MAX_VALUE));
        put("reviveDistance", new DoubleParser(0, 10));
        put("reviveCount", new IntParser(1, Integer.MAX_VALUE));
        put("selfRespawn", new BooleanParser());
        put("askForHelp", new BooleanParser());
        put("bossBarDisplayTime", new IntParser(1, Integer.MAX_VALUE));
    }};

    public static String[] getConfigPaths() {
        return CONFIGS.keySet().toArray(new String[0]);
    }

    public void load() {
        RevivePlugin plugin = RevivePlugin.getInstance();
        plugin.saveDefaultConfig();
        if (config != null) {
            plugin.reloadConfig();
        }
        config = plugin.getConfig();
    }

    public boolean setConfig(String path, String valueString) {
        if (!CONFIGS.containsKey(path)) {
            return false;
        }
        Object value = CONFIGS.get(path).apply(valueString);
        if (value == null) {
            return false;
        }
        RevivePlugin plugin = RevivePlugin.getInstance();
        config.set(path, value);
        plugin.saveConfig();
        return true;
    }

    public int getRespawnTime() {
        return config.getInt("respawnTime");
    }

    public double getReviveDistance() {
        return config.getInt("reviveDistance");
    }

    public int getReviveCount() {
        return config.getInt("reviveCount");
    }

    public boolean canSelfRespawn() {
        return config.getBoolean("selfRespawn");
    }

    public boolean canAskForHelp() {
        return config.getBoolean("askForHelp");
    }

    public int getBossBarDisplayTime() {
        return config.getInt("bossBarDisplayTime");
    }
}
