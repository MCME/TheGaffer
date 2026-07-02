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
import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.storage.JobDatabase;
import com.mcmiddleearth.thegaffer.utilities.PermissionsUtil;
import com.mcmiddleearth.thegaffer.utilities.PromptStyle;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JobAdminConversation implements CommandExecutor, ConversationAbandonedListener {

    private final ConversationFactory conversationFactory;

    private final List<String> actions = new ArrayList<>();

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
        actions.add("teleportall");
        actions.add("teleport");
        actions.add("listworkers");
        actions.add("inviteworker");
        actions.add("uninviteworker");
        actions.add("setradius");
        actions.add("clearworkerinven");
        Collections.sort(actions);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // If the player supplied args (/jobadmin <job> <action> …), forward to the shared
        // one-liner executor so both entry points behave identically — including the
        // confirm-gate and setradius validation.
        // The one-liner expects the admin-prefixed layout: args[0]="admin", args[1]=<job>, …
        // so we prepend "admin" to the conversation's own args before delegating.
        if (args.length > 0) {
            String[] adminArgs = new String[args.length + 1];
            adminArgs[0] = "admin";
            System.arraycopy(args, 0, adminArgs, 1, args.length);
            return JobAdminCommands.executeOneLiner(sender, adminArgs);
        }
        // No args — open the guided conversation as before.
        if (sender instanceof Conversable && sender.hasPermission(PermissionsUtil.getCreatePermission())) {
            conversationFactory.buildConversation((Conversable) sender).begin();
            return true;
        } else {
            sender.sendMessage(ChatColor.RED + "You don't have permission to manage jobs.");
            return true;
        }
    }

    public boolean Start(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Conversable && sender.hasPermission(PermissionsUtil.getCreatePermission())) {
            conversationFactory.buildConversation((Conversable) sender).begin();
            return true;
        } else {
            sender.sendMessage(ChatColor.RED + "You don't have permission to manage jobs.");
            return true;
        }
    }

    @Override
    public void conversationAbandoned(ConversationAbandonedEvent abandonedEvent) {
        if (abandonedEvent.gracefulExit()) {
            abandonedEvent.getContext().getForWhom().sendRawMessage(
                    PromptStyle.TAG + PromptStyle.HINT + "Jobadmin exited.");
        } else {
            abandonedEvent.getContext().getForWhom().sendRawMessage(
                    PromptStyle.TAG + PromptStyle.HINT + "Jobadmin timed out.");
        }
    }

    public class jobAdminPrefix implements ConversationPrefix {

        @Override
        public String getPrefix(ConversationContext context) {
            return PromptStyle.TAG;
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
                return PromptStyle.OK + "Success: " + response.getMessage()
                        .replaceAll("%name%", (String) context.getSessionData("inputname"))
                        .replaceAll("%job%", (String) context.getSessionData("jobname"));
            } else {
                return PromptStyle.ERROR + "Failure: "
                        + response.getMessage()
                        .replaceAll("%name%", (String) context.getSessionData("inputname"))
                        .replaceAll("%job%", (String) context.getSessionData("jobname"))
                        + PromptStyle.hint("Please try again or cancel with !cancel");
            }
        }
    }

    private class whichJobPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            if (context.getSessionData("jobname") == null) {
                return PromptStyle.ask("What job would you like to modify?")
                        + PromptStyle.hint(formatSet())
                        + PromptStyle.hint("type !cancel to exit");
            } else {
                return PromptStyle.ERROR + "That job is not running — please try again."
                        + PromptStyle.hint(formatSet())
                        + PromptStyle.hint("type !cancel to exit");
            }
        }

        private String formatSet() {
            StringBuilder sb = new StringBuilder("Running jobs: ");
            for (String name : JobDatabase.getActiveJobs().keySet()) {
                if (sb.length() > "Running jobs: ".length()) { sb.append(", "); }
                sb.append(PromptStyle.VALUE).append(name).append(PromptStyle.HINT);
            }
            return sb.toString();
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
                return PromptStyle.ERROR + "Please enter a valid action."
                        + PromptStyle.hint(formatFixedSet());
            } else {
                return PromptStyle.ask("What action would you like to perform?")
                        + PromptStyle.hint(formatFixedSet());
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
                case "teleportall": {
                    return new teleportallWorkersPrompt();
                }
                case "teleport": {
                    return new teleportWorkerPrompt();
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
                case "clearworkerinven": {
                    return new clearinven();
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
            return PromptStyle.ask("Who would you like to add as a helper?")
                    + PromptStyle.hint("you may list multiple as player1, player2");
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
            return PromptStyle.ask("Who would you like to remove as a helper?")
                    + PromptStyle.hint("you may list multiple as player1, player2");
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
            return PromptStyle.ask("Who would you like to kick from the job?")
                    + PromptStyle.hint("you may list multiple as player1, player2");
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
            return PromptStyle.ask("Who would you like to ban from the job?")
                    + PromptStyle.hint("you may list multiple as player1, player2");
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
            return PromptStyle.ask("Who would you like to unban from the job?")
                    + PromptStyle.hint("you may list multiple as player1, player2");
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
            return PromptStyle.OK + "Successfully moved the warp to your location.";
        }
    }

    private class teleportallWorkersPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return PromptStyle.ask("This will teleport all online workers to you.") + PromptStyle.opts("confirm / cancel")
                    + PromptStyle.hint("type 'confirm' to proceed, or anything else to cancel");
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            if (input.equalsIgnoreCase("confirm")) {
                AdminMethods am = (AdminMethods) context.getSessionData("am");
                am.teleportall();
                context.getForWhom().sendRawMessage(PromptStyle.OK + "Teleported all online workers to your location.");
            } else {
                context.getForWhom().sendRawMessage(PromptStyle.HINT + "Cancelled.");
            }
            return Prompt.END_OF_CONVERSATION;
        }
    }

    private class teleportWorkerPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return PromptStyle.ask("Which worker would you like to teleport to you?")
                    + PromptStyle.hint("enter the player's name");
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            context.setSessionData("inputname", input);
            AdminMethods am = (AdminMethods) context.getSessionData("am");
            GafferResponse result = am.teleport(input);
            return new responsePrompt(result, this);
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
            return PromptStyle.ask("Who would you like to invite to the job?")
                    + PromptStyle.hint("you may add multiple names as player1, player2");
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
            return PromptStyle.ask("Who would you like to uninvite from the job?")
                    + PromptStyle.hint("you may list multiple as player1, player2");
        }
    }

    private class setRadiusPrompt extends NumericPrompt {

        @Override
        public boolean isNumberValid(ConversationContext context, Number input) {
            int v = input.intValue();
            return v >= 1 && v <= 1000;
        }

        @Override
        public String getFailedValidationText(ConversationContext context, String invalidInput) {
            return PromptStyle.ERROR + "Radius must be a whole number between 1 and 1000.";
        }

        @Override
        public Prompt acceptValidatedInput(ConversationContext context, Number input) {
            int radius = input.intValue();
            context.setSessionData("jobradius", input);
            AdminMethods am = (AdminMethods) context.getSessionData("am");
            // Use the validated AdminMethods path so the single shared validator runs.
            GafferResponse result = am.setradius(String.valueOf(radius));
            return new responsePrompt(result, this);
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return PromptStyle.ask("How big should the job area be?") + PromptStyle.opts("radius 1 – 1000");
        }
    }

    private class clearinven extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return PromptStyle.ask("This will clear all online workers' inventories.") + PromptStyle.opts("confirm / cancel")
                    + PromptStyle.hint("type 'confirm' to proceed, or anything else to cancel");
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            if (input.equalsIgnoreCase("confirm")) {
                AdminMethods am = (AdminMethods) context.getSessionData("am");
                am.clearworkerinven();
                context.getForWhom().sendRawMessage(PromptStyle.OK + "Cleared workers' inventories.");
            } else {
                context.getForWhom().sendRawMessage(PromptStyle.HINT + "Cancelled.");
            }
            return Prompt.END_OF_CONVERSATION;
        }
    }
}
