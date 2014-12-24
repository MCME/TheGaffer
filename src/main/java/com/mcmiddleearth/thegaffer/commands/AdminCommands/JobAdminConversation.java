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
package com.mcmiddleearth.thegaffer.commands.AdminCommands;

import com.mcmiddleearth.thegaffer.GafferResponses.GafferResponse;
import com.mcmiddleearth.thegaffer.GafferResponses.GenericResponse;
import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.storage.JobDatabase;
import com.mcmiddleearth.thegaffer.storage.JobItem;
import com.mcmiddleearth.thegaffer.storage.JobKit;
import com.mcmiddleearth.thegaffer.storage.JobWarp;
import com.mcmiddleearth.thegaffer.utilities.PermissionsUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.Conversable;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.ConversationPrefix;
import org.bukkit.conversations.FixedSetPrompt;
import org.bukkit.conversations.MessagePrompt;
import org.bukkit.conversations.NumericPrompt;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class JobAdminConversation implements CommandExecutor, ConversationAbandonedListener {

    private final ConversationFactory conversationFactory;

    private final List<String> actions = new ArrayList();

    public JobAdminConversation() {
        conversationFactory = new ConversationFactory(TheGaffer.getPluginInstance())
                .withModality(true)
                .withEscapeSequence("!cancel")
                .withPrefix(new jobAdminPrefix())
                .withFirstPrompt(new whichJobPrompt())
                .withTimeout(60)
                .thatExcludesNonPlayersWithMessage("You must be a player to send this command");
        actions.add("addhelper");
        actions.add("removehelper");
        actions.add("kickworker");
        actions.add("banworker");
        actions.add("unbanworker");
        actions.add("setwarp");
        actions.add("bringall");
        actions.add("listworkers");
        actions.add("inviteworker");
        actions.add("uninviteworker");
        actions.add("setradius");
        actions.add("setkit");
        actions.add("clearworkerinven");
        actions.add("setTeamSpeakwarp");
        actions.add("setTeamSpeakchannel");
        Collections.sort(actions);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Conversable && sender.hasPermission(PermissionsUtil.getCreatePermission())) {
            conversationFactory.buildConversation((Conversable) sender).begin();
            return true;
        } else {
            return false;
        }
    }
    
    public boolean Start(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Conversable && sender.hasPermission(PermissionsUtil.getCreatePermission())) {
            conversationFactory.buildConversation((Conversable) sender).begin();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void conversationAbandoned(ConversationAbandonedEvent abandonedEvent) {
        if (abandonedEvent.gracefulExit()) {
            abandonedEvent.getContext().getForWhom().sendRawMessage(ChatColor.AQUA + "Jobadmin exited.");
        } else {
            abandonedEvent.getContext().getForWhom().sendRawMessage(ChatColor.AQUA + "Jobadmin timed out");
        }
    }

    public class jobAdminPrefix implements ConversationPrefix {

        @Override
        public String getPrefix(ConversationContext context) {
            String prefix = ChatColor.GRAY + "";
            String jobname = (String) context.getSessionData("jobname");
            if (jobname != null) {
                prefix += "editing " + ChatColor.GOLD + jobname + ChatColor.AQUA + "\n";
            }
            return prefix;
        }

    }

    private class responsePrompt extends MessagePrompt {

        GafferResponse response;
        Prompt last;

        public responsePrompt(GafferResponse resp, Prompt lastPrompt) {
            response = resp;
            last = lastPrompt;
        }

        @Override
        public Prompt getNextPrompt(ConversationContext context) {
            if (response.isSuccessful()) {
                return Prompt.END_OF_CONVERSATION;
            } else {
                return last;
            }
        }

        @Override
        public String getPromptText(ConversationContext context) {
            if (response.isSuccessful()) {
                return ChatColor.GREEN + "Success: " + response.getMessage().replaceAll("%name%", (String) context.getSessionData("inputname")).replaceAll("%job%", (String) context.getSessionData("jobname"));
            } else {
                return ChatColor.RED + "Failure: "
                        + response.getMessage()
                        .replaceAll("%name%", (String) context.getSessionData("inputname"))
                        .replaceAll("%job%", (String) context.getSessionData("jobname"))
                        + "\n" + " Please try again or cancel with !cancel";
            }
        }
    }

    private class whichJobPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            if (context.getSessionData("jobname") == null) {
                return "What job would you like to modify? \n" + formatSet() + "\n" + "or exit with !cancel";
            } else {
                return "That job is not running, please try again.";
            }
        }

        private String formatSet() {
            return StringUtils.join(JobDatabase.getActiveJobs().keySet(), ", ");
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            context.setSessionData("jobname", input);
            if (JobDatabase.getActiveJobs().containsKey(input)) {
                context.setSessionData("job", JobDatabase.getActiveJobs().get(input));
                return new whichActionPrompt();
            } else {
                return new whichJobPrompt();
            }
        }
    }

    private class whichActionPrompt extends FixedSetPrompt {

        public whichActionPrompt() {
            super(actions.toArray(new String[actions.size()]));
        }

        @Override
        public String getPromptText(ConversationContext context) {
            if (context.getSessionData("action") != null) {
                return "Please enter a valid action \n" + formatFixedSet();
            } else {
                return "What action would you like to perform? \n" + formatFixedSet();
            }
        }

        @Override
        public Prompt acceptValidatedInput(ConversationContext context, String input) {
            context.setSessionData("action", input);
            context.setSessionData("am", new AdminMethods((Job) context.getSessionData("job"), (Player) context.getForWhom()));
            switch (input) {
                case "addhelper": {
                    return new addHelperPrompt();
                }
                case "removehelper": {
                    return new removeHelperPrompt();
                }
                case "kickworker": {
                    return new kickWorkerPrompt();
                }
                case "banworker": {
                    return new banWorkerPrompt();
                }
                case "unbanworker": {
                    return new unbanWorkerPrompt();
                }
                case "setwarp": {
                    return new updateWarpPrompt();
                }
                case "bringall": {
                    return new bringallWorkersPrompt();
                }
                case "listworkers": {
                    return new listWorkersPrompt();
                }
                case "inviteworker": {
                    return new inviteWorkerPrompt();
                }
                case "uninviteworker": {
                    return new uninviteWorkerPrompt();
                }
                case "setradius": {
                    return new setRadiusPrompt();
                }
                case "setkit": {
                    return new setKitPrompt();
                }
                case "clearworkerinven": {
                    return new clearinven();
                }
                case "setTeamSpeakwarp": {
                    return new setTeamSpeakwarp();
                }
                case "setTeamSpeakchannel": {
                    return new setTSchannel();
                }
                default: {
                    return new whichActionPrompt();
                }
            }
        }
    }

    private class addHelperPrompt extends StringPrompt {

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            context.setSessionData("inputname", input);
            AdminMethods am = (AdminMethods) context.getSessionData("am");
            return new responsePrompt(am.addhelper(input), this);
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return "Who would you like to add as a helper?";
        }
    }

    private class removeHelperPrompt extends StringPrompt {

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            context.setSessionData("inputname", input);
            AdminMethods am = (AdminMethods) context.getSessionData("am");
            return new responsePrompt(am.removehelper(input), this);
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return "Who would you like to remove as a helper?";
        }
    }

    private class kickWorkerPrompt extends StringPrompt {

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            context.setSessionData("inputname", input);
            AdminMethods am = (AdminMethods) context.getSessionData("am");
            return new responsePrompt(am.kickworker(input), this);
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return "Who would you like to kick from the job?";
        }
    }

    private class banWorkerPrompt extends StringPrompt {

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            context.setSessionData("inputname", input);
            AdminMethods am = (AdminMethods) context.getSessionData("am");
            return new responsePrompt(am.banworker(input), this);
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return "Who would you like to ban from the job?";
        }
    }

    private class unbanWorkerPrompt extends StringPrompt {

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            context.setSessionData("inputname", input);
            AdminMethods am = (AdminMethods) context.getSessionData("am");
            return new responsePrompt(am.unbanworker(input), this);
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return "Who would you like to unban from the job?";
        }
    }

    private class updateWarpPrompt extends MessagePrompt {

        @Override
        public Prompt getNextPrompt(ConversationContext context) {
            AdminMethods am = (AdminMethods) context.getSessionData("am");
            am.setwarp();
            return Prompt.END_OF_CONVERSATION;
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return "Successfully moved the warp to your location.";
        }
    }

    private class bringallWorkersPrompt extends MessagePrompt {

        @Override
        public Prompt getNextPrompt(ConversationContext context) {
             
            AdminMethods am = (AdminMethods) context.getSessionData("am");
            am.bringall();
            return Prompt.END_OF_CONVERSATION;
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return "Brought all online workers to your location.";
        }
    }

    private class listWorkersPrompt extends MessagePrompt {

        @Override
        public Prompt getNextPrompt(ConversationContext context) {
            return Prompt.END_OF_CONVERSATION;
        }

        @Override
        public String getPromptText(ConversationContext context) {
            AdminMethods am = (AdminMethods) context.getSessionData("am");
            return am.listworkers();
        }
    }

    private class inviteWorkerPrompt extends StringPrompt {

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            context.setSessionData("inputname", input);
            AdminMethods am = (AdminMethods) context.getSessionData("am");
            return new responsePrompt(am.inviteworker(input), this);
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return "Who would you like to invite to the job? (You may add multiple names with player1, player2)";
        }
    }

    private class uninviteWorkerPrompt extends StringPrompt {

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            context.setSessionData("inputname", input);
            AdminMethods am = (AdminMethods) context.getSessionData("am");
            return new responsePrompt(am.uninviteworker(input), this);
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return "Who would you like to uninvite from the job?";
        }
    }

    private class setRadiusPrompt extends NumericPrompt {

        @Override
        public Prompt acceptValidatedInput(ConversationContext context, Number input) {
            context.setSessionData("jobradius", input);
            AdminMethods am = (AdminMethods) context.getSessionData("am");
            am.setradius(String.valueOf(input));
            return new responsePrompt(GenericResponse.SUCCESS, this);
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return "Should big should the job area be? (radius 0 - 1000)";
        }
    }

    private class setKitPrompt extends MessagePrompt {

        @Override
        public Prompt getNextPrompt(ConversationContext context) {
            AdminMethods am = (AdminMethods) context.getSessionData("am");
            am.setkit();
            return Prompt.END_OF_CONVERSATION;
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return "Successfully set the kit of the job to your inventory.";
        }
    }
    private class clearinven extends MessagePrompt {

        @Override
        public Prompt getNextPrompt(ConversationContext context) {
            AdminMethods am = (AdminMethods) context.getSessionData("am");
            am.clearworkerinvens();
            return Prompt.END_OF_CONVERSATION;
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return "Cleared workers' inventories";
        }
    }
    private class setTeamSpeakwarp extends MessagePrompt {

        @Override
        public Prompt getNextPrompt(ConversationContext context) {
            AdminMethods am = (AdminMethods) context.getSessionData("am");
            am.setteamspeakwarp();
            return Prompt.END_OF_CONVERSATION;
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return "Set the TeamSpeak warp:";
        }
    }
    private class setTSchannel extends StringPrompt {
        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            AdminMethods am = (AdminMethods) context.getSessionData("am");
            return new responsePrompt(GenericResponse.SUCCESS, this);
        }

        @Override
        public String getPromptText(ConversationContext context) {
            if(TheGaffer.isTSenabled()){
                return "Enter the name of the Channel, 0 for none";
            }else{
                return "Unavailable";
            }
        }
    }

}
