package com.mcmiddleearth.thegaffer.utilities;

import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.storage.JobDatabase;
import com.mcmiddleearth.thegaffer.storage.JobStats;
import com.mcmiddleearth.thegaffer.storage.JobStatsStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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

    /** Test seam: when non-null, statsDir() returns this instead of the real plugin data folder. */
    static File statsDirOverride = null;

    private StatsManager() { }

    private static File statsDir() {
        if (statsDirOverride != null) { return statsDirOverride; }
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
            // Synchronous: job-end is infrequent (not a hot path), and the record must be
            // durable + visible to the end-of-job recap immediately. The frequent periodic
            // flushActive stays async.
            JobStatsStorage.save(s, statsDir(), false);
            new File(activeDir(), JobStatsStorage.recordFileName(jobName, 0L)).delete();
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

    /** Finds a player's aggregate by case-insensitive name among players with recorded stats; null if none. */
    public static PlayerAggregate findPlayerTotalsByName(String name) {
        for (PlayerAggregate a : aggregate.values()) {
            if (name.equalsIgnoreCase(Util.nameOf(a.getId()))) {
                return a;
            }
        }
        return null;
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

    // ---- active snapshot (flush / reload) ----

    /** Snapshots every live job's counters to stats/active/{@code <job>-0.yml}. */
    public static void flushActive(boolean async) {
        for (JobStats s : live.values()) {
            JobStatsStorage.save(s, activeDir(), async); // endTime == 0 => "<job>-0.yml"
        }
    }

    /**
     * Reloads in-progress counters for jobs that are still running (call on enable, after jobs load).
     * Only restores entries whose job name appears in {@code JobDatabase.getActiveJobs()}.
     */
    public static void loadActive() {
        for (JobStats s : JobStatsStorage.readAll(activeDir())) {
            if (JobDatabase.getActiveJobs().containsKey(s.getName())) {
                live.put(s.getName(), s);
            }
        }
    }

    // ---- test seams (no Bukkit Job required) ----

    static void beginForTest(String name, UUID owner, String project, String world,
                             int cx, int cz, int radius, long startTime) {
        beginInternal(name, owner, project, world, cx, cz, radius, startTime);
    }

    static JobStats finishForTest(String name, long endTime) {
        return finishInternal(name, endTime, false);
    }

    /** Test seam for loadActive: unconditionally restores all active snapshots matching the given name. */
    static void loadActiveForTest(String name) {
        for (JobStats s : JobStatsStorage.readAll(activeDir())) {
            if (s.getName().equals(name)) { live.put(s.getName(), s); }
        }
    }

    // ---- display / query helpers ----

    /**
     * Returns the live entry for {@code jobName} if one exists; otherwise reads finished
     * records from disk and returns the one with the highest endTime (i.e. newest).
     */
    public static JobStats findJobStats(String jobName) {
        JobStats liveEntry = live.get(jobName);
        if (liveEntry != null) { return liveEntry; }
        JobStats newest = null;
        for (JobStats s : JobStatsStorage.readAll(statsDir())) {
            if (s.getName().equals(jobName)
                    && (newest == null || s.getEndTime() > newest.getEndTime())) {
                newest = s;
            }
        }
        return newest;
    }

    /** Renders a finished (or live) job's stats as a chat Component. */
    public static Component renderJobStats(JobStats s) {
        Component out = Component.text(s.getName(), NamedTextColor.AQUA)
                .append(Component.text(" stats", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("Owner: ", NamedTextColor.GRAY))
                .append(Component.text(Util.nameOf(s.getOwner()), NamedTextColor.AQUA))
                .append(Component.newline())
                .append(Component.text("Participants: ", NamedTextColor.GRAY))
                .append(Component.text(String.valueOf(s.getParticipants().size()), NamedTextColor.AQUA))
                .append(Component.newline())
                .append(Component.text("Duration: ", NamedTextColor.GRAY))
                .append(Component.text(formatDuration(s.getDurationMillis()), NamedTextColor.AQUA))
                .append(Component.newline())
                .append(Component.text("Location: ", NamedTextColor.GRAY))
                .append(Component.text(s.getWorld() + " (" + s.getCenterX() + ", " + s.getCenterZ() + ")", NamedTextColor.AQUA))
                .append(Component.newline())
                .append(Component.text("Blocks: ", NamedTextColor.GRAY))
                .append(Component.text(s.getTotalPlaced() + " placed, " + s.getTotalBroke() + " broken", NamedTextColor.AQUA));
        for (Map.Entry<UUID, JobStats.BuilderStat> e : s.getBuilders().entrySet()) {
            out = out.append(Component.newline())
                    .append(Component.text("  " + Util.nameOf(e.getKey()) + ": ", NamedTextColor.GRAY))
                    .append(Component.text(e.getValue().getPlaced() + " / " + e.getValue().getBroke(), NamedTextColor.AQUA));
        }
        return out;
    }

    /** Converts a millisecond duration to a human-readable {@code Xh Ym} string. */
    public static String formatDuration(long millis) {
        long mins = millis / 60000;
        return (mins / 60) + "h " + (mins % 60) + "m";
    }

    // ---- CSV export ----

    /**
     * Renders a list of finished (or live) job stats as a CSV string.
     * One row per (job, builder); jobs with no builder rows are omitted.
     * Header: job,owner,project,world,centerX,centerZ,startTime,endTime,builder,placed,broke
     */
    public static String toCsv(List<JobStats> all) {
        StringBuilder sb = new StringBuilder("job,owner,project,world,centerX,centerZ,startTime,endTime,builder,placed,broke");
        for (JobStats s : all) {
            for (Map.Entry<UUID, JobStats.BuilderStat> e : s.getBuilders().entrySet()) {
                sb.append("\n")
                  .append(csv(s.getName())).append(",").append(csv(Util.nameOf(s.getOwner()))).append(",")
                  .append(csv(s.getProject())).append(",").append(csv(s.getWorld())).append(",")
                  .append(s.getCenterX()).append(",").append(s.getCenterZ()).append(",")
                  .append(s.getStartTime()).append(",").append(s.getEndTime()).append(",")
                  .append(csv(Util.nameOf(e.getKey()))).append(",")
                  .append(e.getValue().getPlaced()).append(",").append(e.getValue().getBroke());
            }
        }
        return sb.toString();
    }

    /** RFC-4180 minimal CSV escape: wraps in quotes only when the value contains a comma or quote. */
    private static String csv(String v) {
        if (v == null) { return ""; }
        return v.contains(",") || v.contains("\"") ? "\"" + v.replace("\"", "\"\"") + "\"" : v;
    }

    /**
     * Writes all finished records to {@code stats/export-<stamp>.csv} and returns the file.
     * Returns {@code null} if the write fails (error already logged).
     */
    public static File exportAll(long stamp) {
        File out = new File(statsDir(), "export-" + stamp + ".csv");
        try (java.io.FileWriter w = new java.io.FileWriter(out)) {
            w.write(toCsv(JobStatsStorage.readAll(statsDir())));
        } catch (java.io.IOException ex) {
            Util.severe("Stats export failed: " + ex.getMessage());
            return null;
        }
        return out;
    }

    /** Plain-text recap for the Discord job-end post. Pure (no Bukkit/JDA), so it's unit-testable. */
    public static String buildDiscordSummary(JobStats s) {
        // Leading spaces match the onJobStart Discord recap layout.
        return "__**Recap:**__ **" + s.getName() + "**"
                + "\n        Builders: " + s.getParticipants().size()
                + "\n        Blocks: " + s.getTotalPlaced() + " placed, " + s.getTotalBroke() + " broken"
                + "\n        Duration: " + formatDuration(s.getDurationMillis());
    }
}
