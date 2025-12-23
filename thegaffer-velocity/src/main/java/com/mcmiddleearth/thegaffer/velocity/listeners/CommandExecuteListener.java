package com.mcmiddleearth.thegaffer.velocity.listeners;

import com.mcmiddleearth.thegaffer.Permission;
import com.mcmiddleearth.thegaffer.velocity.Emojis;
import com.mcmiddleearth.thegaffer.velocity.helpers.ServerConnectUtils;
import com.mcmiddleearth.thegaffer.velocity.jobs.Job;
import com.mcmiddleearth.thegaffer.velocity.jobs.JobManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.proxy.Player;

import java.util.Optional;

public class CommandExecuteListener {

    @Subscribe
    public void onCommand(CommandExecuteEvent event) {
        if (!(event.getCommandSource() instanceof Player sender)) {
            return;
        }

        String command = event.getCommand().trim();

        if (command.startsWith("job check")) {
            handleJobCheck(event, sender);
        }
        // Q: Intercept /job join <name> as well? Warn the player to just use /job join???
        else if (command.equals("job join")) {
            handleJobJoin(event, sender);
        }
    }

    private void handleJobCheck(CommandExecuteEvent event, Player sender) {
        event.setResult(CommandExecuteEvent.CommandResult.denied());

        if (!sender.hasPermission(Permission.JOIN.getNode())) {
            sender.sendRichMessage("<red>You don't have permission.");
            return;
        }

        sender.sendMessage(JobManager.buildJobsList(
            "  " + Emojis.CLIPBOARD + " Available Jobs " + Emojis.CLIPBOARD
        ));
    }

    private void handleJobJoin(CommandExecuteEvent event, Player sender) {
        event.setResult(CommandExecuteEvent.CommandResult.denied());

        if (!sender.hasPermission(Permission.JOIN.getNode())) {
            sender.sendRichMessage("<red>You don't have permission.");
            return;
        }

        Optional<Job> singleJob = JobManager.getSingleJob();
        if (singleJob.isEmpty()) {
            // Either 0 jobs or >1 jobs
            sender.sendMessage(JobManager.buildJobsList("Select a job to join!"));
            return;
        }

        var jobServerName = singleJob.get().server();
        sender.getCurrentServer().ifPresent(serverConnection -> {
            String playerServer = serverConnection.getServerInfo().getName();

            if (playerServer.equalsIgnoreCase(jobServerName)) {
                event.setResult(CommandExecuteEvent.CommandResult.forwardToServer());
                return;
            }

            ServerConnectUtils.connectPlayerToServer(
                sender,
                jobServerName,
                // Unable to use 'forwardToServer' - because the command would be forwarded to the original server
                targetServer -> sender.spoofChatInput("/job join")
            );
        });
    }
}
