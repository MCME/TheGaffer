package com.mcmiddleearth.thegaffer.storage;

import be.seeseemelk.mockbukkit.MockBukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobStatsStorageTest {

    @BeforeEach
    void setUp() { MockBukkit.mock(); }

    @AfterEach
    void tearDown() { MockBukkit.unmock(); }

    @Test
    void roundTripPreservesAllFields() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID builder = UUID.randomUUID();
        JobStats stats = new JobStats("river", owner, "nothing", "world", 10, -20, 100, 1000L, 5000L);
        stats.addParticipant(owner);
        stats.addParticipant(builder);
        stats.recordPlace(builder, 7);
        stats.recordBreak(builder, 3);

        String yaml = JobStatsStorage.toYaml(stats).saveToString();
        YamlConfiguration reloaded = new YamlConfiguration();
        reloaded.loadFromString(yaml);
        JobStats loaded = JobStatsStorage.fromYaml(reloaded);

        assertEquals("river", loaded.getName());
        assertEquals(owner, loaded.getOwner());
        assertEquals("nothing", loaded.getProject());
        assertEquals("world", loaded.getWorld());
        assertEquals(10, loaded.getCenterX());
        assertEquals(-20, loaded.getCenterZ());
        assertEquals(100, loaded.getRadius());
        assertEquals(4000L, loaded.getDurationMillis());
        assertTrue(loaded.getParticipants().contains(builder));
        assertEquals(7, loaded.getBuilders().get(builder).getPlaced());
        assertEquals(3, loaded.getBuilders().get(builder).getBroke());
    }
}
