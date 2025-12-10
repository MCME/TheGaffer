package com.mcmiddleearth.thegaffer.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

@Plugin(id = "thegaffer", name = "TheGaffer-Proxy", version = "0.1.0-SNAPSHOT")
public class VelocityGafferPlugin {
    private final ProxyServer proxy;
    private final Logger logger;

    @Inject
    public VelocityGafferPlugin(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("TheGaffer Velocity plugin (groundwork) enabled.");
    }
}
