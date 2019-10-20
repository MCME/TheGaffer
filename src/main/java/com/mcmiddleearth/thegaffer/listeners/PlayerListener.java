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
import com.mcmiddleearth.thegaffer.storage.JobDatabase;
import com.mcmiddleearth.thegaffer.utilities.BuildProtection;
import com.mcmiddleearth.thegaffer.utilities.PermissionsUtil;
import com.mcmiddleearth.thegaffer.utilities.ProtectionUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().setGlowing(false);
        if (JobDatabase.getActiveJobs().size() > 0 && event.getPlayer().hasPermission(PermissionsUtil.getJoinPermission())) {
            event.getPlayer().sendMessage(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "There is a job running! Use /job check to find out what it is!");
            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 2f);
        }
    }
    
    private List<UUID> playersSwitchedToCreative = new ArrayList<>();
    
    @EventHandler(priority = EventPriority.LOW)
    public void playerMove(PlayerMoveEvent event) {
        if(event.getFrom().getBlock()!=event.getTo().getBlock()) {
            if(JobDatabase.getJobWorking(event.getPlayer())==null) {
                return;
            }
            Player player = event.getPlayer();
            if(player.hasPermission(PermissionsUtil.getIgnoreWorldProtection())) {
                playersSwitchedToCreative.remove(player.getUniqueId());
                return;
            }
            if(ProtectionUtil.getBuildProtection(player, player.getLocation()).equals(BuildProtection.ALLOWED)) {
                if(player.getGameMode()==GameMode.SURVIVAL) {
                    if(!playersSwitchedToCreative.contains(player.getUniqueId())) {
                        playersSwitchedToCreative.add(player.getUniqueId());
                    }
                    player.setGameMode(GameMode.CREATIVE);
                }
            }
            else {
                if(playersSwitchedToCreative.contains(player.getUniqueId())) {
                    boolean flying = false;
                    if(player.isFlying()) {
                        flying = true;  
                    }
                    player.setGameMode(GameMode.SURVIVAL);
                    if(TheGaffer.getPluginInstance().getConfig().getBoolean("enableFlight",true)) {
                        player.setAllowFlight(true);
                        player.setFlying(flying);
                    }
                    playersSwitchedToCreative.remove(player.getUniqueId());
                }
            }
        }
    }
    
    
}
