package com.mcmiddleearth.thegaffer.velocity.helpers;

import com.mcmiddleearth.thegaffer.velocity.VelocityGafferPlugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.Optional;
import java.util.function.Consumer;

public final class ServerConnectUtils {

    private ServerConnectUtils() { }

    /**
     * Attempts to connect a player to a target server.
     * Handles errors and sends messages automatically.
     *
     * @param sender The player to connect
     * @param targetServerName Name of the target server
     * @param callback Runs on successful connection (provides the player's connection)
     */
    public static void connectPlayerToServer(
        Player sender,
        String targetServerName,
        Consumer<ServerConnection> callback
    ) {
        ProxyServer proxy = VelocityGafferPlugin.getProxy();
        Optional<RegisteredServer> optTargetServer = proxy.getServer(targetServerName);

        if (optTargetServer.isEmpty()) {
            sender.sendRichMessage(
                "<red>Server '<server>' not found!",
                Placeholder.unparsed("server", targetServerName)
            );
            return;
        }

        RegisteredServer targetServer = optTargetServer.get();
        sender.createConnectionRequest(targetServer).connect()
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    sender.sendRichMessage(
                        "<red>Unable to connect to server '<server>' - server may be offline!",
                        Placeholder.unparsed("server", targetServerName)
                    );
                    VelocityGafferPlugin.getLogger().error(
                        "Server '{}' may be offline! '{}' was unable to connect.",
                        targetServerName,
                        sender.getUsername()
                    );
                    return;
                }

                if (!result.isSuccessful()) {
                    Component reason = result.getReasonComponent()
                        .orElse(Component.text("Unknown reason"));
                    sender.sendRichMessage(
                        "<red>Unable to connect to server '<server>' - <reason>",
                        Placeholder.unparsed("server", targetServerName),
                        Placeholder.component("reason", reason)
                    );
                    return;
                }

                sender.getCurrentServer().ifPresent(callback);
            });
    }
}