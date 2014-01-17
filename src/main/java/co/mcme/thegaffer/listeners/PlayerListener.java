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
package co.mcme.thegaffer.listeners;

import co.mcme.thegaffer.storage.Job;
import co.mcme.thegaffer.storage.JobDatabase;
import co.mcme.thegaffer.utilities.PermissionsUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (JobDatabase.getActiveJobs().size() > 0) {
            event.getPlayer().sendMessage(ChatColor.GRAY + "There is a job running! Use /job check to find out what it is!");
        }
        if (event.getPlayer().hasPermission(PermissionsUtil.getCreatePermission())) {

        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (JobDatabase.getActiveJobs().size() > 0) {
            Job workingon = JobDatabase.getJobWorking(event.getPlayer());
            if (workingon != null) {
                if (event.getMessage().charAt(0) == '!') {
                    event.getMessage().replaceFirst("!", "");
                    return;
                }
                Player p = event.getPlayer();
                String[] chat = new String[2];
                chat[0] = event.getFormat().replace(event.getMessage(), "").replace("%1$s", p.getDisplayName()).replace("%2$s", "");
                chat[1] = event.getMessage();
                workingon.jobChat(p, chat);
                event.getRecipients().clear();
            }
        }
    }
}
