package co.mcme.jobs;

import co.mcme.jobs.commands.JobAdminCommand;
import co.mcme.jobs.commands.JobCommand;
import co.mcme.jobs.commands.JobCreationConversation;
import co.mcme.jobs.listeners.PlayerListener;
import co.mcme.jobs.listeners.ProtectionListener;
import co.mcme.thegaffer.utilities.Util;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class Jobs extends JavaPlugin {

    static Configuration conf;
    public static HashMap<String, Job> runningJobs = new HashMap();
    public static TreeMap<String, Job> notRunningJobs = new TreeMap();
    public static HashMap<Job, Long> timedout_waiting = new HashMap();
    public static boolean debug = false;
    public static String version;
    @Getter
    static File inactiveDir;
    @Getter
    static File activeDir;
    @Getter
    static File outdatedDir;
    @Getter
    static Server serverInstance;
    @Getter
    static Jobs pluginInstance;
    @Getter
    static File pluginDataFolder;
    @Getter
    static String fileSeperator = System.getProperty("file.separator");

    @Override
    public void onEnable() {
        serverInstance = getServer();
        pluginInstance = this;
        pluginDataFolder = pluginInstance.getDataFolder();
        inactiveDir = new File(pluginDataFolder.getPath() + fileSeperator + "jobs" + fileSeperator + "inactive");
        activeDir = new File(pluginDataFolder.getPath() + fileSeperator + "jobs" + fileSeperator + "active");
        outdatedDir = new File(pluginDataFolder.getPath() + fileSeperator + "jobs" + fileSeperator + "outdated");
        if (!activeDir.exists()) {
            activeDir.mkdirs();
            Util.info("Did not find the active jobs directory");
        }
        if (!inactiveDir.exists()) {
            inactiveDir.mkdirs();
            Util.info("Did not find the inactive jobs directory");
        }
        if (!outdatedDir.exists()) {
            outdatedDir.mkdirs();;
            Util.info("Did not find the outdated jobs directory");
        }
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(), this);
        setupConfig();
        debug = getConfig().getBoolean("general.debug");
        getCommand("jobadmin").setExecutor(new JobAdminCommand());
        getCommand("job").setExecutor(new JobCommand());
        getCommand("createjob").setExecutor(new JobCreationConversation());
        getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                Util.debug("Starting cleanup");
                Cleanup.scheduledCleanup();
            }
        }, 0, (5 * 60) * 20);
        version = getServer().getPluginManager().getPlugin("TheGaffer").getDescription().getVersion();
    }

    @Override
    public void onDisable() {
        for (Job job : runningJobs.values()) {
            if (job.isDirty()) {
                try {
                    job.writeToFile();
                } catch (IOException ex) {
                    Logger.getLogger(Jobs.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public void setupConfig() {
        conf = getConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        loadJobs();
    }

    public static void storeJob(String jobname, String admin, String status, boolean invite) {
        Player p = Bukkit.getPlayer(admin);
        if (status.equalsIgnoreCase("new")) {
            Location adminloc = Bukkit.getPlayer(admin).getLocation();
            int newx = (int) adminloc.getX();
            int newy = (int) adminloc.getY();
            int newz = (int) adminloc.getZ();
            adminloc.setX(newx);
            adminloc.setY(newy);
            adminloc.setZ(newz);
            Job newjob = new Job(jobname, admin, true, adminloc, adminloc.getWorld().getName(), invite);
            runningJobs.put(jobname, newjob);
            try {
                newjob.writeToFile();
            } catch (IOException ex) {
                Logger.getLogger(Jobs.class.getName()).log(Level.SEVERE, null, ex);
            }
            for (Player targetP : Bukkit.getOnlinePlayers()) {
                targetP.sendMessage(ChatColor.GRAY + admin + " has started a new job called '" + jobname + "'");
                targetP.playSound(targetP.getLocation(), Sound.WITHER_DEATH, 1, 100);
            }
        }
        if (status.equalsIgnoreCase("remove")) {
            if (runningJobs.containsKey(jobname)) {
                Job oldjob = runningJobs.get(jobname);
                oldjob.setStatus(false);
                oldjob.sendToAll(ChatColor.GRAY + "The " + ChatColor.AQUA + oldjob.getName() + ChatColor.GRAY + " job has ended.");
                notRunningJobs.put(oldjob.getName(), oldjob);
                runningJobs.remove(jobname);
                try {
                    oldjob.writeToFile();
                } catch (IOException ex) {
                    Logger.getLogger(Jobs.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private void loadJobs() {
        Util.debug("Loaded " + co.mcme.jobs.files.Loader.loadActiveJobs() + " active jobs from file.");
        for (Job job : runningJobs.values()) {
            getServer().getPluginManager().registerEvents(job, this);
        }
        //Util.debug("Loaded " + co.mcme.jobs.files.Loader.loadInactiveJobs() + " inactive jobs from file.");
    }

    public static String getJobInfo(Job job) {
        StringBuilder out = new StringBuilder();
        out.append(ChatColor.GRAY).append(job.getName()).append("\n");
        out.append("Started by: ").append(ChatColor.AQUA).append(job.getAdmin().getName()).append("\n").append(ChatColor.GRAY);
        out.append("Started on: ").append(ChatColor.AQUA).append(new Date(job.getRunningSince()).toGMTString()).append("\n").append(ChatColor.GRAY);
        out.append("Location: ").append(ChatColor.AQUA).append(job.getWorld().getName()).append(" (x: ").append(job.getWarp().getX()).append(", y: ").append(job.getWarp().getY()).append(", z: ").append(job.getWarp().getZ()).append(")").append("\n").append(ChatColor.GRAY);
        out.append("Stored in: ").append(ChatColor.AQUA).append(job.getFileName()).append("\n").append(ChatColor.GRAY);
        String status = (job.getStatus()) ? ChatColor.GREEN + "OPEN" : ChatColor.RED + "CLOSED";
        out.append("Status: ").append(status);
        return out.toString();
    }

    public String jobExists(String name) {
        String out = "never";
        if (runningJobs.containsKey(name)) {
            out = "active";
        }
        if (notRunningJobs.containsKey(name)) {
            out = "dormant";
        }
        if (!(notRunningJobs.containsKey(name) || runningJobs.containsKey(name))) {
            out = "never";
        }
        return out;
    }

    static void scheduleAdminTimeout(Job job) {
        Long time = System.currentTimeMillis();
        timedout_waiting.put(job, time);
    }

    public static void disableJob(Job job) {
        if (runningJobs.containsValue(job)) {
            job.setStatus(false);
            notRunningJobs.put(job.getName(), job);
            runningJobs.remove(job.getName());
            try {
                job.writeToFile();
            } catch (IOException ex) {
                Logger.getLogger(Jobs.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void enableJob(Job job) {
        if (notRunningJobs.containsValue(job)) {
            job.setStatus(true);
            runningJobs.put(job.getName(), job);
            notRunningJobs.remove(job.getName());
            try {
                job.writeToFile();
            } catch (IOException ex) {
                Logger.getLogger(Jobs.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
