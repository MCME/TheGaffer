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
import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.storage.JobDatabase;
import com.mcmiddleearth.thegaffer.storage.JobKit;
import com.mcmiddleearth.thegaffer.storage.JobWarp;
import com.mcmiddleearth.thegaffer.TeamSpeak.TSfetcher;
import com.mcmiddleearth.thegaffer.utilities.PermissionsUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
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
            conversationFactory.buildConversation((Conversable) sender).begin();
            return true;
        } else {
            return false;
        }
    }
    
    public boolean Start(CommandSender sender, Command command, String label, String[] args){
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
                return new privatePrompt();
            } else {
                return new jobAlreadyRunningPrompt();
            }
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

    private class kitPrompt extends BooleanPrompt {

        
        
        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, boolean input) {
            context.setSessionData("setkit", input);
            return new tsPrompt();
        }

        @Override
        public String getPromptText(ConversationContext context) {
            
            return "Set the kit of the job now? (true or false)";
        }

    }

    private class howBigPrompt extends NumericPrompt {

        @Override
        public Prompt acceptValidatedInput(ConversationContext context, Number input) {
            context.setSessionData("jobradius", input);
            return new kitPrompt();
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return "How big should the job area be? (radius 0 - 1000)";
        }
    }

    private class tsPrompt extends StringPrompt {
        
        public ArrayList<String> Lobbies = new ArrayList<String>();

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            if(TheGaffer.isTSenabled()){
                if(Lobbies.contains(input) || input.equalsIgnoreCase("0")){
                    context.setSessionData("setTs", input);
                    return new finishedPrompt();
                }
                return new TSfailPrompt();
            }else{
                context.setSessionData("setTs", input);
            }
            return new finishedPrompt();
        }

        @Override
        public String getPromptText(ConversationContext context) {
            if(TheGaffer.isTSenabled()){
                try {
                    String dbPath = System.getProperty("user.dir") + "/plugins/TheGaffer/LobbyDB";
                    Scanner s;
                    s = new Scanner(new File(dbPath + "/lobbies.txt"));
                    while (s.hasNext()){
                        Lobbies.add(s.nextLine());
                    }
                    s.close();
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(TSfetcher.class.getName()).log(Level.SEVERE, null, ex);
                }
                String returner = "What is the name of the TeamSpeak channel? (0 for none) \n Current lobbies: " + ChatColor.AQUA + "\n";
                for(String channel : Lobbies){
                    returner += channel + ", ";
                }
                return returner;
            }
            return "What is the name of the TeamSpeak channel? (0 for none)";
        }

    }
    
    private class TSfailPrompt extends MessagePrompt{
        
        @Override
        protected Prompt getNextPrompt(ConversationContext context) {
            return new tsPrompt();
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return "That TeamSpeak channel doesn't exist!";
        }
        
    }

    private class finishedPrompt extends MessagePrompt {

        @Override
        protected Prompt getNextPrompt(ConversationContext context) {
            return Prompt.END_OF_CONVERSATION;
        }

        @Override
        public String getPromptText(ConversationContext context) {
            String ts= (String) context.getSessionData("setTs");
            String jobname = (String) context.getSessionData("jobname");
            String owner = ((Player) context.getForWhom()).getName();
            JobWarp warp = new JobWarp(((Player) context.getForWhom()).getLocation());
            JobWarp tsWarp = new JobWarp(((Player) context.getForWhom()).getLocation());
            boolean Private = (boolean) context.getSessionData("private");
            boolean setKit = (boolean) context.getSessionData("setkit");
            int radius = ((Number) context.getSessionData("jobradius")).intValue();
            Job jerb = new Job(jobname, owner, true, warp, warp.getWorld(), Private, radius, ts, tsWarp);
            if (setKit) {
                JobKit kit = new JobKit(((Player) context.getForWhom()).getInventory());
                jerb.setKit(kit);
            }
            JobDatabase.activateJob(jerb);
            return "Successfully created the " + jobname + " job!";
        }

    }
}
