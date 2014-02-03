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
package co.mcme.thegaffer.utilities;

import co.mcme.thegaffer.TheGaffer;
import co.mcme.thegaffer.storage.Job;
import co.mcme.thegaffer.storage.JobDatabase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import lombok.Getter;
import org.bukkit.OfflinePlayer;

public class CleanupUtil {

    @Getter
    private static HashMap<Job, Long> waiting = new HashMap();
    
    public static void scheduledCleanup() {
        for (Job job : waiting.keySet()) {
            if (!job.getOwnerAsOfflinePlayer().isOnline()) {
                Long since = waiting.get(job);
                Long For = System.currentTimeMillis() - since;
                Long max = (long) 250000;
                if (For >= max) {
                    Util.debug("Job: " + job.getName() + " awaiting new owner for " + For / 1000 + " seconds. Selecting new owner now.");
                    selectNewOwner(job);
                } else {
                     Util.debug("Job: " + job.getName() + " awaiting new owner for " + For / 1000 + " seconds. Selecting new owner in " + (max - For) / 1000 + " seconds.");
                }
            } else {
                Util.debug("Job: " + job.getName() + " owner was online, removed from cleanup queue");
                waiting.remove(job);
            }
        }
        Util.debug("Finished running job cleanup.");
    }
    
    public static void scheduledAbandonersCleanup() {
        Long max = 300000L;
        Util.debug("Starting to clean up abandoners");
        for (Job job : JobDatabase.getActiveJobs().values()) {
            if (job.getLeft().size() > 0) {
                for (OfflinePlayer p : job.getLeft().keySet()) {
                    Long since = job.getLeft().get(p);
                    Long For = System.currentTimeMillis() - since;
                    if (For >= max) {
                        Util.debug("Player: " + p.getName() + " has been offfline for " + For / 1000 + " seconds. Removing from job.");
                        job.removeWorker(p);
                    } else {
                        Util.debug("Player: " + p.getName() + " has been offfline for " + For / 1000 + " seconds. Removing from job in " + (max - For) / 1000 + " seconds.");
                    }
                }
            }
        }
        Util.debug("Finished cleaning up abandoners");
    }
    
    public static void selectNewOwner(Job job) {
        ArrayList<OfflinePlayer> possibles = new ArrayList();
        for (String name : job.getHelpers()) {
            OfflinePlayer p = TheGaffer.getServerInstance().getOfflinePlayer(name);
            if (p.isOnline()) {
                possibles.add(p);
            }
        }
        if (possibles.size() > 0) {
            int Min = 0;
            int Max = possibles.size() - 1;
            int index = Min + (int) (Math.random() * ((Max - Min) + 1));
            Collections.shuffle(possibles);
            OfflinePlayer choice = possibles.get(index);
            job.addHelper(TheGaffer.getServerInstance().getOfflinePlayer(job.getOwner()));
            job.setOwner(choice.getName());
            Util.debug("Selecting " + choice.getName() + " as " + job.getName() + "'s new owner.");
        } else {
            Util.debug("No new owner found for " + job.getName() + ". Disabling job.");
            JobDatabase.deactivateJob(job);
        }
    }
}
