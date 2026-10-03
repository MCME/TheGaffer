package com.mcmiddleearth.thegaffer.storage;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.mcmiddleearth.thegaffer.GafferResponses.BanWorkerResponse;
import com.mcmiddleearth.thegaffer.GafferResponses.HelperResponse;
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
 * Verifies A1 fix: unbanWorker correctly removes a banned player and returns
 * UNBAN_SUCCESS; and returns ALREADY_UNBANNED for a player who was not banned.
 */
class JobBanTest {

    private ServerMock server;

    @BeforeEach
    void setUp() throws Exception {
        server = MockBukkit.mock();
        // addHelper announces to the helpers, which looks players up via TheGaffer.getServerInstance().
        setServerInstance(server);
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

    @Test
    void unbanWorker_successPath_removesUuidAndReturnsSuccess() {
        Job job = new Job();
        PlayerMock player = server.addPlayer();

        // Manually add the player to the banned list to simulate a prior ban.
        job.getBannedWorkers().add(player.getUniqueId());

        BanWorkerResponse result = job.unbanWorker(List.of((OfflinePlayer) player));

        assertEquals(BanWorkerResponse.UNBAN_SUCCESS, result,
                "unbanWorker should return UNBAN_SUCCESS when the player is banned");
        assertFalse(job.getBannedWorkers().contains(player.getUniqueId()),
                "the player's UUID should be removed from the banned list after unban");
    }

    @Test
    void unbanWorker_notBannedPath_returnsAlreadyUnbanned() {
        Job job = new Job();
        PlayerMock player = server.addPlayer();

        // Player is NOT in the banned list — unban should report ALREADY_UNBANNED.
        BanWorkerResponse result = job.unbanWorker(List.of((OfflinePlayer) player));

        assertEquals(BanWorkerResponse.ALREADY_UNBANNED, result,
                "unbanWorker should return ALREADY_UNBANNED for a player who was never banned");
    }

    @Test
    void banWorker_owner_isRejected() {
        Job job = new Job();
        PlayerMock owner = server.addPlayer();
        job.setOwner(owner.getUniqueId());

        BanWorkerResponse result = job.banWorker(List.of((OfflinePlayer) owner));

        assertEquals(BanWorkerResponse.CANNOT_BAN_OWNER, result,
                "the owner can never be banned from their own job");
        assertFalse(job.getBannedWorkers().contains(owner.getUniqueId()),
                "the owner's UUID must not be added to the banned list");
    }

    @Test
    void banWorker_nonOwner_succeeds() {
        Job job = new Job();
        PlayerMock owner = server.addPlayer();
        PlayerMock worker = server.addPlayer();
        job.setOwner(owner.getUniqueId());

        BanWorkerResponse result = job.banWorker(List.of((OfflinePlayer) worker));

        assertEquals(BanWorkerResponse.BAN_SUCCESS, result,
                "a non-owner should still be bannable");
        assertTrue(job.getBannedWorkers().contains(worker.getUniqueId()),
                "the banned worker's UUID should be on the banned list");
    }

    @Test
    void addHelper_bannedPlayer_isRefused() {
        Job job = new Job();
        job.setName("river");
        job.setOwner(server.addPlayer().getUniqueId());
        PlayerMock banned = server.addPlayer();
        banned.setOp(true); // has the staff permission, so only the ban stands in the way
        job.getBannedWorkers().add(banned.getUniqueId());

        assertEquals(HelperResponse.WORKER_BANNED, job.addHelper(banned));
        assertFalse(job.isPlayerHelper(banned), "a banned player must not become a helper");
    }
}
