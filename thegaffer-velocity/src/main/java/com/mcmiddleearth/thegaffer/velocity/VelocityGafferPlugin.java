package com.mcmiddleearth.thegaffer.velocity;

import com.google.inject.Inject;
import com.mcmiddleearth.thegaffer.velocity.helpers.ServerConnectUtils;
import com.mcmiddleearth.thegaffer.velocity.jobs.JobManager;
import com.mcmiddleearth.thegaffer.velocity.listeners.MessageListener;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.util.Optional;

// TODO: Derive version from gradle
@Plugin(id = "thegaffer", name = "TheGaffer-Proxy", version = "0.1.0-SNAPSHOT")
public class VelocityGafferPlugin {
    private static VelocityGafferPlugin instance;

    private static final String jobJoinSpace = "job join ";

    private final ProxyServer proxy;
    private final Logger logger;

    @Inject
    public VelocityGafferPlugin(ProxyServer proxy, Logger logger) {
        instance = this;

        this.proxy = proxy;
        this.logger = logger;
    }

    private static VelocityGafferPlugin getInstance() { return instance; }
    public static ProxyServer getProxy() { return getInstance().proxy; }
    public static Logger getLogger() { return getInstance().logger; }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        proxy.getEventManager().register(this, new MessageListener());
        proxy.getChannelRegistrar().register(ChannelIdentifiers.MAIN_ID);
    }

    @Subscribe
    public void onCommand(CommandExecuteEvent event) {
        // Q: Move inside job check/join? Prevents checking source if command doesn't match!
        if (!(event.getCommandSource() instanceof Player sender)) {
            return;
        }

        String command = event.getCommand().trim();

        // TODO: switch? + Extract handlers
        if (command.startsWith("job check")) {
            event.setResult(CommandExecuteEvent.CommandResult.denied());
            sender.sendMessage(JobManager.getAllJobsComponent());
        }
        else if (command.equals("job join")) {
            event.setResult(CommandExecuteEvent.CommandResult.denied());

            // Q: Simplify? All I need is the server name
           Optional<JobManager.ServerJob> singleJob = JobManager.getSingleJob();
           if (singleJob.isEmpty()) {
               // Either 0 jobs or >1 jobs
               sender.sendMessage(JobManager.getAllJobsComponent());
               return;
           }

           var serverJob = singleJob.get();
           sender.getCurrentServer().ifPresent(serverConnection -> {
               String playerServer = serverConnection.getServerInfo().getName();

               if (playerServer.equalsIgnoreCase(serverJob.serverName())) {
                   event.setResult(CommandExecuteEvent.CommandResult.forwardToServer());
                   return;
               }

               ServerConnectUtils.connectPlayerToServer(
                   sender,
                   serverJob.serverName(),
                   // Unable to use forwardToServer - because the command would be forwarded to the original server
                   targetServer -> sender.spoofChatInput("/job join")
               );
           });
        }
    }
}
