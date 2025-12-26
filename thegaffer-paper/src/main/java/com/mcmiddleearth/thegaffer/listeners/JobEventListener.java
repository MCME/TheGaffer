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
package com.mcmiddleearth.thegaffer.listeners;

import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.events.*;
import com.mcmiddleearth.thegaffer.messages.JobCreateMessage;
import com.mcmiddleearth.thegaffer.messages.JobDeleteMessage;
import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.utilities.DiscordUtil;
import com.mcmiddleearth.thegaffer.utilities.PluginMessenger;
import com.mcmiddleearth.thegaffer.utilities.VentureChatUtil;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Emote;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class JobEventListener implements Listener {

    @EventHandler
    public void onJobEnd(JobEndEvent event) {
        Job job = event.getJob();

        job.sendToAll(ChatColor.GRAY + "The " + job.getName() + " job has ended.");
        for (Player p : job.getAllAsPlayersArray()) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 1f);
            VentureChatUtil.leaveJobChannel(p);
        }

        // Notify the proxy
        JobDeleteMessage message = new JobDeleteMessage(job.getName());
        TheGaffer.getServerInstance().getOnlinePlayers()
            .stream().findFirst()
            .ifPresent(player -> PluginMessenger.sendToPlayer(player, message));

        if(job.isDiscordSend()) {
            //TextChannel channel = DiscordUtil.getTextChannelById(TheGaffer.getDiscordChannel());
            String emoji =(TheGaffer.getDiscordJobEmoji()==null 
                          || TheGaffer.getDiscordJobEmoji().isEmpty() ?"":":"+TheGaffer.getDiscordJobEmoji()+":");
            DiscordUtil.sendDiscord(emoji+" __**Info:**__ The job " + job.getName()
                           + " has ended at " + getLondonTime() + ".");
        }
    }

    @EventHandler
    public void onJobStart(JobStartEvent event) {
        Job job = event.getJob();

        // Notify the proxy
        JobCreateMessage message = new JobCreateMessage(job.getName(), job.getDescription());
        Player owner = TheGaffer.getServerInstance().getOfflinePlayer(job.getOwner()).getPlayer();
        if (owner != null) {
            PluginMessenger.sendToPlayer(owner, message);
        }

        if (job.isDiscordSend()) {
           Guild guild = DiscordSRV.getPlugin().getMainGuild();
           String emojiString = "";
           if((TheGaffer.getDiscordJobEmoji()!=null && !TheGaffer.getDiscordJobEmoji().isEmpty())) {
                List<Emote> emojiList = guild.getEmotesByName(TheGaffer.getDiscordJobEmoji(),true);
                if(!emojiList.isEmpty()) {
                    emojiString = "<:"+TheGaffer.getDiscordJobEmoji()+":"+emojiList.getFirst().getId()+">";
                }
            }
           String tag = "";
           for(String name:job.getDiscordTags()) {
               if(name!=null && !name.isEmpty()) {
//Logger.getGlobal().info("DiscordTag: "+name);
                   String discTag = "@\""+name+"\"";
                   tag = tag + discTag+", ";
               }
           }
           //"<@&724391251604013198>"
           String discordMessage = emojiString+" ***"+tag+"there is a new job!!!*** "
                          +emojiString+"\n        __**Leader:**__        " + job.getOwner()
                   + "\n        __**Title:**__            " + job.getName()
                   + "\n        __**World:**__            " + job.getBukkitWorld().getName()
                   + "\n        __**Time Start:**__ " +getLondonTime() 
                   + "\nTo join the job type in game chat: ```css\n/job join " + job.getName() + "```";
           if(TheGaffer.isJobDescription()) {
                   discordMessage = discordMessage + "__**Job Description:**__ "+job.getDescription();
           }
           DiscordUtil.sendDiscord(discordMessage);
           /*sendDiscord(":ring1 @everyone, there is a new job!!! :ring1"
                                    +"\n         __**Leader:**__      "+job.getOwner()
                                    +"\n         __**Title:**__          "+job.getName()
                                    +"\n         __**Time Start:**__ "+getLondonTime()
                                    +"\n         To join the job typ in game chat: ```css\n/job join "+job.getName());*/
        }
    }

    private String getLondonTime() {
        Calendar calendar = new GregorianCalendar();
        TimeZone zone = calendar.getTimeZone();
        zone.setID("Europe/London");
        zone.setRawOffset(0);
        calendar.setTimeZone(zone);
        SimpleDateFormat format = (SimpleDateFormat) DateFormat.getTimeInstance(DateFormat.SHORT,Locale.UK);
        format.setCalendar(calendar);
        format.applyPattern("HH:mm z");
        return format.format(calendar.getTime());
     }


    @EventHandler
    public void onJobProtection(JobProtectionInteractEvent event) {
       //Util.info("Got event: " + event.getEventName() + "blocked: " + event.isBlocked());
    }

    @EventHandler
    public void onJobProtection(JobProtectionBlockPlaceEvent event) {
        //Util.info("Got event: " + event.getEventName() + "blocked: " + event.isBlocked());
    }

    @EventHandler
    public void onJobProtection(JobProtectionBlockBreakEvent event) {
        //Util.info("Got event: " + event.getEventName() + "blocked: " + event.isBlocked());
    }

    @EventHandler
    public void onJobProtection(JobProtectionHangingBreakEvent event) {
        //Util.info("Got event: " + event.getEventName() + "blocked: " + event.isBlocked());
    }

    @EventHandler
    public void onJobProtection(JobProtectionHangingPlaceEvent event) {
        //Util.info("Got event: " + event.getEventName() + "blocked: " + event.isBlocked());
    }
}
