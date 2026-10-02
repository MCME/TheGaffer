package com.mcmiddleearth.thegaffer.utilities;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.storage.JobDatabase;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.geom.Rectangle2D;
import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Build protection for a worker whose job runs in one world while another job runs in a second
 * world. The worker's job bounds are a bare X/Z rectangle, so without a world check the worker
 * could build in the second world at their own job's coordinates.
 */
class ProtectionUtilTest {

    private ServerMock server;
    private World world;
    private World other;
    private PlayerMock worker;

    @BeforeEach
    void setUp() throws Exception {
        server = MockBukkit.mock();
        // getBuildProtection resolves each job's world via TheGaffer.getServerInstance().
        setServerInstance(server);
        world = server.addSimpleWorld("world");
        other = server.addSimpleWorld("other");
        worker = server.addPlayer(); // not op, so no thegaffer.ignoreprotection
        JobDatabase.getActiveJobs().clear();

        activeJob("river", "world", 0, 0).getWorkers().add(worker.getUniqueId());
        activeJob("anduin", "other", 1000, 1000);
    }

    @AfterEach
    void tearDown() throws Exception {
        JobDatabase.getActiveJobs().clear();
        setServerInstance(null);
        MockBukkit.unmock();
    }

    private static void setServerInstance(Object value) throws Exception {
        Field f = TheGaffer.class.getDeclaredField("serverInstance");
        f.setAccessible(true);
        f.set(null, value);
    }

    /** Registers an active job in {@code worldName} with 100x100 bounds centred on (cx, cz). */
    private static Job activeJob(String name, String worldName, int cx, int cz) {
        Job job = new Job();
        job.setName(name);
        job.setOwner(UUID.randomUUID());
        job.setWorld(worldName);
        job.setBounds(new Rectangle2D.Double(cx - 50, cz - 50, 100, 100));
        JobDatabase.getActiveJobs().put(name, job);
        return job;
    }

    @Test
    void getBuildProtection_insideOwnJobArea_isAllowed() {
        assertEquals(BuildProtection.ALLOWED,
                ProtectionUtil.getBuildProtection(worker, new Location(world, 5, 64, 5)));
    }

    @Test
    void getBuildProtection_ownJobXZInAnotherJobsWorld_isOutOfBounds() {
        assertEquals(BuildProtection.OUT_OF_BOUNDS,
                ProtectionUtil.getBuildProtection(worker, new Location(other, 5, 64, 5)));
    }
}
