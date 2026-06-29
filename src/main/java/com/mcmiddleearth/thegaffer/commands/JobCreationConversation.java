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
package com.mcmiddleearth.thegaffer.commands;

import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.ext.ExternalProjectHandler;
import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.storage.JobDatabase;
import com.mcmiddleearth.thegaffer.storage.JobKit;
import com.mcmiddleearth.thegaffer.storage.JobWarp;
import com.mcmiddleearth.thegaffer.utilities.PermissionsUtil;
import java.io.File;
import java.util.ArrayList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.BooleanPrompt;
import org.bukkit.conversations.Conversable;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.ConversationPrefix;
import org.bukkit.conversations.MessagePrompt;
import org.bukkit.conversations.NumericPrompt;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;

public class JobCreationConversation implements CommandExecutor, ConversationAbandonedListener {

    private final ConversationFactory conversationFactory;

    public JobCreationConversation() {
        conversationFactory = new ConversationFactory(TheGaffer.getPluginInstance())
                .withModality(true)
                .withEscapeSequence("!cancel")
                .withPrefix(new jobCreatePrefix())
                .withFirstPrompt(new namePrompt())
                .withTimeout(600)
                .thatExcludesNonPlayersWithMessage("You must be a player to send this command")
                .addConversationAbandonedListener(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Conversable && sender.hasPermission(PermissionsUtil.getCreatePermission())) {
            if (sender instanceof Player) {
                Job current = JobDatabase.getJobWorking((Player) sender);
                if (current != null) {
                    sender.sendMessage(Component.text("You are already in the job ", NamedTextColor.RED)
                            .append(Component.text(current.getName(), NamedTextColor.AQUA))
                            .append(Component.text(" - leave or stop it before creating a new one.", NamedTextColor.RED)));
                    return true;
                }
            }
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
            abandonedEvent.getContext().getForWhom().sendRawMessage(ChatColor.AQUA + "Create job exited.");
        } else {
            abandonedEvent.getContext().getForWhom().sendRawMessage(ChatColor.AQUA + "Create job timed out");
        }
    }

    public class jobCreatePrefix implements ConversationPrefix {

        @Override
        public String getPrefix(ConversationContext context) {
            String prefix = ChatColor.GRAY + "";
            String jobname = (String) context.getSessionData("jobname");
            if (jobname != null) {
                prefix += "creating " + ChatColor.GOLD + jobname + ChatColor.AQUA + "\n";
            }
            return prefix;
        }

    }

    private class namePrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return "What would you like the name of the job to be?"
                    + "\n" + "or exit with !cancel";
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            input = input.replaceAll(" ", "_");
            File newJob = new File(TheGaffer.getPluginDataFolder() + TheGaffer.getFileSeperator() + "jobs"
                    + TheGaffer.getFileSeperator() + input + TheGaffer.getFileExtension());
            if (newJob.exists()) {
                return new jobAlreadyExistsPrompt();
            }
            if (!JobDatabase.getActiveJobs().containsKey(input)) {
                context.setSessionData("jobname", input);
                if (TheGaffer.isJobDescription()) {
                    return new descriptionPrompt();
                } else {
                    context.setSessionData("description", "");
                    return new privatePrompt();
                }
            } else {
                return new jobAlreadyRunningPrompt();
            }
        }

    }

    private class descriptionPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return "Please give a short job description.";
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            context.setSessionData("description", input);
            return new privatePrompt();
        }

    }

    private class jobAlreadyExistsPrompt extends MessagePrompt {

        @Override
        protected Prompt getNextPrompt(ConversationContext context) {
            return new namePrompt();
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return "A job by that name has been run in the past, please pick a different name.";
        }

    }

    private class jobAlreadyRunningPrompt extends MessagePrompt {

        @Override
        protected Prompt getNextPrompt(ConversationContext context) {
            return new namePrompt();
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return "A job by that name is already running, pick another name.";
        }

    }

    private class privatePrompt extends BooleanPrompt {

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, boolean input) {
            context.setSessionData("private", input);
            return new howBigPrompt();
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return "Should this job be private? (true or false)";
        }

    }

    private class howBigPrompt extends NumericPrompt {

        @Override
        public Prompt acceptValidatedInput(ConversationContext context, Number input) {
            context.setSessionData("jobradius", input);
            if (TheGaffer.isJobKitsEnabled()) {
                return new kitPrompt();
            } else {
                context.setSessionData("setkit", false);
                return newDiscordOrFinishPrompt();
            }
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return "How big should the job area be? (radius 0 - 1000)";
        }

    }

    private class projectNotExistsPrompt extends MessagePrompt {

        @Override
        protected Prompt getNextPrompt(ConversationContext context) {
            return new projectPrompt();
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return "A project with that name doesn't exists";
        }

    }

    public Prompt newDiscordOrFinishPrompt() {
        if (TheGaffer.isDiscordEnabled()) {
            return new discordAnnouncePrompt();
        }
        if (TheGaffer.isProjectsEnabled() == true) {
            return new projectPrompt();
        } 
        if (TheGaffer.isGlowing()) {
            return new GlowEffectPrompt();
        }
        return new finishedPrompt();
    }

    private class kitPrompt extends BooleanPrompt {

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, boolean input) {
            context.setSessionData("setkit", input);
            return newDiscordOrFinishPrompt();//new discordAnnouncePrompt();
        }

        @Override
        public String getPromptText(ConversationContext context) {

            return "Set the kit of the job now? (true or false)";
        }

    }

    private class discordAnnouncePrompt extends BooleanPrompt {

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, boolean input) {
            context.setSessionData("discordSend", input);
            if (input) {
                return new discordTagPrompt();
            }
            if (TheGaffer.isProjectsEnabled() == true) {
                return new projectPrompt();
            } 
            if (TheGaffer.isGlowing()) {
                return new GlowEffectPrompt();
            }
            return new finishedPrompt();
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return "Should this job be announced on Discord? (true or false)";
        }

    }

    private class discordTagPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return "who should be notified at discord about the job?"
                    + "\n" + "May be player names separated by \",\" or roles like \"Commoner\" or \"everyone\""
                    + "\n" + "Example: Eriol_Eandur, Commoner";
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            input = input.replace(" ", "");
            context.setSessionData("discordTag", input);
            if (TheGaffer.isProjectsEnabled() == true) {
                return new projectPrompt();
            } 
            if (TheGaffer.isGlowing()) {
                return new GlowEffectPrompt();
            }
            return new finishedPrompt();
        }

    }

    private class projectPrompt extends StringPrompt {

        ExternalProjectHandler mcproject =
                (Bukkit.getPluginManager().getPlugin("McMeProject") instanceof ExternalProjectHandler h) ? h : null;

        @Override
        public String getPromptText(ConversationContext context) {
            return "Please give the name of the project linked to this job(if not type 'nothing')";

        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {

            if (mcproject != null && mcproject.getProjectNames().contains(input)) {

                context.setSessionData("project", input);
                String jobname = (String) context.getSessionData("jobname");

                return new GlowEffectPrompt();
            } else if (mcproject == null || input.equalsIgnoreCase("nothing")) {

                context.setSessionData("project", "nothing");

                return new GlowEffectPrompt();
            } else {
                return new projectNotExistsPrompt();
            }

        }

    }

    private class GlowEffectPrompt extends BooleanPrompt {

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, boolean input) {
            context.setSessionData("glowEffect", input);
            return new finishedPrompt();
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return "Should people in this job get a glow effect? (true or false)";
        }

    }

    private class finishedPrompt extends MessagePrompt {

        @Override
        protected Prompt getNextPrompt(ConversationContext context) {
            return Prompt.END_OF_CONVERSATION;
        }

        @Override
        public String getPromptText(ConversationContext context) {
            String jobname = (String) context.getSessionData("jobname");
            String owner = ((Player) context.getForWhom()).getName();
            JobWarp warp = new JobWarp(((Player) context.getForWhom()).getLocation());
            boolean Private = (boolean) context.getSessionData("private");
            boolean setKit = (boolean) context.getSessionData("setkit");
            boolean discordSend = (context.getSessionData("discordSend") != null && (boolean) context.getSessionData("discordSend"));
            String[] discordTags = (context.getSessionData("discordTag") != null ? ((String) context.getSessionData("discordTag")).split(",") : new String[0]);
            String description = (String) context.getSessionData("description");
            int radius = ((Number) context.getSessionData("jobradius")).intValue();
            boolean glowing = false;
            Object temp = context.getSessionData("glowEffect");
            String project = (String) context.getSessionData("project");
            if (temp != null) {
                glowing = (boolean) temp;
            }
            Job jerb = new Job(jobname, description, owner, true, warp, warp.getWorld(), Private, radius,
                    discordSend, discordTags, project);
            if (glowing) {
                jerb.setGlowing();
            }
            if (setKit) {
                JobKit kit = new JobKit(((Player) context.getForWhom()).getInventory());
                jerb.setKit(kit);
            }
            JobDatabase.activateJob(jerb);
            return "Successfully created the " + jobname + " job!";
        }

    }
}
