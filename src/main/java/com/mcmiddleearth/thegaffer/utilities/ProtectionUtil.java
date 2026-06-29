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
            if (JobDatabase.getActiveJobs().isEmpty()) {
                return BuildProtection.NO_JOB;
            }
            // Single pass: find whether any job is in this world and which job (if any)
            // the player is a worker in. One job per player is enforced, so at most one.
            World world = location.getWorld();
            boolean jobInWorld = false;
            Job playerJob = null;
            for (Job job : JobDatabase.getActiveJobs().values()) {
                if (world.equals(job.getBukkitWorld())) {
                    jobInWorld = true;
                }
                if (job.isPlayerWorking(player)) {
                    playerJob = job;
                }
            }
            if (!jobInWorld) {
                return BuildProtection.WORLD_DENIED;
            }
            if (playerJob == null) {
                return BuildProtection.NOT_IN_JOB;
            }
            if (playerJob.getBounds().contains(location.getBlockX(), location.getBlockZ())) {
                return playerJob.isPaused() ? BuildProtection.JOB_PAUSED : BuildProtection.ALLOWED;
            }
            return BuildProtection.OUT_OF_BOUNDS;
        }
    }
    
}
