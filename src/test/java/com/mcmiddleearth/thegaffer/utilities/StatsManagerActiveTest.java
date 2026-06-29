package com.mcmiddleearth.thegaffer.utilities;

import be.seeseemelk.mockbukkit.MockBukkit;
import com.mcmiddleearth.thegaffer.storage.JobStats;
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
    }

    @AfterEach
    void tearDown() {
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
}
