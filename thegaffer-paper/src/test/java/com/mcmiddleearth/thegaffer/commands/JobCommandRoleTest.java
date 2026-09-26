package com.mcmiddleearth.thegaffer.commands;

import com.mcmiddleearth.thegaffer.storage.Job;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pure unit tests for {@link JobCommand#roleOf(Job, UUID)} — no Bukkit needed.
 */
class JobCommandRoleTest {

    @Test
    void ownerIsRecognisedAsOwner() {
        UUID ownerUuid = UUID.randomUUID();
        Job job = new Job();
        job.setOwner(ownerUuid);

        assertEquals("Owner", JobCommand.roleOf(job, ownerUuid));
    }

    @Test
    void helperIsRecognisedAsHelper() {
        UUID ownerUuid = UUID.randomUUID();
        UUID helperUuid = UUID.randomUUID();
        Job job = new Job();
        job.setOwner(ownerUuid);
        job.getHelpers().add(helperUuid);

        assertEquals("Helper", JobCommand.roleOf(job, helperUuid));
    }

    @Test
    void workerIsRecognisedAsWorker() {
        UUID ownerUuid = UUID.randomUUID();
        UUID workerUuid = UUID.randomUUID();
        Job job = new Job();
        job.setOwner(ownerUuid);
        job.getWorkers().add(workerUuid);

        assertEquals("Worker", JobCommand.roleOf(job, workerUuid));
    }

    @Test
    void ownerTakesPriorityOverHelperIfBothPresent() {
        UUID uuid = UUID.randomUUID();
        Job job = new Job();
        job.setOwner(uuid);
        // Even if somehow also in helpers list, Owner wins
        job.getHelpers().add(uuid);

        assertEquals("Owner", JobCommand.roleOf(job, uuid));
    }
}
