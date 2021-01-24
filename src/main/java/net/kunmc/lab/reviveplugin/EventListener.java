package net.kunmc.lab.reviveplugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.Map;

public class EventListener implements Listener {
    private final Map<Player, DeadPlayer> fakePlayerMap = new HashMap<>();

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        DeadPlayer deadPlayer = new DeadPlayer(player);
        deadPlayer.spawn();
        fakePlayerMap.put(player, deadPlayer);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        DeadPlayer deadPlayer = fakePlayerMap.get(event.getPlayer());
        deadPlayer.remove();
    }
}
