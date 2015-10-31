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
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 *
 * @author Ivan1pl
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
    
}
