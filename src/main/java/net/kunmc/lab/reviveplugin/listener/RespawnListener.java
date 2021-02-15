package net.kunmc.lab.reviveplugin.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import net.kunmc.lab.reviveplugin.DeadPlayer;
import net.kunmc.lab.reviveplugin.RevivePlugin;
import net.kunmc.lab.reviveplugin.config.ConfigManager;
import net.minecraft.server.v1_15_R1.*;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class RespawnListener extends PacketAdapter implements Listener {
    private final Map<Player, Long> unrespawnablePlayers = new HashMap<>();

    public RespawnListener() {
        super(RevivePlugin.getInstance(), PacketType.Play.Client.CLIENT_COMMAND);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player eventPlayer = event.getPlayer();
        EnumWrappers.ClientCommand clientCommand = event.getPacket().getClientCommands().read(0);
        ConfigManager configManager = RevivePlugin.getInstance().getConfigManager();
        if (configManager.canSelfRespawn() && !unrespawnablePlayers.containsKey(eventPlayer)) {
            return;
        }
        long epoch = unrespawnablePlayers.get(eventPlayer);
        long remainingTime = Math.max(0, epoch - Instant.now().getEpochSecond());
        if (clientCommand == EnumWrappers.ClientCommand.PERFORM_RESPAWN) {
            EntityPlayer player = ((CraftPlayer)eventPlayer).getHandle();
            PlayerConnection connection = player.playerConnection;
            event.setCancelled(true);
            connection.sendPacket(new PacketPlayOutCombatEvent(new CombatTracker(player), PacketPlayOutCombatEvent.EnumCombatEventType.ENTITY_DIED));
            DeadPlayer deadPlayer = DeadPlayer.getDeadPlayers().get(eventPlayer);
            eventPlayer.sendMessage("リスポーン可能まであと" + remainingTime + "秒");
            if (configManager.canAskForHelp()) {
                deadPlayer.askForHelp();
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        startRespawnTimer(player);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.isDead()) {
            startRespawnTimer(player);
        }
    }

    private void startRespawnTimer(Player player) {
        ConfigManager configManager = RevivePlugin.getInstance().getConfigManager();
        EntityPlayer entityPlayer = ((CraftPlayer)player).getHandle();
        int respawnTime = configManager.getRespawnTime();
        AtomicInteger counter = new AtomicInteger(respawnTime);
        setScore(entityPlayer, counter.getAndDecrement());
        unrespawnablePlayers.put(player, Instant.now().getEpochSecond() + respawnTime);
        new BukkitRunnable() {
            @Override
            public void run() {
                int newScore = counter.getAndDecrement();
                setScore(entityPlayer, newScore);
                if (newScore == 0) {
                    unrespawnablePlayers.remove(player);
                    cancel();
                }
            }
        }.runTaskTimer(RevivePlugin.getInstance(), 20, 20);
    }

    private void setScore(EntityPlayer entityPlayer, int score) {
        DataWatcher watcher = entityPlayer.getDataWatcher();
        entityPlayer.setScore(score);
        entityPlayer.playerConnection.sendPacket(new PacketPlayOutEntityMetadata(entityPlayer.getId(), watcher, false));
    }
}
