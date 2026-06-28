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
package com.mcmiddleearth.thegaffer.commands;

import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.storage.JobDatabase;
import com.mcmiddleearth.thegaffer.utilities.JobChat;
import com.mcmiddleearth.thegaffer.utilities.PermissionsUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /jobchat (alias /jc): toggles sticky job chat, or with arguments sends a
 * single message to the player's job. Runs on the main thread, so job lookups
 * are safe.
 */
public class JobChatCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be a player to use job chat.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission(PermissionsUtil.getJoinPermission())) {
            player.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }
        Job job = JobDatabase.getJobWorking(player);
        if (job == null) {
            JobChat.clear(player.getName());
            player.sendMessage(ChatColor.RED + "You are not in a job.");
            return true;
        }
        if (args.length > 0) {
            String message = String.join(" ", args);
            job.sendToAll(ChatColor.AQUA + "[Job] " + ChatColor.RESET + player.getName() + ": " + ChatColor.WHITE + message);
            return true;
        }
        boolean on = JobChat.toggle(player.getName());
        if (on) {
            player.sendMessage(ChatColor.GREEN + "Job chat enabled - your messages now go to your job. Use /jc again to turn it off.");
        } else {
            player.sendMessage(ChatColor.GREEN + "Job chat disabled.");
        }
        return true;
    }
}
