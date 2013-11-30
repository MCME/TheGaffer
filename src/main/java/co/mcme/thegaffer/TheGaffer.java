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

import co.mcme.thegaffer.storage.Job;
import co.mcme.thegaffer.storage.JobDatabase;
import co.mcme.thegaffer.storage.JobWarp;
import java.io.File;
import java.io.IOException;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Server;
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
    static ObjectMapper jsonMapper = new ObjectMapper().configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
    @Getter
    static String fileExtension = ".job";

    @Override
    public void onEnable() {
        serverInstance = getServer();
        pluginInstance = this;
        pluginDataFolder = pluginInstance.getDataFolder();
        try {
            JobDatabase.loadJobs();
        } catch (IOException ex) {
            //Log this
        }
        Location loc = new Location(serverInstance.getWorlds().get(0), 0, 85, 0);
        JobWarp warp = new JobWarp(loc);
        Job debug = new Job("derpjob", "meggawatts", true, warp, warp.getWorld(), false);
        JobDatabase.activateJob(debug);
    }

    @Override
    public void onDisable() {
        try {
            JobDatabase.saveJobs();
        } catch (IOException ex) {
            //Log this
        }
    }
}
