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
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

public class JobWarp {

    @Getter @Setter
    private double x;
    @Getter @Setter
    private double y;
    @Getter @Setter
    private double z;
    @Getter @Setter
    private float yaw;
    @Getter @Setter
    private float pitch;
    @Getter @Setter
    private String world;

    public JobWarp(Location loc) {
        this.x = loc.getX();
        this.y = loc.getY();
        this.z = loc.getZ();
        this.yaw = loc.getYaw();
        this.pitch = loc.getPitch();
        this.world = loc.getWorld().getName();
    }
    
    public JobWarp() {
        
    }

    public void updateLocation(Location loc) {
        this.x = loc.getX();
        this.y = loc.getY();
        this.z = loc.getZ();
        this.yaw = loc.getYaw();
        this.pitch = loc.getPitch();
        this.world = loc.getWorld().getName();
    }

    public Location toBukkitLocation() {
        return new Location(TheGaffer.getServerInstance().getWorld(world), x, y, z, yaw, pitch);
    }
}
