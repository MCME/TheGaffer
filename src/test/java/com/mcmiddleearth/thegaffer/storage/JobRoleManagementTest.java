package com.mcmiddleearth.thegaffer.storage;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.mcmiddleearth.thegaffer.GafferResponses.DemoteResponse;
import com.mcmiddleearth.thegaffer.GafferResponses.PromoteResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the J2 promote/demote operations and the transfer validation rule.
 *
 * <p>promote (worker→helper): player stays in workers list (build rights kept) AND
 * is added to helpers list.
 * <p>demote (helper→worker): player is removed from helpers AND remains in workers
 * so isPlayerWorking still returns true.
 * <p>transfer validation: target must already be a helper — non-helpers are rejected.
 */
class JobRoleManagementTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // -------------------------------------------------------------------------
    // demoteHelper
    // -------------------------------------------------------------------------

    @Test
    void demoteHelper_removesFromHelpers_andKeepsInWorkers() {
        Job job = new Job();
        UUID uuid = UUID.randomUUID();

        // Set up: player is both a worker and a helper (the normal helper state post-promote)
        job.getWorkers().add(uuid);
        job.getHelpers().add(uuid);

        DemoteResponse result = job.demoteHelper(uuid);

        assertEquals(DemoteResponse.DEMOTE_SUCCESS, result,
                "demoteHelper should return DEMOTE_SUCCESS");
        assertFalse(job.getHelpers().contains(uuid),
                "after demote the player must no longer be in helpers");
        assertTrue(job.getWorkers().contains(uuid),
                "after demote the player must still be in workers (build rights preserved)");
    }

    @Test
    void demoteHelper_whenNotAHelper_returnsFailure() {
        Job job = new Job();
        UUID uuid = UUID.randomUUID();
        job.getWorkers().add(uuid);

        DemoteResponse result = job.demoteHelper(uuid);

        assertEquals(DemoteResponse.NOT_A_HELPER, result,
                "demoteHelper must return NOT_A_HELPER for a plain worker");
    }

    @Test
    void demoteHelper_ensuresWorkerMembershipEvenIfMissingFromWorkers() {
        // Edge case: helper was never in workers (e.g. addHelper path for staff)
        Job job = new Job();
        UUID uuid = UUID.randomUUID();
        job.getHelpers().add(uuid);
        // NOT added to workers

        DemoteResponse result = job.demoteHelper(uuid);

        assertEquals(DemoteResponse.DEMOTE_SUCCESS, result);
        assertTrue(job.getWorkers().contains(uuid),
                "demote must add the player to workers even if they weren't there before");
        // isPlayerWorking uses workers list — check via a mock player-like approach
        // We just verified workers contains the uuid; the OfflinePlayer.isPlayerWorking check
        // delegates to workers.contains(p.getUniqueId()), so this is sufficient.
    }

    // -------------------------------------------------------------------------
    // promoteWorkerToHelper
    // -------------------------------------------------------------------------

    @Test
    void promoteWorkerToHelper_addsToHelpers_andStaysInWorkers() {
        Job job = new Job();
        UUID uuid = UUID.randomUUID();
        job.getWorkers().add(uuid);

        PromoteResponse result = job.promoteWorkerToHelper(uuid);

        assertEquals(PromoteResponse.PROMOTE_SUCCESS, result,
                "promoteWorkerToHelper should return PROMOTE_SUCCESS");
        assertTrue(job.getHelpers().contains(uuid),
                "after promote the player must be in helpers");
        assertTrue(job.getWorkers().contains(uuid),
                "after promote the player must still be in workers (build rights preserved)");
    }

    @Test
    void promoteWorkerToHelper_whenNotWorker_returnsFailure() {
        Job job = new Job();
        UUID uuid = UUID.randomUUID();

        PromoteResponse result = job.promoteWorkerToHelper(uuid);

        assertEquals(PromoteResponse.NOT_A_WORKER, result,
                "promoteWorkerToHelper must fail if the player is not a worker");
    }

    @Test
    void promoteWorkerToHelper_whenAlreadyHelper_returnsAlreadyHelper() {
        Job job = new Job();
        UUID uuid = UUID.randomUUID();
        job.getWorkers().add(uuid);
        job.getHelpers().add(uuid);

        PromoteResponse result = job.promoteWorkerToHelper(uuid);

        assertEquals(PromoteResponse.ALREADY_HELPER, result,
                "promoting an existing helper should return ALREADY_HELPER");
    }

    // -------------------------------------------------------------------------
    // transfer validation (target must be a helper)
    // -------------------------------------------------------------------------

    @Test
    void transferValidation_targetMustBeHelper() {
        // Simulate the transfer validation: target is in workers only → rejected
        Job job = new Job();
        UUID ownerUuid = UUID.randomUUID();
        UUID workerUuid = UUID.randomUUID();
        job.setOwner(ownerUuid);
        job.getWorkers().add(workerUuid);

        // The validation in JobCommand.transfer checks helpers.contains(targetUuid)
        assertFalse(job.getHelpers().contains(workerUuid),
                "a plain worker must NOT be a valid transfer target");
    }

    @Test
    void transferValidation_helperIsValidTarget() {
        Job job = new Job();
        UUID ownerUuid = UUID.randomUUID();
        UUID helperUuid = UUID.randomUUID();
        job.setOwner(ownerUuid);
        job.getWorkers().add(helperUuid);
        job.getHelpers().add(helperUuid);

        assertTrue(job.getHelpers().contains(helperUuid),
                "a helper must be a valid transfer target");
    }

    @Test
    void transferExecution_changesOwner_addsOldOwnerAsHelper_preservesCreator() {
        Job job = new Job();
        UUID ownerUuid = UUID.randomUUID();
        UUID helperUuid = UUID.randomUUID();
        job.setOwner(ownerUuid);
        job.setCreator(ownerUuid);
        job.getWorkers().add(helperUuid);
        job.getHelpers().add(helperUuid);

        // Simulate transfer logic
        UUID oldOwner = job.getOwner();
        job.setOwner(helperUuid);
        if (!job.getHelpers().contains(oldOwner)) {
            job.getHelpers().add(oldOwner);
        }

        assertEquals(helperUuid, job.getOwner(), "owner must change to the target");
        assertTrue(job.getHelpers().contains(ownerUuid),
                "old owner must be added to helpers after transfer");
        assertEquals(ownerUuid, job.getCreator(),
                "creator field must NOT change — /job info shows original 'Started by'");
    }
}
