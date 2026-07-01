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

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.events.*;
import com.mcmiddleearth.thegaffer.listeners.PlayerListener;
import com.mcmiddleearth.thegaffer.utilities.JobBorderManager;
import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.storage.JobStats;
import com.mcmiddleearth.thegaffer.utilities.Msg;
import com.mcmiddleearth.thegaffer.utilities.StatsManager;
import com.mcmiddleearth.thegaffer.utilities.Util;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.MessageBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.util.DiscordUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.awt.*;
import java.time.Instant;
import java.util.logging.Logger;

public class JobEventListener implements Listener {

    @EventHandler
    public void onJobEnd(JobEndEvent event) {
        Job job = event.getJob();
        // Clear job borders for all participants before the job is deregistered.
        JobBorderManager.clearAll(job);
        job.sendToAll(Component.text("The " + job.getName() + " job has ended.", NamedTextColor.GRAY));
        for (Player p : job.getAllAsPlayersArray()) {
            // Guarded (see onJobStart): a third-party sound-packet listener must not abort
            // the job-end Discord recap below.
            try {
                p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 1f);
            } catch (Exception ex) {
                Util.debug("Job-end sound suppressed for " + p.getName() + ": " + ex.getMessage());
            }
            // #15: Immediately revert to Survival any member we switched to Creative.
            PlayerListener.revertToSurvivalIfSwitched(p);
        }
        if(job.isDiscordSend()) {
            String emoji =(TheGaffer.getDiscordJobEmoji()==null 
                          || TheGaffer.getDiscordJobEmoji().equals("")?"":":"+TheGaffer.getDiscordJobEmoji()+":");
            sendDiscord(emoji+" __**Info:**__ The job " + job.getName()
                           + " has ended " + discordTimestamp(System.currentTimeMillis(), 'R') + ".");
            JobStats stats = StatsManager.findJobStats(job.getName());
            if (stats != null) {
                sendDiscord(StatsManager.buildDiscordSummary(stats));
            }
        }
    }

    @EventHandler
    public void onJobStart(JobStartEvent event) {
        Job job = event.getJob();
        String message = "";
        String first = "";
        String second = "";
        for(int i = 0; i<20; i++) {
            net.md_5.bungee.api.ChatColor color = net.md_5.bungee.api.ChatColor.of(new Color(0,120+i * 6, 90+i*7));
            first = first + color + "~";
            second = color + "~" + second;
        }
        message = first + second +"\n"
                          + ChatColor.AQUA + ChatColor.BOLD + Util.nameOf(job.getOwner()) + ChatColor.GRAY + ChatColor.BOLD
                          + " has started a job.\n"
                          + ChatColor.GRAY + "Job Name: " + ChatColor.AQUA + job.getName();
        if(TheGaffer.isJobDescription()) {
            message = message +"\n"+ChatColor.GRAY+"Job Description: "+ChatColor.AQUA+job.getDescription();
        }
        message = message + "\n"+ChatColor.GRAY+"To join the job do "+ChatColor.AQUA+"/job join "+job.getName()
                          + "\n"+ChatColor.GRAY+"If you're on another world, travel to the "+ChatColor.AQUA+job.getBukkitWorld().getName()+ChatColor.GRAY+" world first."
                          + "\n"+ first + second;
        Plugin connectPlugin = Bukkit.getPluginManager().getPlugin("MCME-Connect");
        Player player = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if(player !=null && connectPlugin != null && connectPlugin.isEnabled()) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Message");
            out.writeUTF("ALL");
            out.writeUTF(message);
            player.sendPluginMessage(TheGaffer.getPluginInstance(), "BungeeCord", out.toByteArray());
//Logger.getGlobal().info("Bungee Broadcast sent! "+message);
        } else {
            TheGaffer.getServerInstance().broadcastMessage(message);
        }
        
        // A clickable join button + a gentle audio cue for players on THIS server. The text
        // announcement above is delivered network-wide as legacy text via the MCME-Connect proxy,
        // which strips Adventure click data — so the clickable button is sent locally here, to the
        // only players who can act on it in one click (remote players must switch servers first).
        Component joinButton = Component.text("» ", NamedTextColor.DARK_GREEN)
                .append(Msg.button("Click here to join " + job.getName(), NamedTextColor.GREEN,
                        "/job join " + job.getName(), "Join " + job.getName()));
        for (Player p : TheGaffer.getServerInstance().getOnlinePlayers()) {
            p.sendMessage(joinButton);
            // Guarded: a third-party outbound-packet listener (e.g. PremiumVanish's NamedSoundEffect
            // module via ProtocolLib) can throw when the sound packet is sent — a COSMETIC cue must
            // not abort the rest of this handler.
            try {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.5f);
            } catch (Exception ex) {
                Util.debug("Job-start sound suppressed for " + p.getName() + ": " + ex.getMessage());
            }
        }
        if (job.isDiscordSend()) {
            // discord.channel is a DiscordSRV game-channel NAME, not a raw snowflake ID, so
            // getTextChannelById(...) returned null and the embed was silently skipped. Resolve it
            // the SAME way sendDiscord (the job-end path) does — that's why the end message worked.
            DiscordSRV discordPlugin = DiscordSRV.getPlugin();
            TextChannel channel = (discordPlugin != null)
                    ? discordPlugin.getDestinationTextChannelForGameChannelName(TheGaffer.getDiscordChannel())
                    : null;
            if (channel != null) {
                Guild guild = discordPlugin.getMainGuild();
                String ping = "";
                for (String role : TheGaffer.getAllowedPingRoles()) {
                    if (role != null && !role.isEmpty()) {
                        ping = ping + DiscordUtil.convertMentionsFromNames("@" + role, guild) + " ";
                    }
                }
                ping = ping.trim();
                long startMillis = (job.getStartTime() != null && job.getStartTime() > 0)
                        ? job.getStartTime() : System.currentTimeMillis();
                String title = "🛠 New job: " + job.getName();
                if (title.length() > 256) { title = title.substring(0, 256); }
                EmbedBuilder embed = new EmbedBuilder()
                        .setColor(new java.awt.Color(46, 160, 90))
                        .setTitle(title)
                        .addField("Leader", Util.nameOf(job.getOwner()), true)
                        .addField("World", job.getBukkitWorld().getName(), true)
                        .addField("Started", discordTimestamp(startMillis, 'R'), true)
                        .addField("Join in-game", "`/job join " + job.getName() + "`", false)
                        .setFooter("MCME")
                        .setTimestamp(Instant.ofEpochMilli(startMillis));
                if (TheGaffer.isJobDescription() && job.getDescription() != null && !job.getDescription().isEmpty()) {
                    String desc = job.getDescription();
                    if (desc.length() > 4096) { desc = desc.substring(0, 4096); }
                    embed.setDescription(desc);
                }
                final Message msg = new MessageBuilder().setContent(ping).setEmbed(embed.build()).build();
                // Send off the main thread: sendMessageBlocking does a synchronous Discord
                // REST call and must not block the server tick.
                new BukkitRunnable() {
                    @Override
                    public void run() { DiscordUtil.sendMessageBlocking(channel, msg, false); }
                }.runTaskAsynchronously(TheGaffer.getPluginInstance());
            } else {
                Logger.getLogger("TheGaffer").warning("Discord channel '" + TheGaffer.getDiscordChannel()
                        + "' not found — job-start embed not sent.");
            }
        }
    }

    /** A Discord timestamp token, e.g. {@code <t:1719774000:R>} (R = relative "x ago", f = full local). */
    static String discordTimestamp(long epochMillis, char style) {
        return "<t:" + (epochMillis / 1000L) + ":" + style + ">";
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
    
    // ---- /job listen protection warnings (QA item 1) ---------------------------------
    // /job listen fills TheGaffer.getListening(); these two handlers are what finally read
    // it. On a protection VIOLATION (event.isBlocked() == true) every online listener is
    // told who tried to build outside a job and where. Throttled per-offender so a held
    // click can't flood the listeners.

    /** Per-offender timestamp (ms) of the last listen warning we broadcast, for throttling. */
    private final java.util.Map<java.util.UUID, Long> lastListenWarn = new java.util.HashMap<>();

    /** Min gap between listen warnings for the same offender. */
    private static final long LISTEN_WARN_THROTTLE_MS = 3000L;

    @EventHandler
    public void onProtectionBlockPlace(JobProtectionBlockPlaceEvent event) {
        if (event.isBlocked()) {
            warnListeners(event.getPlayer(), event.getLocation(), "place");
        }
    }

    @EventHandler
    public void onProtectionBlockBreak(JobProtectionBlockBreakEvent event) {
        if (event.isBlocked()) {
            warnListeners(event.getPlayer(), event.getLocation(), "break");
        }
    }

    /**
     * Notifies every online /job listen subscriber that {@code offender} hit protection at
     * {@code loc}. Skips the offender themselves (they already get the deny message) and
     * throttles to one warning per offender per {@link #LISTEN_WARN_THROTTLE_MS}.
     */
    private void warnListeners(Player offender, org.bukkit.Location loc, String action) {
        if (offender == null || loc == null) {
            return;
        }
        if (TheGaffer.getListening().isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = lastListenWarn.get(offender.getUniqueId());
        if (last != null && (now - last) < LISTEN_WARN_THROTTLE_MS) {
            return;
        }
        lastListenWarn.put(offender.getUniqueId(), now);

        String worldName = (loc.getWorld() != null) ? loc.getWorld().getName() : "?";
        Component message = Component.text("[Listen] ", NamedTextColor.GOLD)
                .append(Component.text(offender.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" tried to " + action + " at " + worldName + " "
                        + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ(),
                        NamedTextColor.GRAY));
        for (Player listener : TheGaffer.getListening()) {
            // Only online listeners, and never the offender themselves.
            if (listener != null && listener.isOnline()
                    && !listener.getUniqueId().equals(offender.getUniqueId())) {
                listener.sendMessage(message);
            }
        }
    }

    // NOTE: The five empty onJobProtection handlers that were here have been removed.
    // StatsListener now handles JobProtectionBlockPlace/BreakEvent for stats counting
    // (closes audit item H3 — dead handler stubs replaced by real behaviour).
}
