package com.mcmiddleearth.thegaffer.utilities;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.storage.JobDatabase;
import com.mcmiddleearth.thegaffer.storage.JobWarp;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.*;

import java.awt.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.*;

class StatsManagerRecordBuildTest {
    private ServerMock server;
    private World world;

    @BeforeEach
    void up() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        StatsManager.reset();
        JobDatabase.getActiveJobs().clear();
    }

    @AfterEach
    void down() { MockBukkit.unmock(); }

    /**
     * Registers a running job named "river" centred on (cx, cz) with a 100x100 bounds and
     * {@code owner} as a working member. Mirrors StatsListenerTest#runningJobAt: bounds are
     * set directly via setBounds() so we never touch warp.toBukkitLocation() (which would
     * need TheGaffer.serverInstance).
     */
    private Job runningJobAt(int cx, int cz, PlayerMock owner) {
        JobWarp warp = new JobWarp();
        warp.setX(cx); warp.setY(64); warp.setZ(cz); warp.setWorld("world");

        Job job = new Job();
        job.setName("river");
        job.setOwner(owner.getUniqueId());
        job.setWarp(warp);
        job.setWorld("world");
        job.setJobRadius(50);
        job.setProjectname("nothing");
        job.setBounds(new Rectangle2D.Double(cx - 50, cz - 50, 100, 100));

        JobDatabase.getActiveJobs().put(job.getName(), job);
        job.getWorkers().add(owner.getUniqueId());
        StatsManager.begin(job);
        return job;
    }

    @Test void recordBuild_noJob_isNoOp() {
        PlayerMock p = server.addPlayer();
        StatsManager.recordBuild(p, p.getLocation(), true);
        assertNull(StatsManager.getLive("anything"));
    }

    @Test void recordBuild_inBounds_incrementsPlacedAndBroke() {
        PlayerMock p = server.addPlayer();
        runningJobAt(0, 0, p);

        Location inside = new Location(world, 5, 64, 5);
        StatsManager.recordBuild(p, inside, true);
        StatsManager.recordBuild(p, inside, false);

        assertEquals(1, StatsManager.getLive("river").getBuilders().get(p.getUniqueId()).getPlaced());
        assertEquals(1, StatsManager.getLive("river").getBuilders().get(p.getUniqueId()).getBroke());
    }
}
