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
package com.mcmiddleearth.thegaffer.listeners;

import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.storage.JobDatabase;
import com.mcmiddleearth.thegaffer.utilities.JobChat;
import com.mcmiddleearth.thegaffer.utilities.Msg;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Routes a player's chat to their job when they have sticky job chat enabled.
 *
 * AsyncChatEvent fires off the main thread, so this handler does no off-thread
 * reads of job state: it checks only the concurrent toggle, then cancels the
 * event and hops to the main thread, where job membership can be read safely
 * via the existing accessors. Cancelling also keeps job chat private (it does
 * not flow to the main chat or the Discord bridge), which is the intended
 * channel behaviour.
 */
public class JobChatListener implements Listener {

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        final Player sender = event.getPlayer();
        if (!JobChat.isSticky(sender.getName())) {
            return;
        }
        event.setCancelled(true);
        final Component body = event.message();
        Bukkit.getScheduler().runTask(TheGaffer.getPluginInstance(), () -> {
            Job job = JobDatabase.getJobWorking(sender);
            if (job == null) {
                JobChat.clear(sender.getName());
                sender.sendMessage(Component.text("You are not in a job - job chat turned off.", NamedTextColor.RED));
                return;
            }
            // #17 — use shared Msg.jobChat formatter (identical to the one-off /jc path)
            Component formatted = Msg.jobChat(sender.getName(), body);
            for (Player member : job.getAllAsPlayersArray()) {
                member.sendMessage(formatted);
            }
        });
    }
}
