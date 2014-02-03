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
package co.mcme.thegaffer;

import co.mcme.thegaffer.commands.JobAdminConversation;
import co.mcme.thegaffer.commands.JobCommand;
import co.mcme.thegaffer.commands.JobCreationConversation;
import co.mcme.thegaffer.listeners.JobEventListener;
import co.mcme.thegaffer.listeners.PlayerListener;
import co.mcme.thegaffer.listeners.ProtectionListener;
import co.mcme.thegaffer.servlet.GafferServer;
import co.mcme.thegaffer.storage.Job;
import co.mcme.thegaffer.utilities.Util;
import co.mcme.thegaffer.storage.JobDatabase;
import co.mcme.thegaffer.utilities.CleanupUtil;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;
import org.bukkit.Server;
import org.bukkit.configuration.Configuration;
import org.bukkit.plugin.java.JavaPlugin;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

public class TheGaffer extends JavaPlugin {

    @Getter
    static Server serverInstance;
    @Getter
    static TheGaffer pluginInstance;
    @Getter
    static File pluginDataFolder;
    @Getter
    static String fileSeperator = System.getProperty("file.separator");
    @Getter
    static ObjectMapper jsonMapper;
    @Getter
    static String fileExtension = ".job";
    @Getter
    static boolean debug = false;
    @Getter
    static Configuration pluginConfig;
    @Getter
    static int servletPort;
    GafferServer server;

    @Override
    public synchronized void onEnable() {
        serverInstance = getServer();
        pluginInstance = this;
        pluginDataFolder = pluginInstance.getDataFolder();
        debug = getConfig().getBoolean("general.debug");
        jsonMapper = new ObjectMapper().configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
        setupConfig();
        try {
            int jobsLoaded = JobDatabase.loadJobs();
            Util.info("Loaded " + jobsLoaded + " jobs.");
        } catch (IOException ex) {
            Util.severe(ex.getMessage());
        }
        getCommand("createjob").setExecutor(new JobCreationConversation());
        getCommand("job").setExecutor(new JobCommand());
        getCommand("jobadmin").setExecutor(new JobAdminConversation());
        serverInstance.getPluginManager().registerEvents(new PlayerListener(), this);
        serverInstance.getPluginManager().registerEvents(new ProtectionListener(), this);
        serverInstance.getPluginManager().registerEvents(new JobEventListener(), this);
        serverInstance.getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                Util.debug("Starting running job cleanup.");
                CleanupUtil.scheduledCleanup();
                CleanupUtil.scheduledAbandonersCleanup();
            }
        }, 0, (5 * 60) * 20);
        server = new GafferServer(servletPort);
        try {
            server.startServer();
        } catch (Exception ex) {
            Util.severe(ex.toString());
        }
    }
    
    @Override
    public synchronized void onDisable() {
        try {
            server.stopServer();
        } catch (Exception ex) {
            Logger.getLogger(TheGaffer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setupConfig() {
        pluginConfig = getConfig();
        getConfig().options().copyDefaults(true);
        debug = pluginConfig.getBoolean("general.debug");
        servletPort = pluginConfig.getInt("servlet.port");
        saveConfig();
    }

    public static void scheduleOwnerTimeout(Job job) {
        Long time = System.currentTimeMillis();
        CleanupUtil.getWaiting().put(job, time);
    }
}
