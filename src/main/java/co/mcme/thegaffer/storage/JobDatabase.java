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
package co.mcme.thegaffer.storage;

import co.mcme.thegaffer.TheGaffer;
import co.mcme.thegaffer.utilities.Util;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class JobDatabase {

    @Getter
    private static final TreeMap<String, Job> activeJobs = new TreeMap();
    @Getter
    private static final TreeMap<String, Job> inactiveJobs = new TreeMap();

    public static int loadJobs() throws IOException {
        int count = 0;
        File activeJobFolder = new File(TheGaffer.getPluginDataFolder() + TheGaffer.getFileSeperator() + "jobs" + TheGaffer.getFileSeperator() + "active");
        if (!activeJobFolder.exists()) {
            activeJobFolder.mkdirs();
        }
        String[] aJ = activeJobFolder.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(TheGaffer.getFileExtension());
            }
        });
        ArrayList<Job> tjobs = new ArrayList();
        for (String fName : aJ) {
            File jFile = new File(activeJobFolder, fName);
            if (!jFile.isDirectory()) {
                Job job = TheGaffer.getJsonMapper().readValue(jFile, Job.class);
                tjobs.add(job);
            }
        }
        for (Job jerb : tjobs) {
            jerb.setDirty(false);
            if (jerb.isRunning()) {
                activeJobs.put(jerb.getName(), jerb);
                count++;
            }
        }
        File inactiveJobFolder = new File(TheGaffer.getPluginDataFolder() + TheGaffer.getFileSeperator() + "jobs" + TheGaffer.getFileSeperator() + "inactive");
        if (!inactiveJobFolder.exists()) {
            inactiveJobFolder.mkdirs();
        }
        String[] iJ = inactiveJobFolder.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(TheGaffer.getFileExtension());
            }
        });
        ArrayList<Job> tijobs = new ArrayList();
        for (String fName : iJ) {
            File jFile = new File(activeJobFolder, fName);
            if (!jFile.isDirectory()) {
                Job job = TheGaffer.getJsonMapper().readValue(jFile, Job.class);
                tijobs.add(job);
            }
        }
        for (Job jerb : tijobs) {
            jerb.setDirty(false);
            if (jerb.isRunning()) {
                inactiveJobs.put(jerb.getName(), jerb);
                count++;
            }
        }
        return count;
    }

    public static void saveJobs() throws IOException {
        File activeJobFolder = new File(TheGaffer.getPluginDataFolder() + TheGaffer.getFileSeperator() + "jobs" + TheGaffer.getFileSeperator() + "active");
        File inactiveJobFolder = new File(TheGaffer.getPluginDataFolder() + TheGaffer.getFileSeperator() + "jobs" + TheGaffer.getFileSeperator() + "inactive");
        if (!activeJobFolder.exists()) {
            activeJobFolder.mkdirs();
        }
        if (!inactiveJobFolder.exists()) {
            inactiveJobFolder.mkdirs();
        }
        for (Job jerb : activeJobs.values()) {
            File location = new File(activeJobFolder, jerb.getName() + TheGaffer.getFileExtension());
            TheGaffer.getJsonMapper().writeValue(location, jerb);
            jerb.setDirty(false);
        }
        for (Job jerb : inactiveJobs.values()) {
            File location = new File(inactiveJobFolder, jerb.getName() + TheGaffer.getFileExtension());
            TheGaffer.getJsonMapper().writeValue(location, jerb);
            jerb.setDirty(false);
        }
    }

    public static boolean activateJob(Job j) {
        if (activeJobs.containsKey(j.getName())) {
            return false;
        }
        activeJobs.put(j.getName(), j);
        TheGaffer.getServerInstance().broadcastMessage(ChatColor.AQUA + j.getOwner() + ChatColor.GRAY + " has started a job called \"" + j.getName() + ChatColor.GRAY + "\"");
        for (Player p : TheGaffer.getServerInstance().getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.WITHER_DEATH, 10, 1);
        }
        TheGaffer.getServerInstance().getPluginManager().registerEvents(j, TheGaffer.getPluginInstance());
        try {
            saveJobs();
        } catch (IOException ex) {
            Util.severe(ex.getMessage());
        }
        return true;
    }

    public static boolean deactivateJob(Job j) {
        if (!activeJobs.containsKey(j.getName())) {
            return false;
        }
        j.setRunning(false);
        j.sendToAll(ChatColor.GRAY + "The " + j.getName() + " job has ended.");
        for (Player p : j.getWorkersAsPlayers()) {
            p.playSound(p.getLocation(), Sound.ENDERDRAGON_WINGS, 10, 1);
        }
        j.setDirty(true);
        activeJobs.remove(j.getName());
        inactiveJobs.put(j.getName(), j);
        HandlerList.unregisterAll(j);
        try {
            saveJobs();
        } catch (IOException ex) {
            Util.severe(ex.getMessage());
        }
        return true;
    }
}
