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
import com.mcmiddleearth.thegaffer.utilities.StatsManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/** Counts successful, in-job, in-bounds block actions into StatsManager. */
public class StatsListener implements Listener {

    @EventHandler
    public void onPlace(JobProtectionBlockPlaceEvent event) {
        if (!event.isBlocked()) {
            StatsManager.recordBuild(event.getPlayer(), event.getLocation(), true);
        }
    }

    @EventHandler
    public void onBreak(JobProtectionBlockBreakEvent event) {
        if (!event.isBlocked()) {
            StatsManager.recordBuild(event.getPlayer(), event.getLocation(), false);
        }
    }
}
