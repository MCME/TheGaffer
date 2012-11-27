package com.meggawatts.jobs;

/**
 *
 * @author meggawatts <megga@mcmiddleearth.com>
 */
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class Jobs extends JavaPlugin implements Listener{

    private final Logger log = Logger.getLogger("Minecraft");
    private int port;
    private JobServer server;
    private static int jobCount;
    static Configuration conf;

    @Override
    public void onDisable() {
        try {
            server.getListener().close();
        } catch (IOException ex) {
            log.log(Level.WARNING, "Unable to close the MCMEJobs listener", ex);
        }
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        setupConfig();
        try {
            server = new JobServer(this, "ANY", port);
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Error initializing MCMEJobs", ex);
        }
        if (server == null) {
            throw new IllegalStateException("Cannot enable - MCMEJobs not initialized");
        }

        // Start the server normally.
        server.start();
    }

    public int getPort() {
        return port;
    }

    public void setupConfig() {
        conf = getConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        updateJobCount();
        port = 22876;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ((args.length > 0) && ((sender instanceof Player))) {
            Player player = (Player) sender;
            if (label.equalsIgnoreCase("job")) {
                if (args.length == 2 && player.hasPermission("jobs.run")) {
                    if (args[1].equals("on")) {
                        storeJob(args[0], player.getName(), "on");
                        player.sendMessage(ChatColor.GREEN + "Created job called " + args[0] + ".");
                    }
                    if (args[1].equals("off")) {
                        storeJob(args[0], player.getName(), "off");
                        player.sendMessage(ChatColor.RED + "Removed job called " + args[0] + ".");
                    }
                }
                if (args[0].equalsIgnoreCase("check")) {
                    String running = getRunning();
                    if (running.isEmpty()) {
                        player.sendMessage(ChatColor.RED + "No jobs currently running.");
                    } else {
                        player.sendMessage(makeHuman(running));
                    }
                }

            }

        }
        return true;
    }

    public void storeJob(String jobname, String admin, String status) {
        if (status.equalsIgnoreCase("on")) {
            getConfig().set("jobs." + jobname, admin);
            saveConfig();
        } else if (status.equalsIgnoreCase("off")) {
            getConfig().set("jobs." + jobname, null);
            saveConfig();
        }
        updateJobCount();
    }

    public String getRunner(String jobname) {
        return getConfig().getString("jobs." + jobname);
    }

    public static String getRunning() {
        StringBuilder out = new StringBuilder();
        ConfigurationSection jobs = getConf().getConfigurationSection("jobs");
        Set keys = jobs.getKeys(false);
        Iterator<String> iterator = keys.iterator();
        while (iterator.hasNext()) {
            String next = iterator.next();
            out.append(next);
            out.append(",");
            out.append(jobs.get(next, "none"));
            out.append(":");
        }
        return out.toString();
    }

    public static String getNames() {
        StringBuilder out = new StringBuilder();
        ConfigurationSection jobs = getConf().getConfigurationSection("jobs");
        Set keys = jobs.getKeys(false);
        Iterator<String> iterator = keys.iterator();
        while (iterator.hasNext()) {
            String next = iterator.next();
            out.append(next);
            out.append(",");
        }
        return out.toString();
    }

    public String makeHuman(String input) {
        StringBuilder humanout = new StringBuilder();
        humanout.append(ChatColor.GREEN).append("Job Name").append(ChatColor.DARK_PURPLE).append(" | ").append(ChatColor.RED).append("Admin Running Job").append("\n");
        String[] splited = input.split(":");
        for (int i = 0; i < splited.length; i++) {
            String joob = splited[i];
            String[] job = joob.split(",");
            String moo = job[0].toString();
            String admin = job[1].toString();
            humanout.append(ChatColor.GREEN).append(moo).append(ChatColor.DARK_PURPLE).append(" | ").append(ChatColor.RED).append(admin);
            humanout.append("\n");
        }
        return humanout.toString();
    }

    public static String makeJSON(String input) {
        StringBuilder JSONout = new StringBuilder();
        JSONout.append("\n");
        String[] splited = input.split(":");
        for (int i = 0; i < splited.length; i++) {
            String joob = splited[i];
            String[] job = joob.split(",");
            String moo = job[0].toString();
            String admin = job[1].toString();
            JSONout.append("{").append("\"").append("Name").append("\"").append(":").append("\"").append(moo).append("\"").append(",");
            JSONout.append("\"").append("Admin").append("\"").append(":").append("\"").append(admin).append("\"").append("}").append(",");
        }
        return JSONout.toString();
    }

    public static int jobCount() {
        return jobCount;
    }

    public static Configuration getConf() {
        return conf;
    }

    public void updateJobCount() {
        ConfigurationSection jobs = getConf().getConfigurationSection("jobs");
        Set keys = jobs.getKeys(false);
        Iterator<String> iterator = keys.iterator();
        jobCount = 0;
        if (iterator.hasNext()) {
            while (iterator.hasNext()) {
                jobCount++;
                return;
            }
        }
        else {
            jobCount = 0;
            return;
        }
        return;
    }
    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        if (jobCount > 0){
         player.sendMessage(ChatColor.LIGHT_PURPLE + "Current Number of Jobs: " + ChatColor.DARK_GREEN + jobCount);
         player.sendMessage(ChatColor.LIGHT_PURPLE + "Use \"/job check\" to find out what they are!");
        }
        if (jobCount < 1){
         player.sendMessage(ChatColor.LIGHT_PURPLE + "Current Number of Jobs: " + ChatColor.DARK_RED + jobCount);
         player.sendMessage(ChatColor.LIGHT_PURPLE + "Sorry, no jobs running right now.");
        }  
    }
}
