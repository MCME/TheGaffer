package com.mcmiddleearth.thegaffer.utilities;

import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.storage.JobDatabase;
import com.mcmiddleearth.thegaffer.storage.JobStats;
import com.mcmiddleearth.thegaffer.storage.JobStatsStorage;
import com.mcmiddleearth.thegaffer.storage.Project;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
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

    public static class ProjectAggregate {
        private final String project;
        private int jobCount;
        private final Set<UUID> builders = new HashSet<>();
        private long placed;
        private long broke;
        private long durationMillis;
        private long firstStart = Long.MAX_VALUE;
        private long lastEnd;
        private final Map<UUID, JobStats.BuilderStat> perBuilder = new HashMap<>();
        private final List<String> jobNames = new ArrayList<>();

        public ProjectAggregate(String project) { this.project = project; }

        public String getProject() { return project; }
        public int getJobCount() { return jobCount; }
        public int getBuilderCount() { return builders.size(); }
        public long getPlaced() { return placed; }
        public long getBroke() { return broke; }
        public long getDurationMillis() { return durationMillis; }
        public long getFirstStart() { return firstStart == Long.MAX_VALUE ? 0L : firstStart; }
        public long getLastEnd() { return lastEnd; }
        public Map<UUID, JobStats.BuilderStat> getPerBuilder() { return java.util.Collections.unmodifiableMap(perBuilder); }
        public List<String> getJobNames() { return java.util.Collections.unmodifiableList(jobNames); }
        public boolean isEmpty() { return jobCount == 0; }
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

    /**
     * Unified counting path for a successful, in-bounds block action: resolves the player's
     * active job and folds one place ({@code place == true}) or break into its live stats.
     * No-op if the player isn't working a job, the job has no bounds, or the location is
     * outside those bounds. <b>Assumes the action is allowed</b> — the caller (the block-event
     * listener, or a cooperating plugin via {@link TheGaffer#recordExternalBuild}) is
     * responsible for filtering blocked events. Main thread.
     */
    public static void recordBuild(org.bukkit.entity.Player player, org.bukkit.Location location, boolean place) {
        if (player == null || location == null) { return; }
        Job job = JobDatabase.getJobWorking(player);
        if (job == null || job.getBounds() == null) { return; }
        if (!job.getBounds().contains(location.getBlockX(), location.getBlockZ())) { return; }
        if (place) {
            recordPlace(job.getName(), player.getUniqueId());
        } else {
            recordBreak(job.getName(), player.getUniqueId());
        }
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

    /**
     * Rolls up every job belonging to {@code project} (canonical match) — finished
     * records on disk plus in-progress live entries — into a single aggregate.
     * Returns an empty aggregate (all zeros) when no job matches; this also serves
     * the orphan case (a name with stats but no registry record).
     */
    public static ProjectAggregate getProjectAggregate(String project) {
        String canon = Project.canonical(project);
        ProjectAggregate agg = new ProjectAggregate(project);
        // Finished records live directly in statsDir(); readAll() does not recurse into the
        // active/ subdir, and finish() removes a job from `live` before writing its record,
        // so no job is folded twice.
        List<JobStats> all = JobStatsStorage.readAll(statsDir());
        all.addAll(live.values());
        for (JobStats s : all) {
            if (!Project.canonical(s.getProject()).equals(canon)) { continue; }
            foldIntoProject(agg, s);
        }
        return agg;
    }

    private static void foldIntoProject(ProjectAggregate agg, JobStats s) {
        agg.jobCount += 1;
        agg.jobNames.add(s.getName());
        agg.builders.addAll(s.getParticipants());
        agg.placed += s.getTotalPlaced();
        agg.broke += s.getTotalBroke();
        agg.durationMillis += s.getDurationMillis();
        if (s.getStartTime() > 0 && s.getStartTime() < agg.firstStart) { agg.firstStart = s.getStartTime(); }
        if (s.getEndTime() > agg.lastEnd) { agg.lastEnd = s.getEndTime(); }
        for (Map.Entry<UUID, JobStats.BuilderStat> e : s.getBuilders().entrySet()) {
            JobStats.BuilderStat acc = agg.perBuilder.computeIfAbsent(e.getKey(), k -> new JobStats.BuilderStat());
            acc.addPlaced(e.getValue().getPlaced());
            acc.addBroke(e.getValue().getBroke());
        }
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
        // Any running job without an active snapshot (first deploy of stats, or a
        // crash before the first flush) gets fresh empty counters so counting works
        // this session — matches the spec's "counting starts from now".
        for (Job job : JobDatabase.getActiveJobs().values()) {
            if (!live.containsKey(job.getName())) {
                begin(job);
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

    /** Renders a registered project's metadata and rolled-up stats as a chat Component. */
    public static Component renderProjectStats(Project p, ProjectAggregate a) {
        Component out = Component.text(p.getName(), NamedTextColor.GOLD)
                .append(Component.text(" [" + p.getStatus().name().toLowerCase() + "]", NamedTextColor.GRAY));
        if (p.getDescription() != null && !p.getDescription().isEmpty()) {
            out = out.append(Component.newline())
                    .append(Component.text("Description: ", NamedTextColor.GRAY))
                    .append(Component.text(p.getDescription(), NamedTextColor.WHITE));
        }
        if (p.getGoal() != null && !p.getGoal().isEmpty()) {
            out = out.append(Component.newline())
                    .append(Component.text("Goal: ", NamedTextColor.GRAY))
                    .append(Component.text(p.getGoal(), NamedTextColor.WHITE));
        }
        out = out.append(Component.newline())
                .append(Component.text("Lead: ", NamedTextColor.GRAY))
                .append(Component.text(Util.nameOf(p.getLead()), NamedTextColor.AQUA));
        if (!p.getManagers().isEmpty()) {
            StringBuilder mgrs = new StringBuilder();
            for (UUID m : p.getManagers()) { mgrs.append(Util.nameOf(m)).append(" "); }
            out = out.append(Component.newline())
                    .append(Component.text("Managers: ", NamedTextColor.GRAY))
                    .append(Component.text(mgrs.toString().trim(), NamedTextColor.AQUA));
        }
        out = out.append(Component.newline()).append(renderProjectTotals(a));
        // Jobs list (distinct names, same canonical-match set as the aggregate)
        List<String> names = new ArrayList<>(new LinkedHashSet<>(a.getJobNames()));
        out = out.append(Component.newline())
                .append(Component.text("Job list: ", NamedTextColor.GRAY));
        if (names.isEmpty()) {
            out = out.append(Component.text("none", NamedTextColor.AQUA));
        } else {
            for (int i = 0; i < names.size(); i++) {
                String jn = names.get(i);
                out = out.append(Msg.button(jn, NamedTextColor.AQUA,
                        "/job info " + jn, "Click to view job info"));
                if (i < names.size() - 1) {
                    out = out.append(Component.text(", ", NamedTextColor.GRAY));
                }
            }
        }
        return out;
    }

    /** Renders stats for a project name that has records but no registry entry. */
    public static Component renderOrphanProjectStats(String name, ProjectAggregate a) {
        return Component.text(name, NamedTextColor.GOLD)
                .append(Component.text(" (no project record)", NamedTextColor.DARK_GRAY))
                .append(Component.newline())
                .append(renderProjectTotals(a));
    }

    private static Component renderProjectTotals(ProjectAggregate a) {
        Component out = Component.text("Jobs: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(a.getJobCount()), NamedTextColor.AQUA))
                .append(Component.text("   Builders: ", NamedTextColor.GRAY))
                .append(Component.text(String.valueOf(a.getBuilderCount()), NamedTextColor.AQUA))
                .append(Component.newline())
                .append(Component.text("Blocks: ", NamedTextColor.GRAY))
                .append(Component.text(a.getPlaced() + " placed, " + a.getBroke() + " broken", NamedTextColor.AQUA))
                .append(Component.newline())
                .append(Component.text("Total build time: ", NamedTextColor.GRAY))
                .append(Component.text(formatDuration(a.getDurationMillis()), NamedTextColor.AQUA));
        List<Map.Entry<UUID, JobStats.BuilderStat>> top = new ArrayList<>(a.getPerBuilder().entrySet());
        top.sort(Comparator.comparingInt((Map.Entry<UUID, JobStats.BuilderStat> e) -> e.getValue().getPlaced()).reversed());
        int shown = 0;
        for (Map.Entry<UUID, JobStats.BuilderStat> e : top) {
            if (shown++ >= 5) { break; }
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

    /** RFC-4180 minimal CSV escape: wraps in quotes when the value contains a comma, quote, or line break. */
    private static String csv(String v) {
        if (v == null) { return ""; }
        return v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")
                ? "\"" + v.replace("\"", "\"\"") + "\"" : v;
    }

    /**
     * Writes all finished records to {@code stats/export-<stamp>.csv} and returns the file.
     * Returns {@code null} if the write fails (error already logged).
     */
    public static File exportAll(long stamp) {
        File out = new File(statsDir(), "export-" + stamp + ".csv");
        return writeCsvFile(out, JobStatsStorage.readAll(statsDir()));
    }

    /**
     * Writes only the finished records belonging to {@code projectName} (canonical match)
     * to {@code stats/export-<safeProjectName>-<stamp>.csv} and returns the file.
     * Returns {@code null} if the write fails or if there are no matching rows (error already logged).
     */
    public static File exportProject(String projectName, long stamp) {
        String canon = Project.canonical(projectName);
        // Derive a filesystem-safe name: replace any character that isn't alphanumeric, hyphen, or
        // apostrophe with an underscore (project names allow spaces — keep them readable as underscores).
        String safeName = projectName.replaceAll("[^A-Za-z0-9'\\-]", "_");
        File out = new File(statsDir(), "export-" + safeName + "-" + stamp + ".csv");
        List<JobStats> filtered = new ArrayList<>();
        for (JobStats s : JobStatsStorage.readAll(statsDir())) {
            if (Project.canonical(s.getProject()).equals(canon)) {
                filtered.add(s);
            }
        }
        return writeCsvFile(out, filtered);
    }

    /**
     * Shared CSV file writer used by both {@link #exportAll} and {@link #exportProject}.
     * Creates the parent directory if needed; uses explicit UTF-8. Returns the file on
     * success, or {@code null} on failure (error already logged).
     */
    private static File writeCsvFile(File out, List<JobStats> rows) {
        out.getParentFile().mkdirs(); // no-op if it already exists; avoids a misleading "export failed" on a fresh install
        // Explicit UTF-8 so non-ASCII player/project names survive on a Windows server
        // (the platform-default FileWriter would use windows-1252).
        try (Writer w = new OutputStreamWriter(new FileOutputStream(out), StandardCharsets.UTF_8)) {
            w.write(toCsv(rows));
        } catch (IOException ex) {
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
