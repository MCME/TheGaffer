package com.mcmiddleearth.thegaffer.utilities;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class StatsManagerRecordBuildTest {
    private ServerMock server;
    @BeforeEach void up() { server = MockBukkit.mock(); }
    @AfterEach void down() { MockBukkit.unmock(); }
    @Test void recordBuild_noJob_isNoOp() {
        PlayerMock p = server.addPlayer();
        StatsManager.recordBuild(p, p.getLocation(), true);
        assertNull(StatsManager.getLive("anything"));
    }
}
