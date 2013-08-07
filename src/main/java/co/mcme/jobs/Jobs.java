package co.mcme.jobs;

import co.mcme.jobs.util.Util;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class Jobs extends JavaPlugin implements Listener {

    private final Logger log = Logger.getLogger("Minecraft");
    private static int jobCount;
    static Configuration conf;
    public static HashMap<String, Job> runningJobs = new HashMap();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        setupConfig();
    }

    @Override
    public void onDisable() {
        for (Job job : runningJobs.values()) {
            try {
                job.writeToFile();
            } catch (IOException ex) {
                Logger.getLogger(Jobs.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void setupConfig() {
        conf = getConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        loadJobs();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ((args.length > 0) && ((sender instanceof Player))) {
            Player player = (Player) sender;
            if (label.equalsIgnoreCase("job")) {
                if (args.length > 0) {
                    if (args[0].equalsIgnoreCase("reload") && player.hasPermission("jobs.reload")) {
                        for (Job job : runningJobs.values()) {
                            try {
                                job.writeToFile();
                            } catch (IOException ex) {
                                Logger.getLogger(Jobs.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                        runningJobs = new HashMap();
                        player.sendMessage(ChatColor.GRAY + "Loaded " + co.mcme.jobs.files.Loader.loadJobs() + " old job(s) from file.");
                        Util.info(runningJobs.size() + " of which are active.");
                    }
                    if (args[0].equalsIgnoreCase("write") && player.hasPermission("jobs.write")) {
                        if (runningJobs.containsKey(args[1])) {
                            Job writing = runningJobs.get(args[1]);
                            try {
                                writing.writeToFile();
                            } catch (IOException ex) {
                                Logger.getLogger(Jobs.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }

                    if (args.length == 2 && player.hasPermission("jobs.run")) {
                        if (args[1].equals("on")) {
                            storeJob(args[0], player.getName(), true);
                            player.sendMessage(ChatColor.GREEN + "Created job called " + args[0] + ".");
                        }
                        if (args[1].equals("off")) {
                            storeJob(args[0], player.getName(), false);
                            player.sendMessage(ChatColor.RED + "Removed job called " + args[0] + ".");
                        }
                    }
                    if (args[0].equalsIgnoreCase("check")) {
                        if (runningJobs.size() > 0) {
                            StringBuilder out = new StringBuilder();
                            out.append(ChatColor.GRAY).append("Running Jobs:");
                            for (String jobName : runningJobs.keySet()) {
                                Job job = runningJobs.get(jobName);
                                out.append("\n").append(ChatColor.AQUA).append(jobName).append(ChatColor.GRAY).append(" with ").append(job.getAdmin().getName()).append(" (").append(job.getWorkers().size()).append(")");
                            }
                            player.sendMessage(out.toString());
                            System.out.println(runningJobs.toString());
                        } else {
                            player.sendMessage(ChatColor.GRAY + "No jobs currently running.");
                        }
                    }
                    if (args[0].equalsIgnoreCase("join")) {
                        if (runningJobs.size() > 0) {
                            if (args.length > 1) {
                                if (runningJobs.containsKey(args[1])) {
                                    Job jobToJoin = runningJobs.get(args[1]);
                                    if (jobToJoin.addWorker(player)) {
                                        player.sendMessage(ChatColor.GRAY + "You have joined the job " + ChatColor.AQUA + jobToJoin.getName());
                                    } else {
                                        player.sendMessage(ChatColor.RED + "You cannot be added to that job.");
                                    }
                                } else {
                                    player.sendMessage(ChatColor.RED + "No job ruuning by the name of `" + args[1] + "`");
                                }
                            } else {
                                player.sendMessage(ChatColor.RED + "What job would you like to join?");
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "No jobs currently running.");
                        }
                    }
                    if (args[0].equalsIgnoreCase("warpto")) {
                        if (runningJobs.size() > 0) {
                            if (args.length > 1) {
                                if (runningJobs.containsKey(args[1])) {
                                    Job jobToJoin = runningJobs.get(args[1]);
                                    player.teleport(jobToJoin.getWarp());
                                } else {
                                    player.sendMessage(ChatColor.RED + "No job ruuning by the name of `" + args[1] + "`");
                                }
                            } else {
                                player.sendMessage(ChatColor.RED + "What job would you like to warp to?");
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "No jobs currently running.");
                        }
                    }
                }
            }
        }
        return true;
    }

    public void storeJob(String jobname, String admin, boolean status) {
        Player p = Bukkit.getPlayer(admin);
        if (status) {
            Location adminloc = Bukkit.getPlayer(admin).getLocation();
            Job newjob = new Job(jobname, admin, true, adminloc, adminloc.getWorld().getName());
            runningJobs.put(jobname, newjob);
            try {
                newjob.writeToFile();
            } catch (IOException ex) {
                Logger.getLogger(Jobs.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            if (runningJobs.containsKey(jobname)){
                Job oldjob = runningJobs.get(jobname);
                oldjob.setStatus(false);
            }
        }
    }

    private void loadJobs() {
        Util.info("Loaded " + co.mcme.jobs.files.Loader.loadJobs() + " old job(s) from file.");
        for (Job job : runningJobs.values()){
            getServer().getPluginManager().registerEvents(job, this);
        }
        Util.info(runningJobs.size() + " of which are active.");
    }
}
