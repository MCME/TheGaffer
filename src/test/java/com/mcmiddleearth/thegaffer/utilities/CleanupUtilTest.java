package com.mcmiddleearth.thegaffer.utilities;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.storage.Job;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the QA item 4 behaviour change in {@link CleanupUtil#selectNewOwner}: when the
 * owner is gone and there is no helper online to promote, the job is now PAUSED (and flagged
 * autoPaused) instead of being archived. The owner here is a random offline UUID and the job
 * has no helpers, so selectNewOwner takes the no-helper-online branch.
 *
 * <p>This is a pure-logic assertion: the job is never registered in JobDatabase, so the old
 * code path's {@code deactivateJob} would have been a no-op anyway — what we assert is the
 * positive new effect (paused + autoPaused), which the old archiving branch never set.
 */
class CleanupUtilTest {

    private ServerMock server;

    @BeforeEach
    void setUp() throws Exception {
        server = MockBukkit.mock();
        // CleanupUtil / Job reach TheGaffer.getServerInstance() (e.g. getOfflinePlayer).
        setTheGafferField("serverInstance", server);
    }

    @AfterEach
    void tearDown() throws Exception {
        setTheGafferField("serverInstance", null);
        MockBukkit.unmock();
    }

    private static void setTheGafferField(String name, Object value) throws Exception {
        Field f = TheGaffer.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(null, value);
    }

    @Test
    void noHelperOnlinePausesJobInsteadOfArchiving() {
        Job job = new Job();
        job.setName("river");
        job.setRunning(true);
        job.setOwner(UUID.randomUUID()); // offline owner, no helpers added

        assertFalse(job.isPaused(), "precondition: job starts un-paused");
        assertFalse(job.isAutoPaused(), "precondition: job starts without the auto-pause flag");

        CleanupUtil.selectNewOwner(job);

        assertTrue(job.isPaused(), "no helper online → job must be paused");
        assertTrue(job.isAutoPaused(), "the pause must be flagged as automatic");
        assertTrue(job.isRunning(), "the job must stay active (not archived)");
    }

    @Test
    void scheduledCleanupDropsAutoPausedJobFromWaitQueue() {
        CleanupUtil.getWaiting().clear();
        Job job = new Job();
        job.setName("anduin");
        job.setRunning(true);
        job.setOwner(UUID.randomUUID()); // offline owner, no helpers

        // Queue it as having waited past the 250s owner-timeout threshold.
        CleanupUtil.getWaiting().put(job, System.currentTimeMillis() - 300_000L);

        CleanupUtil.scheduledCleanup();

        assertTrue(job.isAutoPaused(), "owner gone + no helper online → auto-paused");
        assertFalse(CleanupUtil.getWaiting().containsKey(job),
                "an auto-paused job must be dropped from the wait queue (prevents the re-pause loop)");
    }
}
