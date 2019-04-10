package com.fracturedfactions.flytimer;

import org.bukkit.entity.Player;

import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public class Cooldown {

    private static HashMap<UUID, Date> cooldowns = new HashMap<>();

    public static boolean hasCooldown(final Player p){
        UUID pID = p.getUniqueId();
        if(cooldowns.containsKey(pID)){
          if(cooldowns.get(pID).getTime() > new Date().getTime())
              return true;
          else{
              cooldowns.remove(pID);
              return false;
          }
        }
        return false;
    }

    public static int cooldownTimeLeft(final Player p){
        UUID pID = p.getUniqueId();
       return (int) (cooldowns.getOrDefault(pID, new Date()).getTime() - new Date().getTime())/1000;
    }

    public static void setCooldown(final double seconds, final Player p) {
        UUID pID = p.getUniqueId();
        //If the player is registered in the hashmap.
        if (!cooldowns.containsKey(pID)){
            //Cooldown is not set, so we will set the cooldown then return false.
            cooldowns.put(pID, new Date((long) (new Date().getTime() + (seconds*1000))));
        }
    }
}
