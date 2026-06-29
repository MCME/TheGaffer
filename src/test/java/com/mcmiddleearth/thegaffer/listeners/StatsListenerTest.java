package com.mcmiddleearth.thegaffer.listeners;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.mcmiddleearth.thegaffer.events.JobProtectionBlockBreakEvent;
import com.mcmiddleearth.thegaffer.events.JobProtectionBlockPlaceEvent;
import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.storage.JobDatabase;
import com.mcmiddleearth.thegaffer.storage.JobWarp;
import com.mcmiddleearth.thegaffer.utilities.StatsManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StatsListenerTest {

    private ServerMock server;
    private World world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        StatsManager.reset();
        JobDatabase.getActiveJobs().clear();
    }

    @AfterEach
    void tearDown() { MockBukkit.unmock(); }

    /**
     * Build a running job at (cx, cz) with radius 50 without triggering
     * warp.toBukkitLocation() (which needs TheGaffer.serverInstance).
     * We set bounds directly via setBounds().
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
        // Set bounds manually: a 100x100 square centred on (cx, cz)
        job.setBounds(new Rectangle2D.Double(cx - 50, cz - 50, 100, 100));

        JobDatabase.getActiveJobs().put(job.getName(), job);   // register without firing events
        job.getWorkers().add(owner.getUniqueId());             // make the owner a working member
        StatsManager.begin(job);
        return job;
    }

    @Test
    void countsInBoundsPlaceAndBreak() {
        PlayerMock p = server.addPlayer();
        runningJobAt(0, 0, p);
        StatsListener listener = new StatsListener();

        Location inside = new Location(world, 5, 64, 5);
        listener.onPlace(new JobProtectionBlockPlaceEvent(p, inside, inside.getBlock(), false));
        listener.onBreak(new JobProtectionBlockBreakEvent(p, inside, inside.getBlock(), false));

        assertEquals(1, StatsManager.getLive("river").getBuilders().get(p.getUniqueId()).getPlaced());
        assertEquals(1, StatsManager.getLive("river").getBuilders().get(p.getUniqueId()).getBroke());
    }

    @Test
    void ignoresBlockedAndOutOfBounds() {
        PlayerMock p = server.addPlayer();
        runningJobAt(0, 0, p);
        StatsListener listener = new StatsListener();

        Location inside = new Location(world, 5, 64, 5);
        Location outside = new Location(world, 5000, 64, 5000);
        listener.onPlace(new JobProtectionBlockPlaceEvent(p, inside, inside.getBlock(), true));    // blocked
        listener.onPlace(new JobProtectionBlockPlaceEvent(p, outside, outside.getBlock(), false)); // out of bounds

        assertNull(StatsManager.getLive("river").getBuilders().get(p.getUniqueId()));
    }
}
