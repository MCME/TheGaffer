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
package com.mcmiddleearth.thegaffer.utilities;

import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.ext.ExternalProtectionHandler;
import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.storage.JobDatabase;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 *
 * @author Ivan1pl, Eriol_Eandur
 */
public class ProtectionUtil {
    
    public static boolean isAllowedToBuild(Player player, Location location) {
        boolean ret = false;
        for(ExternalProtectionHandler handler : TheGaffer.getExternalProtectionAllowHandlers()) {
            ret = ret || handler.handle(player, location);
        }
        return ret;
    }
    
    public static boolean isDeniedToBuild(Player player, Location location) {
        boolean ret = false;
        for(ExternalProtectionHandler handler : TheGaffer.getExternalProtectionDenyHandlers()) {
            ret = ret || handler.handle(player, location);
        }
        return ret;
    }
    
    public static BuildProtection getBuildProtection(Player player, Location location) {
        if (ProtectionUtil.isDeniedToBuild(player,location)) {
            return BuildProtection.LOC_DENIED;
        } else if (player.hasPermission(PermissionsUtil.getIgnoreWorldProtection()) || TheGaffer.getUnprotectedWorlds().contains(location.getWorld().getName()) ||
                ProtectionUtil.isAllowedToBuild(player, location)) {
            return BuildProtection.ALLOWED;
        } else {
            World world = location.getWorld();
            if (JobDatabase.getActiveJobs().isEmpty()) {
                return BuildProtection.NO_JOB;
            } else {
                HashMap<Job, World> workingworlds = new HashMap<>();
                HashMap<Job, Rectangle2D> areas = new HashMap<>();
                for (Job job : JobDatabase.getActiveJobs().values()) {
                    workingworlds.put(job, job.getBukkitWorld());
                    areas.put(job, job.getBounds());
                }
                if (!workingworlds.containsValue(world)) {
                    return BuildProtection.WORLD_DENIED;
                } else {
                    boolean playerisworking = false;
                    for (Job job : workingworlds.keySet()) {
                        if (job.isPlayerWorking(player)) {
                            playerisworking = true;
                        }
                    }
                    if (!playerisworking) {
                        return BuildProtection.NOT_IN_JOB;
                    } else {
                        boolean isinjobarea = false;
                        int x = location.getBlockX();
                        int z = location.getBlockZ();
                        for (Job job : JobDatabase.getActiveJobs().values()) {
                            if (job.isPlayerWorking(player) && job.getBounds().contains(x, z)) {
                                isinjobarea = true;
                                if (job.isPaused()) {
                                    return BuildProtection.JOB_PAUSED;
                                }
                            }
                        }
                        if (isinjobarea) {
                            return BuildProtection.ALLOWED;
                        } else {
                            return BuildProtection.OUT_OF_BOUNDS;
                        }
                    }
                }
            }
        }
    }
    
}
