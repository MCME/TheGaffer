package com.mcmiddleearth.thegaffer.utilities;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.mcmiddleearth.thegaffer.storage.JobStats;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
}
