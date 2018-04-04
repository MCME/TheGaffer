/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mcmiddleearth.thegaffer.utilities;

import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.storage.JobDatabase;
import java.util.UUID;
import mineverse.Aust1n46.chat.api.MineverseChatAPI;
import mineverse.Aust1n46.chat.api.MineverseChatPlayer;
import org.bukkit.OfflinePlayer;

/**
 *
 * @author Eriol_Eandur
 */
public class VentureChatUtil {
    
    public static void joinJobChannel(String player) {
        MineverseChatPlayer cp = MineverseChatAPI.getOnlineMineverseChatPlayer(player);
        if(cp!=null) 
            cp.addListening(TheGaffer.getDiscordChannel());
    }
    
    public static void joinJobChannel(UUID player) {
        MineverseChatPlayer cp = MineverseChatAPI.getOnlineMineverseChatPlayer(player);
        if(cp!=null) 
            cp.addListening(TheGaffer.getDiscordChannel());
    }
    
    public static void leaveJobChannel(OfflinePlayer player) {
        if(JobDatabase.getJobWorking(player)==null) {
            MineverseChatPlayer cp = MineverseChatAPI.getOnlineMineverseChatPlayer(player.getUniqueId());
            if(cp!=null) 
                cp.removeListening(TheGaffer.getDiscordChannel());
        }
    }
    
}
