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

import com.mcmiddleearth.thegaffer.commands.AdminCommands.JobAdminCommands;
import com.mcmiddleearth.thegaffer.GafferResponses.GafferResponse;
import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.storage.JobDatabase;
import com.mcmiddleearth.thegaffer.storage.JobStats;
import com.mcmiddleearth.thegaffer.utilities.CleanupUtil;
import com.mcmiddleearth.thegaffer.utilities.Msg;
import com.mcmiddleearth.thegaffer.utilities.PermissionsUtil;
import com.mcmiddleearth.thegaffer.utilities.StatsManager;
import com.mcmiddleearth.thegaffer.utilities.Util;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.ChatPaginator;

public class JobCommand implements TabExecutor {

    private HashMap<Player, InvHolder> invs = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof ConsoleCommandSender && args.length > 0 && args[0].equalsIgnoreCase("reloadConfig")) {
            TheGaffer.getPluginInstance().reloadConfig();
            TheGaffer.setupConfig();
            sender.sendMessage("Configuration reloaded from config.yml");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("You are not a player");
            return true;
        }
        if (args.length > 0) {
            Player player = (Player) sender;
            if (args.length >= 2) {
                if (args[0].equalsIgnoreCase("stop")) {
                    if (player.hasPermission(PermissionsUtil.getCreatePermission())) {
                        if (args[1] != null) {
                            String jobname = args[1];
                            if (JobDatabase.getActiveJobs().containsKey(jobname)) {

                                JobDatabase.deactivateJob(JobDatabase.getActiveJobs().get(jobname));
                                player.sendMessage(Component.text("Successfully closed the ", NamedTextColor.GRAY)
                                        .append(Component.text(jobname, NamedTextColor.AQUA))
                                        .append(Component.text(" job.", NamedTextColor.GRAY)));
                            } else {
                                player.sendMessage(Component.text("No job found by that name.", NamedTextColor.RED));
                            }
                        } else {
                            player.sendMessage(Component.text("You must provide a job name.", NamedTextColor.RED));
                        }
                    } else {
                        player.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
                    }
                    return true;
                }
                if (args[0].equalsIgnoreCase("pause")) {
                    if (player.hasPermission(PermissionsUtil.getCreatePermission())) {
                        if (args[1] != null) {
                            String jobname = args[1];
                            if (JobDatabase.getActiveJobs().containsKey(jobname)) {
                                Job target = JobDatabase.getActiveJobs().get(jobname);
                                target.pauseJob(player.getName());
                                player.sendMessage(Component.text("Paused " + target.getName(), NamedTextColor.GREEN));
                            } else {
                                player.sendMessage(Component.text("No job found by that name.", NamedTextColor.RED));
                            }
                        } else {
                            player.sendMessage(Component.text("You must provide a job name.", NamedTextColor.RED));
                        }
                    } else {
                        player.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
                    }
                    return true;
                }
                if (args[0].equalsIgnoreCase("unpause")) {
                    if (player.hasPermission(PermissionsUtil.getCreatePermission())) {
                        if (args[1] != null) {
                            String jobname = args[1];
                            if (JobDatabase.getActiveJobs().containsKey(jobname)) {
                                Job target = JobDatabase.getActiveJobs().get(jobname);
                                target.unpauseJob(player.getName());
                                player.sendMessage(Component.text("Un paused " + target.getName(), NamedTextColor.GREEN));
                            } else {
                                player.sendMessage(Component.text("No job found by that name.", NamedTextColor.RED));
                            }
                        } else {
                            player.sendMessage(Component.text("You must provide a job name.", NamedTextColor.RED));
                        }
                    } else {
                        player.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
                    }
                    return true;
                }
            }
            if (args[0].equalsIgnoreCase("listen")) {
                if (player.hasPermission(PermissionsUtil.getCreatePermission())) {
                    if (TheGaffer.getListening().contains(player)) {
                        TheGaffer.getListening().remove(player);
                        player.sendMessage(Component.text("Removed from protection listening.", NamedTextColor.GREEN));
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 2f);
                    } else {
                        TheGaffer.getListening().add(player);
                        player.sendMessage(Component.text("Added to protection listening, you will now be notified when someone tries to edit the map outside of a job.", NamedTextColor.GREEN));
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 0.5f);
                    }
                } else {
                    player.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("prep")) {
                if (player.hasPermission(PermissionsUtil.getCreatePermission())) {
                    if (invs.containsKey(player)) {
                        InvHolder holder = invs.get(player);
                        player.getInventory().clear();
                        player.getInventory().setArmorContents(holder.getArmorContents());
                        player.getInventory().setContents(holder.getContents());
                        player.sendMessage(Component.text("Recovered previous inventory.", NamedTextColor.GREEN));
                        invs.remove(player);
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 2f);
                        player.updateInventory();
                    } else {
                        invs.put(player, new InvHolder(player.getInventory()));
                        player.getInventory().clear();
                        player.sendMessage(Component.text("Stored your inventory.", NamedTextColor.GREEN));
                        player.sendMessage(Component.text("When ready, run this command again to get your inventory back.", NamedTextColor.GREEN));
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 0.5f);
                        player.updateInventory();
                    }
                } else {
                    player.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("aero")) {
                player.sendMessage(Util.dino);
                return true;
            }
            if (args[0].equalsIgnoreCase("debug")) {
                if (player.hasPermission(PermissionsUtil.getCreatePermission())) {
                    player.sendMessage(Component.text("TheGaffer Debug", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)
                            .append(Component.newline())
                            .append(Component.text("Number of active jobs: ", NamedTextColor.GRAY))
                            .append(Component.text(String.valueOf(JobDatabase.getActiveJobs().size()), NamedTextColor.AQUA))
                            .append(Component.newline())
                            .append(Component.text("Number of inactive jobs: ", NamedTextColor.GRAY))
                            .append(Component.text(String.valueOf(JobDatabase.getInactiveJobs().size()), NamedTextColor.AQUA))
                            .append(Component.newline())
                            .append(Component.text("Number of jobs timing out: ", NamedTextColor.GRAY))
                            .append(Component.text(String.valueOf(CleanupUtil.getWaiting().size()), NamedTextColor.AQUA))
                            .append(Component.newline())
                            .append(Component.text("Join permission: ", NamedTextColor.GRAY))
                            .append(Component.text(PermissionsUtil.getJoinPermission().getName(), NamedTextColor.AQUA))
                            .append(Component.newline())
                            .append(Component.text("Ignore protection permission: ", NamedTextColor.GRAY))
                            .append(Component.text(PermissionsUtil.getIgnoreWorldProtection().getName(), NamedTextColor.AQUA))
                            .append(Component.newline())
                            .append(Component.text("Create permission: ", NamedTextColor.GRAY))
                            .append(Component.text(PermissionsUtil.getCreatePermission().getName(), NamedTextColor.AQUA)));
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("check")) {
                if (player.hasPermission(PermissionsUtil.getJoinPermission())) {
                    if (JobDatabase.getActiveJobs().size() > 0) {
                        Component out = Component.text("Running Jobs:", NamedTextColor.GRAY);
                        for (String jobName : JobDatabase.getActiveJobs().keySet()) {
                            Job job = JobDatabase.getActiveJobs().get(jobName);
                            out = out.append(Component.newline())
                                    .append(Msg.button(jobName, NamedTextColor.AQUA, "/job join " + jobName, "Click to join " + jobName))
                                    .append(Component.text(" with " + Util.nameOf(job.getOwner()) + " (" + job.getWorkers().size() + ")", NamedTextColor.GRAY));
                        }
                        player.sendMessage(out);
                    } else {
                        player.sendMessage(Component.text("No jobs currently running.", NamedTextColor.GRAY));
                    }
                } else {
                    player.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("join")) {
                if (player.hasPermission(PermissionsUtil.getJoinPermission())) {
                    if (JobDatabase.getActiveJobs().size() > 0) {
                        if (args.length > 1 || JobDatabase.getActiveJobs().size()==1) {
                            if ((args.length == 1 && JobDatabase.getActiveJobs().size()==1)
                                    || (args.length > 1 && JobDatabase.getActiveJobs().containsKey(args[1]))) {
                                Job jobToJoin = (args.length>1?
                                                 JobDatabase.getActiveJobs().get(args[1]):
                                                 JobDatabase.getActiveJobs().firstEntry().getValue());
                                GafferResponse resp = jobToJoin.addWorker(player);
                                if (resp.isSuccessful()) {
                                    player.sendMessage(Component.text("You have joined the job ", NamedTextColor.GRAY)
                                            .append(Component.text(jobToJoin.getName(), NamedTextColor.AQUA)));
                                } else {
                                    player.sendMessage(Component.text("Error: " + resp.getMessage().replaceAll("%name%", player.getName()).replaceAll("%job%", jobToJoin.getName()), NamedTextColor.RED));
                                }
                            } else {
                                player.sendMessage(Component.text("No job running by the name of `" + args[1] + "`", NamedTextColor.RED));
                            }
                        } else {
                            player.sendMessage(Component.text("You must provide the name of the job you would like to join.", NamedTextColor.RED));
                        }
                    } else {
                        player.sendMessage(Component.text("No jobs currently running.", NamedTextColor.RED));
                    }
                } else {
                    player.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
                }
                return true;
            }
            if(args[0].equalsIgnoreCase("leave")){
                if(player.hasPermission(PermissionsUtil.getJoinPermission())){
                    if (JobDatabase.getActiveJobs().size() > 0){
                        Job jobToLeave = JobDatabase.getJobWorking(player);
                        if(jobToLeave != null) {
                            GafferResponse resp = jobToLeave.leaveJob(player);
                            if (resp.isSuccessful()) {
                                player.sendMessage(Component.text("You left the job ", NamedTextColor.GRAY)
                                        .append(Component.text(jobToLeave.getName(), NamedTextColor.AQUA)));
                            } else {
                                player.sendMessage(Component.text("Error: " + resp.getMessage().replaceAll("%name%", player.getName()).replaceAll("%job%", jobToLeave.getName()), NamedTextColor.RED));
                            }
                        }else{
                            player.sendMessage(Component.text("You are not part of a job.", NamedTextColor.RED));
                        }
                    }else {
                        player.sendMessage(Component.text("No jobs currently running.", NamedTextColor.RED));
                    }
                }else{
                    player.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("info")) {
                if (args.length > 1) {
                    if (JobDatabase.getActiveJobs().containsKey(args[1])) {
                        Job jobToJoin = JobDatabase.getActiveJobs().get(args[1]);
                        player.sendMessage(jobToJoin.getInfo());
                    } else if (JobDatabase.getInactiveJobs().containsKey(args[1])) {
                        Job jobToJoin = JobDatabase.getInactiveJobs().get(args[1]);
                        player.sendMessage(jobToJoin.getInfo());
                    } else {
                        player.sendMessage(Component.text("No job found by the name of `" + args[1] + "`", NamedTextColor.RED));
                    }
                } else {
                    player.sendMessage(Component.text("What job would you like to get info on?", NamedTextColor.RED));
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("warpto")) {
                if (player.hasPermission(PermissionsUtil.getJoinPermission())) {
                    if (JobDatabase.getActiveJobs().size() > 0) {
                        if (args.length > 1) {
                            if (JobDatabase.getActiveJobs().containsKey(args[1])) {
                                Job jobToJoin = JobDatabase.getActiveJobs().get(args[1]);
                                player.teleport(jobToJoin.getWarp().toBukkitLocation());
                                player.sendMessage(Component.text("Warped to ", NamedTextColor.GRAY)
                                        .append(Component.text(jobToJoin.getName(), NamedTextColor.AQUA)));
                            } else {
                                player.sendMessage(Component.text("No job running by the name of `" + args[1] + "`", NamedTextColor.RED));
                            }
                        } else {
                            player.sendMessage(Component.text("You must provide the name of the job you would like to warp to.", NamedTextColor.RED));
                        }
                    } else {
                        player.sendMessage(Component.text("No jobs currently running.", NamedTextColor.RED));
                    }
                } else {
                    player.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("archive")) {
                if (player.hasPermission(PermissionsUtil.getJoinPermission())) {
                    if (JobDatabase.getInactiveJobs().size() > 0) {
                        // Archive uses ChatPaginator, which works on legacy strings, so this
                        // listing stays ChatColor-based (paginated output, not interactive).
                        StringBuilder out = new StringBuilder();
                        int pageNum = 1;
                        boolean first = true;
                        for (String jobName : JobDatabase.getInactiveJobs().keySet()) {
                            Job job = JobDatabase.getInactiveJobs().get(jobName);
                            if (!first) {
                                out.append("\n");
                            }
                            out.append(ChatColor.AQUA).append(job.getName()).append(ChatColor.GRAY).append(" with ").append(Util.nameOf(job.getOwner())).append(" (").append(job.getWorkers().size()).append(")");
                            if (first) {
                                first = false;
                            }
                        }
                        if (args.length > 1) {
                            try {
                                pageNum = Integer.parseInt(args[1]);
                            } catch (NumberFormatException ex) {
                                pageNum = 1;
                            }
                        }
                        ChatPaginator.ChatPage page = ChatPaginator.paginate(out.toString(), pageNum, ChatPaginator.AVERAGE_CHAT_PAGE_WIDTH, 8);
                        player.sendMessage(ChatColor.AQUA + "Job Archive Page: " + page.getPageNumber() + " of " + page.getTotalPages());
                        player.sendMessage(page.getLines());
                    } else {
                        player.sendMessage(Component.text("No jobs found in archive.", NamedTextColor.GRAY));
                    }
                } else {
                    player.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("create")) {
                Bukkit.getServer().dispatchCommand(sender, "createjob");
                return true;
            }
            if (args[0].equalsIgnoreCase("stats")) {
                if (!player.hasPermission(PermissionsUtil.getJoinPermission())) {
                    player.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
                    return true;
                }
                if (args.length > 1 && args[1].equalsIgnoreCase("export")) {
                    if (!player.hasPermission(PermissionsUtil.getCreatePermission())) {
                        player.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
                        return true;
                    }
                    java.io.File f = StatsManager.exportAll(System.currentTimeMillis());
                    player.sendMessage(f != null
                            ? Component.text("Exported to " + f.getName(), NamedTextColor.GREEN)
                            : Component.text("Export failed (see console).", NamedTextColor.RED));
                    return true;
                }
                if (args.length > 1) {
                    // Try to find a job by that name first; if not found treat as a player name
                    JobStats js = StatsManager.findJobStats(args[1]);
                    if (js != null) {
                        player.sendMessage(StatsManager.renderJobStats(js));
                    } else {
                        StatsManager.PlayerAggregate a = StatsManager.findPlayerTotalsByName(args[1]);
                        if (a != null) {
                            player.sendMessage(Component.text(Util.nameOf(a.getId()) + ": ", NamedTextColor.AQUA)
                                    .append(Component.text(a.getPlaced() + " placed, " + a.getBroke()
                                            + " broken across " + a.getJobs() + " jobs", NamedTextColor.GRAY)));
                        } else {
                            player.sendMessage(Component.text("No job or player found by that name.", NamedTextColor.RED));
                        }
                    }
                } else {
                    player.sendMessage(Component.text("Usage: /job stats <job|player>", NamedTextColor.RED));
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("leaderboard") || args[0].equalsIgnoreCase("top")) {
                if (!player.hasPermission(PermissionsUtil.getJoinPermission())) {
                    player.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
                    return true;
                }
                StatsManager.SortKey key = StatsManager.SortKey.PLACED;
                if (args.length > 1 && args[1].equalsIgnoreCase("broke")) { key = StatsManager.SortKey.BROKE; }
                else if (args.length > 1 && args[1].equalsIgnoreCase("active")) { key = StatsManager.SortKey.ACTIVE; }
                Component out = Component.text("Top builders (" + key.name().toLowerCase() + "):", NamedTextColor.GRAY);
                int rank = 1;
                for (StatsManager.PlayerAggregate a : StatsManager.getLeaderboard(key, 10)) {
                    out = out.append(Component.newline())
                            .append(Component.text(rank++ + ". ", NamedTextColor.GRAY))
                            .append(Msg.button(Util.nameOf(a.getId()), NamedTextColor.AQUA,
                                    "/job stats " + Util.nameOf(a.getId()), "View stats"))
                            .append(Component.text("  " + a.getPlaced() + " placed / " + a.getBroke() + " broken", NamedTextColor.GRAY));
                }
                if (rank == 1) {
                    out = out.append(Component.newline())
                            .append(Component.text("No stats recorded yet.", NamedTextColor.GRAY));
                }
                player.sendMessage(out);
                return true;
            }
            if (args[0].equalsIgnoreCase("admin")) {
                JobAdminCommands jAC = new JobAdminCommands();
                return jAC.onCommand(sender, command, label, args);
            }
            return false;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args[0].equalsIgnoreCase("archive")) {
            return null;
        }
        if (args[0].equalsIgnoreCase("check")) {
            return null;
        }
        if (args[0].equalsIgnoreCase("info")) {
            List<String> jobs = new ArrayList<>();
            if (args[1] == null) {
                jobs.addAll(JobDatabase.getActiveJobs().keySet());
                jobs.addAll(JobDatabase.getInactiveJobs().keySet());
            } else {
                for (String s : JobDatabase.getActiveJobs().keySet()) {
                    if (s.startsWith(args[1])) {
                        jobs.add(s);
                    }
                }
                for (String s : JobDatabase.getInactiveJobs().keySet()) {
                    if (s.startsWith(args[1])) {
                        jobs.add(s);
                    }
                }
                if (jobs.isEmpty()) {
                    return null;
                }
            }
            Set<String> jobsUnique = new HashSet<>(jobs);
            jobs.removeAll(jobs);
            jobs.addAll(jobsUnique);
            return jobs;
        }
        if (args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase("stop")
                || args[0].equalsIgnoreCase("pause") || args[0].equalsIgnoreCase("unpause")
                || args[0].equalsIgnoreCase("warpto")) {
            List<String> jobs = new ArrayList<>();

            if (args.length > 1) {
                if (args[1] == null) {
                    jobs.addAll(JobDatabase.getActiveJobs().keySet());
                } else {
                    for (String s : JobDatabase.getActiveJobs().keySet()) {
                        if (s.startsWith(args[1])) {
                            jobs.add(s);
                        }
                    }
                    if (jobs.isEmpty()) {
                        return null;
                    }
                }
                Set<String> jobsUnique = new HashSet<>(jobs);
                jobs.removeAll(jobs);
                jobs.addAll(jobsUnique);
                return jobs;
            }
        }
        List<String> actions = new ArrayList<>();
        actions.add("archive");
        actions.add("warpto");
        actions.add("info");
        actions.add("join");
        actions.add("check");
        actions.add("leave");
        actions.add("stats");
        actions.add("leaderboard");
        actions.add("top");
        if (sender.hasPermission(PermissionsUtil.getCreatePermission())) {
            actions.add("stop");
            actions.add("debug");
            actions.add("prep");
            actions.add("pause");
            actions.add("unpause");
        }
        Collections.sort(actions);
        return actions;
    }
}
