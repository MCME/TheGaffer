/*  This file is part of TheGaffer.
 * 
 *  TheGaffer is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  TheGaffer is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with TheGaffer.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mcmiddleearth.thegaffer.listeners;

import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.events.JobProtectionBlockBreakEvent;
import com.mcmiddleearth.thegaffer.events.JobProtectionBlockPlaceEvent;
import com.mcmiddleearth.thegaffer.events.JobProtectionHangingBreakEvent;
import com.mcmiddleearth.thegaffer.events.JobProtectionHangingPlaceEvent;
import com.mcmiddleearth.thegaffer.events.JobProtectionInteractEvent;
import com.mcmiddleearth.thegaffer.utilities.BuildProtection;
import com.mcmiddleearth.thegaffer.utilities.ProtectionUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class ProtectionListener implements Listener {

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) {
            return;
        }
        JobProtectionBlockPlaceEvent jobEvent;
        BuildProtection buildProtection = ProtectionUtil.getBuildProtection(event.getPlayer(),
                                                                    event.getBlock().getLocation());
        switch(buildProtection) {
            case ALLOWED:
                event.setCancelled(false);
                jobEvent = new JobProtectionBlockPlaceEvent(event.getPlayer(), event.getBlock().getLocation(), event.getBlock(), false);
                TheGaffer.getServerInstance().getPluginManager().callEvent(jobEvent);
                return;
            case JOB_PAUSED:
            case OUT_OF_BOUNDS:
                event.setCancelled(true);
                break;
            default:
                event.setBuild(false);
                break;
        }
        event.getPlayer().sendMessage(ChatColor.DARK_RED + buildProtection.getMessage());
        jobEvent = new JobProtectionBlockPlaceEvent(event.getPlayer(), event.getBlock().getLocation(), event.getBlock(), true);
        TheGaffer.getServerInstance().getPluginManager().callEvent(jobEvent);
    }
                
    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        JobProtectionBlockBreakEvent jobEvent;
        if (event.isCancelled()) {
            return;
        }
        BuildProtection buildProtection = ProtectionUtil.getBuildProtection(event.getPlayer(),
                                                                    event.getBlock().getLocation());
        switch(buildProtection) {
            case ALLOWED:
                event.setCancelled(false);
                jobEvent = new JobProtectionBlockBreakEvent(event.getPlayer(), event.getBlock().getLocation(), event.getBlock(), false);
                TheGaffer.getServerInstance().getPluginManager().callEvent(jobEvent);
                return;
            default:
                event.setCancelled(true);
                break;
        }
        event.getPlayer().sendMessage(ChatColor.DARK_RED + buildProtection.getMessage());
        jobEvent = new JobProtectionBlockBreakEvent(event.getPlayer(), event.getBlock().getLocation(), event.getBlock(), true);
        TheGaffer.getServerInstance().getPluginManager().callEvent(jobEvent);
    }
    
    @EventHandler
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        JobProtectionHangingBreakEvent jobEvent;
        if (event.isCancelled()) {
            return;
        }
        if (!(event.getRemover() instanceof Player)) {
            return;
        }
        Player player = (Player)event.getRemover();
        BuildProtection buildProtection = ProtectionUtil.getBuildProtection(player,
                                                                    event.getEntity().getLocation());
        switch(buildProtection) {
            case ALLOWED:
                event.setCancelled(false);
                jobEvent = new JobProtectionHangingBreakEvent(player, event.getEntity().getLocation(), event.getEntity(), false);
                TheGaffer.getServerInstance().getPluginManager().callEvent(jobEvent);
                return;
            default:
                event.setCancelled(true);
                break;
        }
        player.sendMessage(ChatColor.DARK_RED + buildProtection.getMessage());
        jobEvent = new JobProtectionHangingBreakEvent(player, event.getEntity().getLocation(), event.getEntity(), true);
        TheGaffer.getServerInstance().getPluginManager().callEvent(jobEvent);
    }
    
    @EventHandler
    public void onHangingPlace(HangingPlaceEvent event) {
        JobProtectionHangingPlaceEvent jobEvent;
        if (event.isCancelled()) {
            return;
        }
        Player player = event.getPlayer();
        BuildProtection buildProtection = ProtectionUtil.getBuildProtection(player,
                                                                    event.getEntity().getLocation());
        switch(buildProtection) {
            case ALLOWED:
                event.setCancelled(false);
                jobEvent = new JobProtectionHangingPlaceEvent(player, event.getEntity().getLocation(), event.getEntity(), false);
                TheGaffer.getServerInstance().getPluginManager().callEvent(jobEvent);
                return;
            default:
                event.setCancelled(true);
                break;
        }
        player.sendMessage(ChatColor.DARK_RED + buildProtection.getMessage());
        jobEvent = new JobProtectionHangingPlaceEvent(player, event.getEntity().getLocation(), event.getEntity(), true);
        TheGaffer.getServerInstance().getPluginManager().callEvent(jobEvent);
    }
    
    private boolean existsMethodGetHand(PlayerInteractEvent event) {
        try {
            event.getClass().getMethod("getHand");
            return true;
        } catch(NoSuchMethodException e) {
            return false;
        }
    }
    
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        JobProtectionInteractEvent jobEvent;
        if(existsMethodGetHand(event) 
                && !event.getAction().equals(Action.PHYSICAL)
                && !event.getHand().equals(EquipmentSlot.HAND)) {
            return;
        }
        if (event.isCancelled()) {
            return;
        }
        boolean restricted = false;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        Material halfSlab = Material.STONE_SLAB;
        final Block block = event.getClickedBlock();
        final BlockFace blockFace = event.getBlockFace();
        final Block relativeBlock = (event.hasBlock()?block.getRelative(blockFace):null);
        final Material fireMaterial = Material.FIRE;

        if (item != null && event.hasBlock()) {
            if (item.getType().equals(Material.BONE_MEAL)) {
                if (event.getClickedBlock().getType() == Material.GRASS
                        || event.getClickedBlock().getType() == Material.SPRUCE_SAPLING
                        || event.getClickedBlock().getType() == Material.ACACIA_SAPLING
                        || event.getClickedBlock().getType() == Material.BIRCH_SAPLING
                        || event.getClickedBlock().getType() == Material.DARK_OAK_SAPLING
                        || event.getClickedBlock().getType() == Material.JUNGLE_SAPLING
                        || event.getClickedBlock().getType() == Material.OAK_SAPLING
                        || event.getClickedBlock().getType() == Material.WHEAT
                        || event.getClickedBlock().getType() == Material.BROWN_MUSHROOM
                        || event.getClickedBlock().getType() == Material.RED_MUSHROOM
                        || event.getClickedBlock().getType() == Material.PUMPKIN_STEM
                        || event.getClickedBlock().getType() == Material.MELON_STEM
                        || event.getClickedBlock().getType() == Material.POTATO
                        || event.getClickedBlock().getType() == Material.CARROT
                        || event.getClickedBlock().getType() == Material.COCOA
                        || event.getClickedBlock().getType() == Material.TALL_GRASS) {
                    restricted = true;
                } else if (event.getItem().getType().equals(Material.COCOA_BEANS)) {
                    restricted = true;
                }
            }

            if (event.getClickedBlock().getType().equals(Material.FLOWER_POT)) {
                //cancelling this currently does nothing.
                if (item.getType() == Material.POPPY
                        || item.getType() == Material.DANDELION_YELLOW
                        || item.getType() == Material.SPRUCE_SAPLING
                        || item.getType() == Material.ACACIA_SAPLING
                        || item.getType() == Material.BIRCH_SAPLING
                        || item.getType() == Material.DARK_OAK_SAPLING
                        || item.getType() == Material.JUNGLE_SAPLING
                        || item.getType() == Material.OAK_SAPLING
                        || item.getType() == Material.RED_MUSHROOM
                        || item.getType() == Material.BROWN_MUSHROOM
                        || item.getType() == Material.CACTUS
                        || item.getType() == Material.TALL_GRASS
                        || item.getType() == Material.DEAD_BUSH) {
                    restricted = true;
                    player.sendBlockChange(event.getClickedBlock().getLocation(), Material.STONE, (byte)0);
                }
            }

            if(item.getType() == halfSlab){
                restricted = true;
            }

            if(item.getType().equals(Material.LILY_PAD)){
                event.setCancelled(true);
                restricted=true;
            }
        }

        if (event.hasBlock() && relativeBlock.getType() == fireMaterial) {
            player.sendBlockChange(relativeBlock.getLocation(), fireMaterial, (byte) 0);
            event.setCancelled(true);
            restricted = true;
        }

        if(!restricted) {
            return;
        }

        BuildProtection buildProtection = ProtectionUtil.getBuildProtection(player,
                                                                    event.getClickedBlock().getLocation());
        switch(buildProtection) {
            case ALLOWED:
                event.setCancelled(false);
                jobEvent = new JobProtectionInteractEvent(player, event.getClickedBlock().getLocation(), event.getClickedBlock(), event.getItem(), false);
                TheGaffer.getServerInstance().getPluginManager().callEvent(jobEvent);
                return;
            default:
                event.setCancelled(true);
                break;
        }
        player.sendMessage(ChatColor.DARK_RED + buildProtection.getMessage());
        jobEvent = new JobProtectionInteractEvent(player, event.getClickedBlock().getLocation(), event.getClickedBlock(), event.getItem(), true);
        TheGaffer.getServerInstance().getPluginManager().callEvent(jobEvent);
    }
}
