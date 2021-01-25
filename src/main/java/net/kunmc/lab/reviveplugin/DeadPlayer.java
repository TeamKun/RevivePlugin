package net.kunmc.lab.reviveplugin;

import com.mojang.authlib.GameProfile;
import net.kunmc.lab.reviveplugin.config.ConfigManager;
import net.minecraft.server.v1_15_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_15_R1.CraftServer;
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class DeadPlayer {
    private static final Map<Player, DeadPlayer> deadPlayers = new HashMap<>();
    private static BukkitTask task;
    private final Player source;
    private final EntityPlayer deadPlayer;
    private Set<Player> trackers;
    private final BlockData blockDataCache;
    private final Location bedLocation;
    private final BlockPosition blockPos;
    private final PacketPlayOutBlockChange fakeBedPacket;
    private PacketPlayOutEntityMetadata metadataPacket;
    private int remainReviveCount;

    public DeadPlayer(Player source) {
        ConfigManager configManager = RevivePlugin.getInstance().getConfigManager();
        this.remainReviveCount = configManager.getReviveCount();
        this.source = source;
        this.trackers = new HashSet<>();
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (source.getWorld().equals(onlinePlayer.getWorld())) {
                trackers.add(onlinePlayer);
            }
        }
        MinecraftServer server = ((CraftServer)Bukkit.getServer()).getServer();
        WorldServer world = ((CraftWorld)source.getWorld()).getHandle();
        Location location = source.getLocation();
        EnumDirection direction = EnumDirection.fromAngle(source.getLocation().getYaw());
        this.deadPlayer = new EntityPlayer(server, world, new GameProfile(source.getUniqueId(), source.getName()), new PlayerInteractManager(world));
        this.deadPlayer.setLocation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        this.bedLocation = new Location(location.getWorld(), location.getX(), 1, location.getZ());
        this.blockDataCache = bedLocation.getBlock().getBlockData();
        this.blockPos = new BlockPosition(location.getX(), 1, location.getZ());
        IBlockAccess fakeBed = new IBlockAccess() {
            @Override
            public TileEntity getTileEntity(BlockPosition blockPosition) {
                return null;
            }

            @Override
            public IBlockData getType(BlockPosition blockPosition) {
                return Blocks.WHITE_BED.getBlockData().set(BlockBed.PART, BlockPropertyBedPart.HEAD).set(BlockBed.FACING, direction);
            }

            @Override
            public Fluid getFluid(BlockPosition blockPosition) {
                return null;
            }
        };
        this.fakeBedPacket = new PacketPlayOutBlockChange(fakeBed, blockPos);
        setMetadata();
    }

    private void setMetadata() {
        byte overlays = ((CraftPlayer)source).getHandle().getDataWatcher().get(DataWatcherRegistry.a.a(16));
        DataWatcher watcher = deadPlayer.getDataWatcher();
        watcher.set(DataWatcherRegistry.s.a(6), EntityPose.SLEEPING);
        watcher.set(DataWatcherRegistry.a.a(16), overlays);
        watcher.set(DataWatcherRegistry.m.a(13), Optional.of(blockPos));
        metadataPacket = new PacketPlayOutEntityMetadata(deadPlayer.getId(), watcher, true);
    }

    public void spawn() {
        for (Player player : trackers) {
            spawnToPlayer(player);
        }
        if (deadPlayers.isEmpty()) {
            task = Bukkit.getScheduler().runTaskTimer(RevivePlugin.getInstance(), () -> {
                for (DeadPlayer deadPlayer : deadPlayers.values()) {
                    deadPlayer.tick();
                }
            }, 0, 1);
        }
        deadPlayers.put(source, this);
    }

    public void remove() {
        for (Player player : trackers) {
            removeToPlayer(player);
        }
        deadPlayers.remove(source);
        if (deadPlayers.isEmpty()) {
            Bukkit.getScheduler().cancelTask(task.getTaskId());
        }
    }

    public Location getLocation() {
        Vec3D vector = deadPlayer.getPositionVector();
        return new Location(deadPlayer.world.getWorld(), vector.getX(), vector.getY(), vector.getZ());
    }

    public void tryRevive() {
        remainReviveCount--;
        if (remainReviveCount == 0) {
            source.spigot().respawn();
        }
    }

    public void askForHelp() {

    }

    private void spawnToPlayer(Player player) {
        PlayerConnection connection = ((CraftPlayer)player).getHandle().playerConnection;
        connection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, deadPlayer));
        connection.sendPacket(new PacketPlayOutNamedEntitySpawn(deadPlayer));
        connection.sendPacket(fakeBedPacket);
        connection.sendPacket(metadataPacket);
        connection.sendPacket(new PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook(deadPlayer.getId(), (short)0, (short)2, (short)0, (byte)0, (byte)0, true));
    }

    private void removeToPlayer(Player player) {
        PlayerConnection connection = ((CraftPlayer)player).getHandle().playerConnection;
        connection.sendPacket(new PacketPlayOutEntityDestroy(deadPlayer.getId()));
        player.sendBlockChange(bedLocation, blockDataCache);
    }

    private void tick() {
        Set<Player> newTrackers = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (source.getWorld().equals(player.getWorld())) {
                if (!trackers.contains(player)) {
                    spawnToPlayer(player);
                }
                newTrackers.add(player);
            }
        }
        for (Player player : trackers) {
            if (!newTrackers.contains(player)) {
                removeToPlayer(player);
            }
        }
        trackers = newTrackers;
        for (Player player : trackers) {
            PlayerConnection connection = ((CraftPlayer)player).getHandle().playerConnection;
            connection.sendPacket(fakeBedPacket);
        }
    }

    public static Map<Player, DeadPlayer> getDeadPlayers() {
        return deadPlayers;
    }
}
