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
import com.mcmiddleearth.thegaffer.listeners.PlayerListener;
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

    /**
     * Returns the player's role label in the given job: "Owner", "Helper", or "Worker".
     * Pure function — no side effects, easily unit-tested.
     */
    static String roleOf(Job job, java.util.UUID uuid) {
        if (uuid.equals(job.getOwner())) {
            return "Owner";
        }
        if (job.getHelpers().contains(uuid)) {
            return "Helper";
        }
        return "Worker";
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
                if (!player.hasPermission(PermissionsUtil.getJoinPermission())) {
                    player.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
                    return true;
                }
                // /job check <job> — show that job's details with a clickable Join, so the
                // player gets a choice instead of joining straight from the list.
                if (args.length > 1) {
                    Job job = JobDatabase.getActiveJobs().get(args[1]);
                    if (job == null) {
                        player.sendMessage(Component.text("No job running by the name of ", NamedTextColor.RED)
                                .append(Component.text(args[1], NamedTextColor.AQUA))
                                .append(Component.text(" — use ", NamedTextColor.RED))
                                .append(Msg.button("/job check", NamedTextColor.AQUA, "/job check", "List running jobs"))
                                .append(Component.text(" to see running jobs.", NamedTextColor.RED)));
                        return true;
                    }
                    player.sendMessage(job.getInfo());
                    player.sendMessage(Component.text("  ")
                            .append(Msg.button("[ ▶ Join " + job.getName() + " ]", NamedTextColor.GREEN,
                                    "/job join " + job.getName(), "Join " + job.getName())));
                    return true;
                }
                // /job check — list running jobs; clicking one opens its details (not an instant join).
                if (JobDatabase.getActiveJobs().size() > 0) {
                    Component out = Component.text("Running Jobs:", NamedTextColor.GRAY)
                            .append(Component.text(" (click a job for details)", NamedTextColor.DARK_GRAY));
                    for (String jobName : JobDatabase.getActiveJobs().keySet()) {
                        Job job = JobDatabase.getActiveJobs().get(jobName);
                        out = out.append(Component.newline())
                                .append(Msg.button(jobName, NamedTextColor.AQUA, "/job check " + jobName, "View details for " + jobName))
                                .append(Component.text(" with " + Util.nameOf(job.getOwner()) + " (" + job.getWorkers().size() + ")", NamedTextColor.GRAY));
                    }
                    player.sendMessage(out);
                } else {
                    player.sendMessage(Component.text("No jobs currently running.", NamedTextColor.GRAY));
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
            if (args[0].equalsIgnoreCase("mine")) {
                if (player.hasPermission(PermissionsUtil.getJoinPermission())) {
                    Job myJob = JobDatabase.getJobWorking(player);
                    if (myJob != null) {
                        String role = roleOf(myJob, player.getUniqueId());
                        Component msg = Component.text(myJob.getName(), NamedTextColor.AQUA)
                                .append(Component.text("  [" + role + "]", NamedTextColor.GRAY));
                        if (myJob.isPaused()) {
                            msg = msg.append(Component.text("  PAUSED", NamedTextColor.YELLOW));
                        }
                        if (myJob.isGlowing()) {
                            msg = msg.append(Component.text("  glowing", NamedTextColor.GREEN));
                        }
                        msg = msg.append(Component.newline())
                                .append(Msg.button("[warpto]", NamedTextColor.AQUA,
                                        "/job warpto " + myJob.getName(), "Warp to " + myJob.getName()))
                                .append(Component.text("  ", NamedTextColor.GRAY))
                                .append(Msg.button("[leave]", NamedTextColor.RED,
                                        "/job leave", "Leave this job"));
                        player.sendMessage(msg);
                    } else {
                        player.sendMessage(
                                Component.text("You are not currently in a job — ", NamedTextColor.GRAY)
                                        .append(Msg.button("/job check", NamedTextColor.AQUA,
                                                "/job check", "Run /job check"))
                                        .append(Component.text(" to find one.", NamedTextColor.GRAY)));
                    }
                } else {
                    player.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
                }
                return true;
            }
            // Owner/helper convenience aliases for the admin summon actions, scoped to the
            // caller's own running job (the /job admin <job> teleport* forms still exist).
            if (args[0].equalsIgnoreCase("teleportall")) {
                Job job = JobDatabase.getJobWorking(player);
                if (job == null || !(job.getOwner().equals(player.getUniqueId())
                        || job.getHelpers().contains(player.getUniqueId()))) {
                    player.sendMessage(Component.text("You must be the owner or a helper of a running job to summon its workers.", NamedTextColor.RED));
                    return true;
                }
                job.bringAllWorkers(player.getLocation());
                player.sendMessage(Component.text("Teleported all online workers in ", NamedTextColor.GREEN)
                        .append(Component.text(job.getName(), NamedTextColor.AQUA))
                        .append(Component.text(" to you.", NamedTextColor.GREEN)));
                return true;
            }
            if (args[0].equalsIgnoreCase("teleport")) {
                Job job = JobDatabase.getJobWorking(player);
                if (job == null || !(job.getOwner().equals(player.getUniqueId())
                        || job.getHelpers().contains(player.getUniqueId()))) {
                    player.sendMessage(Component.text("You must be the owner or a helper of a running job to summon a worker.", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: ", NamedTextColor.GRAY)
                            .append(Component.text("/job teleport <player>", NamedTextColor.AQUA)));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null || !target.isOnline()) {
                    player.sendMessage(Component.text(args[1] + " is not online.", NamedTextColor.RED));
                    return true;
                }
                if (!job.getWorkers().contains(target.getUniqueId())
                        && !job.getHelpers().contains(target.getUniqueId())) {
                    player.sendMessage(Component.text(target.getName() + " is not a worker on ", NamedTextColor.RED)
                            .append(Component.text(job.getName(), NamedTextColor.AQUA))
                            .append(Component.text(".", NamedTextColor.RED)));
                    return true;
                }
                target.teleport(player.getLocation());
                player.sendMessage(Component.text("Teleported ", NamedTextColor.GREEN)
                        .append(Component.text(target.getName(), NamedTextColor.AQUA))
                        .append(Component.text(" to you.", NamedTextColor.GREEN)));
                target.sendMessage(Component.text("You were summoned to the job leader.", NamedTextColor.GRAY));
                return true;
            }
            if (args[0].equalsIgnoreCase("who")) {
                if (!player.hasPermission(PermissionsUtil.getJoinPermission())) {
                    player.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
                    return true;
                }
                // Resolve the job: named arg → active or inactive; else current job
                Job whoJob = null;
                if (args.length > 1) {
                    String jobArg = args[1];
                    if (JobDatabase.getActiveJobs().containsKey(jobArg)) {
                        whoJob = JobDatabase.getActiveJobs().get(jobArg);
                    } else if (JobDatabase.getInactiveJobs().containsKey(jobArg)) {
                        whoJob = JobDatabase.getInactiveJobs().get(jobArg);
                    } else {
                        player.sendMessage(Component.text("No job found by the name of ", NamedTextColor.RED)
                                .append(Component.text(jobArg, NamedTextColor.AQUA))
                                .append(Component.text(".", NamedTextColor.RED)));
                        return true;
                    }
                } else {
                    whoJob = JobDatabase.getJobWorking(player);
                    if (whoJob == null) {
                        player.sendMessage(Component.text("You're not in a job — name one: ", NamedTextColor.GRAY)
                                .append(Msg.button("/job who <job>", NamedTextColor.AQUA,
                                        "/job check", "Run /job check to see running jobs")));
                        return true;
                    }
                }
                // Build the roster component
                Component roster = Component.text(whoJob.getName(), NamedTextColor.AQUA);
                // Owner
                boolean ownerOnline = Bukkit.getPlayer(whoJob.getOwner()) != null;
                roster = roster.append(Component.newline())
                        .append(Component.text("Owner: ", NamedTextColor.GRAY))
                        .append(Component.text(Util.nameOf(whoJob.getOwner()),
                                ownerOnline ? NamedTextColor.GREEN : NamedTextColor.GRAY));
                // Helpers
                roster = roster.append(Component.newline())
                        .append(Component.text("Helpers: ", NamedTextColor.GRAY));
                if (whoJob.getHelpers().isEmpty()) {
                    roster = roster.append(Component.text("none", NamedTextColor.GRAY));
                } else {
                    boolean firstHelper = true;
                    for (java.util.UUID hUuid : whoJob.getHelpers()) {
                        if (!firstHelper) {
                            roster = roster.append(Component.text(", ", NamedTextColor.GRAY));
                        }
                        boolean hOnline = Bukkit.getPlayer(hUuid) != null;
                        roster = roster.append(Component.text(Util.nameOf(hUuid),
                                hOnline ? NamedTextColor.GREEN : NamedTextColor.GRAY));
                        firstHelper = false;
                    }
                }
                // Workers
                roster = roster.append(Component.newline())
                        .append(Component.text("Workers: ", NamedTextColor.GRAY));
                if (whoJob.getWorkers().isEmpty()) {
                    roster = roster.append(Component.text("none", NamedTextColor.GRAY));
                } else {
                    boolean firstWorker = true;
                    for (java.util.UUID wUuid : whoJob.getWorkers()) {
                        if (!firstWorker) {
                            roster = roster.append(Component.text(", ", NamedTextColor.GRAY));
                        }
                        boolean wOnline = Bukkit.getPlayer(wUuid) != null;
                        roster = roster.append(Component.text(Util.nameOf(wUuid),
                                wOnline ? NamedTextColor.GREEN : NamedTextColor.GRAY));
                        firstWorker = false;
                    }
                }
                player.sendMessage(roster);
                return true;
            }
            if (args[0].equalsIgnoreCase("manage")) {
                if (!player.hasPermission(PermissionsUtil.getCreatePermission())) {
                    sendStaffDenied(player);
                    return true;
                }
                // Resolve the job: named arg (active only) or the staff member's current job
                Job manageJob = null;
                if (args.length > 1) {
                    manageJob = JobDatabase.getActiveJobs().get(args[1]);
                    if (manageJob == null) {
                        player.sendMessage(Component.text("No active job found by the name of ", NamedTextColor.RED)
                                .append(Component.text(args[1], NamedTextColor.AQUA))
                                .append(Component.text(".", NamedTextColor.RED)));
                        return true;
                    }
                } else {
                    manageJob = JobDatabase.getJobWorking(player);
                    if (manageJob == null) {
                        player.sendMessage(Component.text("You are not in a job — specify one: ", NamedTextColor.GRAY)
                                .append(Msg.button("/job manage <job>", NamedTextColor.AQUA,
                                        "/job check", "Run /job check to see running jobs")));
                        return true;
                    }
                }
                final Job mj = manageJob;
                // Build the clickable roster panel
                Component panel = Component.text("=== Manage: ", NamedTextColor.GOLD)
                        .append(Component.text(mj.getName(), NamedTextColor.AQUA))
                        .append(Component.text(" ===", NamedTextColor.GOLD));
                // Owner line (no action buttons — hint transfer)
                panel = panel.append(Component.newline())
                        .append(Component.text("Owner: ", NamedTextColor.GRAY))
                        .append(Component.text(Util.nameOf(mj.getOwner()), NamedTextColor.GOLD))
                        .append(Component.text("  ", NamedTextColor.GRAY))
                        .append(Msg.button("[Transfer…]", NamedTextColor.YELLOW,
                                "/job transfer", "Use /job transfer <player> (target must be a helper)"));
                // Helpers (Kick, Ban, Demote)
                if (!mj.getHelpers().isEmpty()) {
                    panel = panel.append(Component.newline())
                            .append(Component.text("Helpers:", NamedTextColor.GRAY));
                    for (java.util.UUID hUuid : mj.getHelpers()) {
                        String hName = Util.nameOf(hUuid);
                        panel = panel.append(Component.newline())
                                .append(Component.text("  " + hName, NamedTextColor.AQUA))
                                .append(Component.text("  ", NamedTextColor.GRAY))
                                .append(Msg.button("[Kick]", NamedTextColor.RED,
                                        "/job admin " + mj.getName() + " kickworker " + hName,
                                        "Kick " + hName))
                                .append(Component.text(" ", NamedTextColor.GRAY))
                                .append(Msg.button("[Ban]", NamedTextColor.DARK_RED,
                                        "/job admin " + mj.getName() + " banworker " + hName,
                                        "Ban " + hName))
                                .append(Component.text(" ", NamedTextColor.GRAY))
                                .append(Msg.button("[Demote]", NamedTextColor.YELLOW,
                                        "/job admin " + mj.getName() + " demote " + hName,
                                        "Demote " + hName + " back to worker"));
                    }
                }
                // Workers (excluding those also in helpers list)
                java.util.List<java.util.UUID> pureWorkers = new java.util.ArrayList<>();
                for (java.util.UUID wUuid : mj.getWorkers()) {
                    if (!mj.getHelpers().contains(wUuid)) {
                        pureWorkers.add(wUuid);
                    }
                }
                if (!pureWorkers.isEmpty()) {
                    panel = panel.append(Component.newline())
                            .append(Component.text("Workers:", NamedTextColor.GRAY));
                    for (java.util.UUID wUuid : pureWorkers) {
                        String wName = Util.nameOf(wUuid);
                        panel = panel.append(Component.newline())
                                .append(Component.text("  " + wName, NamedTextColor.AQUA))
                                .append(Component.text("  ", NamedTextColor.GRAY))
                                .append(Msg.button("[Kick]", NamedTextColor.RED,
                                        "/job admin " + mj.getName() + " kickworker " + wName,
                                        "Kick " + wName))
                                .append(Component.text(" ", NamedTextColor.GRAY))
                                .append(Msg.button("[Ban]", NamedTextColor.DARK_RED,
                                        "/job admin " + mj.getName() + " banworker " + wName,
                                        "Ban " + wName))
                                .append(Component.text(" ", NamedTextColor.GRAY))
                                .append(Msg.button("[Promote]", NamedTextColor.GREEN,
                                        "/job admin " + mj.getName() + " promote " + wName,
                                        "Promote " + wName + " to helper"));
                    }
                }
                if (mj.getHelpers().isEmpty() && pureWorkers.isEmpty()) {
                    panel = panel.append(Component.newline())
                            .append(Component.text("  (no workers or helpers yet)", NamedTextColor.GRAY));
                }
                player.sendMessage(panel);
                return true;
            }
            if(args[0].equalsIgnoreCase("leave")){
                if(player.hasPermission(PermissionsUtil.getJoinPermission())){
                    if (JobDatabase.getActiveJobs().size() > 0){
                        Job jobToLeave = JobDatabase.getJobWorking(player);
                        if(jobToLeave != null) {
                            // The owner can't just leave — that would orphan the job (they'd
                            // stay owner-of-record with no way back). Direct them to hand off
                            // or close it instead.
                            if (jobToLeave.getOwner().equals(player.getUniqueId())) {
                                player.sendMessage(Component.text("You own ", NamedTextColor.RED)
                                        .append(Component.text(jobToLeave.getName(), NamedTextColor.AQUA))
                                        .append(Component.text(" — hand it off with ", NamedTextColor.RED))
                                        .append(Msg.button("/job transfer <helper>", NamedTextColor.AQUA,
                                                "/job transfer ", "Transfer ownership to a helper"))
                                        .append(Component.text(" or close it with ", NamedTextColor.RED))
                                        .append(Msg.button("/job stop", NamedTextColor.AQUA,
                                                "/job stop " + jobToLeave.getName(), "Stop " + jobToLeave.getName()))
                                        .append(Component.text(" instead.", NamedTextColor.RED)));
                                return true;
                            }
                            GafferResponse resp = jobToLeave.leaveJob(player);
                            if (resp.isSuccessful()) {
                                player.sendMessage(Component.text("You left the job ", NamedTextColor.GRAY)
                                        .append(Component.text(jobToLeave.getName(), NamedTextColor.AQUA)));
                                // #15: Immediately revert to Survival if we switched this player to Creative.
                                PlayerListener.revertToSurvivalIfSwitched(player);
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
                    // /job stats export json  → write leaderboard.json feed
                    if (args.length > 2 && args[2].equalsIgnoreCase("json")) {
                        java.io.File f = StatsManager.writeFeed();
                        player.sendMessage(Component.text(
                                "Feed queued → " + f.getName(), NamedTextColor.GREEN));
                        return true;
                    }
                    // /job stats export        → CSV (existing behaviour)
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
                                            + " broken across " + a.getJobs() + " jobs", NamedTextColor.GRAY))
                                    .append(Component.newline())
                                    .append(Component.text("Active build time: ", NamedTextColor.GRAY))
                                    .append(Component.text(StatsManager.formatDuration(a.getActiveBuildMillis()), NamedTextColor.AQUA)));
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
                else if (args.length > 1 && args[1].equalsIgnoreCase("time")) { key = StatsManager.SortKey.TIME; }
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
            if (args[0].equalsIgnoreCase("transfer")) {
                if (!player.hasPermission(PermissionsUtil.getCreatePermission())) {
                    sendStaffDenied(player);
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /job transfer <player>", NamedTextColor.GRAY));
                    return true;
                }
                // Resolve sender's current job — must be the owner (or admin override)
                Job transferJob = JobDatabase.getJobWorking(player);
                if (transferJob == null) {
                    player.sendMessage(Component.text("You are not in a job. You can only transfer a job you own.", NamedTextColor.RED));
                    return true;
                }
                boolean isOwner = transferJob.getOwner().equals(player.getUniqueId());
                boolean isAdmin = player.hasPermission("thegaffer.project.admin");
                if (!isOwner && !isAdmin) {
                    player.sendMessage(Component.text("Only the job owner (or an admin) can transfer ownership.", NamedTextColor.RED));
                    return true;
                }
                String targetName = args[1];
                org.bukkit.OfflinePlayer targetOffline = Bukkit.getOfflinePlayer(targetName);
                java.util.UUID targetUuid = targetOffline.getUniqueId();
                // Target must already be a helper
                if (!transferJob.getHelpers().contains(targetUuid)) {
                    player.sendMessage(Component.text("You can only transfer to a helper — promote them first (", NamedTextColor.RED)
                            .append(Msg.button("/job manage", NamedTextColor.AQUA,
                                    "/job manage " + transferJob.getName(), "Open management panel"))
                            .append(Component.text(").", NamedTextColor.RED)));
                    return true;
                }
                // Execute the transfer
                java.util.UUID oldOwner = transferJob.getOwner();
                transferJob.setOwner(targetUuid);
                // creator field is intentionally unchanged — /job info shows original "Started by"
                // Add old owner as a helper (if not already)
                if (!transferJob.getHelpers().contains(oldOwner)) {
                    transferJob.getHelpers().add(oldOwner);
                }
                transferJob.setDirty(true);
                // Message old owner
                player.sendMessage(Component.text("You transferred ownership of ", NamedTextColor.GREEN)
                        .append(Component.text(transferJob.getName(), NamedTextColor.AQUA))
                        .append(Component.text(" to ", NamedTextColor.GREEN))
                        .append(Component.text(Util.nameOf(targetUuid), NamedTextColor.AQUA))
                        .append(Component.text(". You are now a helper.", NamedTextColor.GREEN)));
                // Message new owner if online
                org.bukkit.entity.Player targetPlayer = Bukkit.getPlayer(targetUuid);
                if (targetPlayer != null) {
                    targetPlayer.sendMessage(Component.text(Util.nameOf(oldOwner), NamedTextColor.AQUA)
                            .append(Component.text(" transferred ownership of ", NamedTextColor.GREEN))
                            .append(Component.text(transferJob.getName(), NamedTextColor.AQUA))
                            .append(Component.text(" to you. You are now the owner!", NamedTextColor.GREEN)));
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("admin")) {
                // If no job/action supplied (/job admin alone), open the guided conversation
                // so the behaviour mirrors bare /jobadmin.
                if (args.length == 1) {
                    // Reuse the conversation entry-point (no args → guided flow).
                    return new com.mcmiddleearth.thegaffer.commands.AdminCommands.JobAdminConversation()
                            .onCommand(sender, command, label, new String[0]);
                }
                JobAdminCommands jAC = new JobAdminCommands();
                return jAC.onCommand(sender, command, label, args);
            }
            if (args[0].equalsIgnoreCase("border")) {
                if (player.hasPermission(PermissionsUtil.getJoinPermission())) {
                    boolean on = JobBorderManager.toggle(player);
                    if (on) {
                        player.sendMessage(Component.text("Job boundary shown.", NamedTextColor.GREEN));
                    } else if (JobDatabase.getJobWorking(player) != null) {
                        player.sendMessage(Component.text("Job boundary hidden.", NamedTextColor.GRAY));
                    } else {
                        player.sendMessage(Component.text("You're not in a job here — no boundary to show. ", NamedTextColor.GRAY)
                                .append(Msg.button("[/job check]", NamedTextColor.AQUA, "/job check", "See running jobs")));
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
                    .append(Component.text("  check, join, leave, mine, who, border, warpto, info, archive, stats, leaderboard, top", NamedTextColor.AQUA));
            if (player.hasPermission(PermissionsUtil.getCreatePermission())) {
                help = help.append(Component.newline())
                        .append(Component.text("  create, stop, pause, unpause, prep, listen, admin, debug", NamedTextColor.AQUA))
                        .append(Component.newline())
                        .append(Component.text("  manage [job], transfer <player>, teleportall, teleport <player>", NamedTextColor.AQUA));
            }
            player.sendMessage(help);
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // First word only: complete the subcommand name. Never re-suggest a subcommand
        // once a space is typed — this is what made /job create <tab> keep offering
        // "create" and no-arg subcommands leak completions into later argument slots.
        if (args.length == 1) {
            List<String> actions = new ArrayList<>();
            actions.add("archive");
            actions.add("border");
            actions.add("warpto");
            actions.add("info");
            actions.add("join");
            actions.add("check");
            actions.add("leave");
            actions.add("mine");
            actions.add("who");
            actions.add("stats");
            actions.add("leaderboard");
            actions.add("top");
            if (sender.hasPermission(PermissionsUtil.getCreatePermission())) {
                actions.add("create");
                actions.add("stop");
                actions.add("debug");
                actions.add("prep");
                actions.add("pause");
                actions.add("unpause");
                actions.add("admin");
                actions.add("listen");
                actions.add("manage");
                actions.add("transfer");
                actions.add("teleportall");
                actions.add("teleport");
            }
            String prefix = args[0];
            if (!prefix.isEmpty()) {
                actions.removeIf(a -> !a.startsWith(prefix.toLowerCase()));
            }
            Collections.sort(actions);
            return actions;
        }
        // No-argument subcommands: return empty (never player names) for arg slots 2+.
        if (args[0].equalsIgnoreCase("leave")
                || args[0].equalsIgnoreCase("mine")
                || args[0].equalsIgnoreCase("border")
                || args[0].equalsIgnoreCase("listen")
                || args[0].equalsIgnoreCase("prep")
                || args[0].equalsIgnoreCase("teleportall")
                || args[0].equalsIgnoreCase("create")
                || args[0].equalsIgnoreCase("debug")) {
            return Collections.emptyList();
        }
        // transfer: second arg is a player name — handled separately below with helper-name completions
        if (args[0].equalsIgnoreCase("transfer") && args.length > 2) {
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
                // Archived jobs are intentionally NOT suggested (they accumulate unboundedly);
                // type an archived job's name to view its stats, or use /job archive.
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
            // /job stats export <tab> → suggest "json"
            if (args.length == 3 && args[1].equalsIgnoreCase("export")) {
                String prefix = args[2];
                if ("json".startsWith(prefix)) {
                    return Collections.singletonList("json");
                }
            }
            return Collections.emptyList();
        }
        // who: complete active job names at the job-name slot only
        if (args[0].equalsIgnoreCase("who")) {
            if (args.length != 2) {
                return Collections.emptyList();
            }
            List<String> jobs = new ArrayList<>();
            String prefix = args[1];
            for (String s : JobDatabase.getActiveJobs().keySet()) {
                if (s.startsWith(prefix)) {
                    jobs.add(s);
                }
            }
            return jobs;
        }
        // info: complete active job names at the job-name slot only
        if (args[0].equalsIgnoreCase("info")) {
            if (args.length != 2) {
                return Collections.emptyList();
            }
            List<String> jobs = new ArrayList<>();
            String prefix = args[1];
            for (String s : JobDatabase.getActiveJobs().keySet()) {
                if (s.startsWith(prefix)) {
                    jobs.add(s);
                }
            }
            // Only ACTIVE jobs are suggested. Archived jobs are still viewable by typing the
            // name (the /job info handler resolves inactive jobs too) or via /job archive —
            // but suggesting every closed job would grow this list unboundedly over time.
            Set<String> jobsUnique = new HashSet<>(jobs);
            jobs.clear();
            jobs.addAll(jobsUnique);
            return jobs;
        }
        // Active-job-name completions for check/join/stop/pause/unpause/warpto (job-name slot only)
        if (args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase("stop")
                || args[0].equalsIgnoreCase("pause") || args[0].equalsIgnoreCase("unpause")
                || args[0].equalsIgnoreCase("warpto") || args[0].equalsIgnoreCase("check")) {
            if (args.length != 2) {
                return Collections.emptyList();
            }
            String prefix = args[1];
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
                for (String k : new String[]{"placed", "broke", "active", "time"}) {
                    if (k.startsWith(prefix)) {
                        keys.add(k);
                    }
                }
                return keys;
            }
            return Collections.emptyList();
        }
        // manage: complete active job names
        if (args[0].equalsIgnoreCase("manage")) {
            if (args.length == 2) {
                String prefix = args[1];
                List<String> jobs = new ArrayList<>();
                for (String s : JobDatabase.getActiveJobs().keySet()) {
                    if (s.startsWith(prefix)) {
                        jobs.add(s);
                    }
                }
                return jobs;
            }
            return Collections.emptyList();
        }
        // transfer: complete current job's helper names
        if (args[0].equalsIgnoreCase("transfer")) {
            if (args.length == 2 && sender instanceof Player) {
                Player tSender = (Player) sender;
                Job tJob = JobDatabase.getJobWorking(tSender);
                if (tJob != null) {
                    String prefix = args[1];
                    List<String> helpers = new ArrayList<>();
                    for (java.util.UUID hUuid : tJob.getHelpers()) {
                        String hName = Util.nameOf(hUuid);
                        if (hName.startsWith(prefix)) {
                            helpers.add(hName);
                        }
                    }
                    return helpers;
                }
            }
            return Collections.emptyList();
        }
        // teleport: complete the caller's own job's worker/helper names (name slot only)
        if (args[0].equalsIgnoreCase("teleport")) {
            if (args.length == 2 && sender instanceof Player) {
                Job tpJob = JobDatabase.getJobWorking((Player) sender);
                if (tpJob != null) {
                    String prefix = args[1];
                    List<String> names = new ArrayList<>();
                    for (java.util.UUID u : tpJob.getWorkers()) {
                        String n = Util.nameOf(u);
                        if (n.startsWith(prefix)) { names.add(n); }
                    }
                    for (java.util.UUID u : tpJob.getHelpers()) {
                        String n = Util.nameOf(u);
                        if (n.startsWith(prefix)) { names.add(n); }
                    }
                    return names;
                }
            }
            return Collections.emptyList();
        }
        // Any 2nd+ argument not handled above: no suggestions (prevents stray player-name
        // completions and re-suggesting subcommands in later slots).
        return Collections.emptyList();
    }
}
