package com.protocolmc.flytimer;

import com.earth2me.essentials.IEssentials;
import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class FlyTimer extends JavaPlugin implements Listener {

    private String VERSION = "4.4.2";
    private HashMap<Player, FlyingPlayer> players = new HashMap<>();
    private LinkedHashMap<String, Integer[]> timeList = new LinkedHashMap<>();
    private IEssentials ess;
    boolean ncp = true;

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
        for (Player p : Bukkit.getServer().getOnlinePlayers()) {
            parsePlayer(p);
        }
        ess = (IEssentials) Bukkit.getPluginManager().getPlugin("Essentials");
        if (this.getServer().getPluginManager().getPlugin("NoCheatPlus") == null) {
            this.ncp = false;
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

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("togglefly") || cmd.getName().equalsIgnoreCase("toggleflight")) { //checking command
            if (sender instanceof Player) {
                Player p = ((Player) sender).getPlayer();
                if (!p.hasPermission("flytimer.override")) {
                    FlyingPlayer flyP = players.get(p);
                    if (!flyP.getCooldown()) {
                        int status = flyP.getStatus();
                        if (status != -1) {
                            if (p.isFlying()) {
                                safeFlyDisable(p);
                            }
                            p.setAllowFlight(false);
                            flyP.setStatus(-1);
                            p.sendMessage(ChatColor.DARK_GRAY.toString() + "» " + ChatColor.GRAY.toString() + "Set fly mode " + ChatColor.DARK_AQUA.toString() + "disabled " + ChatColor.GRAY.toString() + "for " + ChatColor.DARK_AQUA.toString() + p.getName());
                            return true;
                        } else {
                            flyP.setStatus(0);
                            p.setAllowFlight(true);
                            p.sendMessage(ChatColor.DARK_GRAY.toString() + "» " + ChatColor.GRAY.toString() + "Set fly mode " + ChatColor.DARK_AQUA.toString() + "enabled " + ChatColor.GRAY.toString() + "for " + ChatColor.DARK_AQUA.toString() + p.getName());
                            return true;
                        }
                    } else {
                        p.sendMessage(ChatColor.RED.toString() + "Please wait until your cooldown expires before toggling flying!");
                    }
                }
            }
            return true;
        } else if (cmd.getName().equalsIgnoreCase("flytimer")) {
            if (sender.hasPermission("flytimer.admin")) {
                if (args[0].equalsIgnoreCase("reload")) {
                    Bukkit.getPluginManager().disablePlugin(this);
                    Bukkit.getPluginManager().enablePlugin(this);
                    sender.sendMessage(ChatColor.GREEN.toString() + "[FT] FlyTimer was successfully reloaded!");
                } else if (args[0].equalsIgnoreCase("check")) {
                    if (args.length > 1) {
                        Player p = Bukkit.getPlayer(args[1]);
                        if (p != null) {
                            if (players.containsKey(p)) {
                                FlyingPlayer flyP = players.get(p);
                                sender.sendMessage("[FT] The status of " + p.getDisplayName() + " is " + flyP.getStatus() + " | On Cooldown: " + flyP.getCooldown() + " | Cooldown secs: " + (flyP.getCooldownExpiry() - System.currentTimeMillis()) / 1000 + " | Paused: " + flyP.getCooldownPaused() + " | Can Fly: " + p.getAllowFlight());
                            } else
                                sender.sendMessage(ChatColor.RED.toString() + "[FT] No player was found in list with the name: " + p.getDisplayName() + ". They might have the override permission!");
                        } else
                            sender.sendMessage(ChatColor.RED.toString() + "[FT] No player was found with the name: " + args[1]);
                    } else
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
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        parsePlayer(p);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onLeave(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        players.remove(p);
    }


    @EventHandler(priority = EventPriority.LOW)
    public void onDoubleJump(PlayerToggleFlightEvent e) {
        Player p = e.getPlayer();
        if (!p.hasPermission("flytimer.override")) {
            if(p.isFlying())
                p.setFlying(false);
            else {
                FlyingPlayer flyP = players.get(p);
                if (flyP == null) {
                    e.setCancelled(true);
                    return;
                }
                int status = flyP.getStatus();
                if ((status == 0 && flyP.getCanSafezoneFly()) || status == 2) {
                    p.setAllowFlight(true);
                    p.setFlying(true);
                } else if (flyP.getCooldown() || status == -1)
                    p.setAllowFlight(false);
                else if (!flyP.getFlying())
                    startFlying(p, flyP);
            }
        }

    }

    private void everySecond() {
        for (FlyingPlayer flyP : players.values()) {
            Player p = flyP.getPlayer();
            if(!ess.getUser(p).isAfk()) {
                if (((flyP.getCooldownExpiry() <= System.currentTimeMillis()) && !flyP.getCooldownPaused()) && flyP.getCooldown()) {
                    flyP.endCooldown();
                }

                Location l = p.getLocation();
                setPlayerStatus(flyP, p, flyP.getFactionPlayer(), Board.getInstance().getFactionAt(new FLocation(p.getLocation())), l, flyP.getStatus());
            }
        }
    }

    private void setPlayerStatus(@NotNull FlyingPlayer flyP, Player p, FPlayer fp, Faction currentFac, Location l, int laststatus) {
        //set status here **DONT DO delayed threads**
        // set flying true/false here
        int status = flyP.getStatus();
        if (status != -1) {
            if (currentFac.isWarZone()) {
                if(inRegion("spawn", l)){
                    flyP.setStatus(0);
                    safezoneFly(flyP, p);
                }
                else {
                    if(status == 0 || status == 2)
                        this.getServer().dispatchCommand(Bukkit.getConsoleSender(), "ncp unexempt " + p.getName() + " moving_survivalfly");
                    flyP.setStatus(1);
                    if (flyP.getCooldownPaused()) {
                        p.setAllowFlight(false);
                        flyP.resumeCooldown();
                    }
                }
            } else if (fp.hasFaction() && fp.isInOwnTerritory()) {
                flyP.setStatus(2);
                p.setAllowFlight(true);
                if (this.ncp) {
                    this.getServer().dispatchCommand(Bukkit.getConsoleSender(), "ncp exempt " + ((Player) p).getName() + " moving_survivalfly");
                }
                if (flyP.getCooldown() && !flyP.getCooldownPaused()){
                    flyP.pauseCooldown();
                }
            } else if (fp.isInAllyTerritory() || currentFac.isWilderness()) {
                if(status == 0 || status == 2){
                    safeFlyDisable(p);
                    this.getServer().dispatchCommand(Bukkit.getConsoleSender(), "ncp unexempt " + p.getName() + " moving_survivalfly");
                }
                flyP.setStatus(3);
                if (flyP.getCooldownPaused()){
                    p.setAllowFlight(true);
                    flyP.resumeCooldown();
                }
            } else if (currentFac.isSafeZone()) {
                flyP.setStatus(0);
                safezoneFly(flyP, p);
            } else {
                flyP.setStatus(1);
            }
        }
    }

    private void safezoneFly(@NotNull FlyingPlayer flyP, Player p) {
        if (flyP.getCanSafezoneFly()) {
            if (flyP.getCooldown()) {
                flyP.pauseCooldown();
            }
            if (this.ncp) {
                this.getServer().dispatchCommand(Bukkit.getConsoleSender(), "ncp exempt " + ((Player) p).getName() + " moving_survivalfly");
            }
            p.setAllowFlight(true);
        }
        else{
            p.setAllowFlight(false);
        }
    }


    private void startFlying(@NotNull Player p, @NotNull FlyingPlayer flyP) {
            long flytime = flyP.getFlyTime() / 1000;
            long endFlyTime = System.currentTimeMillis() + (flytime * 1000);

        if (this.ncp) {
            this.getServer().dispatchCommand(Bukkit.getConsoleSender(), "ncp exempt " + ((Player) p).getName() + " moving_survivalfly");
        }

        p.setFlying(true);
        flyP.setFlying(true);

        BukkitTask abFly1 = new BukkitRunnable() {
            @Override
            public void run() {
                sendActionBar(p, "You have " + ChatColor.GREEN.toString() + ChatColor.BOLD.toString() + (int) ((endFlyTime - System.currentTimeMillis()) / 1000) + ChatColor.WHITE.toString() + " seconds of flying left!");
            }
        }.runTaskTimerAsynchronously(this, 0, 19);

        new BukkitRunnable() {
            @Override
            public void run() {
                flyP.setFlying(false);
                flyP.startCooldown();
                abFly1.cancel();
                safeFlyDisable(p);
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "ncp unexempt " + p.getName() + " moving_survivalfly");
            }
        }.runTaskLater(this, flytime * 19);

        new BukkitRunnable() {
            @Override
            public void run() {
                if ((flyP.getCooldownExpiry() <= System.currentTimeMillis()) && !flyP.getCooldownPaused())
                    this.cancel();
                if (!flyP.getCooldownPaused()) {
                    p.setAllowFlight(false);
                    sendActionBar(p, "You have " + ChatColor.RED.toString() + ChatColor.BOLD.toString() + (int) ((flyP.getCooldownExpiry() - System.currentTimeMillis()) / 1000) + ChatColor.WHITE.toString() + " seconds of flying cooldown left!");
                }
                else
                    sendActionBar(p, ChatColor.RED.toString() + ChatColor.BOLD.toString() + "Flying cooldown paused!");
            }
        }.runTaskTimerAsynchronously(this, flytime * 20, 19);

//            new BukkitRunnable() {
//                @Override
//                public void run() {
//                    p.setAllowFlight(true);
//                    flyP.endCooldown();
//                    p.sendMessage(ChatColor.GREEN.toString() + "You are now able to fly!");
//                }
//            }.runTaskLater(this, cooldownTime/50+flytime*20); // divided by 1000 then * 20
        p.sendMessage(ChatColor.GREEN.toString() + "You are able to fly for " + ChatColor.AQUA.toString() + ChatColor.BOLD.toString() + flytime + ChatColor.GREEN + " seconds! Purchase ranks to upgrade this time!");
    }

    private void parsePlayer(@NotNull Player p) {
        if (!p.hasPermission("flytimer.override")) {
            int time;
            int cooldown;
            int extendedTimeinNeutral;
            if (p.hasPermission("flytimer.t5")) {
                time = timeList.get("flytimer.t5")[0];
                cooldown = timeList.get("flytimer.t5")[1];
                extendedTimeinNeutral = timeList.get("flytimer.t5")[2];
            } else if (p.hasPermission("flytimer.t4")) {
                time = timeList.get("flytimer.t4")[0];
                cooldown = timeList.get("flytimer.t4")[1];
                extendedTimeinNeutral = timeList.get("flytimer.t4")[2];
            } else if (p.hasPermission("flytimer.t3")) {
                time = timeList.get("flytimer.t3")[0];
                cooldown = timeList.get("flytimer.t3")[1];
                extendedTimeinNeutral = timeList.get("flytimer.t3")[2];
            } else if (p.hasPermission("flytimer.t2")) {
                time = timeList.get("flytimer.t2")[0];
                cooldown = timeList.get("flytimer.t2")[1];
                extendedTimeinNeutral = timeList.get("flytimer.t2")[2];
            } else {
                time = timeList.get("flytimer.t1")[0];
                cooldown = timeList.get("flytimer.t1")[1];
                extendedTimeinNeutral = timeList.get("flytimer.t1")[2];
            }
            p.setAllowFlight(true);
            players.put(p, new FlyingPlayer(p, time, cooldown, extendedTimeinNeutral, p.hasPermission("flytimer.safezone")));
        }
    }

    private void safeFlyDisable(@NotNull Player p) {
        p.setFallDistance(-1000);
        p.setFlying(false);
        p.setAllowFlight(false);
        new BukkitRunnable() {
            @Override
            public void run() {
                p.setFallDistance(0);
            }
        }.runTaskLater(this, 7 * 20);
    }

    private static boolean inRegion(String regionName, Location loc) {
        ApplicableRegionSet set = WGBukkit.getPlugin().getRegionManager(loc.getWorld()).getApplicableRegions(loc);
        Iterator var4 = set.iterator();

        while(var4.hasNext()) {
            ProtectedRegion region = (ProtectedRegion) var4.next();
            if (regionName.equalsIgnoreCase(region.getId())) {
                return true;
            }
        }

        return false;
    }

    private static void sendActionBar(Player p, String msg) {
        String s = ChatColor.translateAlternateColorCodes('&', msg);
        IChatBaseComponent icbc = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + s + "\"}");
        PacketPlayOutChat bar = new PacketPlayOutChat(icbc, (byte) 2);
        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(bar);
    }
}