package com.mcmiddleearth.thegaffer.velocity.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.mcmiddleearth.thegaffer.messages.JobCreateMessage;
import com.mcmiddleearth.thegaffer.messages.JobDeleteMessage;
import com.mcmiddleearth.thegaffer.messages.Subchannel;
import com.mcmiddleearth.thegaffer.velocity.ChannelIdentifiers;
import com.mcmiddleearth.thegaffer.velocity.Emojis;
import com.mcmiddleearth.thegaffer.velocity.Sounds;
import com.mcmiddleearth.thegaffer.velocity.VelocityGafferPlugin;
import com.mcmiddleearth.thegaffer.velocity.jobs.Job;
import com.mcmiddleearth.thegaffer.velocity.jobs.JobManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;

public class MessageListener {

    @Subscribe
    public void onPluginMessageFromBackend(PluginMessageEvent event) {
        if (!ChannelIdentifiers.MAIN_ID.equals(event.getIdentifier())) {
            return;
        }

        // Regardless of the source, set the message as handled so that the message never gets forwarded
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        // Only attempt parsing the data if the source is a backend server
        if (!(event.getSource() instanceof ServerConnection backend)) {
            return;
        }

        Player player = backend.getPlayer();
        String backendName = backend.getServerInfo().getName();

        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        Subchannel subchannel = Subchannel.from(in.readUTF());

        switch (subchannel) {
            case JOB_CREATED -> {
                JobCreateMessage message = JobCreateMessage.deserialise(in);
                handleJobCreation(backendName, message.jobName(), player, message.description());
            }
            case JOB_DELETED -> {
                JobDeleteMessage message = JobDeleteMessage.deserialise(in);
                JobManager.removeJob(backendName, message.jobName());
            }
            case null, default -> VelocityGafferPlugin.getLogger().warn("Subchannel '{}' from '{}' has no handler!", subchannel, backendName);
        }
    }

    private void handleJobCreation(String backendName, String jobName, Player creator, String description) {
        Job newJob = new Job(jobName, creator.getUsername(), backendName, description);
        JobManager.addJob(newJob);

        Component announcement = JobManager.buildJobBlock(
            newJob,
            "    %s NEW JOB AVAILABLE %s".formatted(Emojis.CLIPBOARD, Emojis.CLIPBOARD)
        );
        VelocityGafferPlugin.getProxy().sendMessage(announcement);

        // To play a sound with Velocity an emitter is required
        VelocityGafferPlugin.getProxy().playSound(Sounds.ActiveJob, Sound.Emitter.self());
    }
}