package com.mcmiddleearth.thegaffer.velocity.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.mcmiddleearth.thegaffer.messages.JobCreateMessage;
import com.mcmiddleearth.thegaffer.messages.JobDeleteMessage;
import com.mcmiddleearth.thegaffer.messages.JobSyncMessage;
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
            case JOB_SYNC -> {
                // A backend restating a job it already has. Registry only: no broadcast, no sound,
                // or every login would re-announce every running job to the whole network.
                JobSyncMessage message = JobSyncMessage.deserialise(in);
                JobManager.addJob(new Job(
                    message.jobName(), message.creator(), backendName, message.description()));
            }
            case null, default -> VelocityGafferPlugin.getLogger().warn("Subchannel '{}' from '{}' has no handler!", subchannel, backendName);
        }
    }

    private void handleJobCreation(String backendName, String jobName, Player creator, String description) {
        Job newJob = new Job(jobName, creator.getUsername(), backendName, description);
        JobManager.addJob(newJob);

        Component announcement = JobManager.buildJobBlock(
            newJob,
            "%s NEW JOB AVAILABLE %s".formatted(Emojis.CLIPBOARD, Emojis.CLIPBOARD)
        );

        // Announce to the rest of the network only. The backend that started the job already told
        // its own players, with a world-specific message and a one-click button that needs no
        // transfer -- broadcasting to everyone would show them the same job twice. This is the half
        // the backend cannot do: reach players who would have to change server to take part.
        VelocityGafferPlugin.getProxy().getAllPlayers().stream()
            .filter(player -> !isOn(player, backendName))
            .forEach(player -> {
                player.sendMessage(announcement);
                // To play a sound with Velocity an emitter is required
                player.playSound(Sounds.ActiveJob, Sound.Emitter.self());
            });
    }

    /** True when {@code player} is currently connected to the backend called {@code serverName}. */
    private static boolean isOn(Player player, String serverName) {
        return player.getCurrentServer()
            .map(connection -> connection.getServerInfo().getName().equalsIgnoreCase(serverName))
            .orElse(false);
    }
}