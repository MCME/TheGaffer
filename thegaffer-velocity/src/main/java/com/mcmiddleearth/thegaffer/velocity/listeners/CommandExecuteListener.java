package com.mcmiddleearth.thegaffer.velocity.listeners;

import com.mcmiddleearth.thegaffer.Permission;
import com.mcmiddleearth.thegaffer.velocity.Emojis;
import com.mcmiddleearth.thegaffer.velocity.helpers.ServerConnectUtils;
import com.mcmiddleearth.thegaffer.velocity.jobs.Job;
import com.mcmiddleearth.thegaffer.velocity.jobs.JobManager;
import com.mcmiddleearth.thegaffer.velocity.jobs.JoinRouting;
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
        else if (command.equals("job join")) {
            handleJobJoin(event, sender);
        }
        else {
            handleNamedJobJoin(event, sender, command);
        }
    }

    /**
     * {@code /job join <name>} typed by hand. The backend answers this fine for its own jobs, so the
     * proxy only steps in when the named job is on a different backend -- otherwise the command falls
     * through untouched and the backend keeps ownership of its own replies. {@link JoinRouting} holds
     * the decision and is unit-tested; this method only carries it out.
     *
     * <p>The permission is checked before moving anyone: without it the remote backend would refuse
     * the join anyway, and being teleported across the network first is worse than a plain refusal
     * from the server you are already standing on.
     */
    private void handleNamedJobJoin(CommandExecuteEvent event, Player sender, String command) {
        String currentServer = sender.getCurrentServer()
            .map(serverConnection -> serverConnection.getServerInfo().getName())
            .orElse("");

        JoinRouting.routeNamedJoin(command, currentServer, JobManager.allJobs())
            .ifPresent(transfer -> {
                if (!sender.hasPermission(Permission.JOIN.getNode())) {
                    return;
                }

                event.setResult(CommandExecuteEvent.CommandResult.denied());
                ServerConnectUtils.connectPlayerToServer(
                    sender,
                    transfer.targetServer(),
                    // Same reason as the bare form: forwardToServer would forward to the OLD server.
                    newConnection -> sender.spoofChatInput(transfer.commandToReplay())
                );
            });
    }

    private void handleJobCheck(CommandExecuteEvent event, Player sender) {
        event.setResult(CommandExecuteEvent.CommandResult.denied());

        if (!sender.hasPermission(Permission.JOIN.getNode())) {
            sender.sendRichMessage("<red>You don't have permission.");
            return;
        }

        sender.sendMessage(JobManager.buildJobsList(
            Emojis.CLIPBOARD + " Available Jobs " + Emojis.CLIPBOARD
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
                newConnection -> sender.spoofChatInput("/job join")
            );
        });
    }
}
