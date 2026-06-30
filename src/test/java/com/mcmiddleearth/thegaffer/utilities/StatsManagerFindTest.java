package com.mcmiddleearth.thegaffer.utilities;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.storage.JobStats;
import com.mcmiddleearth.thegaffer.storage.JobStatsStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StatsManagerFindTest {

    @TempDir
    File tmp;

    private ServerMock server;

    @BeforeEach
    void setUp() throws Exception {
        server = MockBukkit.mock();
        // Util.<clinit> calls TheGaffer.getServerInstance().getLogger() — provide it.
        setTheGafferField("serverInstance", server);
        StatsManager.reset();
        StatsManager.statsDirOverride = tmp;
    }

    @AfterEach
    void tearDown() throws Exception {
        StatsManager.statsDirOverride = null;
        setTheGafferField("serverInstance", null);
        MockBukkit.unmock();
    }

    private static void setTheGafferField(String name, Object value) throws Exception {
        Field f = TheGaffer.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(null, value);
    }

    /** Test A: findJobStats prefers a live (in-progress) job over any finished record. */
    @Test
    void findJobStatsPreferLiveOverFinished() {
        UUID owner = UUID.randomUUID();
        // Write a finished record to disk so there is something to compete against
        JobStats finished = new JobStats("river", owner, "p", "world", 0, 0, 10, 100L, 200L);
        JobStatsStorage.save(finished, tmp, false);

        // Begin a live entry
        StatsManager.beginForTest("river", owner, "p", "world", 0, 0, 10, 1000L);

        JobStats found = StatsManager.findJobStats("river");
        assertNotNull(found, "findJobStats must return non-null when a live job exists");
        assertEquals(0L, found.getEndTime(), "Live job has endTime == 0");
    }

    /** Test B: findJobStats returns the newest finished record by name when not live. */
    @Test
    void findJobStatsReturnsNewestFinishedByName() {
        UUID owner = UUID.randomUUID();

        // Two finished records with different endTimes — newer one should win
        JobStats older = new JobStats("river", owner, "p", "world", 0, 0, 10, 100L, 100L);
        JobStats newer = new JobStats("river", owner, "p", "world", 0, 0, 10, 100L, 200L);
        JobStatsStorage.save(older, tmp, false);
        JobStatsStorage.save(newer, tmp, false);

        // Also add a record for a different job to ensure name filtering works
        JobStats other = new JobStats("dale", owner, "p", "world", 0, 0, 10, 50L, 300L);
        JobStatsStorage.save(other, tmp, false);

        JobStats found = StatsManager.findJobStats("river");
        assertNotNull(found, "findJobStats must return non-null when finished records exist");
        assertEquals(200L, found.getEndTime(), "Should return the newest finished record (endTime 200)");
    }

    /** findPlayerTotalsByName resolves a player by case-insensitive name, and returns null for unknown names. */
    @Test
    void findPlayerTotalsByNameResolvesKnownAndRejectsUnknown() {
        // addPlayer() registers the name->UUID mapping so Util.nameOf resolves it.
        PlayerMock builder = server.addPlayer();

        JobStats s = new JobStats("river", builder.getUniqueId(), "p", "world", 0, 0, 10, 0L, 1000L);
        s.recordPlace(builder.getUniqueId(), 5);
        StatsManager.ingest(s);

        StatsManager.PlayerAggregate found = StatsManager.findPlayerTotalsByName(builder.getName());
        assertNotNull(found, "findPlayerTotalsByName must resolve a known player name");
        assertEquals(builder.getUniqueId(), found.getId(), "Resolved aggregate must be the matching player's");

        // case-insensitive
        assertNotNull(StatsManager.findPlayerTotalsByName(builder.getName().toUpperCase()),
                "Name lookup must be case-insensitive");

        assertNull(StatsManager.findPlayerTotalsByName("nobody"),
                "Unknown names must return null");
    }

    /** Optional light check: renderJobStats returns a non-null Component for a sample JobStats. */
    @Test
    void renderJobStatsReturnsNonNull() {
        UUID owner = UUID.randomUUID();
        JobStats s = new JobStats("river", owner, "p", "world", 100, -200, 50, 0L, 3_600_000L);
        s.recordPlace(owner, 42);
        s.recordBreak(owner, 7);

        assertNotNull(StatsManager.renderJobStats(s), "renderJobStats must return a non-null Component");
    }
}
