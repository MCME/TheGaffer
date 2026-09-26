package com.mcmiddleearth.thegaffer.messages;

public enum Subchannel {
    JOB_CREATED,
    JOB_DELETED,
    /** A backend restating a job it already has, so the proxy can rebuild its list silently. */
    JOB_SYNC;

    /**
     * @return the matching subchannel, or {@code null} if the wire value is not one we know.
     *         A null here means a peer sent a subchannel this build does not understand —
     *         treat it as a version skew between proxy and backend, not as an error.
     */
    public static Subchannel from(String subchannel) {
        try {
            return Subchannel.valueOf(subchannel);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}