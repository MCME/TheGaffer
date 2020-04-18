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
import com.mcmiddleearth.thegaffer.events.JobEndEvent;
import com.mcmiddleearth.thegaffer.events.JobProtectionBlockBreakEvent;
import com.mcmiddleearth.thegaffer.events.JobProtectionBlockPlaceEvent;
import com.mcmiddleearth.thegaffer.events.JobProtectionHangingBreakEvent;
import com.mcmiddleearth.thegaffer.events.JobProtectionHangingPlaceEvent;
import com.mcmiddleearth.thegaffer.events.JobProtectionInteractEvent;
import com.mcmiddleearth.thegaffer.events.JobStartEvent;
import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.utilities.VentureChatUtil;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.util.DiscordUtil;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class JobEventListener implements Listener {

    @EventHandler
    public void onJobEnd(JobEndEvent event) {
        Job job = event.getJob();
        job.sendToAll(ChatColor.GRAY + "The " + job.getName() + " job has ended.");
        for (Player p : job.getAllAsPlayersArray()) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 1f);
            VentureChatUtil.leaveJobChannel(p);
        }
        if(job.isDiscordSend()) {
            TextChannel channel = DiscordUtil.getTextChannelById(TheGaffer.getDiscordChannel());
            String emoji =(TheGaffer.getDiscordJobEmoji()==null 
                          || TheGaffer.getDiscordJobEmoji().equals("")?"":":"+TheGaffer.getDiscordJobEmoji()+":");
            sendDiscord(emoji+" __**Info:**__ The job " + job.getName() 
                           + " has ended at " + getLondonTime() + ".");
        }
    }

    @EventHandler
    public void onJobStart(JobStartEvent event) {
        Job job = event.getJob();
        String message = ChatColor.AQUA + job.getOwner() + ChatColor.GRAY 
                          + " has started a job called \"" + job.getName() + ChatColor.GRAY + "\"";
        if(TheGaffer.isJobDescription()) {
            message = message +"\n"+ChatColor.GRAY+"Job Description: "+ChatColor.AQUA+job.getDescription();
        }
        TheGaffer.getServerInstance().broadcastMessage(message);
        
        for (Player p : TheGaffer.getServerInstance().getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.8f, 2f);
        }
        if(job.isDiscordSend()) {
            TextChannel channel = DiscordUtil.getTextChannelById(TheGaffer.getDiscordChannel());
           String emoji =(TheGaffer.getDiscordJobEmoji()==null 
                          || TheGaffer.getDiscordJobEmoji().equals("")?"":":"+TheGaffer.getDiscordJobEmoji()+":");
           Guild guild = DiscordSRV.getPlugin().getMainGuild();
           String tag = "";
           for(String name:job.getDiscordTags()) {
               if(name!=null && !name.equals("")) {
                String discTag = DiscordUtil.convertMentionsFromNames("@"+name, guild);
                tag = tag + discTag+", ";
               }
           }
           String discordMessage = emoji+" ***"+tag+"there is a new job!!!*** "
                          +emoji+"\n        __**Leader:**__        " + job.getOwner() 
                   + "\n        __**Title:**__            " + job.getName() 
                   + "\n        __**Time Start:**__ " +getLondonTime() 
                   + "\nTo join the job type in game chat: ```css\n/job join " + job.getName() + "```";
           if(TheGaffer.isJobDescription()) {
                   discordMessage = discordMessage + "__**Job Description:**__ "+job.getDescription();
           }
           sendDiscord(discordMessage);
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

    private void sendDiscord(String message) {
        if ((TheGaffer.getDiscordChannel() != null) && (!TheGaffer.getDiscordChannel().equals("")))
        {
          DiscordSRV discordPlugin = DiscordSRV.getPlugin();
          if (discordPlugin != null)
          {
            TextChannel channel = discordPlugin.getDestinationTextChannelForGameChannelName(TheGaffer.getDiscordChannel());
            if (channel != null) {
              DiscordUtil.sendMessage(channel, message, 0, false);
            } else {
              Logger.getLogger("TheGaffer").warning("Discord channel not found.");
            }
          }
          else
          {
            Logger.getLogger("TheGaffer").warning("DiscordSRV plugin not found.");
          }
        }
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
