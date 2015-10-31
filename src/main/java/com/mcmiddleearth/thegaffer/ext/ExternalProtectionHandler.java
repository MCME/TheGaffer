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
package com.mcmiddleearth.thegaffer.ext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 *
 * @author Ivan1pl
 */
public class ExternalProtectionHandler {
    
    private final String pluginName;
    private final String methodName;
    private Plugin plugin;
    private Method checkPermsMethod;
    
    public ExternalProtectionHandler(String pluginName, String methodName) {
        this.pluginName = pluginName;
        this.methodName = methodName;
        load(pluginName, methodName);
    }
    
    public boolean handle(Player player, Location location) {
        if(plugin == null || checkPermsMethod == null) {
            load(pluginName, methodName);
        }
        if(plugin == null || checkPermsMethod == null || !plugin.isEnabled()) {
            return false;
        }
        try {
            return (boolean) checkPermsMethod.invoke(null, player, location);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger(ExternalProtectionHandler.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    private void load(String pluginName, String methodName) {
        plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if(plugin != null) {
            try {
                checkPermsMethod = plugin.getClass().getMethod(methodName, Player.class, Location.class);
            } catch (NoSuchMethodException | SecurityException ex) {
                Logger.getLogger(ExternalProtectionHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
}
