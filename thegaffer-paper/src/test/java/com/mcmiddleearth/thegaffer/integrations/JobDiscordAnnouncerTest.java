package com.mcmiddleearth.thegaffer.integrations;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JobDiscordAnnouncerTest {

    @Test
    void discordTimestampFormatsSecondsAndStyle() {
        // millis -> seconds, with the requested style suffix
        assertEquals("<t:2:R>", JobDiscordAnnouncer.discordTimestamp(2000L, 'R'));
        assertEquals("<t:1719774000:f>", JobDiscordAnnouncer.discordTimestamp(1719774000000L, 'f'));
        assertEquals("<t:0:R>", JobDiscordAnnouncer.discordTimestamp(0L, 'R'));
    }
}
