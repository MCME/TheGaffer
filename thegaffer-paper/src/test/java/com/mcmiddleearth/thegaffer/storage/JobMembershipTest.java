package com.mcmiddleearth.thegaffer.storage;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.mcmiddleearth.thegaffer.GafferResponses.WorkerResponse;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the H5 change: player identity (membership, bans) is keyed by UUID,
 * not name. The ban test is the headline win — a banned player can't dodge the
 * ban by renaming, because the ban is stored against their immutable UUID.
 */
class JobMembershipTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void membershipIsCheckedByUuid() {
        Job job = new Job();
        PlayerMock worker = server.addPlayer();
        PlayerMock other = server.addPlayer();

        job.getWorkers().add(worker.getUniqueId());

        assertTrue(job.isPlayerWorking(worker), "the worker should be recognized by UUID");
        assertFalse(job.isPlayerWorking(other), "a different player must not match");
    }

    @Test
    void bansAreKeyedByUuidAndBlockRejoin() {
        Job job = new Job();
        PlayerMock player = server.addPlayer();

        List<OfflinePlayer> toBan = new ArrayList<>();
        toBan.add(player);
        job.banWorker(toBan);

        // The ban is stored against the UUID, not the (mutable) name.
        assertTrue(job.getBannedWorkers().contains(player.getUniqueId()),
                "ban should be stored as the player's UUID");
        assertFalse(job.getBannedWorkers().contains(player.getName()),
                "ban must NOT be stored as the player's name");

        // A rejoin attempt is refused regardless of any later name change.
        assertEquals(WorkerResponse.WORKER_BANNED, job.addWorker(player),
                "a banned UUID must be blocked from rejoining");
    }
}
