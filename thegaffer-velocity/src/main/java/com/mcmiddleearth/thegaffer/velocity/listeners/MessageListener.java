package com.mcmiddleearth.thegaffer.velocity.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.mcmiddleearth.thegaffer.velocity.ChannelIdentifiers;
import com.mcmiddleearth.thegaffer.velocity.VelocityGafferPlugin;
import com.mcmiddleearth.thegaffer.velocity.jobs.Job;
import com.mcmiddleearth.thegaffer.velocity.jobs.JobManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;

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

        // Q: Why is this in BETA? but not in mcme-warps?
        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        String subchannel = in.readUTF();
        String jobName = in.readUTF();

        switch (subchannel) {
            // TODO: Broadcast on job create (+ job delete?)
            case "CREATE" -> JobManager.addJob(backendName, new Job(jobName, player.getUsername()));
            case "DELETE" -> JobManager.removeJob(backendName, jobName);
            default -> VelocityGafferPlugin.getLogger().warn("Subchannel '{}' has no handler!", subchannel);
        }
    }
}
