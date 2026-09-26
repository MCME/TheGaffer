package com.mcmiddleearth.thegaffer.velocity.jobs;

import com.mcmiddleearth.thegaffer.velocity.VelocityGafferPlugin;
import com.mcmiddleearth.thegaffer.velocity.helpers.ServerConnectUtils;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class JobManager {
    private static final Set<Job> serverJobs = new HashSet<>();

    public static boolean isEmpty() {
        return serverJobs.isEmpty();
    }

    public static void addJob(Job job) {
        serverJobs.add(job);
    }

    public static void removeJob(String serverName, String jobName) {
        serverJobs.removeIf(job -> job.equals(jobName, serverName));
    }

    public static Optional<Job> getSingleJob() {
       if (serverJobs.isEmpty()) return Optional.empty();
       if (serverJobs.size() > 1) return Optional.empty();

       return Optional.of(serverJobs.iterator().next());
    }

    // TODO: Auto-indent / centre the header & scale the border
    public static Component buildJobBlock(Job job, String header) {
        Component border = VelocityGafferPlugin.mm.deserialize(
            "<gradient:#5e4fa2:red:#5e4fa2>~~~~~~~~~~~~~~~~~~~~~~~~~</gradient>"
        );

        Component title = Component.text(header)
            .color(NamedTextColor.GOLD)
            .decorate(TextDecoration.BOLD);

        Component jobLine = Component.text(" Job: ", NamedTextColor.GRAY)
            .append(Component.text(job.name(), NamedTextColor.AQUA));

        Component creatorLine = VelocityGafferPlugin.mm.deserialize(
            " <gray>Creator: <player-tag><creator>",
            Placeholder.unparsed("creator", job.creator())
        );

        Component descriptionLine = Component.text(" Description: ", NamedTextColor.GRAY)
            .append(Component.text(job.description(), TextColor.fromHexString("#dedede")));

        Component joinButton = Component.text("          ")
            .append(buildJoinButton(job, "⟫ Click to Join ⟪"));

        return Component.empty()
            .append(border).appendNewline()
            .append(title).appendNewline().appendNewline()
            .append(jobLine).appendNewline()
            .append(creatorLine).appendNewline()
            .append(descriptionLine).appendNewline().appendNewline()
            .append(joinButton).appendNewline().appendNewline()
            .append(border);
    }

    // TODO: Auto-indent / centre the header
    public static Component buildJobsList(String header) {
        if (serverJobs.isEmpty()) {
            return Component.text("There are no active jobs.")
                .color(NamedTextColor.GRAY);
        }

        final TextComponent.Builder result = Component.text().appendNewline();

        result.append(
            Component.text(header)
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD)
                .appendNewline()
        );

        serverJobs.stream()
            .sorted(Comparator.comparing(Job::name))
            .forEach(job -> {
                Component jobName = Component.text(job.name())
                    .color(NamedTextColor.AQUA)
                    .hoverEvent(
                        HoverEvent.showText(Component.text(job.description()))
                    );

                String text = "  <gray>- <job> (by <player-tag><creator><gray>)<join>";
                Component jobLine = VelocityGafferPlugin.mm.deserialize(
                    text,
                    Placeholder.unparsed("creator", job.creator()),
                    Placeholder.component("job", jobName),
                    Placeholder.component("join", buildJoinButton(job, "〘JOIN〙"))
                );

                result.appendNewline().append(jobLine);
        });

        return result.appendNewline().build();
    }

    private static Component buildJoinButton(Job job, String clickText) {
        Component hoverText = Component.text("Click to join ", NamedTextColor.LIGHT_PURPLE)
            .append(Component.text(job.name(), NamedTextColor.AQUA))
            .append(Component.text(" (" + job.server() + ")"))
            .append(Component.text(" (single-use)")
                .color(TextColor.color(0x5c5c5c))
                .decorate(TextDecoration.ITALIC));

        ClickEvent clickToJoinEvent = ClickEvent.callback(audience -> {
            if (!(audience instanceof Player player)) {
                return;
            }

            player.getCurrentServer().ifPresent(serverConnection -> {
                String playerServer = serverConnection.getServerInfo().getName();

                if (playerServer.equalsIgnoreCase(job.server())) {
                    player.spoofChatInput("/job join " + job.name());
                    return;
                }

                ServerConnectUtils.connectPlayerToServer(
                    player,
                    job.server(),
                    // Unable to use forwardToServer - because the command would be forwarded to the original server
                    newConnection -> player.spoofChatInput("/job join " + job.name())
                );
            });
        });

        return Component.text(clickText)
            .color(NamedTextColor.AQUA)
            .decorate(TextDecoration.BOLD)
            .hoverEvent(HoverEvent.showText(hoverText))
            .clickEvent(clickToJoinEvent);
    }
}