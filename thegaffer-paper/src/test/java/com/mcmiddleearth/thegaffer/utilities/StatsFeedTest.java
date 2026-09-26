package com.mcmiddleearth.thegaffer.utilities;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.storage.JobStats;
import com.mcmiddleearth.thegaffer.storage.JobStatsStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link StatsFeed#buildJson}.
 *
 * All data is passed in as parameters — no Bukkit scheduler, no disk I/O.
 * The only Bukkit dependency is {@code Util.nameOf(UUID)}, which calls
 * {@code Bukkit.getOfflinePlayer(id).getName()}, so we spin up MockBukkit
 * and register a real player so the name resolves correctly.
 */
class StatsFeedTest {

    @TempDir
    File tmp;

    private ServerMock server;
    private PlayerMock player;

    @BeforeEach
    void setUp() throws Exception {
        server = MockBukkit.mock();
        player = server.addPlayer();
        // Util.<clinit> needs TheGaffer.getServerInstance()
        setField("serverInstance", server);
        StatsManager.reset();
        StatsManager.statsDirOverride = tmp;
    }

    @AfterEach
    void tearDown() throws Exception {
        StatsManager.statsDirOverride = null;
        setField("serverInstance", null);
        MockBukkit.unmock();
    }

    private static void setField(String name, Object value) throws Exception {
        Field f = TheGaffer.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(null, value);
    }

    /**
     * Primary test: builds JSON from hand-made data and asserts top-level keys
     * plus presence of key player-level fields including {@code tier} and
     * {@code activeBuildMillis}.
     */
    @Test
    void buildJsonProducesRequiredTopLevelKeysAndPlayerFields() {
        UUID uid = player.getUniqueId();

        // Build a JobStats record and persist it to the tmp dir so
        // getAllProjectAggregates() (which reads from disk) can find it.
        JobStats record = new JobStats("river", uid, "Rohan", "world",
                100, -200, 50, 1_000_000L, 2_000_000L);
        record.setBuilderStat(uid, 1500, 300, 3_600_000L); // 1h active
        JobStatsStorage.save(record, tmp, false); // synchronous save

        // Ingest into StatsManager to get a real PlayerAggregate
        StatsManager.ingest(record);

        StatsManager.PlayerAggregate playerAgg = StatsManager.getPlayerTotals(uid);

        // getAllProjectAggregates reads finished records from tmp (via statsDirOverride)
        Collection<StatsManager.ProjectAggregate> projAggs = StatsManager.getAllProjectAggregates();

        long now = 2_500_000L; // some timestamp
        String json = StatsFeed.buildJson(
                List.of(record),
                List.of(playerAgg),
                projAggs,
                now);

        assertNotNull(json, "buildJson must return non-null");
        assertFalse(json.isEmpty(), "JSON must not be empty");

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        // Top-level keys
        assertTrue(root.has("players"),     "root must have 'players'");
        assertTrue(root.has("projects"),    "root must have 'projects'");
        assertTrue(root.has("leaderboards"),"root must have 'leaderboards'");
        assertTrue(root.has("jobs"),        "root must have 'jobs'");
        assertTrue(root.has("generatedAt"), "root must have 'generatedAt'");
        assertEquals(now, root.get("generatedAt").getAsLong());

        // players array — at least one entry
        JsonArray playersArr = root.getAsJsonArray("players");
        assertEquals(1, playersArr.size(), "One player aggregate => one entry");
        JsonObject p = playersArr.get(0).getAsJsonObject();
        assertTrue(p.has("uuid"),              "player must have uuid");
        assertTrue(p.has("name"),              "player must have name");
        assertTrue(p.has("placed"),            "player must have placed");
        assertTrue(p.has("broke"),             "player must have broke");
        assertTrue(p.has("activeBuildMillis"), "player must have activeBuildMillis");
        assertTrue(p.has("tier"),              "player must have tier");
        assertTrue(p.has("badges"),            "player must have badges");
        assertTrue(p.has("rankByPlaced"),      "player must have rankByPlaced");
        assertTrue(p.has("rankByActiveTime"),  "player must have rankByActiveTime");
        assertTrue(p.has("currentStreakDays"), "player must have currentStreakDays");
        assertTrue(p.has("longestStreakDays"), "player must have longestStreakDays");

        // Values
        assertEquals(uid.toString(), p.get("uuid").getAsString());
        assertEquals(1500L, p.get("placed").getAsLong());
        assertEquals(300L,  p.get("broke").getAsLong());
        assertTrue(p.get("activeBuildMillis").getAsLong() > 0,
                "activeBuildMillis must be positive");
        assertNotNull(p.get("tier").getAsString());
        assertFalse(p.get("tier").getAsString().isEmpty());

        // projects array — at least one entry
        JsonArray projArr = root.getAsJsonArray("projects");
        assertEquals(1, projArr.size(), "One project aggregate => one entry");
        JsonObject proj = projArr.get(0).getAsJsonObject();
        assertTrue(proj.has("project"),          "project must have 'project'");
        assertTrue(proj.has("jobCount"),         "project must have 'jobCount'");
        assertTrue(proj.has("builderCount"),     "project must have 'builderCount'");
        assertTrue(proj.has("placed"),           "project must have 'placed'");
        assertTrue(proj.has("broke"),            "project must have 'broke'");
        assertTrue(proj.has("activeBuildMillis"),"project must have 'activeBuildMillis'");
        assertTrue(proj.has("firstStart"),       "project must have 'firstStart'");
        assertTrue(proj.has("lastEnd"),          "project must have 'lastEnd'");
        assertTrue(proj.has("mvp"),              "project must have 'mvp'");
        assertTrue(proj.has("topBuilders"),      "project must have 'topBuilders'");

        // leaderboards object
        JsonObject lb = root.getAsJsonObject("leaderboards");
        assertTrue(lb.has("allTimeByPlaced"),    "leaderboards must have allTimeByPlaced");
        assertTrue(lb.has("allTimeByActiveTime"),"leaderboards must have allTimeByActiveTime");
        assertTrue(lb.has("weekByPlaced"),       "leaderboards must have weekByPlaced");
        assertTrue(lb.has("monthByPlaced"),      "leaderboards must have monthByPlaced");

        // jobs array — one entry
        JsonArray jobsArr = root.getAsJsonArray("jobs");
        assertEquals(1, jobsArr.size(), "One record => one entry in jobs[]");
        JsonObject job = jobsArr.get(0).getAsJsonObject();
        assertTrue(job.has("name"),           "job must have name");
        assertTrue(job.has("owner"),          "job must have owner");
        assertTrue(job.has("project"),        "job must have project");
        assertTrue(job.has("world"),          "job must have world");
        assertTrue(job.has("startTime"),      "job must have startTime");
        assertTrue(job.has("endTime"),        "job must have endTime");
        assertTrue(job.has("durationMillis"), "job must have durationMillis");
        assertTrue(job.has("placed"),         "job must have placed");
        assertTrue(job.has("broke"),          "job must have broke");
        assertTrue(job.has("builders"),       "job must have builders");
    }

    /**
     * Week/month leaderboard: a record within the week window should show up
     * in weekByPlaced but not if outside the 7-day window.
     */
    @Test
    void weekLeaderboardFiltersOnEndTime() {
        UUID uid = player.getUniqueId();
        long now = 86_400_000L * 100L; // day 100
        long inWindow  = now - 86_400_000L * 3;  // 3 days ago — inside week
        long outWindow = now - 86_400_000L * 14; // 14 days ago — outside week (but inside month)

        JobStats recent = new JobStats("recent", uid, "p", "world", 0, 0, 10,
                inWindow - 1_000L, inWindow);
        recent.setBuilderStat(uid, 200, 0, 0L);

        JobStats old = new JobStats("old", uid, "p", "world", 0, 0, 10,
                outWindow - 1_000L, outWindow);
        old.setBuilderStat(uid, 50, 0, 0L);

        StatsManager.ingest(recent);
        StatsManager.ingest(old);

        String json = StatsFeed.buildJson(
                List.of(recent, old),
                List.of(StatsManager.getPlayerTotals(uid)),
                Collections.emptyList(),
                now);

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray week  = root.getAsJsonObject("leaderboards").getAsJsonArray("weekByPlaced");
        JsonArray month = root.getAsJsonObject("leaderboards").getAsJsonArray("monthByPlaced");

        // week: only the recent record (200 placed); old (50) is outside 7-day window
        assertEquals(1, week.size(), "Only recent record is within the week window");
        assertEquals(200L, week.get(0).getAsJsonObject().get("placed").getAsLong());

        // month (30-day): both are inside 30-day window (recent=3d, old=14d ago)
        // They are from the same builder so they merge into one entry: 200+50=250
        assertEquals(1, month.size(), "Both records collapse to one builder in 30-day window");
        assertEquals(250L, month.get(0).getAsJsonObject().get("placed").getAsLong(),
                "Month sum should be 200+50=250");
    }

    /** Empty inputs produce valid JSON with empty arrays. */
    @Test
    void emptyInputsProduceValidJson() {
        String json = StatsFeed.buildJson(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                12345L);

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        assertEquals(0, root.getAsJsonArray("players").size());
        assertEquals(0, root.getAsJsonArray("projects").size());
        assertEquals(0, root.getAsJsonArray("jobs").size());
        JsonObject lb = root.getAsJsonObject("leaderboards");
        assertEquals(0, lb.getAsJsonArray("allTimeByPlaced").size());
        assertEquals(0, lb.getAsJsonArray("weekByPlaced").size());
    }
}
