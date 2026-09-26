package com.mcmiddleearth.thegaffer.utilities;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.storage.JobStats;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatsManagerTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        StatsManager.reset();
    }

    @AfterEach
    void tearDown() { MockBukkit.unmock(); }

    @Test
    void countsAccumulateWhileLiveAndFinishProducesRecord() {
        UUID builder = UUID.randomUUID();
        StatsManager.beginForTest("river", UUID.randomUUID(), "nothing", "world", 0, 0, 50, 1000L);
        StatsManager.onJoin("river", builder);
        StatsManager.recordPlace("river", builder);
        StatsManager.recordPlace("river", builder);
        StatsManager.recordBreak("river", builder);

        JobStats finished = StatsManager.finishForTest("river", 4000L);
        assertEquals(2, finished.getBuilders().get(builder).getPlaced());
        assertEquals(1, finished.getBuilders().get(builder).getBroke());
        assertEquals(3000L, finished.getDurationMillis());
        // live entry cleared after finish
        assertNull(StatsManager.getLive("river"));
    }

    @Test
    void aggregateRanksBuildersByPlaced() {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();

        JobStats j1 = new JobStats("j1", alice, "p", "world", 0, 0, 10, 0L, 1000L);
        j1.recordPlace(alice, 100); j1.recordPlace(bob, 50);
        JobStats j2 = new JobStats("j2", bob, "p", "world", 0, 0, 10, 0L, 1000L);
        j2.recordPlace(alice, 25);

        StatsManager.ingest(j1);
        StatsManager.ingest(j2);

        java.util.List<StatsManager.PlayerAggregate> top = StatsManager.getLeaderboard(StatsManager.SortKey.PLACED, 10);
        assertEquals(alice, top.get(0).getId());   // 125 placed
        assertEquals(bob, top.get(1).getId());     //  50 placed
        assertEquals(125, StatsManager.getPlayerTotals(alice).getPlaced());
        assertEquals(2, StatsManager.getPlayerTotals(alice).getJobs());

        // ACTIVE sort: alice is in 2 jobs, bob in 1
        java.util.List<StatsManager.PlayerAggregate> byActive =
                StatsManager.getLeaderboard(StatsManager.SortKey.ACTIVE, 10);
        assertEquals(alice, byActive.get(0).getId());

        // limit truncation
        assertEquals(1, StatsManager.getLeaderboard(StatsManager.SortKey.PLACED, 1).size());
    }

    @Test
    void discordSummaryContainsTotals() {
        JobStats s = new JobStats("river", UUID.randomUUID(), "p", "world", 0, 0, 10, 0L, 3600000L);
        s.recordPlace(UUID.randomUUID(), 1200);
        String msg = StatsManager.buildDiscordSummary(s);
        assertTrue(msg.contains("river"));
        assertTrue(msg.contains("1200"));
        assertTrue(msg.contains("1h 0m"));
        assertTrue(msg.contains("Builders: 1")); // recordPlace adds the builder as a participant
        assertTrue(msg.contains("0 broken"));    // no breaks recorded
    }

    /**
     * toCsv produces the canonical header and a data row containing the expected placed/broke counts.
     *
     * toCsv calls Util.nameOf(...), whose class static initializer reads
     * TheGaffer.getServerInstance().getLogger(). We set serverInstance to the MockBukkit
     * server (already live from @BeforeEach) via reflection, exactly as StatsManagerFindTest does.
     */
    @Test
    void exportWritesCsvRows() throws Exception {
        Field f = TheGaffer.class.getDeclaredField("serverInstance");
        f.setAccessible(true);
        f.set(null, server);
        try {
            UUID alice = UUID.randomUUID();
            JobStats s = new JobStats("river", alice, "p", "world", 1, 2, 10, 0L, 1000L);
            s.recordPlace(alice, 5);
            s.recordBreak(alice, 2);

            String csv = StatsManager.toCsv(Collections.singletonList(s));
            String[] lines = csv.split("\n");

            assertEquals("job,owner,project,world,centerX,centerZ,startTime,endTime,builder,placed,broke",
                    lines[0], "First line must be the CSV header");
            assertEquals(2, lines.length, "One builder => header + exactly one data row");
            // The data row must contain the job name and the placed/broke counts
            assertTrue(csv.contains("river,"), "CSV must contain the job name");
            assertTrue(csv.contains(",5,2"), "CSV must contain placed=5 and broke=2 as last two columns");
        } finally {
            f.set(null, null);
        }
    }
}
