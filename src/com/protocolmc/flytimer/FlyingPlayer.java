package com.protocolmc.flytimer;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class FlyingPlayer {

    private Player p;
    private FPlayer fp;
    private int status;
    private boolean onCooldown, cooldownPaused, canSafezoneFly, startedFlying;
    private long cooldownExpiry, flyTime, cooldownTime, cooldownTimeLeft, extendedTime;

    FlyingPlayer(Player p, int flyTime, int cooldownTime, int extendedTime, boolean canSafezoneFly) {
        this.p = p;
        this.status = 0;
        this.onCooldown = false;
        this.cooldownPaused = false;
        this.flyTime = flyTime*1000;
        this.cooldownTime = cooldownTime*1000;
        this.extendedTime = extendedTime*1000;
        this.canSafezoneFly = canSafezoneFly;
        this.fp = FPlayers.getInstance().getByPlayer(p);
    }

    void startCooldown() {
        this.onCooldown = true;
        this.cooldownExpiry = System.currentTimeMillis() + this.cooldownTime;
    }

    void pauseCooldown(){
        this.cooldownPaused = true;
        this.cooldownTimeLeft = this.cooldownExpiry - System.currentTimeMillis();
    }

    void resumeCooldown(){
        this.cooldownPaused = false;
        this.cooldownExpiry = System.currentTimeMillis() + this.cooldownTimeLeft;
    }

    void endCooldown(){
        this.onCooldown = false;
        this.cooldownPaused = false;
        this.p.setAllowFlight(true);
        this.p.sendMessage(ChatColor.GREEN.toString() + "You are now able to fly!");
        this.cooldownExpiry = System.currentTimeMillis() - 10;
    }

    void setStatus(int status) {
        this.status = status;
    }

    void setFlying(boolean flying){ this.startedFlying = flying;}

    boolean getFlying(){return this.startedFlying;}

    Player getPlayer(){
        return this.p;
    }

    FPlayer getFactionPlayer(){
        return this.fp;
    }

    boolean getCooldown() {
        return (this.onCooldown && !this.cooldownPaused);
    }

    long getCooldownExpiry(){
        return this.cooldownExpiry;
    }

    boolean getCooldownPaused(){
        return this.cooldownPaused;
    }

    int getStatus() {
        return this.status;
    }

    boolean getCanSafezoneFly(){
        return this.canSafezoneFly;
    }

    long getFlyTime(){
        return (status == 3) ? flyTime + extendedTime : flyTime;
    }
}
