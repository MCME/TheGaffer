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
import com.mcmiddleearth.thegaffer.events.JobEndEvent;
import com.mcmiddleearth.thegaffer.events.JobProtectionBlockBreakEvent;
import com.mcmiddleearth.thegaffer.events.JobProtectionBlockPlaceEvent;
import com.mcmiddleearth.thegaffer.events.JobProtectionHangingBreakEvent;
import com.mcmiddleearth.thegaffer.events.JobProtectionHangingPlaceEvent;
import com.mcmiddleearth.thegaffer.events.JobProtectionInteractEvent;
import com.mcmiddleearth.thegaffer.events.JobStartEvent;
import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.utilities.Util;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class JobEventListener implements Listener {

    @EventHandler
    public void onJobEnd(JobEndEvent event) {
        Job job = event.getJob();
        job.sendToAll(ChatColor.GRAY + "The " + job.getName() + " job has ended.");
        for (Player p : job.getWorkersAsPlayersArray()) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERDRAGON_FLAP, 0.8f, 1f);
        }
    }

    @EventHandler
    public void onJobStart(JobStartEvent event) {
        Job job = event.getJob();
        TheGaffer.getServerInstance().broadcastMessage(ChatColor.AQUA + job.getOwner() + ChatColor.GRAY + " has started a job called \"" + job.getName() + ChatColor.GRAY + "\"");
        for (Player p : TheGaffer.getServerInstance().getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.8f, 2f);
        }
    }

    @EventHandler
    public void onJobProtection(JobProtectionInteractEvent event) {
       //Util.info("Got event: " + event.getEventName() + "blocked: " + event.isBlocked());
    }

    @EventHandler
    public void onJobProtection(JobProtectionBlockPlaceEvent event) {
        //Util.info("Got event: " + event.getEventName() + "blocked: " + event.isBlocked());
    }

    @EventHandler
    public void onJobProtection(JobProtectionBlockBreakEvent event) {
        //Util.info("Got event: " + event.getEventName() + "blocked: " + event.isBlocked());
    }

    @EventHandler
    public void onJobProtection(JobProtectionHangingBreakEvent event) {
        //Util.info("Got event: " + event.getEventName() + "blocked: " + event.isBlocked());
    }

    @EventHandler
    public void onJobProtection(JobProtectionHangingPlaceEvent event) {
        //Util.info("Got event: " + event.getEventName() + "blocked: " + event.isBlocked());
    }
}
