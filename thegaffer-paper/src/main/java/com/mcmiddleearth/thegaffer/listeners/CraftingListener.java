/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.mcmiddleearth.thegaffer.listeners;

import com.mcmiddleearth.thegaffer.utilities.PermissionsUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author Donovan
 */
public class CraftingListener implements Listener{
    @EventHandler
    public void preCraftItem(CraftItemEvent event){
        Player p = (Player) event.getWhoClicked();
        if(!p.hasPermission(PermissionsUtil.getIgnoreWorldProtection())){
            event.setCancelled(true);
        }
    }
    
}
