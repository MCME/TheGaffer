package com.mcmiddleearth.thegaffer.storage;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link Job#containsLocation} is the one place that decides whether a location is inside a
 * job's area. The bounds are a bare X/Z rectangle, so the job's world has to match as well.
 */
class JobAreaTest {

    private ServerMock server;
    private World world;
    private World other;
    private Job job;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        other = server.addSimpleWorld("other");
        job = new Job();
        job.setWorld("world");
        job.setBounds(new Rectangle2D.Double(-50, -50, 100, 100));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
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
}
