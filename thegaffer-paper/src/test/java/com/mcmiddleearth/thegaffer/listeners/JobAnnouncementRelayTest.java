package com.mcmiddleearth.thegaffer.listeners;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Who announces a job start to players on OTHER servers.
 *
 * <p>Historically this server relayed its own announcement network-wide as plain text through
 * MCME-Connect. The proxy plugin now posts a richer announcement of its own with a button that
 * transfers the player, so on a network running it both would go out and every player would see the
 * same job announced twice.
 */
class JobAnnouncementRelayTest {

    @Test
    void relaysNetworkWideWhenNothingElseIsAnnouncing() {
        assertTrue(JobEventListener.shouldRelayNetworkWide(false, true, true));
    }

    @Test
    void staysLocalWhenTheProxyPluginIsDoingTheAnnouncing() {
        assertFalse(JobEventListener.shouldRelayNetworkWide(true, true, true),
            "otherwise every player sees the same job announced twice");
    }

    @Test
    void staysLocalWithoutConnectBecauseThereIsNoRelayToUse() {
        assertFalse(JobEventListener.shouldRelayNetworkWide(false, false, true));
    }

    @Test
    void staysLocalWithNobodyOnlineBecauseARelayNeedsAPlayerConnection() {
        assertFalse(JobEventListener.shouldRelayNetworkWide(false, true, false));
    }

    @Test
    void theProxyFlagWinsOverAPresentConnect() {
        // The dangerous ordering: Connect being present must not re-enable the relay.
        assertFalse(JobEventListener.shouldRelayNetworkWide(true, true, true));
        assertFalse(JobEventListener.shouldRelayNetworkWide(true, false, true));
    }
}
