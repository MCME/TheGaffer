package com.mcmiddleearth.thegaffer.velocity.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.mcmiddleearth.thegaffer.velocity.ChannelIdentifiers;
import com.mcmiddleearth.thegaffer.velocity.Sounds;
import com.mcmiddleearth.thegaffer.velocity.VelocityGafferPlugin;
import com.mcmiddleearth.thegaffer.velocity.helpers.ServerConnectUtils;
import com.mcmiddleearth.thegaffer.velocity.jobs.Job;
import com.mcmiddleearth.thegaffer.velocity.jobs.JobManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class MessageListener {

    private static final MiniMessage mm = MiniMessage.miniMessage();

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

        // Q: Broadcast on job deletion?
        // TODO: Magic strings
        switch (subchannel) {
            case "CREATE" -> handleJobCreation(backendName, jobName, player);
            case "DELETE" -> JobManager.removeJob(backendName, jobName);
            default -> VelocityGafferPlugin.getLogger().warn("Subchannel '{}' has no handler!", subchannel);
        }
    }

    private void handleJobCreation(String backendName, String jobName, Player creator) {
        JobManager.addJob(backendName, new Job(jobName, creator.getUsername()));

        String creatorName = creator.getUsername();

        Component separator = mm.deserialize(
            "<gradient:#5e4fa2:red:#5e4fa2>~~~~~~~~~~~~~~~~~~~~~~</gradient>"
        );

        Component title = Component.text(" 📋 NEW JOB AVAILABLE 📋")
            .color(NamedTextColor.GOLD)
            .decorate(TextDecoration.BOLD);

        Component jobLine = Component.text(" Job: ")
            .color(NamedTextColor.GRAY)
            .append(Component.text(jobName, NamedTextColor.AQUA));

        Component creatorLine = Component.text(" Created by: ")
            .color(NamedTextColor.GRAY)
            .append(Component.text(creatorName)
                .color(NamedTextColor.GREEN));

//        Component descriptionLine = Component.text(" Description: ")
//            .color(NamedTextColor.GRAY)
//            .append(Component.text(jobDescription, NamedTextColor.AQUA));

        Component joinButton = Component.text("    ⟫ CLICK TO JOIN ⟪")
            .color(NamedTextColor.AQUA)
            .decorate(TextDecoration.BOLD)
            .hoverEvent(HoverEvent.showText(
                Component.text("Click to join this job", NamedTextColor.LIGHT_PURPLE)
            ))
            .clickEvent(
                // TODO: Reusable click to join event?
                ClickEvent.callback(audience -> {
                    if (!(audience instanceof Player player)) {
                        return;
                    }

                    player.getCurrentServer().ifPresent(serverConnection -> {
                        String playerServer = serverConnection.getServerInfo().getName();

                        if (playerServer.equalsIgnoreCase(backendName)) {
                            player.spoofChatInput("/job join " + jobName);
                            return;
                        }

                        ServerConnectUtils.connectPlayerToServer(
                            player,
                            backendName,
                            // Unable to use forwardToServer - because the command would be forwarded to the original server
                            targetServer -> player.spoofChatInput("/job join " + jobName)
                        );
                    });
                })
            );

        Component newJobMessage = Component.empty()
            .append(separator)
            .appendNewline()
            .append(title)
            .appendNewline().appendNewline()
            .append(jobLine)
            .appendNewline()
            .append(creatorLine)
            .appendNewline().appendNewline()
//            .append(descriptionLine)
//            .appendNewline().appendNewline()
            .append(joinButton)
            .appendNewline()
            .append(separator);

        VelocityGafferPlugin.getProxy().sendMessage(newJobMessage);

        // To play a sound with Velocity an emitter is required
       VelocityGafferPlugin.getProxy().playSound(Sounds.ActiveJob, Sound.Emitter.self());
    }
}