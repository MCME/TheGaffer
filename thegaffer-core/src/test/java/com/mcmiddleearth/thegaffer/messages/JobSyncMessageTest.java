package com.mcmiddleearth.thegaffer.messages;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JobSyncMessageTest {

    @Test
    void survivesARoundTripThroughTheWire() {
        JobSyncMessage sent = new JobSyncMessage("Minas_Tirith", "Gandalf", "the white city");

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        sent.serialise(out);
        JobSyncMessage back = JobSyncMessage.deserialise(ByteStreams.newDataInput(out.toByteArray()));

        assertEquals(sent, back);
    }

    @Test
    void travelsOnItsOwnSubchannelSoTheProxyDoesNotAnnounceIt() {
        assertEquals(Subchannel.JOB_SYNC, new JobSyncMessage("x", "y", "z").subchannel());
    }
}
