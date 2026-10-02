package com.mcmiddleearth.thegaffer.storage;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.mcmiddleearth.thegaffer.GafferResponses.BanWorkerResponse;
import com.mcmiddleearth.thegaffer.GafferResponses.KickWorkerResponse;
import com.mcmiddleearth.thegaffer.TheGaffer;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every way of removing a player from a job (ban, kick, leave, uninvite) must clear both roles
 * the player can hold. A worker promoted with /job promote is on the workers AND the helpers
 * list; a helper added with addhelper is on the helpers list only. Clearing just the workers
 * list leaves the player in the job: JobDatabase.getJobWorking still finds them, so they keep
 * the helper rights, cannot join another job, and can even inherit the job on an owner timeout.
 */
class JobRemovalTest {

    private ServerMock server;
    private Job job;
    private PlayerMock helper;

    @BeforeEach
    void setUp() throws Exception {
        server = MockBukkit.mock();
        // leaveJob announces to the job, which looks players up via TheGaffer.getServerInstance().
        setServerInstance(server);
        job = new Job();
        job.setName("river");
        job.setOwner(server.addPlayer().getUniqueId());
        helper = server.addPlayer();
    }

    @AfterEach
    void tearDown() throws Exception {
        setServerInstance(null);
        MockBukkit.unmock();
    }

    private static void setServerInstance(Object value) throws Exception {
        Field f = TheGaffer.class.getDeclaredField("serverInstance");
        f.setAccessible(true);
        f.set(null, value);
    }

    /** A worker promoted to helper: on both lists. */
    private void makePromotedHelper() {
        job.getWorkers().add(helper.getUniqueId());
        job.getHelpers().add(helper.getUniqueId());
    }

    /** A helper added with addhelper: on the helpers list only. */
    private void makeAddedHelper() {
        job.getHelpers().add(helper.getUniqueId());
    }

    private void assertOutOfTheJob() {
        assertFalse(job.isPlayerWorking(helper), "the player must lose the worker role");
        assertFalse(job.isPlayerHelper(helper), "the player must lose the helper role");
    }

    @Test
    void banWorker_promotedHelper_losesBothRoles() {
        makePromotedHelper();

        assertEquals(BanWorkerResponse.BAN_SUCCESS, job.banWorker(List.of((OfflinePlayer) helper)));
        assertOutOfTheJob();
    }

    @Test
    void banWorker_addedHelper_losesHelperRole() {
        makeAddedHelper();

        assertEquals(BanWorkerResponse.BAN_SUCCESS, job.banWorker(List.of((OfflinePlayer) helper)));
        assertOutOfTheJob();
    }

    @Test
    void banWorker_alreadyBannedHelper_dropsRoleAndSaves() {
        // Builds before this fix left a banned helper on the helpers list, and job files keep it.
        makeAddedHelper();
        job.getBannedWorkers().add(helper.getUniqueId());
        assertFalse(job.isDirty(), "precondition: nothing to save yet");

        assertEquals(BanWorkerResponse.ALREADY_BANNED, job.banWorker(List.of((OfflinePlayer) helper)));
        assertOutOfTheJob();
        assertTrue(job.isDirty(), "the cleanup must be saved even though the ban itself was a no-op");
    }

    @Test
    void kickWorker_promotedHelper_losesBothRoles() {
        makePromotedHelper();

        assertEquals(KickWorkerResponse.KICK_SUCCESS, job.kickWorker(List.of((OfflinePlayer) helper), "test"));
        assertOutOfTheJob();
    }

    @Test
    void kickWorker_addedHelper_isKicked() {
        makeAddedHelper();

        assertEquals(KickWorkerResponse.KICK_SUCCESS, job.kickWorker(List.of((OfflinePlayer) helper), "test"),
                "a helper is in the job, so kick must not answer NOT_IN_JOB");
        assertOutOfTheJob();
    }

    @Test
    void kickWorker_owner_isRefused() {
        // The owner can be on the helpers list, for example a helper who took the job over.
        job.setOwner(helper.getUniqueId());
        makeAddedHelper();

        assertEquals(KickWorkerResponse.CANNOT_KICK_OWNER,
                job.kickWorker(List.of((OfflinePlayer) helper), "test"));
        assertTrue(job.isPlayerHelper(helper), "a refused kick must change nothing");
    }

    @Test
    void kickWorker_nonMember_isNotInJob() {
        assertEquals(KickWorkerResponse.NOT_IN_JOB, job.kickWorker(List.of((OfflinePlayer) helper), "test"));
    }

    @Test
    void leaveJob_promotedHelper_losesBothRoles() {
        makePromotedHelper();

        job.leaveJob(helper);
        assertOutOfTheJob();
    }

    @Test
    void uninviteWorker_promotedHelper_losesBothRoles() {
        job.getInvitedWorkers().add(helper.getUniqueId());
        makePromotedHelper();

        job.uninviteWorker(List.of((OfflinePlayer) helper));
        assertOutOfTheJob();
    }
}
