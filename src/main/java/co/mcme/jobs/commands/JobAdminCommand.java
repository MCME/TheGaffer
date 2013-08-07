package co.mcme.jobs.commands;

import co.mcme.jobs.Job;
import co.mcme.jobs.Jobs;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JobAdminCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (label.equalsIgnoreCase("jobadmin")) {
            Player player = (Player) sender;
            if (args.length > 0) {
                if (args.length >= 3) {
                    if (args[0].equalsIgnoreCase("addhelper") && player.hasPermission("jobs.run")) {
                        if (Jobs.runningJobs.containsKey(args[1])) {
                            Job targetJob = Jobs.runningJobs.get(args[1]);
                            if (targetJob.getAdmin().getName().equals(player.getName()) || targetJob.getHelpers().contains(player.getName()) || player.hasPermission("jobs.ignoreownjob")) {
                                OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                                if (target.isOnline()) {
                                    if (target.getPlayer().hasPermission("jobs.run")) {
                                        if (targetJob.addHelper(target)) {
                                            player.sendMessage(ChatColor.GRAY + "Added " + ChatColor.AQUA + target.getName() + ChatColor.GRAY + " to the job");
                                        } else {
                                            player.sendMessage(ChatColor.RED + target.getName() + " is already added to the job");
                                        }
                                    } else {
                                        player.sendMessage(ChatColor.RED + target.getName() + " does not have the permissions to run a job.");
                                    }
                                } else {
                                    player.sendMessage(ChatColor.RED + target.getName() + " is offline.");
                                }
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "No running job found by that name.");
                        }
                    }
                    if (args[0].equalsIgnoreCase("removehelper") && player.hasPermission("jobs.run")) {
                        if (Jobs.runningJobs.containsKey(args[1])) {
                            Job targetJob = Jobs.runningJobs.get(args[1]);
                            if (targetJob.getAdmin().getName().equals(player.getName()) || targetJob.getHelpers().contains(player.getName()) || player.hasPermission("jobs.ignoreownjob")) {
                                OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                                if (targetJob.removeHelper(target)) {
                                    player.sendMessage(ChatColor.GRAY + "Removed " + ChatColor.AQUA + target.getName() + ChatColor.GRAY + " from the job");
                                } else {
                                    player.sendMessage(ChatColor.RED + target.getName() + " is not on the job.");
                                }
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "No running job found by that name.");
                        }
                    }
                    if (args[0].equalsIgnoreCase("removeworker") && player.hasPermission("jobs.run")) {
                        if (Jobs.runningJobs.containsKey(args[1])) {
                            Job targetJob = Jobs.runningJobs.get(args[1]);
                            if (targetJob.getAdmin().getName().equals(player.getName()) || targetJob.getHelpers().contains(player.getName()) || player.hasPermission("jobs.ignoreownjob")) {
                                OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                                if (targetJob.removeWorker(target)) {
                                    player.sendMessage(ChatColor.GRAY + "Removed " + ChatColor.AQUA + target.getName() + ChatColor.GRAY + " from the job.");
                                } else {
                                    player.sendMessage(ChatColor.RED + target.getName() + " is not on the job.");
                                }
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "No running job found by that name.");
                        }
                    }
                }
                if (args.length >= 2) {
                    if (args[0].equalsIgnoreCase("setwarp") && player.hasPermission("jobs.run")) {
                        if (Jobs.runningJobs.containsKey(args[1])) {
                            Job targetJob = Jobs.runningJobs.get(args[1]);
                            if (targetJob.getAdmin().getName().equals(player.getName()) || targetJob.getHelpers().contains(player.getName()) || player.hasPermission("jobs.ignoreownjob")) {
                                targetJob.setWarp(player.getLocation());
                                player.sendMessage(ChatColor.GRAY + "Set the warp of " + ChatColor.AQUA + targetJob.getName() + ChatColor.GRAY + " to your location.");
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "No running job found by that name.");
                        }
                    }
                    if (args[0].equalsIgnoreCase("listworkers") && player.hasPermission("jobs.run")) {
                        if (Jobs.runningJobs.containsKey(args[1])) {
                            Job job = Jobs.runningJobs.get(args[1]);
                            StringBuilder out = new StringBuilder();
                            out.append(ChatColor.GRAY).append("Workers");
                            for (String name : job.getWorkers()) {
                                out.append("\n").append(ChatColor.AQUA).append(name);
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "No running job found by that name.");
                        }
                    }
                }
            }
        }
        return true;
    }
}
