package com.mcmiddleearth.thegaffer.listeners;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JobEventListenerTest {

    @Test
    void discordTimestampFormatsSecondsAndStyle() {
        // millis -> seconds, with the requested style suffix
        assertEquals("<t:2:R>", JobEventListener.discordTimestamp(2000L, 'R'));
        assertEquals("<t:1719774000:f>", JobEventListener.discordTimestamp(1719774000000L, 'f'));
        assertEquals("<t:0:R>", JobEventListener.discordTimestamp(0L, 'R'));
    }
}
