package co.mcme.jobs.commands;

import co.mcme.jobs.Job;
import co.mcme.jobs.Jobs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public class JobAdminCommand implements TabExecutor {

    private List<String> actions = new ArrayList();

    public JobAdminCommand() {
        actions.add("addhelper");
        actions.add("removehelper");
        actions.add("kickworker");
        actions.add("banworker");
        actions.add("unbanworker");
        actions.add("setwarp");
        actions.add("bringall");
        actions.add("listworkers");
        Collections.sort(actions);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0) {
            if (actions.contains(args[0].toLowerCase())) {
                if (checkPerms(args[0], sender)) {
                    return executeAction(args[0], args, sender);
                } else {
                    sender.sendMessage(ChatColor.RED + "You don't have the permission `jobs.run.actions." + args[0] + "`");
                }
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 2) {
            List<String> jobs = new ArrayList();
            jobs.addAll(Jobs.runningJobs.keySet());
            Collections.sort(jobs);
            return jobs;
        } else if (args.length == 1) {
            return actions;
        } else {
            return null;
        }
    }

    private boolean executeAction(String action, String[] args, CommandSender sender) {
        if (args.length >= 3) {
            switch (action) {
                case "addhelper": {
                    if (Jobs.runningJobs.containsKey(args[1])) {
                        Job targetJob = Jobs.runningJobs.get(args[1]);
                        if (targetJob.getAdmin().getName().equals(sender.getName())
                                || targetJob.getHelpers().contains(sender.getName())
                                || sender.hasPermission("jobs.ignoreownjob")) {
                            OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                            if (target.isOnline()) {
                                if (target.getPlayer().hasPermission("jobs.admin.run")) {
                                    if (targetJob.addHelper(target)) {
                                        sender.sendMessage(ChatColor.GRAY + "Added " + ChatColor.AQUA + target.getName() + ChatColor.GRAY + " to the job");
                                    } else {
                                        sender.sendMessage(ChatColor.RED + target.getName() + " is already added to the job");
                                    }
                                } else {
                                    sender.sendMessage(ChatColor.RED + target.getName() + " does not have the permissions to run a job.");
                                }
                            } else {
                                sender.sendMessage(ChatColor.RED + target.getName() + " is offline.");
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "You don't have permission to edit the " + targetJob.getName() + " job.");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "No running job found by that name.");
                    }
                    return true;
                }
                case "removehelper": {
                    if (Jobs.runningJobs.containsKey(args[1])) {
                        Job targetJob = Jobs.runningJobs.get(args[1]);
                        if (targetJob.getAdmin().getName().equals(sender.getName())
                                || targetJob.getHelpers().contains(sender.getName())
                                || sender.hasPermission("jobs.ignoreownjob")) {
                            OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                            if (targetJob.removeHelper(target)) {
                                sender.sendMessage(ChatColor.GRAY + "Removed " + ChatColor.AQUA + target.getName() + ChatColor.GRAY + " from the job");
                            } else {
                                sender.sendMessage(ChatColor.RED + target.getName() + " is not on the job.");
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "You don't have permission to edit the " + targetJob.getName() + " job.");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "No running job found by that name.");
                    }
                    return true;
                }
                case "kickworker": {
                    if (Jobs.runningJobs.containsKey(args[1])) {
                        Job targetJob = Jobs.runningJobs.get(args[1]);
                        if (targetJob.getAdmin().getName().equals(sender.getName())
                                || targetJob.getHelpers().contains(sender.getName())
                                || sender.hasPermission("jobs.ignoreownjob")) {
                            OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                            if (targetJob.removeWorker(target)) {
                                sender.sendMessage(ChatColor.GRAY + "Removed " + ChatColor.AQUA + target.getName() + ChatColor.GRAY + " from the job.");
                                if (target.isOnline()) {
                                    target.getPlayer().sendMessage(ChatColor.RED + "You have been kicked from the " + targetJob.getName() + " job.");
                                }
                            } else {
                                sender.sendMessage(ChatColor.RED + target.getName() + " is not on the job.");
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "You don't have permission to edit the " + targetJob.getName() + " job.");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "No running job found by that name.");
                    }
                    return true;
                }
                case "banworker": {
                    if (Jobs.runningJobs.containsKey(args[1])) {
                        Job targetJob = Jobs.runningJobs.get(args[1]);
                        if (targetJob.getAdmin().getName().equals(sender.getName())
                                || targetJob.getHelpers().contains(sender.getName())
                                || sender.hasPermission("jobs.ignoreownjob")) {
                            OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                            if (targetJob.banWorker(target)) {
                                if (target.isOnline()) {
                                    target.getPlayer().sendMessage(ChatColor.RED + "You have been banned from the " + targetJob.getName() + " job.");
                                }
                            } else {
                                sender.sendMessage(ChatColor.RED + target.getName() + " is already banned from this job.");
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "You don't have permission to edit the " + targetJob.getName() + " job.");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "No running job found by that name.");
                    }
                    return true;
                }
                case "unbanworker": {
                    if (Jobs.runningJobs.containsKey(args[1])) {
                        Job targetJob = Jobs.runningJobs.get(args[1]);
                        if (targetJob.getAdmin().getName().equals(sender.getName())
                                || targetJob.getHelpers().contains(sender.getName())
                                || sender.hasPermission("jobs.ignoreownjob")) {
                            OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                            if (targetJob.unBanWorker(target)) {
                                if (target.isOnline()) {
                                    target.getPlayer().sendMessage(ChatColor.RED + "You have been unbanned from the " + targetJob.getName() + " job.");
                                }
                            } else {
                                sender.sendMessage(ChatColor.RED + target.getName() + " is not banned from this job.");
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "You don't have permission to edit the " + targetJob.getName() + " job.");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "No running job found by that name.");
                    }
                    return true;
                }
                default: {
                    return false;
                }
            }
        }
        if (args.length >= 2) {
            switch (action) {
                case "bringall": {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        if (Jobs.runningJobs.containsKey(args[1])) {
                            Job targetJob = Jobs.runningJobs.get(args[1]);
                            if (targetJob.getAdmin().getName().equals(player.getName())
                                    || targetJob.getHelpers().contains(player.getName())
                                    || player.hasPermission("jobs.ignoreownjob")) {
                                for (String name : targetJob.getWorkers()) {
                                    if (Bukkit.getOfflinePlayer(name).isOnline()) {
                                        Bukkit.getPlayer(name).teleport(player.getLocation());
                                    }
                                }
                                player.sendMessage(ChatColor.GRAY + "Brought all workers to your location.");
                            } else {
                                player.sendMessage(ChatColor.RED + "You don't have permission to edit the " + targetJob.getName() + " job.");
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "No running job found by that name.");
                        }
                        return true;
                    } else {
                        sender.sendMessage(ChatColor.RED + "You must be a player to execute this action");
                    }
                    return true;
                }
                case "listworkers": {
                    if (Jobs.runningJobs.containsKey(args[1])) {
                        Job job = Jobs.runningJobs.get(args[1]);
                        StringBuilder out = new StringBuilder();
                        out.append(ChatColor.GRAY).append("Workers");
                        for (String name : job.getWorkers()) {
                            out.append("\n").append(ChatColor.AQUA).append(name);
                        }
                        sender.sendMessage(out.toString());
                    } else {
                        sender.sendMessage(ChatColor.RED + "No running job found by that name.");
                    }
                    return true;
                }
                case "setwarp": {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        if (Jobs.runningJobs.containsKey(args[1])) {
                            Job targetJob = Jobs.runningJobs.get(args[1]);
                            if (targetJob.getAdmin().getName().equals(player.getName())
                                    || targetJob.getHelpers().contains(player.getName())
                                    || player.hasPermission("jobs.ignoreownjob")) {
                                targetJob.setWarp(player.getLocation());
                                player.sendMessage(ChatColor.GRAY + "Set the warp of " + ChatColor.AQUA + targetJob.getName() + ChatColor.GRAY + " to your location.");
                            } else {
                                player.sendMessage(ChatColor.RED + "You don't have permission to edit the " + targetJob.getName() + " job.");
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "No running job found by that name.");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "You must be a player to execute this action");
                    }
                    return true;
                }
                default: {
                    return false;
                }
            }
        }
        return false;
    }

    private boolean checkPerms(String action, CommandSender p) {
        return p.hasPermission("jobs.run.actions." + action) || p.hasPermission("jobs.run.actions.*");
    }
}
