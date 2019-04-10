package com.fracturedfactions.flytimer;

import com.massivecraft.factions.*;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

public class FlyTimer extends JavaPlugin implements Listener {

    private String VERSION = "3.2.1";
    private LinkedHashMap<String, Double[]> timeList = new LinkedHashMap<>();
    private HashMap<Player, Integer> playerFlyStatus = new HashMap<>();

    @Override
    public void onEnable() {
        this.saveDefaultConfig(); //creates config
        reloadConfig();
        Iterator it = this.getConfig().getConfigurationSection("config").getKeys(false).iterator();
        while(it.hasNext()){
            final String key = (String) it.next();
            double time = this.getConfig().getDouble("config." + key + ".time");
            double cooldown = this.getConfig().getDouble("config." + key + ".cooldown");
            double extendedTimeinNeutral = this.getConfig().getDouble("config." + key + ".extendedTimeinNeutral");
            Double[] arr = {time, cooldown, extendedTimeinNeutral};
            timeList.put("flytimer." + key, arr);
        }
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getServer().getConsoleSender().sendMessage("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        Bukkit.getServer().getConsoleSender().sendMessage("Developed for protocolmc.com");
        Bukkit.getServer().getConsoleSender().sendMessage("Version " + VERSION);
        Bukkit.getServer().getConsoleSender().sendMessage("FlyTimer is now enabled!");
        Bukkit.getServer().getConsoleSender().sendMessage("~~~~~~~~~~~~~~~~[FT]~~~~~~~~~~~~~~~~");
    }

    @Override
    public void onDisable() {
        Bukkit.getServer().getScheduler().cancelTasks(this);
        Bukkit.getServer().getConsoleSender().sendMessage("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        Bukkit.getServer().getConsoleSender().sendMessage("Developed for protocolmc.com");
        Bukkit.getServer().getConsoleSender().sendMessage("Version " + VERSION);
        Bukkit.getServer().getConsoleSender().sendMessage("FlyTimer is now disabled");
        Bukkit.getServer().getConsoleSender().sendMessage("~~~~~~~~~~~~~~~~[FT]~~~~~~~~~~~~~~~~");
    }

    public boolean onCommand(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("togglefly") || cmd.getName().equalsIgnoreCase("toggleflight")) { //checking command
                if (sender instanceof Player) {
                    Player p = ((Player) sender).getPlayer();
                    if (!Cooldown.hasCooldown(p)) {
                        if (playerFlyStatus.getOrDefault(p, 0) != -1) {
                            p.setFlying(false);
                            p.setAllowFlight(false);
                            playerFlyStatus.put(p, -1);
                            p.setFallDistance(-1000);
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    p.setFallDistance(0);
                                }
                            }.runTaskLater(this, (long) (140));
                            p.sendMessage(ChatColor.DARK_GRAY.toString() + "» " + ChatColor.GRAY.toString() + "Set fly mode " + ChatColor.DARK_AQUA.toString() + "disabled " + ChatColor.GRAY.toString() + "for " + ChatColor.DARK_AQUA.toString() + p.getName());
                            return true;
                        } else {
                            playerFlyStatus.put(p, 0);
                            p.sendMessage(ChatColor.DARK_GRAY.toString() + "» " + ChatColor.GRAY.toString() + "Set fly mode " + ChatColor.DARK_AQUA.toString() + "enabled " + ChatColor.GRAY.toString() + "for " + ChatColor.DARK_AQUA.toString() + p.getName());
                            return true;
                        }
                    } else {
                        p.sendMessage(ChatColor.RED.toString() + "Please wait until your cooldown expires before toggling flying!");
                    }
                }
                return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDoubleJump(PlayerToggleFlightEvent e){
        Player p = e.getPlayer();
        if(!p.hasPermission("flytimer.override")) {
            if(playerFlyStatus.get(p) == 1){
                startFlying(p, false);
            }
            else if(playerFlyStatus.get(p) == 2){
                startFlying(p, true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void canFly(PlayerMoveEvent e){
        Player p = e.getPlayer();
        if(!p.hasPermission("flytimer.override")) {
            if (e.getFrom().getBlockX() == e.getTo().getBlockX() && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) {
                return;
            }
            FPlayer fp = FPlayers.getInstance().getByPlayer(p);
            FLocation loc = new FLocation(p.getLocation());
            Faction currentFac = Board.getInstance().getFactionAt(loc);
            setPlayerStatus(p, fp, currentFac);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void joined(PlayerJoinEvent e){
        Player p = e.getPlayer();
        if(!p.hasPermission("flytimer.override")) {
            FPlayer fp = FPlayers.getInstance().getByPlayer(p);
            FLocation loc = new FLocation(p.getLocation());
            Faction currentFac = Board.getInstance().getFactionAt(loc);
            if (p.isFlying() || (p.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR)) {
                p.setFlying(false);
                p.setAllowFlight(false);
                p.setFallDistance(-1000);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        p.setFallDistance(0);
                        setPlayerStatus(p, fp, currentFac);
                    }
                }.runTaskLater(this, (long) (140));
            } else {
                setPlayerStatus(p, fp, currentFac);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void left(PlayerQuitEvent e){
        Player p = e.getPlayer();
        playerFlyStatus.remove(p);
    }

    private void setPlayerStatus(Player p, FPlayer fp, Faction currentFac) {
        if(playerFlyStatus.getOrDefault(p, 0) != -1) {
            if (currentFac.isWarZone()) {
//                this.getServer().getConsoleSender().sendMessage("Player " + p.getName() + " is in status: 1");
                if (playerFlyStatus.getOrDefault(p, 0) != 1) {
                    p.setFlying(false);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            p.setAllowFlight(true);
                        }
                    }.runTaskLater(this, (long) (Cooldown.cooldownTimeLeft(p)*20 + 150));
                    playerFlyStatus.put(p, 1);
                }
            } else if (fp.hasFaction() && fp.isInOwnTerritory()) {
                if (playerFlyStatus.getOrDefault(p, 0) != 3) {
//                    if(playerFlyStatus.getOrDefault(p, 0) == 2){
//                        //add cancel cooldown from wild
//
//                    }
//                    else{
//
//                    }
//                    this.getServer().getConsoleSender().sendMessage("Player " + p.getName() + " is in status: 3");
                    p.setAllowFlight(true);
                    playerFlyStatus.put(p, 3);
                }
            } else if (fp.isInAllyTerritory() || currentFac.isWilderness()) {
//                this.getServer().getConsoleSender().sendMessage("Player " + p.getName() + " is in status: 2");
                if (playerFlyStatus.getOrDefault(p, 0) != 2) {
                    if(playerFlyStatus.getOrDefault(p, 0) == 3){
//                        this.getServer().getConsoleSender().sendMessage("Player " + p.getName() + " is in status: 2.1");
                        p.setFlying(false);
                        p.setAllowFlight(false);
                        p.setFallDistance(-1000);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                p.setFallDistance(0);
                            }
                        }.runTaskLater(this, (long) (140));
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                p.setAllowFlight(true);
                            }
                        }.runTaskLater(this, (long) (Cooldown.cooldownTimeLeft(p)*20 + 150));
                        playerFlyStatus.put(p, 2);
                    }
                    else{
//                        this.getServer().getConsoleSender().sendMessage("Player " + p.getName() + " is in status: 2.2");
                        p.setFlying(false);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                p.setAllowFlight(true);
                            }
                        }.runTaskLater(this, (long) (Cooldown.cooldownTimeLeft(p)*20 + 150));
                        playerFlyStatus.put(p, 2);
                    }
                }
            } else if (currentFac.isSafeZone()) {
//                this.getServer().getConsoleSender().sendMessage("Player " + p.getName() + " is in status: 4");
                playerFlyStatus.put(p, 4);
                if (p.hasPermission("flytimer.safezone")) {
                    p.setAllowFlight(true);
                } else {
                    p.setFlying(false);
                    p.setAllowFlight(false);
                }
            } else {
//                this.getServer().getConsoleSender().sendMessage("Player " + p.getName() + " is in status: 0");
                playerFlyStatus.put(p, 0);
                p.setAllowFlight(false);
            }
        }
    }

    private void startFlying(Player p, boolean extended){
        if(!Cooldown.hasCooldown(p)) {
            p.setFlying(true);
            double time;
            double cooldown;
            double extendedTimeinNeutral;
            if(p.hasPermission("flytimer.t5")){
                time = timeList.get("flytimer.t5")[0];
                cooldown = timeList.get("flytimer.t5")[1];
                extendedTimeinNeutral = timeList.get("flytimer.t5")[2];
            }
            else if(p.hasPermission("flytimer.t4")){
                time = timeList.get("flytimer.t4")[0];
                cooldown = timeList.get("flytimer.t4")[1];
                extendedTimeinNeutral = timeList.get("flytimer.t4")[2];
            }
            else if(p.hasPermission("flytimer.t3")){
                time = timeList.get("flytimer.t3")[0];
                cooldown = timeList.get("flytimer.t3")[1];
                extendedTimeinNeutral = timeList.get("flytimer.t3")[2];
            }
            else if(p.hasPermission("flytimer.t2")){
                time = timeList.get("flytimer.t2")[0];
                cooldown = timeList.get("flytimer.t2")[1];
                extendedTimeinNeutral = timeList.get("flytimer.t2")[2];
            }
            else{
                time = timeList.get("flytimer.t1")[0];
                cooldown = timeList.get("flytimer.t1")[1];
                extendedTimeinNeutral = timeList.get("flytimer.t1")[2];
            }
            time = extended ? extendedTimeinNeutral+time : time;
            final double cool = cooldown;
            final long endTime = System.currentTimeMillis() + (long) (time*1000);
            BukkitTask abFly1 = new BukkitRunnable() {
                @Override
                public void run() {
                    sendActionBar(p, "You have " + ChatColor.GREEN.toString() + ChatColor.BOLD.toString() + (int) ((endTime - System.currentTimeMillis())/1000) + ChatColor.WHITE.toString() + " seconds of flying left!");
                }
            }.runTaskTimerAsynchronously(this, 0, 18);
            BukkitTask abFly2 = new BukkitRunnable() {
                @Override
                public void run() {
                    sendActionBar(p, "You have " + ChatColor.RED.toString() + ChatColor.BOLD.toString()+ (int) (((endTime + cool*1000) - System.currentTimeMillis())/1000) + ChatColor.WHITE.toString() + " seconds of flying cooldown left!");
                }
            }.runTaskTimerAsynchronously(this, (long) (time*20), 18);
            new BukkitRunnable() {
                @Override
                public void run() {
                    abFly1.cancel();
                    p.setFallDistance(-1000);
                    p.setFlying(false);
                    p.setAllowFlight(false);
                }
            }.runTaskLater(this, (long) (time * 20));
            new BukkitRunnable() {
                @Override
                public void run() {
                    p.setFallDistance(0);
                }
            }.runTaskLater(this, (long) ((time+7) * 20));
            new BukkitRunnable() {
                @Override
                public void run() {
                    abFly2.cancel();
                    p.sendMessage(ChatColor.GREEN.toString() + "You are now able to fly!");
                    p.setAllowFlight(true);
                }
            }.runTaskLater(this, (long) (cooldown*20+time*20));
            Cooldown.setCooldown(cooldown, p);
            p.sendMessage(ChatColor.GREEN.toString() + "You are able to fly for " + ChatColor.AQUA.toString() + ChatColor.BOLD.toString() + time + ChatColor.GREEN + " seconds! Purchase ranks to upgrade this time!");
        }
        else{
            p.setFlying(false);
        }
    }

    public static void sendActionBar(Player p, String msg)
    {
        String s = ChatColor.translateAlternateColorCodes('&', msg);
        IChatBaseComponent icbc = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + s + "\"}");
        PacketPlayOutChat bar = new PacketPlayOutChat(icbc, (byte)2);
        ((CraftPlayer)p).getHandle().playerConnection.sendPacket(bar);
    }
}