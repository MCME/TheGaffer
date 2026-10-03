package com.mcmiddleearth.thegaffer.storage;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import com.mcmiddleearth.thegaffer.TheGaffer;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.geom.Rectangle2D;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link Job#containsLocation} is the one place that decides whether a location is inside a
 * job's area. The bounds are a bare X/Z rectangle, so the job's world has to match as well, and
 * has to move with the warp.
 */
class JobAreaTest {

    private ServerMock server;
    private World world;
    private World other;
    private Job job;

    @BeforeEach
    void setUp() throws Exception {
        server = MockBukkit.mock();
        // updateLocation rebuilds the bounds from the warp, which resolves its world via
        // TheGaffer.getServerInstance().
        setServerInstance(server);
        world = server.addSimpleWorld("world");
        other = server.addSimpleWorld("other");
        job = new Job();
        job.setWorld("world");
        job.setBounds(new Rectangle2D.Double(-50, -50, 100, 100));
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
    void containsLocation_insideBoundsInJobWorld_isTrue() {
        assertTrue(job.containsLocation(new Location(world, 5, 64, 5)));
    }

    @Test
    void containsLocation_outsideBoundsInJobWorld_isFalse() {
        assertFalse(job.containsLocation(new Location(world, 500, 64, 500)));
    }

    @Test
    void containsLocation_sameXZInAnotherWorld_isFalse() {
        assertFalse(job.containsLocation(new Location(other, 5, 64, 5)),
                "the bounds carry no world, so the job's world must match too");
    }

    @Test
    void containsLocation_jobWithoutBounds_isFalse() {
        job.setBounds(null);

        assertFalse(job.containsLocation(new Location(world, 5, 64, 5)));
    }

    @Test
    void updateLocation_inAnotherWorld_movesTheJobsWorld() {
        JobWarp warp = new JobWarp();
        warp.setX(0); warp.setY(64); warp.setZ(0); warp.setWorld("world");
        job.setWarp(warp);
        job.setJobRadius(50);

        job.updateLocation(new Location(other, 500, 64, 500));

        assertEquals("other", job.getWorld(), "the job's world must follow its warp");
        assertTrue(job.containsLocation(new Location(other, 505, 64, 505)));
    }
}
