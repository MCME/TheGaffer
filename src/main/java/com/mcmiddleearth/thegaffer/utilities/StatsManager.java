package com.mcmiddleearth.thegaffer.utilities;

import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.storage.JobStats;
import com.mcmiddleearth.thegaffer.storage.JobStatsStorage;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatsManager {

    private static final Map<String, JobStats> live = new HashMap<>();

    private StatsManager() { }

    private static File statsDir() {
        return new File(TheGaffer.getPluginDataFolder(), TheGaffer.getFileSeperator() + "stats");
    }

    private static File activeDir() {
        return new File(statsDir(), "active");
    }

    // ---- lifecycle ----

    public static void begin(Job job) {
        int cx = (int) job.getWarp().getX();
        int cz = (int) job.getWarp().getZ();
        Long startTime = job.getStartTime();
        beginInternal(job.getName(), job.getOwner(), job.getProjectname(), job.getWorld(),
                cx, cz, job.getJobRadius(), startTime == null ? System.currentTimeMillis() : startTime);
    }

    private static void beginInternal(String name, UUID owner, String project, String world,
                                      int cx, int cz, int radius, long startTime) {
        JobStats s = new JobStats(name, owner, project, world, cx, cz, radius, startTime, 0L);
        s.addParticipant(owner);
        live.put(name, s);
    }

    public static void onJoin(String jobName, UUID id) {
        JobStats s = live.get(jobName);
        if (s != null) { s.addParticipant(id); }
    }

    public static void recordPlace(String jobName, UUID id) {
        JobStats s = live.get(jobName);
        if (s != null) { s.recordPlace(id, 1); }
    }

    public static void recordBreak(String jobName, UUID id) {
        JobStats s = live.get(jobName);
        if (s != null) { s.recordBreak(id, 1); }
    }

    /** Finalizes a job: stamps endTime, persists the record, clears the live entry. */
    public static JobStats finish(Job job) {
        return finishInternal(job.getName(), System.currentTimeMillis(), true);
    }

    private static JobStats finishInternal(String jobName, long endTime, boolean persist) {
        JobStats s = live.remove(jobName);
        if (s == null) { return null; }
        s.setEndTime(endTime);
        if (persist) {
            JobStatsStorage.save(s, statsDir(), true);
            new File(activeDir(), jobName + "-0" + TheGaffer.getFileExtension()).delete();
        }
        return s;
    }

    public static JobStats getLive(String jobName) { return live.get(jobName); }

    public static void reset() { live.clear(); }

    // ---- test seams (no Bukkit Job required) ----

    static void beginForTest(String name, UUID owner, String project, String world,
                             int cx, int cz, int radius, long startTime) {
        beginInternal(name, owner, project, world, cx, cz, radius, startTime);
    }

    static JobStats finishForTest(String name, long endTime) {
        return finishInternal(name, endTime, false);
    }
}
