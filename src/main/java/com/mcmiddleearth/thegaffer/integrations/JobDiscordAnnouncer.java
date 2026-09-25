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
package com.mcmiddleearth.thegaffer.integrations;

import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.storage.JobStats;
import com.mcmiddleearth.thegaffer.utilities.StatsManager;
import com.mcmiddleearth.thegaffer.utilities.Util;
import org.bukkit.Bukkit;

import java.util.logging.Logger;

/**
 * Soft-dependency integration with DiscordSRV: the job-start embed and the job-end recap.
 *
 * <h3>Why this class exists</h3>
 * These announcements used to live inline in {@code JobEventListener}, which also carries
 * {@code onProtectionBlockPlace} and {@code onProtectionBlockBreak} — the handlers that drive the
 * <b>{@code /job listen} moderation warnings</b>, telling subscribed staff when someone hits build
 * protection. Bukkit registers listeners <b>per class</b>, so on a server without DiscordSRV the
 * listener failed to register in its entirety:
 *
 * <pre>
 * Failed to register events for class ...JobEventListener because
 * github/scarsz/discordsrv/dependencies/jda/api/entities/TextChannel does not exist
 * </pre>
 *
 * The listen warnings were therefore silently dead wherever DiscordSRV was absent or disabled —
 * observed on a clean Paper 26.2 server on 2026-09-26, and applicable to any backend running
 * without it. (Job <i>statistics</i> were never affected: they are fed by {@code StatsListener},
 * a separate class. An earlier version of this note claimed otherwise; live testing on
 * 2026-09-26 showed stats counting correctly per builder while this listener was failing.)
 *
 * <h3>Class-load isolation</h3>
 * This is the same discipline {@link JobMapIntegration} applies to Dynmap, and for the same reason.
 * This OUTER class references <b>no DiscordSRV or JDA type</b> in any field, parameter or return
 * type, so loading it can never resolve a {@code github.scarsz.*} descriptor. Every such type lives
 * in the nested {@link Bridge} class, which is only class-loaded once
 * {@link Bukkit#getPluginManager()} confirms DiscordSRV is present <i>and enabled</i>. The boundary
 * is wrapped in {@code catch (Throwable)} rather than {@code catch (Exception)} because the failure
 * mode is {@link NoClassDefFoundError}, which is an {@link Error}.
 */
public final class JobDiscordAnnouncer {

    private JobDiscordAnnouncer() {}

    /** A Discord timestamp token, e.g. {@code <t:1719774000:R>} (R = relative "x ago", f = full local). */
    public static String discordTimestamp(long epochMillis, char style) {
        return "<t:" + (epochMillis / 1000L) + ":" + style + ">";
    }

    /** True only when DiscordSRV is installed AND enabled. Never touches a DiscordSRV type. */
    private static boolean available() {
        try {
            return Bukkit.getPluginManager().isPluginEnabled("DiscordSRV");
        } catch (Throwable t) {
            return false;
        }
    }

    /** Announces a newly started job. No-op when the job opted out or DiscordSRV is absent. */
    public static void announceJobStart(Job job) {
        if (job == null || !job.isDiscordSend() || !available()) {
            return;
        }
        try {
            Bridge.jobStart(job);
        } catch (Throwable t) {
            Logger.getLogger("TheGaffer").warning("Discord job-start announcement failed: " + t);
        }
    }

    /** Announces an ended job, with its stats recap. No-op when opted out or DiscordSRV is absent. */
    public static void announceJobEnd(Job job) {
        if (job == null || !job.isDiscordSend() || !available()) {
            return;
        }
        try {
            Bridge.jobEnd(job);
        } catch (Throwable t) {
            Logger.getLogger("TheGaffer").warning("Discord job-end announcement failed: " + t);
        }
    }

    private static String jobEmoji() {
        String e = TheGaffer.getDiscordJobEmoji();
        return (e == null || e.isEmpty()) ? "" : ":" + e + ":";
    }

    /**
     * Every DiscordSRV and JDA reference in the plugin lives here. Class-loaded only after
     * {@link #available()} has confirmed the plugin is enabled — never before.
     */
    private static final class Bridge {

        private Bridge() {}

        static void jobStart(Job job) {
            // discord.channel is a DiscordSRV game-channel NAME, not a raw snowflake ID, so
            // getTextChannelById(...) returns null. Resolve it the way the job-end path does.
            github.scarsz.discordsrv.DiscordSRV plugin = github.scarsz.discordsrv.DiscordSRV.getPlugin();
            github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel channel =
                    (plugin != null)
                            ? plugin.getDestinationTextChannelForGameChannelName(TheGaffer.getDiscordChannel())
                            : null;
            if (channel == null) {
                Logger.getLogger("TheGaffer").warning("Discord channel '" + TheGaffer.getDiscordChannel()
                        + "' not found — job-start embed not sent.");
                return;
            }
            github.scarsz.discordsrv.dependencies.jda.api.entities.Guild guild = plugin.getMainGuild();
            String ping = "";
            for (String role : TheGaffer.getAllowedPingRoles()) {
                if (role != null && !role.isEmpty()) {
                    ping = ping + github.scarsz.discordsrv.util.DiscordUtil
                            .convertMentionsFromNames("@" + role, guild) + " ";
                }
            }
            ping = ping.trim();
            long startMillis = (job.getStartTime() != null && job.getStartTime() > 0)
                    ? job.getStartTime() : System.currentTimeMillis();
            String title = "🛠 New job: " + job.getName();
            if (title.length() > 256) { title = title.substring(0, 256); }
            github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder embed =
                    new github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder()
                            .setColor(new java.awt.Color(46, 160, 90))
                            .setTitle(title)
                            .addField("Leader", Util.nameOf(job.getOwner()), true)
                            .addField("World", job.getBukkitWorld().getName(), true)
                            .addField("Started", discordTimestamp(startMillis, 'R'), true)
                            .addField("Join in-game", "`/job join " + job.getName() + "`", false)
                            .setFooter("MCME")
                            .setTimestamp(java.time.Instant.ofEpochMilli(startMillis));
            if (TheGaffer.isJobDescription() && job.getDescription() != null && !job.getDescription().isEmpty()) {
                String desc = job.getDescription();
                if (desc.length() > 4096) { desc = desc.substring(0, 4096); }
                embed.setDescription(desc);
            }
            github.scarsz.discordsrv.dependencies.jda.api.entities.Message msg =
                    new github.scarsz.discordsrv.dependencies.jda.api.MessageBuilder()
                            .setContent(ping.isEmpty() ? "🛠" : ping)
                            .setEmbed(embed.build()).build();
            String fallbackText = (ping.isEmpty() ? "" : ping + " ")
                    + "🛠 New job: **" + job.getName() + "** — Leader: " + Util.nameOf(job.getOwner())
                    + " · World: " + job.getBukkitWorld().getName()
                    + " · Started " + discordTimestamp(startMillis, 'R')
                    + " · Join in-game: `/job join " + job.getName() + "`";
            sendWithFallback(channel, msg, fallbackText);
        }

        static void jobEnd(Job job) {
            String emoji = jobEmoji();
            github.scarsz.discordsrv.DiscordSRV plugin = github.scarsz.discordsrv.DiscordSRV.getPlugin();
            github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel channel =
                    (plugin != null)
                            ? plugin.getDestinationTextChannelForGameChannelName(TheGaffer.getDiscordChannel())
                            : null;
            if (channel == null) {
                Logger.getLogger("TheGaffer").warning("Discord channel '" + TheGaffer.getDiscordChannel()
                        + "' not found — job-end embed not sent.");
                return;
            }
            long endMillis = System.currentTimeMillis();
            JobStats stats = StatsManager.findJobStats(job.getName());
            String title = "🏁 Job ended: " + job.getName();
            if (title.length() > 256) { title = title.substring(0, 256); }
            github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder embed =
                    new github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder()
                            .setColor(new java.awt.Color(170, 70, 70))
                            .setTitle(title)
                            .setFooter("MCME")
                            .setTimestamp(java.time.Instant.now());
            if (stats != null) {
                embed.addField("Duration", StatsManager.formatDuration(stats.getDurationMillis()), true)
                     .addField("Blocks placed", String.valueOf(stats.getTotalPlaced()), true)
                     .addField("Blocks broken", String.valueOf(stats.getTotalBroke()), true)
                     .addField("Builders", String.valueOf(stats.getParticipants().size()), true);
            }
            String fallbackText = emoji + " __**Info:**__ The job " + job.getName()
                    + " has ended " + discordTimestamp(endMillis, 'R') + "."
                    + (stats != null ? "\n" + StatsManager.buildDiscordSummary(stats) : "");
            String endContent = emoji.isEmpty() ? "🏁" : emoji;
            github.scarsz.discordsrv.dependencies.jda.api.entities.Message endMsg =
                    new github.scarsz.discordsrv.dependencies.jda.api.MessageBuilder()
                            .setContent(endContent).setEmbed(embed.build()).build();
            sendWithFallback(channel, endMsg, fallbackText);
        }

        /**
         * Sends the embed asynchronously via a blocking REST call. If it returns null (Discord
         * rejected it) or throws, logs why and posts {@code fallbackText} as plain text instead.
         */
        private static void sendWithFallback(
                github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel channel,
                github.scarsz.discordsrv.dependencies.jda.api.entities.Message embedMsg,
                String fallbackText) {
            new org.bukkit.scheduler.BukkitRunnable() {
                @Override
                public void run() {
                    Exception error = null;
                    boolean delivered = false;
                    try {
                        github.scarsz.discordsrv.dependencies.jda.api.entities.Message sent =
                                github.scarsz.discordsrv.util.DiscordUtil
                                        .sendMessageBlocking(channel, embedMsg, false);
                        delivered = (sent != null);
                    } catch (Exception ex) {
                        error = ex;
                    }
                    if (!delivered) {
                        Logger.getLogger("TheGaffer").warning("Discord embed did not send"
                                + (error != null
                                    ? " (" + error.getClass().getSimpleName() + ": " + error.getMessage() + ")"
                                    : " (Discord returned no message)")
                                + " — falling back to a plain-text announcement. If this keeps happening,"
                                + " check that the bot has the 'Embed Links' permission in the target channel.");
                        github.scarsz.discordsrv.util.DiscordUtil
                                .sendMessage(channel, fallbackText, 0, false);
                    }
                }
            }.runTaskAsynchronously(TheGaffer.getPluginInstance());
        }
    }
}
