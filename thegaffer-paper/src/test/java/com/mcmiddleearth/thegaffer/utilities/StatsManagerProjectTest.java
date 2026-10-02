package com.mcmiddleearth.thegaffer.utilities;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import com.mcmiddleearth.thegaffer.TheGaffer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StatsManagerProjectTest {

    @TempDir File tmp;
    private ServerMock server;

    @BeforeEach
    void setUp() throws Exception {
        server = MockBukkit.mock();
        // Wire up serverInstance so Util.nameOf works in exportProject (CSV row writer).
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

    /**
     * exportProject writes only the rows whose job belongs to the requested project
     * (canonical match), and excludes rows from other projects.
     */
    @Test
    void exportProjectFiltersToMatchingProjectOnly() throws Exception {
        UUID alice = UUID.randomUUID();

        // Job belonging to "Minas Tirith"
        com.mcmiddleearth.thegaffer.storage.JobStats mt =
                new com.mcmiddleearth.thegaffer.storage.JobStats(
                        "citadel", alice, "Minas Tirith", "world", 0, 0, 10, 1000L, 2000L);
        mt.recordPlace(alice, 7);
        mt.recordBreak(alice, 3);
        com.mcmiddleearth.thegaffer.storage.JobStatsStorage.save(mt, tmp, false);

        // Job belonging to an unrelated project — must NOT appear in the export
        UUID bob = UUID.randomUUID();
        com.mcmiddleearth.thegaffer.storage.JobStats other =
                new com.mcmiddleearth.thegaffer.storage.JobStats(
                        "harbour", bob, "Pelargir", "world", 100, 100, 10, 3000L, 4000L);
        other.recordPlace(bob, 20);
        com.mcmiddleearth.thegaffer.storage.JobStatsStorage.save(other, tmp, false);

        // Export only "minas tirith" (different case to prove canonical matching)
        File csv = StatsManager.exportProject("minas tirith", 99L);
        assertNotNull(csv, "export should succeed");
        assertTrue(csv.exists(), "CSV file must exist");

        String content = new String(Files.readAllBytes(csv.toPath()), StandardCharsets.UTF_8);
        String[] lines = content.split("\n");

        // Header + exactly one data row (for "citadel"/alice)
        assertEquals(2, lines.length,
                "Expected header + 1 data row; got " + lines.length + " lines:\n" + content);
        assertTrue(content.contains("citadel"), "Row must include the citadel job");
        assertTrue(content.contains(",7,3"),    "Row must include placed=7 and broke=3");
        assertFalse(content.contains("harbour"), "Unrelated project row must be excluded");
        assertFalse(content.contains("Pelargir"), "Unrelated project name must be excluded");

        // File name should embed the safe project name and the timestamp 99
        assertTrue(csv.getName().contains("99"), "filename should include the stamp");
        assertTrue(csv.getName().contains("minas"), "filename should include the project name");
    }
}
