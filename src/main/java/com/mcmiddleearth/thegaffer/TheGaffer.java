
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
import com.mcmiddleearth.thegaffer.commands.JobChatCommand;
import com.mcmiddleearth.thegaffer.commands.JobCreationConversation;
import com.mcmiddleearth.thegaffer.commands.ProjectCommand;
import com.mcmiddleearth.thegaffer.ext.ExternalProtectionHandler;
import com.mcmiddleearth.thegaffer.listeners.CraftingListener;
import com.mcmiddleearth.thegaffer.listeners.JobChatListener;
import com.mcmiddleearth.thegaffer.listeners.JobEventListener;
import com.mcmiddleearth.thegaffer.listeners.PlayerListener;
import com.mcmiddleearth.thegaffer.listeners.ProtectionListener;
import com.mcmiddleearth.thegaffer.listeners.StatsListener;
import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.storage.JobDatabase;
import com.mcmiddleearth.thegaffer.storage.ProjectDatabase;
import com.mcmiddleearth.thegaffer.utilities.BuildProtection;
import com.mcmiddleearth.thegaffer.utilities.CleanupUtil;
import com.mcmiddleearth.thegaffer.utilities.JobBorderManager;
import com.mcmiddleearth.thegaffer.utilities.StatsManager;
import com.mcmiddleearth.thegaffer.utilities.Util;
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

    static String fileExtension = ".yml";
    static boolean debug = false;
    //@Getter
    static Configuration pluginConfig;
    static List<String> unprotectedWorlds = new ArrayList<>();
    static ArrayList<Player> listening = new ArrayList<>();
    static List<ExternalProtectionHandler> externalProtectionAllowHandlers = new ArrayList<>();
    static List<ExternalProtectionHandler> externalProtectionDenyHandlers = new ArrayList<>();
    static String discordChannel;
    static String discordJobEmoji;
    static boolean discordEnabled;
    static boolean jobBorderEnabled;
    static boolean jobDescription;
    static boolean glowing;
    static String helperColor;
    static String workerColor;
    static List<String> allowedPingRoles = new ArrayList<>();

    @Override
    public synchronized void onEnable() {
        serverInstance = getServer();
        pluginInstance = this;
        pluginDataFolder = pluginInstance.getDataFolder();
        setupConfig();

        int jobsLoaded = JobDatabase.loadJobs();
        Util.info("Loaded " + jobsLoaded + " jobs.");
        int projectsLoaded = ProjectDatabase.loadProjects();
        Util.info("Loaded " + projectsLoaded + " projects.");
        StatsManager.loadAggregate();
        StatsManager.loadActive();

        getCommand("createjob").setExecutor(new JobCreationConversation());
        getCommand("job").setExecutor(new JobCommand());
        getCommand("jobadmin").setExecutor(new JobAdminConversation());
        getCommand("jobchat").setExecutor(new JobChatCommand());
        getCommand("project").setExecutor(new ProjectCommand());
        serverInstance.getPluginManager().registerEvents(new PlayerListener(), this);
        serverInstance.getPluginManager().registerEvents(new ProtectionListener(), this);
        serverInstance.getPluginManager().registerEvents(new JobEventListener(), this);
        serverInstance.getPluginManager().registerEvents(new CraftingListener(), this);
        serverInstance.getPluginManager().registerEvents(new JobChatListener(), this);
        serverInstance.getPluginManager().registerEvents(new StatsListener(), this);

        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        // Runs SYNC (main thread): the cleanup reassigns job owners and removes
        // abandoned workers, which call the Bukkit API (scoreboards/teams, player
        // inventories, teleports) and must not run off-thread. Do NOT switch this
        // back to runTaskTimerAsynchronously.
        new BukkitRunnable() {

            @Override
            public void run() {
                Util.debug("Starting running job cleanup.");
                CleanupUtil.scheduledCleanup();
                CleanupUtil.scheduledAbandonersCleanup();
            }
        }.runTaskTimer(this, 0, (5 * 60) * 20);

        // Persist dirty jobs every 60s. The YAML snapshot is built on the main
        // thread inside the task; only the file write itself runs off-thread.
        new BukkitRunnable() {
            @Override
            public void run() {
                JobDatabase.saveAllDirty(true);
                ProjectDatabase.saveAllDirty(true);
                StatsManager.flushActive(true);
            }
        }.runTaskTimer(this, 60 * 20, 60 * 20);

        // Start the particle-wall render task for the visual job boundary.
        // Runs every 10 ticks; draws END_ROD particles for players in active.
        JobBorderManager.startRenderTask(this);
    }

    @Override
    public void onDisable() {
        // Stop the scheduled tasks, then flush any unsaved jobs synchronously
        // (the scheduler can no longer run async tasks during shutdown).
        getServer().getScheduler().cancelTasks(this);
        JobDatabase.saveAllDirty(false);
        ProjectDatabase.saveAllDirty(false);
        StatsManager.flushActive(false);
    }

    public static void setupConfig() {
        pluginConfig = TheGaffer.getPluginInstance().getConfig();
        jobDescription = pluginConfig.getBoolean("jobDescription", false);
        discordEnabled = pluginConfig.contains("discord");
        discordChannel = pluginConfig.getString("discord.channel", null);
        discordJobEmoji = pluginConfig.getString("discord.emoji", "");
        jobBorderEnabled = pluginConfig.getBoolean("showJobBorder", true);
        glowing = pluginConfig.getBoolean("glowing.enabled", true);
        helperColor = pluginConfig.getString("glowing.helperColor", "AQUA");
        workerColor = pluginConfig.getString("glowing.workerColor", "LIGHT_PURPLE");
        debug = pluginConfig.getBoolean("general.debug");
        unprotectedWorlds = pluginConfig.getStringList("unprotectedworlds");
        allowedPingRoles = pluginConfig.getStringList("allowRolePing");
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

    public static void scheduleOwnerTimeout(Job job) {
        Long time = System.currentTimeMillis();
        CleanupUtil.getWaiting().put(job, time);
    }

    /**
     * Reflective integration contract — <b>do not change this signature.</b> External plugins
     * (PlotBuild, MCME-Architect) call this by reflection
     * ({@code getMethod("hasBuildPermission", Player.class, Location.class)}). If the signature
     * drifts, their lookup fails and at least PlotBuild falls <i>open</i> to "allowed", silently
     * bypassing build protection. Pinned by {@code ProtectionApiContractTest}.
     */
    public static boolean hasBuildPermission(Player player, Location location) {
        return getBuildProtection(player, location).equals(BuildProtection.ALLOWED);
    }

    /** Reflective integration contract — keep the {@code (Player, Location) -> String} signature stable (see {@link #hasBuildPermission}). */
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

    public static boolean isJobBorderEnabled() {
        return jobBorderEnabled;
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

    public static List<String> getAllowedPingRoles() {
        return allowedPingRoles;
    }
}
