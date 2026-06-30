package com.mcmiddleearth.thegaffer.storage;

import be.seeseemelk.mockbukkit.MockBukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the real persistence path added for P1 (and the UUID format from
 * H5): build a Job, serialize it to a YAML string, reload it, and assert the
 * state survives. This is the round-trip I could previously only eyeball.
 */
class JobPersistenceTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private Job sampleJob(UUID owner, UUID helper, UUID worker) {
        Job job = new Job();
        job.setName("river");
        job.setOwner(owner);
        job.setRunning(true);
        job.setPaused(false);
        job.setPrivate(true);
        job.setWorld("world");
        job.setJobRadius(120);
        job.setStartTime(1000L);
        job.setEndTime(0L);
        job.setDescription("a river build");
        job.setDiscordSend(false);
        job.setProjectname("nothing");
        job.setHelpers(new ArrayList<>(Arrays.asList(helper)));
        job.setWorkers(new ArrayList<>(Arrays.asList(worker)));
        job.setBannedWorkers(new ArrayList<>());
        job.setInvitedWorkers(new ArrayList<>());
        JobWarp warp = new JobWarp();
        warp.setX(100.5);
        warp.setY(64.0);
        warp.setZ(-200.5);
        warp.setYaw(90.0f);
        warp.setPitch(10.0f);
        warp.setWorld("world");
        job.setWarp(warp);
        return job;
    }

    /** Save -> string -> load: the actual on-disk path, minus the file I/O. */
    private Job roundTrip(Job job) throws Exception {
        String yaml = JobStorage.toYaml(job).saveToString();
        YamlConfiguration reloaded = new YamlConfiguration();
        reloaded.loadFromString(yaml);
        return JobStorage.fromYaml(reloaded);
    }

    @Test
    void coreFieldsSurviveRoundTrip() throws Exception {
        UUID owner = UUID.randomUUID();
        Job loaded = roundTrip(sampleJob(owner, UUID.randomUUID(), UUID.randomUUID()));
        assertEquals("river", loaded.getName());
        assertEquals(owner, loaded.getOwner());
        assertTrue(loaded.isRunning());
        assertTrue(loaded.isPrivate());
        assertEquals("world", loaded.getWorld());
        assertEquals(120, loaded.getJobRadius());
        assertEquals(1000L, loaded.getStartTime().longValue());
        assertEquals("a river build", loaded.getDescription());
    }

    @Test
    void membersSurviveRoundTripAsUuids() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID helper = UUID.randomUUID();
        UUID worker = UUID.randomUUID();
        Job loaded = roundTrip(sampleJob(owner, helper, worker));
        assertEquals(1, loaded.getHelpers().size());
        assertTrue(loaded.getHelpers().contains(helper));
        assertEquals(1, loaded.getWorkers().size());
        assertTrue(loaded.getWorkers().contains(worker));
    }

    @Test
    void warpSurvivesRoundTrip() throws Exception {
        Job loaded = roundTrip(sampleJob(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));
        JobWarp w = loaded.getWarp();
        assertNotNull(w);
        assertEquals(100.5, w.getX());
        assertEquals(64.0, w.getY());
        assertEquals(-200.5, w.getZ());
        assertEquals(90.0f, w.getYaw());
        assertEquals("world", w.getWorld());
    }

    /**
     * The autoPaused flag (QA item 4) defaults to false, and set/clear is a plain
     * field toggle — no Bukkit involved.
     */
    @Test
    void autoPausedDefaultsFalseAndTogglesCleanly() {
        Job job = new Job();
        assertFalse(job.isAutoPaused(), "autoPaused must default to false");
        job.setAutoPaused(true);
        assertTrue(job.isAutoPaused(), "setAutoPaused(true) should take effect");
        job.setAutoPaused(false);
        assertFalse(job.isAutoPaused(), "setAutoPaused(false) should clear the flag");
    }

    /** autoPaused is persisted next to paused, so it must survive the round-trip. */
    @Test
    void autoPausedSurvivesRoundTrip() throws Exception {
        Job job = sampleJob(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        job.setPaused(true);
        job.setAutoPaused(true);
        Job loaded = roundTrip(job);
        assertTrue(loaded.isPaused(), "paused should survive the round-trip");
        assertTrue(loaded.isAutoPaused(), "autoPaused should survive the round-trip");
    }

}
