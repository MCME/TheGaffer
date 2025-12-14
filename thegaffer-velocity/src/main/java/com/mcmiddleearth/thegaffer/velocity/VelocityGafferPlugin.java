package com.mcmiddleearth.thegaffer.velocity;

import com.google.inject.Inject;
import com.mcmiddleearth.thegaffer.Permission;
import com.mcmiddleearth.thegaffer.velocity.helpers.ServerConnectUtils;
import com.mcmiddleearth.thegaffer.velocity.jobs.JobManager;
import com.mcmiddleearth.thegaffer.velocity.listeners.MessageListener;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.sound.Sound;
import org.slf4j.Logger;

import java.util.Optional;

// TODO: Derive version from gradle
@Plugin(id = "thegaffer", name = "TheGaffer-Proxy", version = "0.1.0-SNAPSHOT")
public class VelocityGafferPlugin {
    private static VelocityGafferPlugin instance;

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
    public void onLogin(ServerPostConnectEvent event) {
        if (JobManager.isEmpty()) return;

        // Sounds can't be played during login, so we wait for the first backend connection.
        // If there is no previous server, this is the player's initial join.
        RegisteredServer previousServer = event.getPreviousServer();

        if (previousServer != null) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission(Permission.JOIN.getNode())) return;

        // TODO: Slightly alter the messaging
        player.sendMessage(JobManager.getAllJobsComponent());
        // event.getPlayer().sendMessage(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "There is a job running! Use /job check to find out what it is!");

        // To play a sound with Velocity an emitter is required
        event.getPlayer().playSound(Sounds.ActiveJob, Sound.Emitter.self());
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
