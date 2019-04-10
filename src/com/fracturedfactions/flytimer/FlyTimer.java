package com.fracturedfactions.flytimer;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class FlyTimer extends JavaPlugin implements Listener {

    private String VERSION = "4.1.0";
    private HashMap<UUID, FlyingPlayer> players = new HashMap<>();
    private LinkedHashMap<String, Integer[]> timeList = new LinkedHashMap<>();

    @Override
    public void onEnable() {
        this.saveDefaultConfig(); //creates config
        reloadConfig();
        for (String key : this.getConfig().getConfigurationSection("config").getKeys(false)) {
            int time = this.getConfig().getInt("config." + key + ".time");
            int cooldown = this.getConfig().getInt("config." + key + ".cooldown");
            int extendedTimeinNeutral = this.getConfig().getInt("config." + key + ".extendedTimeinNeutral");
            Integer[] arr = {time, cooldown, extendedTimeinNeutral};
            timeList.put("flytimer." + key, arr);
        }
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, FlyTimer.this::everySecond, 0L, 20L);
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

    private void everySecond(){
        for (FlyingPlayer flyP : players.values()) {
            Player p = flyP.getPlayer();
            if (!(p.getLocation() == flyP.getLastLoc())) {
                if (flyP.getCooldown()) {
                    if (flyP.getCooldownExpiry() <= new Date().getTime()) {
                        flyP.endCooldown();
                    }
                }
                setPlayerStatus(flyP, flyP.getFactionPlayer(), Board.getInstance().getFactionAt(new FLocation(p.getLocation())));
            }
        }
    }

    public boolean onCommand(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("togglefly") || cmd.getName().equalsIgnoreCase("toggleflight")) { //checking command
            if (sender instanceof Player) {
                Player p = ((Player) sender).getPlayer();
                if(!p.hasPermission("flytimer.override")) {
                    FlyingPlayer flyP = players.get(p.getUniqueId());
                    if (flyP.getCooldown()) {
                        int status = flyP.getStatus();
                        if (status != -1) {
                            if(p.isFlying()){
                                p.setFlying(false);
                                p.setFallDistance(-1000);
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        p.setFallDistance(0);
                                    }
                                }.runTaskLater(this, (long) (140));
                            }
                            flyP.setStatus(-1);
                            p.sendMessage(ChatColor.DARK_GRAY.toString() + "» " + ChatColor.GRAY.toString() + "Set fly mode " + ChatColor.DARK_AQUA.toString() + "disabled " + ChatColor.GRAY.toString() + "for " + ChatColor.DARK_AQUA.toString() + p.getName());
                            return true;
                        } else {
                            flyP.setStatus(0);
                            p.sendMessage(ChatColor.DARK_GRAY.toString() + "» " + ChatColor.GRAY.toString() + "Set fly mode " + ChatColor.DARK_AQUA.toString() + "enabled " + ChatColor.GRAY.toString() + "for " + ChatColor.DARK_AQUA.toString() + p.getName());
                            return true;
                        }
                    } else {
                        p.sendMessage(ChatColor.RED.toString() + "Please wait until your cooldown expires before toggling flying!");
                    }
                }
            }
            return true;
        }
        else if(cmd.getName().equalsIgnoreCase("flytimer")){
            if (sender.hasPermission("flytimer.admin")) {
                if(args[0].equalsIgnoreCase("reload")) {
                    Bukkit.getPluginManager().disablePlugin(this);
                    Bukkit.getPluginManager().enablePlugin(this);
                    sender.sendMessage(ChatColor.GREEN.toString() + "[FT] FlyTimer was successfully reloaded!");
                }
                else if(args[0].equalsIgnoreCase("check")){
                    if(args.length > 1){
                        Player p = Bukkit.getPlayer(args[1]);
                        if(p != null){
                            if(players.containsKey(p.getUniqueId())){
                                FlyingPlayer flyP = players.get(p.getUniqueId());
                                sender.sendMessage("[FT] The status of " + p.getDisplayName() + "is " + flyP.getStatus());
                            }
                            else
                                sender.sendMessage(ChatColor.RED.toString() + "[FT] No player was found in list with the name: " + p.getDisplayName() + ". They might have the override permission!");
                        }
                        else
                            sender.sendMessage(ChatColor.RED.toString() + "[FT] No player was found with the name: " + args[1]);
                    }
                    else
                        sender.sendMessage(ChatColor.RED.toString() + "[FT] No player was specified!");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "[FT] No permission to execute this command!");
            }
            return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent e){
        Player p = e.getPlayer();
        if(!p.hasPermission("flytimer.override")) {
            int time;
            int cooldown;
            int extendedTimeinNeutral;
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
            players.put(p.getUniqueId(), new FlyingPlayer(p, time, cooldown, extendedTimeinNeutral, p.hasPermission("flytimer.safezone")));
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onLeave(PlayerQuitEvent e){
        Player p = e.getPlayer();
        if(!p.hasPermission("flytimer.override"))
            players.remove(p.getUniqueId());
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onDoubleJump(PlayerToggleFlightEvent e){
        Player p = e.getPlayer();
        if(!p.hasPermission("flytimer.override")) {
            FlyingPlayer flyP = players.get(e.getPlayer().getUniqueId());
            int status = flyP.getStatus();
            if (flyP.getCooldown() || (status == 0 && !flyP.getCanSafezoneFly()) || status == -1)
                e.setCancelled(true);
            else {
                startFlying(p, flyP);
            }
        }

    }


    private void setPlayerStatus(FlyingPlayer flyP, FPlayer fp, Faction currentFac) {
        //set status here **DONT DO delayed threads**
        // set flying true/false here
        if(flyP.getStatus() != -1) {
            if (currentFac.isWarZone()) {
                flyP.setStatus(1);
            } else if (fp.hasFaction() && fp.isInOwnTerritory()) {
                flyP.setStatus(2);
            } else if (fp.isInAllyTerritory() || currentFac.isWilderness()) {
                flyP.setStatus(3);
            } else if (currentFac.isSafeZone()) {
                flyP.setStatus(0);
            } else {
                flyP.setStatus(0);
            }
        }
    }


    private void startFlying(Player p, FlyingPlayer flyP){
            long flytime = flyP.getFlyTime()/1000;
            long endFlyTime = System.currentTimeMillis() + flytime;
            long cooldown = flyP.getCooldownTimeLeft();

            p.setFlying(true);

            BukkitTask abFly1 = new BukkitRunnable() {
                @Override
                public void run() {
                    sendActionBar(p, "You have " + ChatColor.GREEN.toString() + ChatColor.BOLD.toString() + (int) ((endFlyTime - System.currentTimeMillis())/1000) + ChatColor.WHITE.toString() + " seconds of flying left!");
                }
            }.runTaskTimerAsynchronously(this, 0, 19);
            BukkitTask abFly2 = new BukkitRunnable() {
                @Override
                public void run() {
                    sendActionBar(p, "You have " + ChatColor.RED.toString() + ChatColor.BOLD.toString()+ (int) (cooldown) + ChatColor.WHITE.toString() + " seconds of flying cooldown left!");
                }
            }.runTaskTimerAsynchronously(this, flytime*25, 19);
            new BukkitRunnable() {
                @Override
                public void run() {
                    flyP.startCooldown();
                    abFly1.cancel();
                    p.setFallDistance(-1000);
                    p.setFlying(false);
                }
            }.runTaskLater(this, flytime * 20);
            new BukkitRunnable() {
                @Override
                public void run() {
                    p.setFallDistance(0);
                }
            }.runTaskLater(this, (flytime+7) * 20);
            new BukkitRunnable() {
                @Override
                public void run() {
                    abFly2.cancel();
                    p.sendMessage(ChatColor.GREEN.toString() + "You are now able to fly!");
                }
            }.runTaskLater(this, cooldown*20+flytime*20);
            p.sendMessage(ChatColor.GREEN.toString() + "You are able to fly for " + ChatColor.AQUA.toString() + ChatColor.BOLD.toString() + flytime + ChatColor.GREEN + " seconds! Purchase ranks to upgrade this time!");
    }

    private static void sendActionBar(Player p, String msg)
    {
        String s = ChatColor.translateAlternateColorCodes('&', msg);
        IChatBaseComponent icbc = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + s + "\"}");
        PacketPlayOutChat bar = new PacketPlayOutChat(icbc, (byte)2);
        ((CraftPlayer)p).getHandle().playerConnection.sendPacket(bar);
    }
}