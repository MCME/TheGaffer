package com.mcmiddleearth.thegaffer.utilities;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mcmiddleearth.thegaffer.messages.PluginMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class PluginMessenger {
    
    private final JavaPlugin plugin;
    private final String channel;
    
    public PluginMessenger(JavaPlugin plugin, String channel) {
        this.plugin = plugin;
        this.channel = channel;
    }

    // Q: What to name this? It'll be handled by the proxy
    public void sendToPlayer(Player player, PluginMessage message) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(message.subchannel().toString());
        message.serialise(out);
        player.sendPluginMessage(plugin, channel, out.toByteArray());
    }
}