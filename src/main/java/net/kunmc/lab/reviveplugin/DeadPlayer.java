package net.kunmc.lab.reviveplugin;

import com.mojang.authlib.GameProfile;
import net.kunmc.lab.reviveplugin.config.ConfigManager;
import net.minecraft.server.v1_15_R1.*;
import org.bukkit.World;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.craftbukkit.v1_15_R1.CraftServer;
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftFirework;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.*;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;

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
    private int currentReviveCount;
    private final int requireReviveCount;
    private final BossBar bossBar;
    private final ArmorStand armorStand;
    private final Map<Player, BukkitTask> bossBarHideTaskMap = new HashMap<>();

    public DeadPlayer(Player source) {
        World world = source.getWorld();
        Location location = source.getLocation();
        ConfigManager configManager = RevivePlugin.getInstance().getConfigManager();
        this.requireReviveCount = configManager.getReviveCount();
        this.currentReviveCount = 0;
        this.source = source;
        String title = source.getName() + "の蘇生";
        this.bossBar = Bukkit.createBossBar(title, BarColor.GREEN, BarStyle.SOLID);
        bossBar.setProgress(0);
        Location armorStandLocation = location.clone().add(0, 0.5, 0);
        this.armorStand = (ArmorStand)world.spawnEntity(armorStandLocation, EntityType.ARMOR_STAND);
        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setMarker(true);
        armorStand.setCustomName(configManager.getHelpMessage());
        this.trackers = new HashSet<>();
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (world.equals(onlinePlayer.getWorld())) {
                trackers.add(onlinePlayer);
            }
        }
        MinecraftServer server = ((CraftServer)Bukkit.getServer()).getServer();
        WorldServer worldServer = ((CraftWorld)world).getHandle();
        EnumDirection direction = EnumDirection.fromAngle(source.getLocation().getYaw());
        this.deadPlayer = new EntityPlayer(server, worldServer, new GameProfile(source.getUniqueId(), source.getName()), new PlayerInteractManager(worldServer));
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
        for (Map.Entry<Player, BukkitTask> entry : bossBarHideTaskMap.entrySet()) {
            bossBar.removePlayer(entry.getKey());
            entry.getValue().cancel();
        }
        bossBarHideTaskMap.clear();
    }

    public Location getLocation() {
        Vec3D vector = deadPlayer.getPositionVector();
        return new Location(deadPlayer.world.getWorld(), vector.getX(), vector.getY(), vector.getZ());
    }

    public void playReviveEffect(Location location) {
        World world = location.getWorld();
        BukkitScheduler scheduler = Bukkit.getScheduler();
        scheduler.runTaskLater(RevivePlugin.getInstance(), () -> {
            Firework firework = (Firework)world.spawnEntity(location, EntityType.FIREWORK);
            FireworkMeta meta = firework.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BURST).withColor(Color.WHITE).build());
            firework.setFireworkMeta(meta);
            ((CraftFirework)firework).getHandle().expectedLifespan = 1;
        }, 0);
        scheduler.runTaskLater(RevivePlugin.getInstance(), () -> world.playSound(location, Sound.BLOCK_NOTE_BLOCK_CHIME, 10, (float)Math.pow(2, -1d / 6)), 10);
        scheduler.runTaskLater(RevivePlugin.getInstance(), () -> world.playSound(location, Sound.BLOCK_NOTE_BLOCK_CHIME, 10, (float)Math.pow(2, 1d / 12)), 20);
        scheduler.runTaskLater(RevivePlugin.getInstance(), () -> world.playSound(location, Sound.BLOCK_NOTE_BLOCK_CHIME, 10, (float)Math.pow(2, 1d / 2)), 30);
        scheduler.runTaskLater(RevivePlugin.getInstance(), () -> world.playSound(location, Sound.BLOCK_NOTE_BLOCK_CHIME, 10, (float)Math.pow(2, 5d / 6)), 40);
        scheduler.runTaskLater(RevivePlugin.getInstance(), () -> {
            world.playSound(location, Sound.BLOCK_NOTE_BLOCK_CHIME, 10, (float)Math.pow(2, 11d / 12));
            world.playSound(location, Sound.BLOCK_NOTE_BLOCK_CHIME, 10, (float)Math.pow(2, 5d / 4));
            world.playSound(location, Sound.BLOCK_NOTE_BLOCK_CHIME, 10, (float)Math.pow(2, 3d / 2));
        }, 50);
        scheduler.runTaskLater(RevivePlugin.getInstance(), () -> world.playSound(location, Sound.BLOCK_NOTE_BLOCK_CHIME, 10, (float)Math.pow(2, 2d / 3)), 70);
        scheduler.runTaskLater(RevivePlugin.getInstance(), () -> {
            world.playSound(location, Sound.BLOCK_NOTE_BLOCK_CHIME, 10, (float)Math.pow(2, 1d / 2));
            world.playSound(location, Sound.BLOCK_NOTE_BLOCK_CHIME, 10, (float)Math.pow(2, 5d / 6));
            world.playSound(location, Sound.BLOCK_NOTE_BLOCK_CHIME, 10, (float)Math.pow(2, 13d / 2));
        }, 90);
    }

    public void tryRevive(Player player) {
        updateBossBar(player);
        currentReviveCount++;
        bossBar.setProgress((double)currentReviveCount / requireReviveCount);
        if (currentReviveCount >= requireReviveCount) {
            source.spigot().respawn();
        }
    }

    private void updateBossBar(Player player) {
        int bossBarDisplayTime = RevivePlugin.getInstance().getConfigManager().getBossBarDisplayTime();
        if (!bossBar.getPlayers().contains(player)) {
            bossBar.addPlayer(player);
        }
        if (bossBarHideTaskMap.containsKey(player)) {
            bossBarHideTaskMap.remove(player).cancel();
        }
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                bossBar.removePlayer(player);
                bossBarHideTaskMap.remove(player);
                cancel();
            }
        }.runTaskLater(RevivePlugin.getInstance(), bossBarDisplayTime);
        bossBarHideTaskMap.put(player, task);
    }

    public void askForHelp() {
        armorStand.setCustomNameVisible(true);
        ConfigManager configManager = RevivePlugin.getInstance().getConfigManager();
        int helpMessageDisplayTime = configManager.getHelpMessageDisplayTime();
        Bukkit.getScheduler().runTaskLater(RevivePlugin.getInstance(), () -> armorStand.setCustomNameVisible(false), helpMessageDisplayTime);
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
