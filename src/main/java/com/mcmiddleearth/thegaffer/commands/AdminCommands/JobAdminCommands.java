/*
 * This file is part of TheGaffer.
 * 
 * TheGaffer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * TheGaffer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with TheGaffer.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * 
 */

package com.mcmiddleearth.thegaffer.commands.AdminCommands;

import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.storage.JobDatabase;
import com.mcmiddleearth.thegaffer.utilities.PermissionsUtil;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/**
 *
 * @author Donovan
 */
public class JobAdminCommands implements TabExecutor{
    
    private static HashMap<String, Integer> Methods = new HashMap<>();
    
    private static final List<String> ADMIN_ACTIONS = Arrays.asList(
        "addhelper", "removehelper", "kickworker", "banworker", "unbanworker",
        "inviteworker", "uninviteworker", "setwarp", "setradius", "setkit",
        "clearworkerinven", "bringall", "listworkers"
    );

    // Player-taking actions (Methods value == 1)
    private static final List<String> PLAYER_ACTIONS = Arrays.asList(
        "addhelper", "removehelper", "kickworker", "banworker", "unbanworker",
        "inviteworker", "uninviteworker", "setradius"
    );

    @Override
    public List<String> onTabComplete(CommandSender cs, Command cmd, String label, String[] args) {
        // args[0]="admin", args[1]=<job>, args[2]=<action>, args[3]=<player>
        // When called via /job admin …, args.length >= 1.
        if (args.length == 2) {
            // Completing the job name (args[1])
            String prefix = args[1];
            List<String> matches = new ArrayList<>();
            for (String name : JobDatabase.getActiveJobs().keySet()) {
                if (name.startsWith(prefix)) {
                    matches.add(name);
                }
            }
            return matches;
        }
        if (args.length == 3) {
            // Completing the admin action (args[2])
            String prefix = args[2].toLowerCase();
            List<String> matches = new ArrayList<>();
            for (String action : ADMIN_ACTIONS) {
                if (action.startsWith(prefix)) {
                    matches.add(action);
                }
            }
            return matches;
        }
        if (args.length == 4) {
            // Completing the player name (args[3]) — only for player-taking actions
            if (args.length > 2 && PLAYER_ACTIONS.contains(args[2].toLowerCase())) {
                return null; // let Bukkit supply online player names
            }
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }
    
    public JobAdminCommands(){
        Methods.put("addhelper", 1);
        Methods.put("removehelper", 1);
        Methods.put("kickworker", 1);
        Methods.put("banworker", 1);
        Methods.put("unbanworker", 1);
        Methods.put("setwarp", 0);
        Methods.put("bringall", 0);
        Methods.put("listworkers", 0);
        Methods.put("inviteworker", 1);
        Methods.put("uninviteworker", 1);
        Methods.put("setradius", 1);
        Methods.put("setkit", 0);
        Methods.put("clearworkerinven", 0);
    }

    @Override
    public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args){
        if(cs instanceof Player){
            if(cs.hasPermission(PermissionsUtil.getCreatePermission())){
                Player p = (Player) cs;
                if(args.length <= 2 || !Methods.containsKey(args[2])){  //job admin <job> <command> <args...>
                    return false;                                       //cmd arg 0 arg 1   arg 2    arg 3 -
                }else if(!args[0].equalsIgnoreCase("admin")){
                    return false;
                }else if(args.length>=Methods.get(args[2])+3){
                    Job j = JobDatabase.getActiveJobs().get(args[1]);
                    if(j == null){
                        p.sendMessage(Component.text("No active job by that name.", NamedTextColor.RED));
                        return true;
                    }
                    AdminMethods am = new AdminMethods(j, p);
                    if(Methods.get(args[2]) == 0){
                        boolean success = true;
                        try {
                            Method m = am.getClass().getMethod(args[2].toLowerCase());
                            m.invoke(am);
                        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                            success = false;
                            Logger.getLogger(JobAdminCommands.class.getName()).log(Level.SEVERE, null, ex);
                        }finally{
                            if(success){
                                p.sendMessage(Component.text("Job Edited!", NamedTextColor.AQUA));
                            }else{
                                p.sendMessage(Component.text("Job Edit Failed!", NamedTextColor.RED));
                            }
                        }
                        return true;
                    }else if(Methods.get(args[2]) == 1){
                        boolean success = true;
                        try {
                            Method m = am.getClass().getMethod(args[2].toLowerCase(), String.class);
                            m.invoke(am, args[3]);
                        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex){
                            success = false;
                            Logger.getLogger(JobAdminCommands.class.getName()).log(Level.SEVERE, null, ex);
                        }finally{
                            if(success){
                                p.sendMessage(Component.text("Job Edited!", NamedTextColor.AQUA));
                            }else{
                                p.sendMessage(Component.text("Job Edit Failed!", NamedTextColor.RED));
                            }
                        }
                        return true;
                    }
                }else{
                    return false;
                }
            } else {
                cs.sendMessage(Component.text("You don't have permission to manage jobs.", NamedTextColor.RED));
                return true;
            }
        } else {
            cs.sendMessage("You must be a player to send this command");
            return true;
        }
        return false;
    }
}
