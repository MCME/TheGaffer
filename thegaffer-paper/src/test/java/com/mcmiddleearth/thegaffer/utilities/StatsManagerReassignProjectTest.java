package com.mcmiddleearth.thegaffer.utilities;

import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.storage.JobStats;
import com.mcmiddleearth.thegaffer.storage.JobStatsStorage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.io.File;
import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code /project attach} used to change only {@code Job.projectname}, while every statistic is
 * aggregated from the project recorded inside the {@link com.mcmiddleearth.thegaffer.storage.JobStats}
 * record. That project is set when the job starts and had no writer at all, so attaching a job to a
 * project never moved its blocks, builders or build time with it.
 */
class StatsManagerReassignProjectTest {

    @TempDir File tmp;
    private ServerMock server;

    @BeforeEach
    void setUp() throws Exception {
        server = MockBukkit.mock();
        Field f = TheGaffer.class.getDeclaredField("serverInstance");
        f.setAccessible(true);
        f.set(null, server);
        StatsManager.statsDirOverride = tmp;
        StatsManager.reset();
    }

    @AfterEach
    void tearDown() throws Exception {
        StatsManager.statsDirOverride = null;
        StatsManager.reset();
        Field f = TheGaffer.class.getDeclaredField("serverInstance");
        f.setAccessible(true);
        f.set(null, null);
        MockBukkit.unmock();
    }

    @Test
    void movesALiveJobsBlocksToTheNewProject() {
        UUID alice = UUID.randomUUID();
        StatsManager.beginForTest("gate", alice, "nothing", "world", 0, 0, 50, 1000L);
        StatsManager.recordPlace("gate", alice);
        StatsManager.recordPlace("gate", alice);

        assertTrue(StatsManager.reassignProject("gate", "Minas Tirith"));

        assertEquals(2, StatsManager.getProjectAggregate("Minas Tirith").getPlaced(),
            "the blocks already placed should follow the job into the project");
        assertEquals(0, StatsManager.getProjectAggregate("nothing").getPlaced());
    }

    @Test
    void movesAFinishedJobsRecordOnDisk() {
        UUID alice = UUID.randomUUID();
        StatsManager.beginForTest("wall", alice, "nothing", "world", 0, 0, 50, 1000L);
        StatsManager.recordPlace("wall", alice);
        // finishForTest deliberately does not persist, so write the record the way finish() does
        JobStatsStorage.save(StatsManager.finishForTest("wall", 2000L), tmp, false);

        assertTrue(StatsManager.reassignProject("wall", "Osgiliath"),
            "the job is over, but its record is what the project totals are built from");

        assertEquals(1, StatsManager.getProjectAggregate("Osgiliath").getPlaced());
        assertEquals(1, StatsManager.getProjectAggregate("Osgiliath").getJobCount());
    }

    @Test
    void survivesAReloadBecauseTheChangeIsWrittenToDisk() {
        UUID alice = UUID.randomUUID();
        StatsManager.beginForTest("keep", alice, "nothing", "world", 0, 0, 50, 1000L);
        StatsManager.recordPlace("keep", alice);
        JobStatsStorage.save(StatsManager.finishForTest("keep", 2000L), tmp, false);
        StatsManager.reassignProject("keep", "Osgiliath");

        StatsManager.reset(); // drop every in-memory aggregate, as a restart would

        assertEquals(1, StatsManager.getProjectAggregate("Osgiliath").getJobCount(),
            "a record rewritten in memory only would be lost on the next restart");
    }

    @Test
    void reportsWhenThereIsNoSuchJobToMove() {
        assertFalse(StatsManager.reassignProject("nonexistent", "Osgiliath"));
    }
}
