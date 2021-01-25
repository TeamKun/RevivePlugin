package net.kunmc.lab.reviveplugin.config;

import net.kunmc.lab.reviveplugin.RevivePlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ConfigCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        ConfigManager configManager = RevivePlugin.getInstance().getConfigManager();
        if (args.length == 1 && args[0].equals("reload")) {
            configManager.load();
            return true;
        }
        if (args.length == 3 && args[0].equals("set")) {
            String path = args[1];
            String value = args[2];
            return configManager.setConfig(path, value);
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> suggest = new ArrayList<>();
        if (args.length == 1) {
            suggest.add("reload");
            suggest.add("set");
        } else if (args.length == 2 && args[0].equals("set")) {
            suggest.addAll(ConfigManager.getConfigPaths());
        } else {
            return null;
        }
        return suggest;
    }
}
