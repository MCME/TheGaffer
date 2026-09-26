package com.mcmiddleearth.thegaffer.messages;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;

/**
 * Tells the proxy "this job exists on my server" without announcing it to anybody.
 *
 * <p>{@link JobCreateMessage} is an event -- a job just started -- and the proxy reacts to it by
 * broadcasting "NEW JOB AVAILABLE" network-wide with a sound. A backend also needs to be able to
 * restate the jobs it already has, because the proxy keeps its job list only in memory: restart the
 * proxy, or restart a backend holding a persisted running job, and the list is empty while the job
 * is very much still running. Replaying creates would re-announce old jobs to everyone on every
 * login, so the re-statement travels on its own subchannel instead.
 *
 * <p>Unlike the create message this one carries the creator, because the proxy otherwise takes it
 * from whoever's connection happened to carry the message -- fine for a job that just started (the
 * owner is usually online and preferred) but wrong for an arbitrary player logging in later.
 */
public record JobSyncMessage(String jobName, String creator, String description) implements PluginMessage {

    public Subchannel subchannel() {
        return Subchannel.JOB_SYNC;
    }

    public void serialise(ByteArrayDataOutput out) {
        out.writeUTF(jobName);
        out.writeUTF(creator);
        out.writeUTF(description);
    }

    public static JobSyncMessage deserialise(ByteArrayDataInput in) {
        String jobName = in.readUTF();
        String creator = in.readUTF();
        String description = in.readUTF();
        return new JobSyncMessage(jobName, creator, description);
    }
}
