package com.mcmiddleearth.thegaffer.utilities;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.storage.JobDatabase;
import com.mcmiddleearth.thegaffer.storage.JobStats;
import com.mcmiddleearth.thegaffer.storage.JobWarp;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A running job's stats record carries the job's world, centre and radius, stamped by begin().
 * /job admin setwarp (which can change the world) and setradius move the job afterwards, and the
 * record must follow, or /job stats, the exports and the project totals report the old place.
 */
class StatsManagerLocationTest {

    @TempDir
    File tmp;

    private ServerMock server;
    private World other;
    private Job job;

    @BeforeEach
    void setUp() throws Exception {
        server = MockBukkit.mock();
        // updateLocation rebuilds the bounds from the warp, which resolves its world via
        // TheGaffer.getServerInstance().
        setServerInstance(server);
        server.addSimpleWorld("world");
        other = server.addSimpleWorld("other");
        StatsManager.reset();
        StatsManager.statsDirOverride = tmp;
        JobDatabase.getActiveJobs().clear();
        job = runningJobAtOrigin();
    }

    @AfterEach
    void tearDown() throws Exception {
        JobDatabase.getActiveJobs().clear();
        StatsManager.statsDirOverride = null;
        setServerInstance(null);
        MockBukkit.unmock();
    }

    private static void setServerInstance(Object value) throws Exception {
        Field f = TheGaffer.class.getDeclaredField("serverInstance");
        f.setAccessible(true);
        f.set(null, value);
    }

    /** A running job "river" in world "world" at (0, 0) with radius 50, its stats begun. */
    private static Job runningJobAtOrigin() {
        JobWarp warp = new JobWarp();
        warp.setX(0); warp.setY(64); warp.setZ(0); warp.setWorld("world");

        Job job = new Job();
        job.setName("river");
        job.setOwner(UUID.randomUUID());
        job.setProjectname("nothing");
        job.setWarp(warp);
        job.setWorld("world");
        job.setJobRadius(50);
        JobDatabase.getActiveJobs().put(job.getName(), job);
        StatsManager.begin(job);
        return job;
    }

    private static void assertLocation(String world, int centerX, int centerZ, int radius) {
        JobStats s = StatsManager.getLive("river");
        assertEquals(world, s.getWorld(), "world");
        assertEquals(centerX, s.getCenterX(), "centre X");
        assertEquals(centerZ, s.getCenterZ(), "centre Z");
        assertEquals(radius, s.getRadius(), "radius");
    }

    @Test
    void setwarp_movesTheLiveRecordWithTheJob() {
        job.updateLocation(new Location(other, 500, 64, 700));

        assertLocation("other", 500, 700, 50);
    }

    @Test
    void setradius_updatesTheLiveRecordsRadius() {
        job.updateJobRadius(80);

        assertLocation("world", 0, 0, 80);
    }

    @Test
    void loadActive_takesTheLocationFromTheJob() {
        // The job file and the stats snapshot are written separately, so after a crash between
        // the two they can disagree on where the job is. The job decides.
        StatsManager.flushActive(false); // the snapshot says world (0, 0) radius 50
        StatsManager.reset();
        job.getWarp().setWorld("other");
        job.getWarp().setX(500);
        job.getWarp().setZ(700);
        job.setWorld("other");
        job.setJobRadius(80);

        StatsManager.loadActive();

        assertLocation("other", 500, 700, 80);
    }
}
