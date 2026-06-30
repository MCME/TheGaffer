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
import com.mcmiddleearth.thegaffer.utilities.JobBorderManager;
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
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public class JobCommand implements TabExecutor {

    private HashMap<Player, InvHolder> invs = new HashMap<>();

    /**
     * Sends a standardised staff-action denial that names the required permission,
     * so the player knows what to ask staff for.
     */
    private static void sendStaffDenied(Player player) {
        String perm = PermissionsUtil.getCreatePermission().getName();
        player.sendMessage(Component.text("That's a staff action (", NamedTextColor.RED)
                .append(Component.text(perm, NamedTextColor.YELLOW))
                .append(Component.text(") — ask staff to run it.", NamedTextColor.RED)));
    }

    /** Number of archive pages for {@code inactiveCount} jobs at page-size 8. */
    static int archivePageCount(int inactiveCount) {
        if (inactiveCount <= 0) return 1;
        return (int) Math.ceil(inactiveCount / 8.0);
    }

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
                        sendStaffDenied(player);
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
                                // Manual pause: clear the auto-pause flag so an unrelated rejoin
                                // never auto-resumes a job an admin deliberately paused.
                                target.setAutoPaused(false);
                                player.sendMessage(Component.text("Paused " + target.getName(), NamedTextColor.GREEN));
                            } else {
                                player.sendMessage(Component.text("No job found by that name.", NamedTextColor.RED));
                            }
                        } else {
                            player.sendMessage(Component.text("You must provide a job name.", NamedTextColor.RED));
                        }
                    } else {
                        sendStaffDenied(player);
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
                                // Clear the auto-pause flag too: a manual unpause is an explicit
                                // resume, so there is nothing left for the auto-resume path to do.
                                target.setAutoPaused(false);
                                player.sendMessage(Component.text("Unpaused " + target.getName(), NamedTextColor.GREEN));
                            } else {
                                player.sendMessage(Component.text("No job found by that name.", NamedTextColor.RED));
                            }
                        } else {
                            player.sendMessage(Component.text("You must provide a job name.", NamedTextColor.RED));
                        }
                    } else {
                        sendStaffDenied(player);
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
                    sendStaffDenied(player);
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
                    sendStaffDenied(player);
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
                                    // Line 1: job name + role + gamemode rule
                                    Component joinMsg = Component.text("You joined ", NamedTextColor.GREEN)
                                            .append(Component.text(jobToJoin.getName(), NamedTextColor.AQUA))
                                            .append(Component.text(" as Worker. Creative inside the build area, Survival outside.", NamedTextColor.GREEN))
                                            .append(Component.newline())
                                            // Line 2: clickable command hints
                                            .append(Msg.button("/job border", NamedTextColor.AQUA, "/job border", "Toggle the build-area outline"))
                                            .append(Component.text(" to outline the area · ", NamedTextColor.GRAY))
                                            .append(Msg.button("/job leave", NamedTextColor.AQUA, "/job leave", "Leave this job"))
                                            .append(Component.text(" to exit.", NamedTextColor.GRAY));
                                    if (jobToJoin.isGlowing()) {
                                        joinMsg = joinMsg.append(Component.text(" You will glow while in the job.", NamedTextColor.YELLOW));
                                    }
                                    player.sendMessage(joinMsg);
                                    // Show the job boundary border now that the player is a worker.
                                    JobBorderManager.refresh(player);
                                } else {
                                    player.sendMessage(Component.text("Error: " + resp.getMessage().replaceAll("%name%", player.getName()).replaceAll("%job%", jobToJoin.getName()), NamedTextColor.RED));
                                }
                            } else {
                                player.sendMessage(Component.text("No job running by the name of ", NamedTextColor.RED)
                                        .append(Component.text(args[1], NamedTextColor.AQUA))
                                        .append(Component.text(".", NamedTextColor.RED)));
                            }
                        } else {
                            player.sendMessage(Component.text("You must provide the name of the job you would like to join — use ", NamedTextColor.RED)
                                    .append(Component.text("/job check", NamedTextColor.AQUA))
                                    .append(Component.text(" to see running jobs.", NamedTextColor.RED)));
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
                                // Remove the job boundary border now that the player has left.
                                JobBorderManager.clear(player);
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
                if (!player.hasPermission(PermissionsUtil.getJoinPermission())) {
                    player.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
                    return true;
                }
                if (args.length > 1) {
                    if (JobDatabase.getActiveJobs().containsKey(args[1])) {
                        Job jobToJoin = JobDatabase.getActiveJobs().get(args[1]);
                        player.sendMessage(jobToJoin.getInfo());
                    } else if (JobDatabase.getInactiveJobs().containsKey(args[1])) {
                        Job jobToJoin = JobDatabase.getInactiveJobs().get(args[1]);
                        player.sendMessage(jobToJoin.getInfo());
                    } else {
                        player.sendMessage(Component.text("No job found by the name of ", NamedTextColor.RED)
                                .append(Component.text(args[1], NamedTextColor.AQUA))
                                .append(Component.text(".", NamedTextColor.RED)));
                    }
                } else {
                    player.sendMessage(Component.text("Usage: /job info <job>", NamedTextColor.GRAY));
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
                                player.sendMessage(Component.text("No job running by the name of ", NamedTextColor.RED)
                                        .append(Component.text(args[1], NamedTextColor.AQUA))
                                        .append(Component.text(".", NamedTextColor.RED)));
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
                    if (JobDatabase.getInactiveJobs().isEmpty()) {
                        player.sendMessage(Component.text("No archived jobs yet.", NamedTextColor.GRAY));
                        return true;
                    }
                    List<String> names = new ArrayList<>(JobDatabase.getInactiveJobs().keySet());
                    int totalPages = archivePageCount(names.size());
                    int pageNum = 1;
                    if (args.length > 1) {
                        try {
                            pageNum = Integer.parseInt(args[1]);
                        } catch (NumberFormatException ex) {
                            pageNum = 1;
                        }
                    }
                    if (pageNum < 1 || pageNum > totalPages) {
                        player.sendMessage(Component.text("No page " + pageNum + " — the archive has " + totalPages + " page(s).", NamedTextColor.RED));
                        return true;
                    }
                    // Slice the page (8 entries per page, 0-indexed offset)
                    int fromIdx = (pageNum - 1) * 8;
                    int toIdx = Math.min(fromIdx + 8, names.size());
                    Component out = Component.text("Job Archive — page " + pageNum + " of " + totalPages, NamedTextColor.AQUA);
                    for (int i = fromIdx; i < toIdx; i++) {
                        String name = names.get(i);
                        Job job = JobDatabase.getInactiveJobs().get(name);
                        out = out.append(Component.newline())
                                .append(Msg.button(name, NamedTextColor.AQUA, "/job info " + name, "View " + name))
                                .append(Component.text(" — " + Util.nameOf(job.getOwner())
                                        + " (" + job.getWorkers().size() + " workers)", NamedTextColor.GRAY));
                    }
                    player.sendMessage(out);
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
                        sendStaffDenied(player);
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
                    player.sendMessage(Component.text("Usage: /job stats <job|player>", NamedTextColor.GRAY));
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
            if (args[0].equalsIgnoreCase("border")) {
                if (player.hasPermission(PermissionsUtil.getJoinPermission())) {
                    boolean on = JobBorderManager.toggle(player);
                    if (on) {
                        player.sendMessage(Component.text("Job boundary shown.", NamedTextColor.GREEN));
                    } else {
                        player.sendMessage(Component.text("Job boundary hidden.", NamedTextColor.GRAY));
                    }
                } else {
                    player.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
                }
                return true;
            }
            // B8 — unknown subcommand
            player.sendMessage(Component.text("Unknown subcommand. Type ", NamedTextColor.RED)
                    .append(Component.text("/job", NamedTextColor.AQUA))
                    .append(Component.text(" for the list.", NamedTextColor.RED)));
            return true;
        }
        // B8 — bare /job: send subcommand list
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Component help = Component.text("Job commands:", NamedTextColor.GRAY)
                    .append(Component.newline())
                    .append(Component.text("  check, join, leave, border, warpto, info, archive, stats, leaderboard, top", NamedTextColor.AQUA));
            if (player.hasPermission(PermissionsUtil.getCreatePermission())) {
                help = help.append(Component.newline())
                        .append(Component.text("  create, stop, pause, unpause, prep, listen, admin, debug", NamedTextColor.AQUA));
            }
            player.sendMessage(help);
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // B4 — no-arg subcommands: return empty list so Bukkit doesn't show player names
        if (args[0].equalsIgnoreCase("check")
                || args[0].equalsIgnoreCase("leave")
                || args[0].equalsIgnoreCase("border")
                || args[0].equalsIgnoreCase("listen")
                || args[0].equalsIgnoreCase("prep")) {
            return Collections.emptyList();
        }
        // B1 — route /job admin … tab-complete to JobAdminCommands
        if (args[0].equalsIgnoreCase("admin")) {
            if (args.length > 1) {
                JobAdminCommands jAC = new JobAdminCommands();
                return jAC.onTabComplete(sender, command, alias, args);
            }
            return Collections.emptyList();
        }
        // archive takes a page number; suggest 1..totalPages
        if (args[0].equalsIgnoreCase("archive")) {
            if (args.length > 1) {
                int totalPages = archivePageCount(JobDatabase.getInactiveJobs().size());
                List<String> pages = new ArrayList<>();
                String prefix = args[1];
                for (int i = 1; i <= totalPages; i++) {
                    String num = String.valueOf(i);
                    if (num.startsWith(prefix)) {
                        pages.add(num);
                    }
                }
                return pages.isEmpty() ? Collections.emptyList() : pages;
            }
            return Collections.emptyList();
        }
        // stats: suggest export + active/inactive job names + online player names
        if (args[0].equalsIgnoreCase("stats")) {
            if (args.length == 2) {
                String prefix = args[1];
                List<String> suggestions = new ArrayList<>();
                // "export" is a valid second arg
                if ("export".startsWith(prefix)) {
                    suggestions.add("export");
                }
                // active job names
                for (String s : JobDatabase.getActiveJobs().keySet()) {
                    if (s.startsWith(prefix)) {
                        suggestions.add(s);
                    }
                }
                // inactive job names
                for (String s : JobDatabase.getInactiveJobs().keySet()) {
                    if (s.startsWith(prefix)) {
                        suggestions.add(s);
                    }
                }
                // online player names
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().startsWith(prefix)) {
                        suggestions.add(p.getName());
                    }
                }
                // deduplicate while preserving order
                Set<String> seen = new HashSet<>();
                List<String> unique = new ArrayList<>();
                for (String s : suggestions) {
                    if (seen.add(s)) {
                        unique.add(s);
                    }
                }
                return unique;
            }
            return Collections.emptyList();
        }
        // B3 + info: complete job names even when no space typed yet (args.length == 1)
        if (args[0].equalsIgnoreCase("info")) {
            List<String> jobs = new ArrayList<>();
            String prefix = args.length > 1 ? args[1] : "";
            for (String s : JobDatabase.getActiveJobs().keySet()) {
                if (s.startsWith(prefix)) {
                    jobs.add(s);
                }
            }
            for (String s : JobDatabase.getInactiveJobs().keySet()) {
                if (s.startsWith(prefix)) {
                    jobs.add(s);
                }
            }
            Set<String> jobsUnique = new HashSet<>(jobs);
            jobs.clear();
            jobs.addAll(jobsUnique);
            return jobs;
        }
        // B3 — active-job completions for join/stop/pause/unpause/warpto
        if (args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase("stop")
                || args[0].equalsIgnoreCase("pause") || args[0].equalsIgnoreCase("unpause")
                || args[0].equalsIgnoreCase("warpto")) {
            String prefix = args.length > 1 ? args[1] : "";
            List<String> jobs = new ArrayList<>();
            for (String s : JobDatabase.getActiveJobs().keySet()) {
                if (s.startsWith(prefix)) {
                    jobs.add(s);
                }
            }
            Set<String> jobsUnique = new HashSet<>(jobs);
            jobs.clear();
            jobs.addAll(jobsUnique);
            return jobs;
        }
        // B5 — leaderboard/top sort key
        if (args[0].equalsIgnoreCase("leaderboard") || args[0].equalsIgnoreCase("top")) {
            if (args.length > 1) {
                String prefix = args[1];
                List<String> keys = new ArrayList<>();
                for (String k : new String[]{"placed", "broke", "active"}) {
                    if (k.startsWith(prefix)) {
                        keys.add(k);
                    }
                }
                return keys;
            }
            return Collections.emptyList();
        }
        // Root tab-complete: subcommand list — B2 adds admin + listen
        List<String> actions = new ArrayList<>();
        actions.add("archive");
        actions.add("border");
        actions.add("warpto");
        actions.add("info");
        actions.add("join");
        actions.add("check");
        actions.add("leave");
        actions.add("stats");
        actions.add("leaderboard");
        actions.add("top");
        if (sender.hasPermission(PermissionsUtil.getCreatePermission())) {
            actions.add("create");  // #1
            actions.add("stop");
            actions.add("debug");
            actions.add("prep");
            actions.add("pause");
            actions.add("unpause");
            actions.add("admin");   // B2
            actions.add("listen");  // B2
        }
        // Filter by prefix if the user has started typing
        String prefix = args[0];
        if (!prefix.isEmpty()) {
            actions.removeIf(a -> !a.startsWith(prefix.toLowerCase()));
        }
        Collections.sort(actions);
        return actions;
    }
}
