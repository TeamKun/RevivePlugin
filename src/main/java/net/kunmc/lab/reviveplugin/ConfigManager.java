package net.kunmc.lab.reviveplugin;

public class ConfigManager {
    public ConfigManager() {
    }

    public int getRespawnDuration() {
        return 5;
    }

    public double getReviveDistance() {
        return 1;
    }

    public int getReviveCount() {
        return 5;
    }

    public boolean canSelfRespawn() {
        return true;
    }

    public boolean canAskForHelp() {
        return true;
    }
}
