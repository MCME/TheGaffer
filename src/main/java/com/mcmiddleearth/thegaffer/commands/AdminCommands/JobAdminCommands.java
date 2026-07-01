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

import com.mcmiddleearth.thegaffer.GafferResponses;
import com.mcmiddleearth.thegaffer.GafferResponses.GafferResponse;
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
import java.util.Set;
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
    
    private static final HashMap<String, Integer> Methods = new HashMap<>();
    static {
        // Populate at class-init, NOT in the instance constructor: /jobadmin is registered as
        // a JobAdminConversation, so a JobAdminCommands instance may never be constructed — yet
        // JobAdminConversation forwards to the static executeOneLiner, which needs this map.
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
        Methods.put("clearworkerinven", 0);
    }

    /**
     * Actions that are destructive and require the trailing "confirm" token in the one-liner.
     * These mirror the dialog's typed-confirm step for bringall and clearworkerinven.
     */
    private static final Set<String> CONFIRM_ACTIONS = Set.of("bringall", "clearworkerinven");

    private static final List<String> ADMIN_ACTIONS = Arrays.asList(
        "addhelper", "removehelper", "kickworker", "banworker", "unbanworker",
        "inviteworker", "uninviteworker", "setwarp", "setradius",
        "clearworkerinven", "bringall", "listworkers"
    );

    // Player-taking actions (Methods value == 1, arg is a player name). NOTE: setradius
    // also takes one arg but it's a NUMBER, so it is deliberately excluded — its tab-complete
    // should not offer player names.
    private static final List<String> PLAYER_ACTIONS = Arrays.asList(
        "addhelper", "removehelper", "kickworker", "banworker", "unbanworker",
        "inviteworker", "uninviteworker"
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
            String action = args[2].toLowerCase();
            // Destructive actions: suggest "confirm" as the next token
            if (CONFIRM_ACTIONS.contains(action)) {
                String prefix = args[3].toLowerCase();
                return "confirm".startsWith(prefix) ? Arrays.asList("confirm") : Collections.emptyList();
            }
            // Player-taking actions: let Bukkit supply online player names
            if (PLAYER_ACTIONS.contains(action)) {
                return null;
            }
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }
    
    public JobAdminCommands(){
        // Dispatch map is populated in a static initialiser (above) so it is ready regardless
        // of whether an instance is ever constructed.
    }

    /** Sends a help line listing all known admin actions. */
    private static void sendAdminHelp(CommandSender cs) {
        String actions = String.join(", ", ADMIN_ACTIONS);
        cs.sendMessage(Component.text(
                "Unknown admin action — try: " + actions + " · or /jobadmin for the guided version.",
                NamedTextColor.YELLOW));
    }

    @Override
    public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args){
        return executeOneLiner(cs, args);
    }

    /**
     * Shared one-liner executor for both {@code /job admin …} and {@code /jobadmin …} (with args).
     *
     * <p>The {@code args} array must use the <em>admin-prefixed</em> layout:
     * <pre>args[0]="admin"  args[1]=&lt;job&gt;  args[2]=&lt;action&gt;  args[3…]=extras</pre>
     * {@code JobAdminConversation} prepends {@code "admin"} to its own arg array before calling
     * here, so both entry points go through the same code path, including the confirm-gate and
     * the {@code setradius} validation.</p>
     *
     * @param cs   the command sender (must be an online Player with create permission)
     * @param args the admin-prefixed argument array
     * @return always {@code true} (Bukkit contract)
     */
    public static boolean executeOneLiner(CommandSender cs, String[] args){
        if(cs instanceof Player p){
            if(cs.hasPermission(PermissionsUtil.getCreatePermission())){
                // Validate that arg[0] is "admin" (sanity-check: both entry points set this).
                if (args.length >= 1 && !args[0].equalsIgnoreCase("admin")) {
                    sendAdminHelp(cs);
                    return true;
                }
                // Need at least: admin <job> <action>
                if (args.length <= 2 || !Methods.containsKey(args[2].toLowerCase())) {  //job admin <job> <command> <args...>
                    sendAdminHelp(cs);                                                   //cmd arg 0 arg 1   arg 2    arg 3 -
                    return true;
                } else if(args.length>=Methods.get(args[2].toLowerCase())+3){
                    Job j = JobDatabase.getActiveJobs().get(args[1]);
                    if(j == null){
                        p.sendMessage(Component.text("No active job by that name.", NamedTextColor.RED));
                        return true;
                    }
                    AdminMethods am = new AdminMethods(j, p);
                    if(Methods.get(args[2].toLowerCase()) == 0){
                        String action = args[2].toLowerCase();

                        // Gate destructive one-liners: require a trailing "confirm" token to
                        // mirror the dialog's typed-confirm step. Without it, explain and abort.
                        if (CONFIRM_ACTIONS.contains(action)) {
                            boolean hasConfirm = args.length >= 4
                                    && "confirm".equalsIgnoreCase(args[3]);
                            if (!hasConfirm) {
                                String msg = action + " teleports all online workers to your location."
                                        + " Re-run: /job admin " + args[1] + " " + action + " confirm";
                                if (action.equals("clearworkerinven")) {
                                    msg = action + " wipes every worker's inventory."
                                            + " Re-run: /job admin " + args[1] + " " + action + " confirm";
                                }
                                p.sendMessage(Component.text(msg, NamedTextColor.YELLOW));
                                return true;
                            }
                        }

                        try {
                            Method m = am.getClass().getMethod(action);
                            Object result = m.invoke(am);
                            // Surface the real result instead of a generic "Job Edited!" message.
                            sendResult(p, result, args[1], null);
                        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                            Logger.getLogger(JobAdminCommands.class.getName()).log(Level.SEVERE, null, ex);
                            p.sendMessage(Component.text("Job Edit Failed!", NamedTextColor.RED));
                        }
                        return true;
                    }else if(Methods.get(args[2].toLowerCase()) == 1){
                        try {
                            Method m = am.getClass().getMethod(args[2].toLowerCase(), String.class);
                            Object result = m.invoke(am, args[3]);
                            // Surface the real result instead of a generic "Job Edited!" message.
                            sendResult(p, result, args[1], args[3]);
                        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex){
                            Logger.getLogger(JobAdminCommands.class.getName()).log(Level.SEVERE, null, ex);
                            p.sendMessage(Component.text("Job Edit Failed!", NamedTextColor.RED));
                        }
                        return true;
                    }
                } else {
                    // Not enough arguments for the given action — show help.
                    sendAdminHelp(cs);
                    return true;
                }
            } else {
                cs.sendMessage(Component.text("You don't have permission to manage jobs.", NamedTextColor.RED));
                return true;
            }
        } else {
            cs.sendMessage("You must be a player to send this command");
            return true;
        }
        return true;
    }

    /**
     * Surfaces the return value of an AdminMethods call to the player, mirroring
     * the dialog's responsePrompt logic:
     * <ul>
     *   <li>{@link String} (non-blank) → send the text in AQUA (e.g. listworkers output).</li>
     *   <li>{@link GafferResponse} → GREEN "Success: &lt;msg&gt;" or RED "Failure: &lt;msg&gt;",
     *       with %job% and %name% substituted.</li>
     *   <li>null / other → fall back to the generic "Job Edited!" confirmation.</li>
     * </ul>
     *
     * @param p        the admin player to message
     * @param result   the reflective return value (may be null)
     * @param jobName  the job name, substituted for {@code %job%} in response messages
     * @param nameArg  the player-name argument (args[3]), substituted for {@code %name%}; may be null
     */
    private static void sendResult(Player p, Object result, String jobName, String nameArg) {
        if (result instanceof String s && !s.isBlank()) {
            // listworkers and any future String-returning methods: send raw text
            p.sendMessage(Component.text(s, NamedTextColor.AQUA));
        } else if (result instanceof GafferResponses.SetRadiusResponse srr) {
            // setradius: concrete confirmation without generic "Success:" prefix (#8)
            p.sendMessage(Component.text(srr.getMessage(), NamedTextColor.GREEN));
        } else if (result instanceof GafferResponse gr) {
            // GafferResponse: mirror dialog's "Success: …" / "Failure: …" wording
            String msg = gr.getMessage()
                    .replaceAll("%job%", jobName)
                    .replaceAll("%name%", nameArg != null ? nameArg : "");
            if (gr.isSuccessful()) {
                p.sendMessage(Component.text("Success: " + msg, NamedTextColor.GREEN));
            } else {
                p.sendMessage(Component.text("Failure: " + msg, NamedTextColor.RED));
            }
        } else {
            // void / Boolean / other — generic confirmation (e.g. setwarp, bringall)
            p.sendMessage(Component.text("Job Edited!", NamedTextColor.AQUA));
        }
    }
}
