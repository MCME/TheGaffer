package com.mcmiddleearth.thegaffer.utilities;

import org.mockbukkit.mockbukkit.MockBukkit;
import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.storage.JobDatabase;
import com.mcmiddleearth.thegaffer.storage.JobStats;
import com.mcmiddleearth.thegaffer.storage.JobWarp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StatsManagerActiveTest {

    @TempDir
    File tmp;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        StatsManager.reset();
        StatsManager.statsDirOverride = tmp;
        JobDatabase.getActiveJobs().clear();
    }

    @AfterEach
    void tearDown() {
        JobDatabase.getActiveJobs().clear();
        StatsManager.statsDirOverride = null;
        MockBukkit.unmock();
    }

    @Test
    void activeCountsSurviveFlushAndReload() {
        UUID builder = UUID.randomUUID();
        StatsManager.beginForTest("river", UUID.randomUUID(), "p", "world", 0, 0, 10, 1000L);
        StatsManager.recordPlace("river", builder);
        StatsManager.recordPlace("river", builder);

        StatsManager.flushActive(false);   // sync write into tmp/active/river-0.yml
        StatsManager.reset();              // wipe in-memory state (override is unaffected — it was set in setUp)
        StatsManager.loadActiveForTest("river");

        JobStats reloaded = StatsManager.getLive("river");
        assertNotNull(reloaded, "Reloaded live entry should not be null");
        assertEquals(2, reloaded.getBuilders().get(builder).getPlaced(),
                "Placed count should survive flush and reload");
    }

    @Test
    void runningJobWithoutSnapshotGetsFreshCountersOnLoad() {
        // Simulate a job that was already running when the stats feature first deployed
        // (or that crashed before the first flush): it's registered as active but has
        // NO active/*.yml snapshot in the (empty) temp stats dir.
        UUID owner = UUID.randomUUID();
        JobWarp warp = new JobWarp();
        warp.setX(0);
        warp.setY(64);
        warp.setZ(0);
        warp.setWorld("world");

        Job job = new Job();
        job.setName("delta");
        job.setOwner(owner);
        job.setWarp(warp);
        job.setStartTime(1000L);
        JobDatabase.getActiveJobs().put(job.getName(), job);

        StatsManager.loadActive(); // no snapshot exists -> begin(job) should lazily init counters

        JobStats live = StatsManager.getLive("delta");
        assertNotNull(live, "Running job without a snapshot should get fresh live counters on load");
        assertEquals(1000L, live.getStartTime(), "begin() should preserve the job's persisted startTime");
    }
}
