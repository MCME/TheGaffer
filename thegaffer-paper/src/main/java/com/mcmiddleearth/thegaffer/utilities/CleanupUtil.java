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
package com.mcmiddleearth.thegaffer.utilities;

import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.storage.JobDatabase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.*;

public class CleanupUtil {

    private static HashMap<Job, Long> waiting = new HashMap<>();
    
    public static void scheduledCleanup() {
        List<Job> removeList = new ArrayList<>();
        for (Job job : waiting.keySet()) {
            if (!job.getOwnerAsOfflinePlayer().isOnline()) {
                Long since = waiting.get(job);
                Long For = System.currentTimeMillis() - since;
                Long max = (long) 250000;
                if (For >= max) {
                    Util.debug("Job: " + job.getName() + " awaiting new owner for " + For / 1000 + " seconds. Selecting new owner now.");
                    selectNewOwner(job);
                    // Its fate is now decided (helper promoted, or auto-paused) — drop it from the
                    // wait queue so the timer doesn't re-process it (and re-pause / re-announce it)
                    // every tick. If a promoted owner later leaves, onLeave re-queues it.
                    removeList.add(job);
                } else {
                     Util.debug("Job: " + job.getName() + " awaiting new owner for " + For / 1000 + " seconds. Selecting new owner in " + (max - For) / 1000 + " seconds.");
                }
            } else {
                Util.debug("Job: " + job.getName() + " owner was online, removed from cleanup queue");
                removeList.add(job);
            }
        }
        for(Job job: removeList) {
            waiting.remove(job);
        }
        Util.debug("Finished running job cleanup.");
    }
    
    public static void scheduledAbandonersCleanup() {
        Long max = 300000L;
        List<OfflinePlayer> removeList = new ArrayList<>();
        Util.debug("Starting to clean up abandoners");
        for (Job job : JobDatabase.getActiveJobs().values()) {
            if (job.getLeft().size() > 0) {
                for (UUID uuid : job.getLeft().keySet()) {
                    OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
                    if (p.isOnline()) {
                        Util.debug("Player: " + p.getName() + " was scheduled for abandonment, but is now online and was removed from abandoners list.");
                        removeList.add(p);
                    } else {
                        Long since = job.getLeft().get(p.getUniqueId());
                        Long For = System.currentTimeMillis() - since;
                        if (For >= max) {
                            Util.debug("Player: " + p.getName() + " has been offfline for " + For / 1000 + " seconds. Removing from job.");
                            job.removeWorker(p, "worker abandoned");
                            removeList.add(p);
                        } else {
                            Util.debug("Player: " + p.getName() + " has been offfline for " + For / 1000 + " seconds. Removing from job in " + (max - For) / 1000 + " seconds.");
                        }
                    }
                }
                for(OfflinePlayer player: removeList) {
                    job.getLeft().remove(player.getUniqueId());
                }
            }
        }
        Util.debug("Finished cleaning up abandoners");
    }
    
    public static void selectNewOwner(Job job) {
        ArrayList<OfflinePlayer> possibles = new ArrayList<>();
        for (UUID name : job.getHelpers()) {
            OfflinePlayer p = TheGaffer.getServerInstance().getOfflinePlayer(name);
            if (p.isOnline()) {
                possibles.add(p);
            }
        }
        if (possibles.size() > 0) {
            // A real shuffle randomises order in place, so the first element is a uniformly
            // random pick. (The old code computed a random index against the UNSHUFFLED length
            // and then shuffled — needlessly indirect; .get(0) after the shuffle is enough.)
            Collections.shuffle(possibles);
            OfflinePlayer choice = possibles.get(0);
            job.addHelper(TheGaffer.getServerInstance().getOfflinePlayer(job.getOwner()));
            job.setOwner(choice.getUniqueId());
            Util.debug("Selecting " + choice.getName() + " as " + job.getName() + "'s new owner.");
        } else {
            // No helper online to promote: instead of archiving the job (the old behaviour),
            // pause it and flag the pause as automatic. The job stays ACTIVE so it can be
            // auto-resumed when the owner or a helper rejoins (see PlayerListener.onJoin).
            Util.debug("No new owner found for " + job.getName() + ". Auto-pausing job until owner/helper returns.");
            job.setPaused(true);
            job.setAutoPaused(true);
            job.setDirty(true);
            job.sendToAll(Component.text("The job has been paused because no owner or helper is online. "
                    + "It will resume automatically when one returns.", NamedTextColor.YELLOW));
        }
    }

    public static HashMap<Job, Long> getWaiting() {
        return waiting;
    }
}
