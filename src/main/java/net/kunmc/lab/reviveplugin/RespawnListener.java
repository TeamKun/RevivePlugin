package net.kunmc.lab.reviveplugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import net.minecraft.server.v1_15_R1.*;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class RespawnListener extends PacketAdapter implements Listener {
    private final Set<Player> unrespawnablePlayers = new HashSet<>();

    public RespawnListener() {
        super(RevivePlugin.getInstance(), PacketType.Play.Client.CLIENT_COMMAND);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        EnumWrappers.ClientCommand clientCommand = event.getPacket().getClientCommands().read(0);
        ConfigManager configManager = RevivePlugin.getInstance().getConfigManager();
        if (configManager.canSelfRespawn() && !unrespawnablePlayers.contains(event.getPlayer())) {
            return;
        }
        if (clientCommand == EnumWrappers.ClientCommand.PERFORM_RESPAWN) {
            EntityPlayer player = ((CraftPlayer)event.getPlayer()).getHandle();
            PlayerConnection connection = player.playerConnection;
            event.setCancelled(true);
            connection.sendPacket(new PacketPlayOutCombatEvent(new CombatTracker(player), PacketPlayOutCombatEvent.EnumCombatEventType.ENTITY_DIED));
            DeadPlayer deadPlayer = DeadPlayer.getDeadPlayers().get(event.getPlayer());
            if (configManager.canAskForHelp()) {
                deadPlayer.askForHelp();
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        ConfigManager configManager = RevivePlugin.getInstance().getConfigManager();
        EntityPlayer entityPlayer = ((CraftPlayer)player).getHandle();
        AtomicInteger counter = new AtomicInteger(configManager.getRespawnDuration());
        setScore(entityPlayer, counter.getAndDecrement());
        unrespawnablePlayers.add(player);
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
