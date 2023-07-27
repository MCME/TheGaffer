
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
package com.mcmiddleearth.thegaffer;

import com.mcmiddleearth.thegaffer.commands.AdminCommands.JobAdminConversation;
import com.mcmiddleearth.thegaffer.commands.JobCommand;
import com.mcmiddleearth.thegaffer.commands.JobCreationConversation;
import com.mcmiddleearth.thegaffer.ext.ExternalProtectionHandler;
import com.mcmiddleearth.thegaffer.listeners.CraftingListener;
import com.mcmiddleearth.thegaffer.listeners.JobEventListener;
import com.mcmiddleearth.thegaffer.listeners.PlayerListener;
import com.mcmiddleearth.thegaffer.listeners.ProtectionListener;
import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.utilities.BuildProtection;
import com.mcmiddleearth.thegaffer.utilities.CleanupUtil;
import com.mcmiddleearth.thegaffer.utilities.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.mcmiddleearth.thegaffer.utilities.ProtectionUtil.getBuildProtection;

public class TheGaffer extends JavaPlugin {

    static Server serverInstance;

    static TheGaffer pluginInstance;
    static File pluginDataFolder;
    static String fileSeperator = System.getProperty("file.separator");

    static String fileExtension = ".job";
    static boolean debug = false;
    //@Getter
    //static boolean TS;
    static Configuration pluginConfig;
    static boolean TSenabled;
    static List<String> unprotectedWorlds = new ArrayList<>();
    static ArrayList<Player> listening = new ArrayList<>();
    static List<ExternalProtectionHandler> externalProtectionAllowHandlers = new ArrayList<>();
    static List<ExternalProtectionHandler> externalProtectionDenyHandlers = new ArrayList<>();
    static String discordChannel;
    static String discordJobEmoji;
    static boolean discordEnabled;
    static boolean jobKitsEnabled;
    static boolean jobDescription;
    static boolean glowing;
    static String helperColor;
    static String workerColor;

    @Override
    public synchronized void onEnable() {
        serverInstance = getServer();
        pluginInstance = this;
        pluginDataFolder = pluginInstance.getDataFolder();
        setupConfig();

        //new TSfetcher().runTaskTimer(this, 20, 1200);

        /*
        try {
            int jobsLoaded = JobDatabase.loadJobs();
            Util.info("Loaded " + jobsLoaded + " jobs.");
        } catch (IOException ex) {
            Util.severe(ex.getMessage());
        }
        
         */
        getCommand("createjob").setExecutor(new JobCreationConversation());
        getCommand("job").setExecutor(new JobCommand());
        getCommand("jobadmin").setExecutor(new JobAdminConversation());
        serverInstance.getPluginManager().registerEvents(new PlayerListener(), this);
        serverInstance.getPluginManager().registerEvents(new ProtectionListener(), this);
        serverInstance.getPluginManager().registerEvents(new JobEventListener(), this);
        serverInstance.getPluginManager().registerEvents(new CraftingListener(), this);

        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        new BukkitRunnable() {

            @Override
            public void run() {
                Util.debug("Starting running job cleanup.");
                CleanupUtil.scheduledCleanup();
                CleanupUtil.scheduledAbandonersCleanup();
            }
        }.runTaskTimerAsynchronously(this, 0, (5 * 60) * 20);
    }

    public static void setupConfig() {
        pluginConfig = TheGaffer.getPluginInstance().getConfig();
        if (pluginConfig.contains("TS")) {
            TSenabled = pluginConfig.getBoolean("TS", false);
        } else {
            TSenabled = false;
        }
        jobDescription = pluginConfig.getBoolean("jobDescription", false);
        jobKitsEnabled = pluginConfig.getBoolean("jobKits", false);
        discordEnabled = pluginConfig.contains("discord");
        discordChannel = pluginConfig.getString("discord.channel", null);
        discordJobEmoji = pluginConfig.getString("discord.emoji", "");
        glowing = pluginConfig.getBoolean("glowing.enabled", true);
        helperColor = pluginConfig.getString("glowing.helperColor", "AQUA");
        workerColor = pluginConfig.getString("glowing.workerColor", "LIGHT_PURPLE");
        debug = pluginConfig.getBoolean("general.debug");
        unprotectedWorlds = pluginConfig.getStringList("unprotectedworlds");
        if (pluginConfig.contains("externalProtectionHandlers")) {
            ConfigurationSection section = pluginConfig.getConfigurationSection("externalProtectionHandlers");
            Set<String> handlers = section.getKeys(false);
            for (String pname : handlers) {
                ConfigurationSection pluginSection = section.getConfigurationSection(pname);
                if (pluginSection.contains("allow")) {
                    externalProtectionAllowHandlers.add(new ExternalProtectionHandler(pname, pluginSection.getString("allow")));
                }
                if (pluginSection.contains("deny")) {
                    externalProtectionDenyHandlers.add(new ExternalProtectionHandler(pname, pluginSection.getString("deny")));
                }
            }
        }
        TheGaffer.getPluginInstance().saveDefaultConfig();
    }

    public static boolean isProjectsEnabled() {

        if (Bukkit.getServer().getPluginManager().getPlugin("McMeProject") != null) {
            return true;
        } else {
            return false;
        }

    }

    public static void scheduleOwnerTimeout(Job job) {
        Long time = System.currentTimeMillis();
        CleanupUtil.getWaiting().put(job, time);
    }

    public static boolean hasBuildPermission(Player player, Location location) {
        return getBuildProtection(player, location).equals(BuildProtection.ALLOWED);
    }

    public static String getBuildProtectionMessage(Player player, Location location) {
        return getBuildProtection(player, location).getMessage();
    }

    public static Server getServerInstance() {
        return serverInstance;
    }

    public static TheGaffer getPluginInstance() {
        return pluginInstance;
    }

    public static File getPluginDataFolder() {
        return pluginDataFolder;
    }

    public static String getFileSeperator() {
        return fileSeperator;
    }

    public static String getFileExtension() {
        return fileExtension;
    }

    public static boolean isDebug() {
        return debug;
    }

    public static Configuration getPluginConfig() {
        return pluginConfig;
    }

    public static boolean isTSenabled() {
        return TSenabled;
    }

    public static List<String> getUnprotectedWorlds() {
        return unprotectedWorlds;
    }

    public static ArrayList<Player> getListening() {
        return listening;
    }

    public static List<ExternalProtectionHandler> getExternalProtectionAllowHandlers() {
        return externalProtectionAllowHandlers;
    }

    public static List<ExternalProtectionHandler> getExternalProtectionDenyHandlers() {
        return externalProtectionDenyHandlers;
    }

    public static String getDiscordChannel() {
        return discordChannel;
    }

    public static String getDiscordJobEmoji() {
        return discordJobEmoji;
    }

    public static boolean isDiscordEnabled() {
        return discordEnabled;
    }

    public static boolean isJobKitsEnabled() {
        return jobKitsEnabled;
    }

    public static boolean isJobDescription() {
        return jobDescription;
    }

    public static boolean isGlowing() {
        return glowing;
    }

    public static String getHelperColor() {
        return helperColor;
    }

    public static String getWorkerColor() {
        return workerColor;
    }
}
