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
import com.mcmiddleearth.thegaffer.storage.Project;
import com.mcmiddleearth.thegaffer.storage.ProjectDatabase;
import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.storage.JobDatabase;
import com.mcmiddleearth.thegaffer.storage.JobWarp;
import com.mcmiddleearth.thegaffer.utilities.PermissionsUtil;
import com.mcmiddleearth.thegaffer.utilities.PromptStyle;
import java.io.File;
import java.util.ArrayList;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
            abandonedEvent.getContext().getForWhom().sendRawMessage(
                    PromptStyle.TAG + PromptStyle.HINT + "Create job exited. No job was created.");
        } else {
            abandonedEvent.getContext().getForWhom().sendRawMessage(
                    PromptStyle.TAG + PromptStyle.HINT + "Create job timed out. No job was created.");
        }
    }

    public class jobCreatePrefix implements ConversationPrefix {

        @Override
        public String getPrefix(ConversationContext context) {
            return PromptStyle.TAG;
        }

    }

    private class namePrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return PromptStyle.ask("What would you like the name of the job to be?")
                    + PromptStyle.hint("Spaces will be replaced with underscores · type !cancel to exit");
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            String original = input;
            input = input.replaceAll(" ", "_");
            File newJob = new File(TheGaffer.getPluginDataFolder() + TheGaffer.getFileSeperator() + "jobs"
                    + TheGaffer.getFileSeperator() + input + TheGaffer.getFileExtension());
            if (newJob.exists()) {
                return new jobAlreadyExistsPrompt();
            }
            if (!JobDatabase.getActiveJobs().containsKey(input)) {
                context.setSessionData("jobname", input);
                if (!original.equals(input)) {
                    context.getForWhom().sendRawMessage(
                            PromptStyle.OK + "Name set to " + PromptStyle.VALUE + input
                            + PromptStyle.OK + " (spaces were replaced with underscores).");
                }
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
            return PromptStyle.ask("Enter a short job description.")
                    + PromptStyle.hint("Shown in the job-start message and Discord · keep it brief");
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
            return PromptStyle.ERROR + "A job by that name has been run in the past — please pick a different name.";
        }

    }

    private class jobAlreadyRunningPrompt extends MessagePrompt {

        @Override
        protected Prompt getNextPrompt(ConversationContext context) {
            return new namePrompt();
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return PromptStyle.ERROR + "A job by that name is already running — pick another name.";
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
            return PromptStyle.ask("Should this job be private?") + PromptStyle.opts("true / false")
                    + PromptStyle.hint("private = invite-only · public (false) = anyone with permission can join");
        }

    }

    private class howBigPrompt extends NumericPrompt {

        @Override
        public boolean isNumberValid(ConversationContext context, Number input) {
            return input.intValue() >= 1;
        }

        @Override
        public String getFailedValidationText(ConversationContext context, String invalidInput) {
            return PromptStyle.ERROR + "Radius must be at least 1. Please enter a number between 1 and 1000.";
        }

        @Override
        public Prompt acceptValidatedInput(ConversationContext context, Number input) {
            context.setSessionData("jobradius", input);
            return newDiscordOrFinishPrompt();
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return PromptStyle.ask("How large should the build area be?") + PromptStyle.opts("radius 1 – 1000")
                    + PromptStyle.hint("e.g. " + PromptStyle.VALUE + "50" + PromptStyle.HINT + " makes a 100×100 area centred on you · over 1000 is capped");
        }

    }

    private class projectNotExistsPrompt extends MessagePrompt {

        @Override
        protected Prompt getNextPrompt(ConversationContext context) {
            return new projectPrompt();
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return PromptStyle.ERROR + "No active project by that name — try again, or type 'nothing'.";
        }

    }

    public Prompt newDiscordOrFinishPrompt() {
        if (TheGaffer.isDiscordEnabled()) {
            return new discordAnnouncePrompt();
        }
        if (ProjectDatabase.hasActiveProjects()) {
            return new projectPrompt();
        }
        if (TheGaffer.isGlowing()) {
            return new GlowEffectPrompt();
        }
        return new finishedPrompt();
    }

    private class discordAnnouncePrompt extends BooleanPrompt {

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, boolean input) {
            context.setSessionData("discordSend", input);
            if (ProjectDatabase.hasActiveProjects()) {
                return new projectPrompt();
            }
            if (TheGaffer.isGlowing()) {
                return new GlowEffectPrompt();
            }
            return new finishedPrompt();
        }

        @Override
        public String getPromptText(ConversationContext context) {
            java.util.List<String> roles = TheGaffer.getAllowedPingRoles();
            String roleInfo;
            if (roles != null && !roles.isEmpty()) {
                roleInfo = "will ping: " + PromptStyle.VALUE + String.join(", ", roles) + PromptStyle.HINT;
            } else {
                roleInfo = "(no roles are configured to ping)";
            }
            return PromptStyle.ask("Should this job be announced on Discord?") + PromptStyle.opts("true / false")
                    + PromptStyle.hint("The configured notification roles " + roleInfo);
        }

    }

    private class projectPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            StringBuilder names = new StringBuilder();
            for (Project p : ProjectDatabase.byStatus(Project.Status.ACTIVE)) {
                if (names.length() > 0) { names.append(", "); }
                names.append(PromptStyle.VALUE + p.getName() + PromptStyle.HINT);
            }
            return PromptStyle.ask("Link this job to a project, or type 'nothing'.")
                    + PromptStyle.hint("Active projects: " + names);
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            if (input.equalsIgnoreCase("nothing")) {
                context.setSessionData("project", "nothing");
                if (TheGaffer.isGlowing()) { return new GlowEffectPrompt(); }
                return new finishedPrompt();
            }
            Project p = ProjectDatabase.get(input);
            if (p != null && p.getStatus() == Project.Status.ACTIVE) {
                context.setSessionData("project", p.getName()); // store the canonical display name
                if (TheGaffer.isGlowing()) { return new GlowEffectPrompt(); }
                return new finishedPrompt();
            }
            return new projectNotExistsPrompt();
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
            return PromptStyle.ask("Should workers and helpers in this job glow?") + PromptStyle.opts("true / false")
                    + PromptStyle.hint("a coloured outline visible through walls");
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
            UUID owner = ((Player) context.getForWhom()).getUniqueId();
            JobWarp warp = new JobWarp(((Player) context.getForWhom()).getLocation());
            boolean Private = (boolean) context.getSessionData("private");
            boolean discordSend = (context.getSessionData("discordSend") != null && (boolean) context.getSessionData("discordSend"));
            String description = (String) context.getSessionData("description");
            int radius = ((Number) context.getSessionData("jobradius")).intValue();
            boolean glowing = false;
            Object temp = context.getSessionData("glowEffect");
            String project = (String) context.getSessionData("project");
            if (project == null) { project = "nothing"; }
            if (temp != null) {
                glowing = (boolean) temp;
            }
            Job jerb = new Job(jobname, description, owner, true, warp, warp.getWorld(), Private, radius,
                    discordSend, project);
            if (glowing) {
                jerb.setGlowing();
            }
            JobDatabase.activateJob(jerb);
            return PromptStyle.OK + "Successfully created the " + PromptStyle.VALUE + jobname + PromptStyle.OK + " job!";
        }

    }
}
