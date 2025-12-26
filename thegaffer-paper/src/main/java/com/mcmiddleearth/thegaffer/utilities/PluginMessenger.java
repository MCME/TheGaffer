package com.mcmiddleearth.thegaffer.utilities;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mcmiddleearth.thegaffer.Channels;
import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.messages.PluginMessage;
import org.bukkit.entity.Player;

public final class PluginMessenger {
    private PluginMessenger() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Sends a plugin message to the player
     * The proxy server is expected to intercept and handle the message
     */
    public static void sendToPlayer(Player player, PluginMessage message) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(message.subchannel().toString());
        message.serialise(out);
        player.sendPluginMessage(TheGaffer.getPluginInstance(), Channels.MAIN, out.toByteArray());
    }
}