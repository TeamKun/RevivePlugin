package net.kunmc.lab.reviveplugin.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import net.kunmc.lab.reviveplugin.DeadPlayer;
import net.kunmc.lab.reviveplugin.RevivePlugin;
import net.kunmc.lab.reviveplugin.config.ConfigManager;
import net.minecraft.server.v1_15_R1.CombatTracker;
import net.minecraft.server.v1_15_R1.EntityPlayer;
import net.minecraft.server.v1_15_R1.PacketPlayOutCombatEvent;
import net.minecraft.server.v1_15_R1.PlayerConnection;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class RespawnListener extends PacketAdapter implements Listener {
    private final Map<Player, Long> unrespawnablePlayers = new HashMap<>();

    public RespawnListener() {
        super(RevivePlugin.getInstance(), PacketType.Play.Client.CLIENT_COMMAND);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        EnumWrappers.ClientCommand clientCommand = event.getPacket().getClientCommands().read(0);
        if (clientCommand != EnumWrappers.ClientCommand.PERFORM_RESPAWN) {
            return;
        }
        Player eventPlayer = event.getPlayer();
        if (!unrespawnablePlayers.containsKey(eventPlayer)) {
            return;
        }
        ConfigManager configManager = RevivePlugin.getInstance().getConfigManager();
        boolean canSelfRespawn = configManager.canSelfRespawn();
        long epoch = unrespawnablePlayers.get(eventPlayer);
        long remainingTime = epoch - Instant.now().getEpochSecond();
        if (canSelfRespawn && remainingTime < 0) {
            unrespawnablePlayers.remove(eventPlayer);
            return;
        }
        EntityPlayer player = ((CraftPlayer)eventPlayer).getHandle();
        DeadPlayer deadPlayer = DeadPlayer.getDeadPlayers().get(eventPlayer);
        event.setCancelled(true);
        PlayerConnection connection = player.playerConnection;
        connection.sendPacket(new PacketPlayOutCombatEvent(new CombatTracker(player), PacketPlayOutCombatEvent.EnumCombatEventType.ENTITY_DIED));
        if (canSelfRespawn) {
            eventPlayer.sendMessage("リスポーン可能まであと" + remainingTime + "秒");
        } else {
            eventPlayer.sendMessage("リスポーンはできません");
        }
        if (configManager.canAskForHelp()) {
            deadPlayer.askForHelp();
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
        long respawnTime = configManager.getRespawnTime();
        unrespawnablePlayers.put(player, Instant.now().getEpochSecond() + respawnTime);
    }
}
