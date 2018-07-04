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
import com.mcmiddleearth.thegaffer.TeamSpeak.TSupdate;
import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.storage.JobDatabase;
import com.mcmiddleearth.thegaffer.utilities.CleanupUtil;
import com.mcmiddleearth.thegaffer.utilities.PermissionsUtil;
import com.mcmiddleearth.thegaffer.utilities.Util;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import mineverse.Aust1n46.chat.api.MineverseChatAPI;
import mineverse.Aust1n46.chat.api.MineverseChatPlayer;
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

    private HashMap<Player, InvHolder> invs = new HashMap();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof ConsoleCommandSender && args.length>0 && args[0].equalsIgnoreCase("reloadConfig")) {
            TheGaffer.getPluginInstance().reloadConfig();
            TheGaffer.setupConfig();
            sender.sendMessage("Configuration reloaded from config.yml");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("You are not a player");
            return true;
        }
        if ((args.length > 0) && ((sender instanceof Player))) {
            Player player = (Player) sender;
            if (args.length >= 2) {
                if (args[0].equalsIgnoreCase("stop")) {
                    if (player.hasPermission(PermissionsUtil.getCreatePermission())) {
                        if (args[1] != null) {
                            String jobname = args[1];
                            if (JobDatabase.getActiveJobs().containsKey(jobname)) {
                                JobDatabase.deactivateJob(JobDatabase.getActiveJobs().get(jobname));
                                player.sendMessage(ChatColor.GRAY + "Successfully closed the " + ChatColor.AQUA + jobname + ChatColor.GRAY + " job.");
                            } else {
                                player.sendMessage(ChatColor.RED + "No job found by that name.");
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "You must provide a job name.");
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "You don't have permission.");
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
                                player.sendMessage(ChatColor.GREEN + "Paused " + target.getName());
                            } else {
                                player.sendMessage(ChatColor.RED + "No job found by that name.");
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "You must provide a job name.");
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "You don't have permission.");
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
                                player.sendMessage(ChatColor.GREEN + "Un paused " + target.getName());
                            } else {
                                player.sendMessage(ChatColor.RED + "No job found by that name.");
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "You must provide a job name.");
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "You don't have permission.");
                    }
                    return true;
                }
            }
            if (args[0].equalsIgnoreCase("listen")) {
                if (player.hasPermission(PermissionsUtil.getCreatePermission())) {
                    if (TheGaffer.getListening().contains(player)) {
                        TheGaffer.getListening().remove(player);
                        player.sendMessage(ChatColor.GREEN + "Removed from protection listening.");
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 2f);
                    } else {
                        TheGaffer.getListening().add(player);
                        player.sendMessage(ChatColor.GREEN + "Added to protection listening, you will now be notified when someone tries to edit the map outside of a job.");
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 0.5f);
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "You don't have permission.");
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
                        player.sendMessage(ChatColor.GREEN + "Recovered previous inventory.");
                        invs.remove(player);
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 2f);
                        player.updateInventory();
                    } else {
                        invs.put(player, new InvHolder(player.getInventory()));
                        player.getInventory().clear();
                        player.sendMessage(ChatColor.GREEN + "Stored your inventory.");
                        player.sendMessage(ChatColor.GREEN + "When ready, run this command again to get your inventory back.");
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 0.5f);
                        player.updateInventory();
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "You don't have permission.");
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("aero")) {
                player.sendMessage(Util.dino);
                return true;
            }
            if (args[0].equalsIgnoreCase("debug")) {
                if (player.hasPermission(PermissionsUtil.getCreatePermission())) {
                    StringBuilder out = new StringBuilder();
                    out.append(ChatColor.DARK_PURPLE).append(ChatColor.BOLD).append("TheGaffer Debug").append("\n");
                    out.append(ChatColor.GRAY).append("Servlet port: ").append(ChatColor.AQUA).append(TheGaffer.getServletPort()).append("\n");
                    out.append(ChatColor.GRAY).append("Number of active jobs: ").append(ChatColor.AQUA).append(JobDatabase.getActiveJobs().size()).append("\n");
                    out.append(ChatColor.GRAY).append("Number of inactive jobs: ").append(ChatColor.AQUA).append(JobDatabase.getInactiveJobs().size()).append("\n");
                    out.append(ChatColor.GRAY).append("Number of jobs timing out: ").append(ChatColor.AQUA).append(CleanupUtil.getWaiting().size()).append("\n");
                    out.append(ChatColor.GRAY).append("Join permission: ").append(ChatColor.AQUA).append(PermissionsUtil.getJoinPermission().getName()).append("\n");
                    out.append(ChatColor.GRAY).append("Ignore protection permission: ").append(ChatColor.AQUA).append(PermissionsUtil.getIgnoreWorldProtection().getName()).append("\n");
                    out.append(ChatColor.GRAY).append("Create permission: ").append(ChatColor.AQUA).append(PermissionsUtil.getCreatePermission().getName()).append("\n");
                    player.sendMessage(out.toString());
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("check")) {
                if (player.hasPermission(PermissionsUtil.getJoinPermission())) {
                    if (JobDatabase.getActiveJobs().size() > 0) {
                        StringBuilder out = new StringBuilder();
                        out.append(ChatColor.GRAY).append("Running Jobs:");
                        for (String jobName : JobDatabase.getActiveJobs().keySet()) {
                            Job job = JobDatabase.getActiveJobs().get(jobName);
                            out.append("\n").append(ChatColor.AQUA).append(jobName).append(ChatColor.GRAY).append(" with ").append(job.getOwner()).append(" (").append(job.getWorkers().size()).append(")");
                        }
                        player.sendMessage(out.toString());
                    } else {
                        player.sendMessage(ChatColor.GRAY + "No jobs currently running.");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "You don't have permission.");
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("join")) {
                if (player.hasPermission(PermissionsUtil.getJoinPermission())) {
                    if (JobDatabase.getActiveJobs().size() > 0) {
                        if (args.length > 1) {
                            if (JobDatabase.getActiveJobs().containsKey(args[1])) {
                                Job jobToJoin = JobDatabase.getActiveJobs().get(args[1]);
                                GafferResponse resp = jobToJoin.addWorker(player);
                                if (resp.isSuccessful()) {
                                    player.sendMessage(ChatColor.GRAY + "You have joined the job " + ChatColor.AQUA + jobToJoin.getName());
                                    if(TheGaffer.isTSenabled() && !jobToJoin.getTSchannel().equalsIgnoreCase("0")){
                                        player.teleport(jobToJoin.getTsWarp().toBukkitLocation());
                                        player.sendMessage(ChatColor.GRAY + "The TeamSpeak channel is " + ChatColor.GREEN + jobToJoin.getTSchannel() + ChatColor.GRAY + " the password is " + ChatColor.RED + "beefburgers");
                                    }
                                } else {
                                    player.sendMessage(ChatColor.RED + "Error: " + resp.getMessage().replaceAll("%name%", player.getName()).replaceAll("%job%", jobToJoin.getName()));
                                }
                            } else {
                                player.sendMessage(ChatColor.RED + "No job running by the name of `" + args[1] + "`");
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "You must provide the name of the job you would like to join.");
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "No jobs currently running.");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "You do not have permission.");
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
                        player.sendMessage(ChatColor.RED + "No job found by the name of `" + args[1] + "`");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "What job would you like to get info on?");
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
                                player.sendMessage(ChatColor.GRAY + "Warped to " + ChatColor.AQUA + jobToJoin.getName());
                            } else {
                                player.sendMessage(ChatColor.RED + "No job running by the name of `" + args[1] + "`");
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "You must provide the name of the job you would like to warp to.");
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "No jobs currently running.");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "You do not have permission.");
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("archive")) {
                if (player.hasPermission(PermissionsUtil.getJoinPermission())) {
                    if (JobDatabase.getInactiveJobs().size() > 0) {
                        StringBuilder out = new StringBuilder();
                        int pageNum = 1;
                        boolean first = true;
                        for (String jobName : JobDatabase.getInactiveJobs().keySet()) {
                            Job job = JobDatabase.getInactiveJobs().get(jobName);
                            if (!first) {
                                out.append("\n");
                            }
                            out.append(ChatColor.AQUA).append(job.getName()).append(ChatColor.GRAY).append(" with ").append(job.getOwner()).append(" (").append(job.getWorkers().size()).append(")");
                            if (first) {
                                first = false;
                            }
                        }
                        if (args.length > 1) {
                            pageNum = Integer.valueOf(args[1]);
                        }
                        ChatPaginator.ChatPage page = ChatPaginator.paginate(out.toString(), pageNum, ChatPaginator.AVERAGE_CHAT_PAGE_WIDTH, 8);
                        player.sendMessage(ChatColor.AQUA + "Job Archive Page: " + page.getPageNumber() + " of " + page.getTotalPages());
                        player.sendMessage(page.getLines());
                    } else {
                        player.sendMessage(ChatColor.GRAY + "No jobs found in archive.");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "You don't have permission.");
                }
                return true;
            }
            if(args[0].equalsIgnoreCase("admit")){
                if(TheGaffer.isTSenabled()){
                    Job senderJob = JobDatabase.getJobWorking(player);
                    if(senderJob != null){
                        TSupdate.TSfetch();
                        //if dev
        //                    for(String name : senderJob.getAdmitedWorkers()){
        //                        player.sendMessage(ChatColor.AQUA + name);
        //                    }
                        player.sendMessage("TS forced update!");
                        if(!senderJob.getAdmitedWorkers().contains(player.getName())){
                            player.sendMessage("You are not in TeamSpeak!");
                        }
                        return true;
                        }
                    }else{
                    player.sendMessage("Work in Progress");
                }
                }
            if (args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("create")) {
                Bukkit.getServer().dispatchCommand(sender, "createjob");
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
            List<String> jobs = new ArrayList();
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
            Set<String> jobsUnique = new HashSet(jobs);
            jobs.removeAll(jobs);
            jobs.addAll(jobsUnique);
            return jobs;
        }
        if (args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase("stop")
                || args[0].equalsIgnoreCase("pause") || args[0].equalsIgnoreCase("unpause")
                || args[0].equalsIgnoreCase("warpto")) {
            List<String> jobs = new ArrayList();
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
            Set<String> jobsUnique = new HashSet(jobs);
            jobs.removeAll(jobs);
            jobs.addAll(jobsUnique);
            return jobs;
        }
        List<String> actions = new ArrayList();
        actions.add("archive");
        actions.add("warpto");
        actions.add("info");
        actions.add("join");
        actions.add("check");
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
