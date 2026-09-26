package com.mcmiddleearth.thegaffer.messages;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;

public record JobDeleteMessage(String jobName) implements PluginMessage {
    
    public Subchannel subchannel() {
        return Subchannel.JOB_DELETED;
    }
    
    public void serialise(ByteArrayDataOutput out) {
        out.writeUTF(jobName);
    }
    
    public static JobDeleteMessage deserialise(ByteArrayDataInput in) {
        String jobName = in.readUTF();
        return new JobDeleteMessage(jobName);
    }
}