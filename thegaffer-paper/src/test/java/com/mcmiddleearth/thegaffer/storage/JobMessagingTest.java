package com.mcmiddleearth.thegaffer.storage;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.mcmiddleearth.thegaffer.TheGaffer;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A player can be on more than one of the job's lists: a promoted helper is on the workers and
 * the helpers list, and a helper who took the job over is the owner and on the helpers list.
 * Every job message (including /jobchat) must still reach them once.
 */
class JobMessagingTest {

    private ServerMock server;
    private Job job;

    @BeforeEach
    void setUp() throws Exception {
        server = MockBukkit.mock();
        // The send methods look players up via TheGaffer.getServerInstance().
        setServerInstance(server);
        job = new Job();
        job.setName("river");
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
    void sendToAll_promotedHelper_getsOneCopy() {
        PlayerMock promoted = server.addPlayer();
        job.setOwner(UUID.randomUUID()); // offline owner
        job.getWorkers().add(promoted.getUniqueId());
        job.getHelpers().add(promoted.getUniqueId());

        job.sendToAll(Component.text("hello"));

        assertEquals(Component.text("hello"), promoted.nextComponentMessage());
        assertNull(promoted.nextComponentMessage(), "a player on both lists must get one copy");
    }

    @Test
    void sendToHelpers_ownerOnTheHelpersList_getsOneCopy() {
        PlayerMock owner = server.addPlayer();
        job.setOwner(owner.getUniqueId());
        job.getHelpers().add(owner.getUniqueId());

        job.sendToHelpers(Component.text("hello"));

        assertEquals(Component.text("hello"), owner.nextComponentMessage());
        assertNull(owner.nextComponentMessage(), "the owner must get one copy");
    }
}
