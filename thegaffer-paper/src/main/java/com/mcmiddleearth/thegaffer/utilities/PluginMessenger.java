/*  This file is part of TheGaffer.
 *
 *  TheGaffer is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  TheGaffer is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with TheGaffer.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mcmiddleearth.thegaffer.utilities;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mcmiddleearth.thegaffer.Channels;
import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.messages.PluginMessage;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.logging.Logger;

/**
 * Sends {@link PluginMessage}s to the Velocity proxy, which keeps the network-wide registry of
 * which job is running on which server (see the {@code thegaffer-velocity} module).
 *
 * <h3>Why a player is required</h3>
 * Bukkit has no server-to-proxy socket of its own: a plugin message rides an existing player
 * connection. With nobody online, a backend simply cannot talk to the proxy — so the proxy's view
 * of this server goes stale until someone joins. That is a property of the transport, not a bug
 * here, but it is silent, which is why {@link #sendViaAnyPlayer} logs when it happens.
 */
public final class PluginMessenger {

    private PluginMessenger() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /** Sends {@code message} to the proxy over {@code player}'s connection. */
    public static void sendToPlayer(Player player, PluginMessage message) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(message.subchannel().toString());
        message.serialise(out);
        player.sendPluginMessage(TheGaffer.getPluginInstance(), Channels.MAIN, out.toByteArray());
    }

    /**
     * Sends via {@code preferred} when they are online, otherwise via any online player.
     *
     * @return true if the message went out. False means nobody was online to carry it and the
     *         proxy's registry is now out of date for this server — logged, never silent.
     */
    public static boolean sendViaAnyPlayer(Player preferred, PluginMessage message, String what) {
        if (preferred != null && preferred.isOnline()) {
            sendToPlayer(preferred, message);
            return true;
        }
        Collection<? extends Player> online = TheGaffer.getServerInstance().getOnlinePlayers();
        for (Player p : online) {
            if (p.isOnline()) {
                sendToPlayer(p, message);
                return true;
            }
        }
        Logger.getLogger("TheGaffer").warning("Could not tell the proxy about " + what
                + ": a plugin message needs a player connection and nobody is online on this server."
                + " The proxy's job registry will be stale until this server is told again.");
        return false;
    }
}
