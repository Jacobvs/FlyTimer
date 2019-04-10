package com.fracturedfactions.flytimer;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class FlyingPlayer {

    private Player p;
    private FPlayer fp;
    private Location lastLoc;
    private int status;
    private boolean onCooldown, canSafezoneFly;
    private long cooldownExpiry, flyTime, cooldownTime, extendedTime;

    FlyingPlayer(Player p, int flyTime, int cooldownTime, int extendedTime, boolean canSafezoneFly) {
        this.p = p;
        this.status = 0;
        this.onCooldown = false;
        this.flyTime = flyTime*1000;
        this.cooldownTime = cooldownTime*1000;
        this.extendedTime = extendedTime*1000;
        this.canSafezoneFly = canSafezoneFly;
        this.fp = FPlayers.getInstance().getByPlayer(p);
    }

    public void setLocation(Location l){
        this.lastLoc = l;
    }

    void startCooldown() {
        this.onCooldown = true;
        this.cooldownExpiry = System.currentTimeMillis() + cooldownTime;
    }

    void endCooldown(){
        this.onCooldown = false;
    }

    void setStatus(int status) {
        this.status = status;
    }

    Player getPlayer(){
        return this.p;
    }

    FPlayer getFactionPlayer(){
        return this.fp;
    }

    Location getLastLoc(){
        return lastLoc;
    }

    boolean getCooldown() {
        return this.onCooldown;
    }

    long getCooldownExpiry(){
        return this.cooldownExpiry;
    }

    long getCooldownTimeLeft(){
        return (int) ((this.cooldownExpiry - System.currentTimeMillis()));
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
