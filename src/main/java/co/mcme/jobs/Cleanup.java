package co.mcme.jobs;

import co.mcme.jobs.util.Util;
import java.util.ArrayList;
import java.util.Collections;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class Cleanup {

    public static void scheduledCleanup() {
        for (Job job : Jobs.timedout_waiting.keySet()) {
            if (!job.getAdmin().isOnline()) {
                Long waitingSince = Jobs.timedout_waiting.get(job);
                Long waitingFor = System.currentTimeMillis() - waitingSince;
                Long max_wait = Long.valueOf(250000);
                if (waitingFor >= max_wait) {
                    Util.debug("Job: " + job.getName() + " awaiting new admin for " + waitingFor / 1000 + " seconds. Selecting new admin now.");
                    selectNewAdmin(job);
                } else {
                    Util.debug("Job: " + job.getName() + " awaiting new admin for " + waitingFor / 1000 + " seconds. Selecting new admin in " + (max_wait - waitingFor) / 1000 + " seconds.");
                }
            } else {
                Util.debug(job.getName() + "'s admin was online, removed from cleanup queue");
                Jobs.timedout_waiting.remove(job);
            }
        }
        Util.debug("Cleanup complete");
    }

    private static void selectNewAdmin(Job job) {
        ArrayList<OfflinePlayer> possibles = new ArrayList();
        for (String name : job.getHelpers()) {
            OfflinePlayer p = Bukkit.getOfflinePlayer(name);
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
            job.addHelper(job.getAdmin());
            job.setAdmin(choice);
            Util.debug("Selecting " + choice.getName() + " as " + job.getName() + "'s new admin.");
        } else {
            Util.debug("No new admin found for " + job.getName() + ". Disabling job.");
            Jobs.disableJob(job);
        }
    }
}
