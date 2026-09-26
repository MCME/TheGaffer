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
import com.mcmiddleearth.thegaffer.utilities.Msg;
import com.mcmiddleearth.thegaffer.utilities.PermissionsUtil;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/**
 * /jobchat (alias /jc): toggles sticky job chat, or with arguments sends a
 * single message to the player's job. Runs on the main thread, so job lookups
 * are safe.
 */
public class JobChatCommand implements CommandExecutor, TabCompleter {

    /**
     * The argument is free-text chat, not a player target — return no suggestions so
     * Bukkit doesn't fall back to offering online player names (which read as a target).
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("You must be a player to use job chat."));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission(PermissionsUtil.getJoinPermission())) {
            player.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
            return true;
        }
        Job job = JobDatabase.getJobWorking(player);
        if (job == null) {
            JobChat.clear(player.getName());
            player.sendMessage(Component.text("You are not in a job.", NamedTextColor.RED));
            return true;
        }
        if (args.length > 0) {
            String message = String.join(" ", args);
            // #17 — use shared Msg.jobChat formatter so one-off /jc is byte-for-byte identical to sticky chat
            job.sendToAll(Msg.jobChat(player.getName(), Component.text(message)));
            return true;
        }
        boolean on = JobChat.toggle(player.getName());
        if (on) {
            player.sendMessage(Component.text("Job chat enabled — your messages now go to job ", NamedTextColor.GREEN)
                    .append(Component.text(job.getName(), NamedTextColor.AQUA))
                    .append(Component.text(". Use /jc again to turn it off.", NamedTextColor.GREEN)));
        } else {
            player.sendMessage(Component.text("Job chat disabled.", NamedTextColor.GREEN));
        }
        return true;
    }
}
