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

import com.mcmiddleearth.thegaffer.events.JobProtectionBlockBreakEvent;
import com.mcmiddleearth.thegaffer.events.JobProtectionBlockPlaceEvent;
import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.storage.JobDatabase;
import com.mcmiddleearth.thegaffer.utilities.StatsManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/** Counts successful, in-job, in-bounds block actions into StatsManager. */
public class StatsListener implements Listener {

    @EventHandler
    public void onPlace(JobProtectionBlockPlaceEvent event) {
        Job job = countableJob(event.getPlayer(), event.getLocation(), event.isBlocked());
        if (job != null) {
            StatsManager.recordPlace(job.getName(), event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onBreak(JobProtectionBlockBreakEvent event) {
        Job job = countableJob(event.getPlayer(), event.getLocation(), event.isBlocked());
        if (job != null) {
            StatsManager.recordBreak(job.getName(), event.getPlayer().getUniqueId());
        }
    }

    /** Returns the player's active job iff the action was successful and inside that job's bounds. */
    private Job countableJob(Player player, Location loc, boolean blocked) {
        if (blocked || player == null || loc == null) {
            return null;
        }
        Job job = JobDatabase.getJobWorking(player);
        if (job == null || job.getBounds() == null) {
            return null;
        }
        if (!job.getBounds().contains(loc.getBlockX(), loc.getBlockZ())) {
            return null;
        }
        return job;
    }
}
