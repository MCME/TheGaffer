package com.mcmiddleearth.thegaffer.utilities;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mcmiddleearth.thegaffer.storage.JobStats;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds the machine-readable {@code leaderboard.json} feed consumed by the
 * website / infographics.  Pure function — no Bukkit scheduling, no server
 * state; every input comes in as a parameter so the method is unit-testable.
 */
public final class StatsFeed {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final long WEEK_MS  = 86_400_000L * 7;
    private static final long MONTH_MS = 86_400_000L * 30;
    private static final int  TOP_N    = 10;

    private StatsFeed() { }

    /**
     * Builds the full leaderboard JSON string.
     *
     * @param records   all finished job records (may be empty)
     * @param players   all player aggregates (may be empty)
     * @param projects  all project aggregates (may be empty)
     * @param nowMillis wall-clock time used for streak computation and week/month windows
     * @return pretty-printed JSON string
     */
    public static String buildJson(
            List<JobStats> records,
            Collection<StatsManager.PlayerAggregate> players,
            Collection<StatsManager.ProjectAggregate> projects,
            long nowMillis) {

        JsonObject root = new JsonObject();
        root.addProperty("generatedAt", nowMillis);

        // ---- compute ranks once ----
        List<StatsManager.PlayerAggregate> byPlaced = new ArrayList<>(players);
        byPlaced.sort(Comparator.comparingLong(StatsManager.PlayerAggregate::getPlaced).reversed());

        List<StatsManager.PlayerAggregate> byTime = new ArrayList<>(players);
        byTime.sort(Comparator.comparingLong(StatsManager.PlayerAggregate::getActiveBuildMillis).reversed());

        Map<UUID, Integer> rankByPlaced = new HashMap<>();
        Map<UUID, Integer> rankByTime   = new HashMap<>();
        for (int i = 0; i < byPlaced.size(); i++) { rankByPlaced.put(byPlaced.get(i).getId(), i + 1); }
        for (int i = 0; i < byTime.size();   i++) { rankByTime.put(byTime.get(i).getId(),     i + 1); }

        int todayDay = (int)(nowMillis / 86_400_000L);

        // ---- players[] ----
        JsonArray playersArr = new JsonArray();
        for (StatsManager.PlayerAggregate a : players) {
            JsonObject p = new JsonObject();
            p.addProperty("uuid",                a.getId().toString());
            p.addProperty("name",                Util.nameOf(a.getId()));
            p.addProperty("placed",              a.getPlaced());
            p.addProperty("broke",               a.getBroke());
            p.addProperty("jobs",                a.getJobs());
            p.addProperty("activeBuildMillis",   a.getActiveBuildMillis());
            p.addProperty("tier",                StatsMetrics.tierForPlaced(a.getPlaced()));

            JsonArray badges = new JsonArray();
            for (String b : StatsMetrics.milestonesForPlaced(a.getPlaced())) { badges.add(b); }
            p.add("badges", badges);

            p.addProperty("rankByPlaced",        rankByPlaced.getOrDefault(a.getId(), 0));
            p.addProperty("rankByActiveTime",    rankByTime.getOrDefault(a.getId(), 0));
            p.addProperty("currentStreakDays",   StatsMetrics.currentStreak(a.getActiveDays(), todayDay));
            p.addProperty("longestStreakDays",   StatsMetrics.longestStreak(a.getActiveDays()));
            p.addProperty("distinctWorlds",      a.getWorlds().size());
            p.addProperty("distinctProjects",    a.getProjects().size());
            p.addProperty("coBuilders",          a.getCoBuilders().size());
            p.addProperty("biggestJobBlocks",    a.getBiggestJobBlocks());
            p.addProperty("longestSessionMillis",a.getLongestSessionMillis());
            p.addProperty("firstSeen",           a.getFirstSeen());
            p.addProperty("lastSeen",            a.getLastSeen());
            playersArr.add(p);
        }
        root.add("players", playersArr);

        // ---- projects[] ----
        JsonArray projectsArr = new JsonArray();
        for (StatsManager.ProjectAggregate a : projects) {
            JsonObject proj = new JsonObject();
            proj.addProperty("project",          a.getProject());
            proj.addProperty("jobCount",         a.getJobCount());
            proj.addProperty("builderCount",     a.getBuilderCount());
            proj.addProperty("placed",           a.getPlaced());
            proj.addProperty("broke",            a.getBroke());
            proj.addProperty("activeBuildMillis",a.getActiveBuildMillis());
            proj.addProperty("firstStart",       a.getFirstStart());
            proj.addProperty("lastEnd",          a.getLastEnd());

            // MVP: perBuilder entry with most placed
            Map.Entry<UUID, JobStats.BuilderStat> mvpEntry = null;
            for (Map.Entry<UUID, JobStats.BuilderStat> e : a.getPerBuilder().entrySet()) {
                if (mvpEntry == null || e.getValue().getPlaced() > mvpEntry.getValue().getPlaced()) {
                    mvpEntry = e;
                }
            }
            if (mvpEntry != null) {
                JsonObject mvp = new JsonObject();
                mvp.addProperty("uuid",   mvpEntry.getKey().toString());
                mvp.addProperty("name",   Util.nameOf(mvpEntry.getKey()));
                mvp.addProperty("placed", mvpEntry.getValue().getPlaced());
                proj.add("mvp", mvp);
            } else {
                proj.add("mvp", null);
            }

            // topBuilders: perBuilder sorted by placed desc, capped at 10
            List<Map.Entry<UUID, JobStats.BuilderStat>> topBuilders =
                    new ArrayList<>(a.getPerBuilder().entrySet());
            topBuilders.sort(Comparator.comparingInt(
                    (Map.Entry<UUID, JobStats.BuilderStat> e) -> e.getValue().getPlaced()).reversed());
            JsonArray topArr = new JsonArray();
            int shown = 0;
            for (Map.Entry<UUID, JobStats.BuilderStat> e : topBuilders) {
                if (shown++ >= TOP_N) { break; }
                JsonObject tb = new JsonObject();
                tb.addProperty("uuid",             e.getKey().toString());
                tb.addProperty("name",             Util.nameOf(e.getKey()));
                tb.addProperty("placed",           e.getValue().getPlaced());
                tb.addProperty("broke",            e.getValue().getBroke());
                tb.addProperty("activeBuildMillis",e.getValue().getActiveMillis());
                topArr.add(tb);
            }
            proj.add("topBuilders", topArr);
            projectsArr.add(proj);
        }
        root.add("projects", projectsArr);

        // ---- leaderboards ----
        JsonObject leaderboards = new JsonObject();
        leaderboards.add("allTimeByPlaced",    buildLeaderboardByPlaced(byPlaced, TOP_N, "placed"));
        leaderboards.add("allTimeByActiveTime",buildLeaderboardByTime(byTime,     TOP_N, "activeBuildMillis"));

        // week / month: filter records by endTime, sum placed per builder, top-N
        long weekCutoff  = nowMillis - WEEK_MS;
        long monthCutoff = nowMillis - MONTH_MS;
        leaderboards.add("weekByPlaced",  buildRecentLeaderboard(records, weekCutoff,  TOP_N));
        leaderboards.add("monthByPlaced", buildRecentLeaderboard(records, monthCutoff, TOP_N));
        root.add("leaderboards", leaderboards);

        // ---- jobs[] ----
        JsonArray jobsArr = new JsonArray();
        for (JobStats s : records) {
            JsonObject job = new JsonObject();
            job.addProperty("name",           s.getName());
            job.addProperty("owner",          s.getOwner() == null ? null : s.getOwner().toString());
            job.addProperty("project",        s.getProject());
            job.addProperty("world",          s.getWorld());
            job.addProperty("startTime",      s.getStartTime());
            job.addProperty("endTime",        s.getEndTime());
            job.addProperty("durationMillis", s.getDurationMillis());
            job.addProperty("placed",         s.getTotalPlaced());
            job.addProperty("broke",          s.getTotalBroke());

            JsonArray buildersArr = new JsonArray();
            for (Map.Entry<UUID, JobStats.BuilderStat> e : s.getBuilders().entrySet()) {
                JsonObject b = new JsonObject();
                b.addProperty("uuid",             e.getKey().toString());
                b.addProperty("name",             Util.nameOf(e.getKey()));
                b.addProperty("placed",           e.getValue().getPlaced());
                b.addProperty("broke",            e.getValue().getBroke());
                b.addProperty("activeBuildMillis",e.getValue().getActiveMillis());
                buildersArr.add(b);
            }
            job.add("builders", buildersArr);
            jobsArr.add(job);
        }
        root.add("jobs", jobsArr);

        return GSON.toJson(root);
    }

    // ---- helpers ----

    private static JsonArray buildLeaderboardByPlaced(List<StatsManager.PlayerAggregate> sorted, int n, String metric) {
        JsonArray arr = new JsonArray();
        int count = 0;
        for (StatsManager.PlayerAggregate a : sorted) {
            if (count++ >= n) { break; }
            JsonObject e = new JsonObject();
            e.addProperty("uuid",   a.getId().toString());
            e.addProperty("name",   Util.nameOf(a.getId()));
            e.addProperty(metric,   a.getPlaced());
            arr.add(e);
        }
        return arr;
    }

    private static JsonArray buildLeaderboardByTime(List<StatsManager.PlayerAggregate> sorted, int n, String metric) {
        JsonArray arr = new JsonArray();
        int count = 0;
        for (StatsManager.PlayerAggregate a : sorted) {
            if (count++ >= n) { break; }
            JsonObject e = new JsonObject();
            e.addProperty("uuid",   a.getId().toString());
            e.addProperty("name",   Util.nameOf(a.getId()));
            e.addProperty(metric,   a.getActiveBuildMillis());
            arr.add(e);
        }
        return arr;
    }

    /**
     * Filters finished records to those ending after {@code cutoff}, sums each
     * builder's placed count, and returns the top-N as a JsonArray.
     */
    private static JsonArray buildRecentLeaderboard(List<JobStats> records, long cutoff, int n) {
        Map<UUID, Long> placed = new HashMap<>();
        for (JobStats s : records) {
            if (s.getEndTime() < cutoff) { continue; }
            for (Map.Entry<UUID, JobStats.BuilderStat> e : s.getBuilders().entrySet()) {
                placed.merge(e.getKey(), (long) e.getValue().getPlaced(), Long::sum);
            }
        }
        List<Map.Entry<UUID, Long>> sorted = new ArrayList<>(placed.entrySet());
        sorted.sort(Map.Entry.<UUID, Long>comparingByValue().reversed());

        JsonArray arr = new JsonArray();
        int count = 0;
        for (Map.Entry<UUID, Long> e : sorted) {
            if (count++ >= n) { break; }
            JsonObject entry = new JsonObject();
            entry.addProperty("uuid",   e.getKey().toString());
            entry.addProperty("name",   Util.nameOf(e.getKey()));
            entry.addProperty("placed", e.getValue());
            arr.add(entry);
        }
        return arr;
    }
}
