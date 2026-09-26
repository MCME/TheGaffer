package com.mcmiddleearth.thegaffer.messages;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;

public record JobCreateMessage(String jobName, String description) implements PluginMessage {
    
    public Subchannel subchannel() {
        return Subchannel.JOB_CREATED;
    }
    
    public void serialise(ByteArrayDataOutput out) {
        out.writeUTF(jobName);
        out.writeUTF(description);
    }

    public static JobCreateMessage deserialise(ByteArrayDataInput in) {
        String jobName = in.readUTF();
        String description = in.readUTF();
        return new JobCreateMessage(jobName, description);
    }
}