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
package com.mcmiddleearth.thegaffer.storage;

import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.events.JobEndEvent;
import com.mcmiddleearth.thegaffer.events.JobStartEvent;
import com.mcmiddleearth.thegaffer.utilities.Util;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.TreeMap;

public class JobDatabase {

    private static final TreeMap<String, Job> activeJobs = new TreeMap<>();
    private static final TreeMap<String, Job> inactiveJobs = new TreeMap<>();

    /**
     * Loads every job file from the {@code jobs/} folder into memory. Running
     * jobs are re-activated (bounds regenerated, listener registered, owner
     * timeout rescheduled); stopped jobs go to the inactive map. An unreadable
     * file is logged and skipped rather than aborting startup.
     */
    public static int loadJobs() {
        int count = 0;
        File jobFolder = new File(TheGaffer.getPluginDataFolder(),
                TheGaffer.getFileSeperator() + "jobs");
        if (!jobFolder.exists()) {
            jobFolder.mkdirs();
            return 0;
        }
        File[] files = jobFolder.listFiles((dir, fname) -> fname.endsWith(TheGaffer.getFileExtension()));
        if (files == null) {
            return 0;
        }
        for (File jFile : files) {
            if (jFile.isDirectory()) {
                continue;
            }
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(jFile);
                Job job = JobStorage.fromYaml(config);
                if (job.getName() == null) {
                    Util.severe("Skipping job file without a name: " + jFile.getName());
                    continue;
                }
                if (job.isRunning()) {
                    job.generateBounds();
                    activeJobs.put(job.getName(), job);
                    TheGaffer.getServerInstance().getPluginManager()
                            .registerEvents(job, TheGaffer.getPluginInstance());
                    TheGaffer.scheduleOwnerTimeout(job);
                } else {
                    inactiveJobs.put(job.getName(), job);
                }
                job.setDirty(false);
                count++;
            } catch (Exception ex) {
                Util.severe("Failed to load job file " + jFile.getName() + ": " + ex.getMessage());
            }
        }
        return count;
    }

    /** Persists a single job (file write happens off-thread). */
    public static void saveJob(Job j) {
        writeJobFile(j, true);
        j.setDirty(false);
    }

    /** Persists every dirty job. Pass {@code async=false} only on shutdown. */
    public static void saveAllDirty(boolean async) {
        for (Job j : activeJobs.values()) {
            if (j.isDirty()) {
                writeJobFile(j, async);
                j.setDirty(false);
            }
        }
        for (Job j : inactiveJobs.values()) {
            if (j.isDirty()) {
                writeJobFile(j, async);
                j.setDirty(false);
            }
        }
    }

    // Builds the YAML snapshot on the calling (main) thread, then performs the
    // blocking file write either async (normal runtime) or sync (on disable,
    // when the scheduler can no longer run async tasks). The config is a
    // snapshot, so the async write never touches live job state.
    private static void writeJobFile(Job j, boolean async) {
        final YamlConfiguration config = JobStorage.toYaml(j);
        final File target = j.getFile();
        final String jobName = j.getName();
        Runnable write = () -> {
            File dir = target.getParentFile();
            if (dir != null && !dir.exists()) {
                dir.mkdirs();
            }
            File tmp = new File(dir, target.getName() + ".new");
            try {
                config.save(tmp);
                if (target.exists()) {
                    target.delete();
                }
                tmp.renameTo(target);
            } catch (IOException ex) {
                Util.severe("Failed to save job " + jobName + ": " + ex.getMessage());
            }
        };
        if (async) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    write.run();
                }
            }.runTaskAsynchronously(TheGaffer.getPluginInstance());
        } else {
            write.run();
        }
    }

    public static boolean activateJob(Job j) {
        if (activeJobs.containsKey(j.getName())) {
            return false;
        }
        j.generateBounds();
        activeJobs.put(j.getName(), j);
        TheGaffer.getServerInstance().getPluginManager().registerEvents(j, TheGaffer.getPluginInstance());
        j.setDirty(true);
        saveJob(j);
        TheGaffer.getServerInstance().getPluginManager().callEvent(new JobStartEvent(j));
        return true;
    }

    public static boolean deactivateJob(Job j) {
        if (!activeJobs.containsKey(j.getName())) {
            return false;
        }
        j.setRunning(false);
        j.setEndTime(System.currentTimeMillis());
        j.setDirty(true);
        activeJobs.remove(j.getName());
        inactiveJobs.put(j.getName(), j);
        HandlerList.unregisterAll(j);
        saveJob(j);
        new BukkitRunnable() {
            @Override
            public void run() {
                TheGaffer.getServerInstance().getPluginManager().callEvent(new JobEndEvent(j));
            }
        }.runTask(TheGaffer.getPluginInstance());
        return true;
    }

    public static Job getJobWorking(OfflinePlayer p) {
        for (Job job : activeJobs.values()) {
            if (job.isPlayerWorking(p) || job.isPlayerHelper(p) || p.getName().equals(job.getOwner())) {
                return job;
            }
        }
        return null;
    }

    public static TreeMap<String, Job> getActiveJobs() {
        return activeJobs;
    }

    public static TreeMap<String, Job> getInactiveJobs() {
        return inactiveJobs;
    }
}
