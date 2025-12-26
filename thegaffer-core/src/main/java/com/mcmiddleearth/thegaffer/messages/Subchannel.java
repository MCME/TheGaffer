package com.mcmiddleearth.thegaffer.messages;

import javax.annotation.Nullable;

public enum Subchannel {
    JOB_CREATED,
    JOB_DELETED;

    @Nullable
    public static Subchannel from(String subchannel) {
        try {
            return Subchannel.valueOf(subchannel);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}