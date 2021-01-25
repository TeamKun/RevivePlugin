package net.kunmc.lab.reviveplugin;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class EventListener implements Listener {
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        DeadPlayer deadPlayer = new DeadPlayer(player);
        deadPlayer.spawn();
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        DeadPlayer deadPlayer = DeadPlayer.getDeadPlayers().get(event.getPlayer());
        deadPlayer.remove();
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) {
            return;
        }
        Player eventPlayer = event.getPlayer();
        Location location = eventPlayer.getLocation();
        ConfigManager configManager = RevivePlugin.getInstance().getConfigManager();
        double reviveDistance = configManager.getReviveDistance();
        double minDistance = -1;
        DeadPlayer reviveTarget = null;
        for (DeadPlayer deadPlayer : DeadPlayer.getDeadPlayers().values()) {
            double distance = location.distance(deadPlayer.getLocation());
            if (reviveTarget == null || distance < minDistance) {
                reviveTarget = deadPlayer;
                minDistance = distance;
            }
        }
        if (reviveTarget == null || minDistance > reviveDistance) {
            return;
        }
        reviveTarget.tryRevive();
    }
}
