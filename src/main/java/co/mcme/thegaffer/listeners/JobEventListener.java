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
package co.mcme.thegaffer.listeners;

import co.mcme.thegaffer.TheGaffer;
import co.mcme.thegaffer.events.JobEndEvent;
import co.mcme.thegaffer.events.JobStartEvent;
import co.mcme.thegaffer.storage.Job;
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
            p.playSound(p.getLocation(), Sound.ENDERDRAGON_WINGS, 10, 2);
        }
    }

    @EventHandler
    public void onJobStart(JobStartEvent event) {
        Job job = event.getJob();
        TheGaffer.getServerInstance().broadcastMessage(ChatColor.AQUA + job.getOwner() + ChatColor.GRAY + " has started a job called \"" + job.getName() + ChatColor.GRAY + "\"");
        for (Player p : TheGaffer.getServerInstance().getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.WITHER_DEATH, 10, 2);
        }
    }
}
