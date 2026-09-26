package com.mcmiddleearth.thegaffer;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the test toolchain works: MockBukkit can stand up a fake server and a
 * fake player under JUnit 5, with no real Minecraft server.
 */
class SmokeTest {

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
    void mockServerAndPlayerExist() {
        assertNotNull(server, "MockBukkit should provide a server");
        PlayerMock player = server.addPlayer();
        assertNotNull(player.getUniqueId(), "a mock player should have a UUID");
        assertTrue(server.getOnlinePlayers().contains(player), "added player should be online");
    }
}
