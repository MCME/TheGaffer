package com.mcmiddleearth.thegaffer.messages;

import com.google.common.io.ByteArrayDataOutput;

public interface PluginMessage {
    Subchannel subchannel();
    
    void serialise(ByteArrayDataOutput out);
}