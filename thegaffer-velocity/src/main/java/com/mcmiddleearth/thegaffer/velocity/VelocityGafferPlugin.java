package com.mcmiddleearth.thegaffer.velocity;

import com.google.inject.Inject;
import com.mcmiddleearth.thegaffer.Permission;
import com.mcmiddleearth.thegaffer.velocity.jobs.Job;
import com.mcmiddleearth.thegaffer.velocity.jobs.JobManager;
import com.mcmiddleearth.thegaffer.velocity.listeners.CommandExecuteListener;
import com.mcmiddleearth.thegaffer.velocity.listeners.MessageListener;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;

import java.util.Optional;

// TODO: Derive version from gradle
@Plugin(id = "thegaffer", name = "TheGaffer-Proxy", version = "0.1.0-SNAPSHOT")
public class VelocityGafferPlugin {

    public static final MiniMessage mm = MiniMessage.builder()
        .editTags(t -> t.resolver(Tags.player))
        .build();

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

        proxy.getEventManager().register(this, new CommandExecuteListener());
    }

    @Subscribe
    public void onServerConnect(ServerPostConnectEvent event) {
        if (JobManager.isEmpty()) return;

        // Sounds can't be played during login, so we have to use the first server connection
        // (If there is no previous server, this is the player's initial join)
        RegisteredServer previousServer = event.getPreviousServer();
        if (previousServer != null) return;

        Player player = event.getPlayer();
        if (!player.hasPermission(Permission.JOIN.getNode())) return;

        Optional<Job> singleJob = JobManager.getSingleJob();
        if (singleJob.isEmpty()) {
            // There's more than one job - show a list
            player.sendMessage(JobManager.buildJobsList(
                " %s There are jobs running %s".formatted(Emojis.HAMMER, Emojis.HAMMER)
            ));
        } else {
            player.sendMessage(JobManager.buildJobBlock(
                singleJob.get(),
                " %s There is a job running %s".formatted(Emojis.HAMMER, Emojis.HAMMER)
            ));
        }

        // To play a sound with Velocity an emitter is required
        player.playSound(Sounds.ActiveJob, Sound.Emitter.self());
    }
}
