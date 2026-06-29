package com.mcmiddleearth.thegaffer.utilities;

import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.storage.JobStats;
import com.mcmiddleearth.thegaffer.storage.JobStatsStorage;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StatsManager {

    private static final Map<String, JobStats> live = new HashMap<>();

    public enum SortKey { PLACED, BROKE, ACTIVE }

    public static class PlayerAggregate {
        private final UUID id;
        private long placed;
        private long broke;
        private int jobs;
        private long durationMillis;
        public PlayerAggregate(UUID id) { this.id = id; }
        public UUID getId() { return id; }
        public long getPlaced() { return placed; }
        public long getBroke() { return broke; }
        public int getJobs() { return jobs; }
        public long getDurationMillis() { return durationMillis; }
    }

    private static final Map<UUID, PlayerAggregate> aggregate = new HashMap<>();

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
        ingest(s);
        return s;
    }

    public static JobStats getLive(String jobName) { return live.get(jobName); }

    public static void reset() { live.clear(); aggregate.clear(); }

    // ---- aggregate + queries ----

    /** Folds one finished record into the leaderboard aggregate. */
    public static void ingest(JobStats s) {
        for (UUID id : s.getParticipants()) {
            PlayerAggregate a = aggregate.computeIfAbsent(id, PlayerAggregate::new);
            a.jobs += 1;
            a.durationMillis += s.getDurationMillis();
        }
        for (Map.Entry<UUID, JobStats.BuilderStat> e : s.getBuilders().entrySet()) {
            PlayerAggregate a = aggregate.computeIfAbsent(e.getKey(), PlayerAggregate::new);
            a.placed += e.getValue().getPlaced();
            a.broke += e.getValue().getBroke();
        }
    }

    /** Rebuilds the aggregate from all finished records on disk (call on enable). */
    public static void loadAggregate() {
        aggregate.clear();
        for (JobStats s : JobStatsStorage.readAll(statsDir())) { ingest(s); }
    }

    public static PlayerAggregate getPlayerTotals(UUID id) {
        return aggregate.getOrDefault(id, new PlayerAggregate(id));
    }

    public static List<PlayerAggregate> getLeaderboard(SortKey key, int limit) {
        List<PlayerAggregate> all = new ArrayList<>(aggregate.values());
        Comparator<PlayerAggregate> cmp;
        switch (key) {
            case BROKE:  cmp = Comparator.comparingLong(PlayerAggregate::getBroke); break;
            case ACTIVE: cmp = Comparator.comparingInt(PlayerAggregate::getJobs); break;
            default:     cmp = Comparator.comparingLong(PlayerAggregate::getPlaced); break;
        }
        all.sort(cmp.reversed());
        return all.size() > limit ? all.subList(0, limit) : all;
    }

    // ---- test seams (no Bukkit Job required) ----

    static void beginForTest(String name, UUID owner, String project, String world,
                             int cx, int cz, int radius, long startTime) {
        beginInternal(name, owner, project, world, cx, cz, radius, startTime);
    }

    static JobStats finishForTest(String name, long endTime) {
        return finishInternal(name, endTime, false);
    }
}
