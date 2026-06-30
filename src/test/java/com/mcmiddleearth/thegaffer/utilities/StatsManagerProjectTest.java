package com.mcmiddleearth.thegaffer.utilities;

import be.seeseemelk.mockbukkit.MockBukkit;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StatsManagerProjectTest {

    @TempDir File tmp;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        StatsManager.statsDirOverride = tmp;
        StatsManager.reset();
    }

    @AfterEach
    void tearDown() {
        StatsManager.statsDirOverride = null;
        StatsManager.reset();
        MockBukkit.unmock();
    }

    @Test
    void aggregatesLiveJobsCaseInsensitivelyByProject() {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();

        StatsManager.beginForTest("job1", alice, "Minas Tirith", "world", 0, 0, 50, 1000L);
        StatsManager.recordPlace("job1", alice);
        StatsManager.recordPlace("job1", alice);
        StatsManager.recordBreak("job1", bob);

        StatsManager.beginForTest("job2", bob, "minas tirith", "world", 100, 100, 50, 2000L);
        StatsManager.recordPlace("job2", bob);

        StatsManager.ProjectAggregate agg = StatsManager.getProjectAggregate("MINAS TIRITH");

        assertEquals(2, agg.getJobCount());
        assertEquals(2, agg.getBuilderCount());
        assertEquals(3, agg.getPlaced());
        assertEquals(1, agg.getBroke());
        assertEquals(3, agg.getPerBuilder().get(bob).getPlaced() + agg.getPerBuilder().get(alice).getPlaced());
    }

    @Test
    void unrelatedProjectIsNotCounted() {
        UUID u = UUID.randomUUID();
        StatsManager.beginForTest("jobA", u, "Pelargir", "world", 0, 0, 10, 1000L);
        StatsManager.recordPlace("jobA", u);
        StatsManager.ProjectAggregate agg = StatsManager.getProjectAggregate("Minas Tirith");
        assertTrue(agg.isEmpty());
        assertEquals(0, agg.getJobCount());
    }

    @Test
    void foldsFinishedRecordsFromDisk() {
        UUID u = UUID.randomUUID();
        com.mcmiddleearth.thegaffer.storage.JobStats s =
                new com.mcmiddleearth.thegaffer.storage.JobStats("done", u, "Osgiliath", "world", 0, 0, 10, 1000L, 2000L);
        s.recordPlace(u, 5);
        com.mcmiddleearth.thegaffer.storage.JobStatsStorage.save(s, tmp, false);

        StatsManager.ProjectAggregate agg = StatsManager.getProjectAggregate("osgiliath");
        assertEquals(1, agg.getJobCount());
        assertEquals(5, agg.getPlaced());
        assertEquals(1000L, agg.getDurationMillis());
    }
}
