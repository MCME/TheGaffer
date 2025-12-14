package com.mcmiddleearth.thegaffer.velocity.jobs;

import com.mcmiddleearth.thegaffer.velocity.helpers.ServerConnectUtils;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.*;

public class JobManager {
    public record ServerJob(String serverName, Job job) { }

    // Q: Is there any point in a Map??? Just have an ArrayList of jobs - job contains its server???
    private static final Map<String, Set<Job>> serverJobs = new HashMap<>();

    public static boolean isEmpty() {
        return serverJobs.isEmpty();
    }

    public static void addJob(String serverName, Job job) {
        serverJobs
            .computeIfAbsent(serverName, k -> new HashSet<>())
            .add(job);
    }

    public static void removeJob(String serverName, String jobName) {
        Set<Job> jobs = serverJobs.get(serverName);
        if (jobs == null) {
            return;
        }

        jobs.removeIf(job -> job.name().equals(jobName));

        // Clear out servers with no jobs
        // Q: Is this needed/beneficial?
        if (jobs.isEmpty()) {
            serverJobs.remove(serverName);
        }
    }

    public static Optional<ServerJob> getSingleJob() {
        ServerJob found = null;

        for (var entry : serverJobs.entrySet()) {
            for (Job job : entry.getValue()) {

                if (found != null) {
                    // Return early - more than 1 job exists
                    return Optional.empty();
                }

                found = new ServerJob(entry.getKey(), job);
            }
        }

        return Optional.ofNullable(found);
    }

    public static Component getAllJobsComponent() {
        if (serverJobs.isEmpty()) {
            return Component.text("There are no active jobs.")
                .color(NamedTextColor.GRAY);
        }

        final TextComponent.Builder result = Component.text().appendNewline();

        result.append(
            Component.text("Available Jobs")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD)
                .append(Component.newline())
                .append(Component.newline())
        );

        serverJobs.entrySet().stream()
            // sort servers alphabetically
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                String server = entry.getKey();

                entry.getValue().stream()
                    // sort jobs alphabetically
                    .sorted(Comparator.comparing(Job::name))
                    .forEach(job -> {
                        Component jobLine =
                            Component.text("  - ", NamedTextColor.DARK_GRAY)
                                .append(Component.text(job.name(), NamedTextColor.GREEN))
                                .append(Component.text(" (by " + job.creator() + ") ", NamedTextColor.GRAY))
                                .append(
                                    Component.text("[CLICK]")
                                        .color(NamedTextColor.AQUA)
                                        .decorate(TextDecoration.BOLD)
                                        .clickEvent(
                                            ClickEvent.callback(audience -> {
                                                if (!(audience instanceof Player player)) {
                                                    return;
                                                }

                                                player.getCurrentServer().ifPresent(serverConnection -> {
                                                    String playerServer = serverConnection.getServerInfo().getName();

                                                    if (playerServer.equalsIgnoreCase(server)) {
                                                        player.spoofChatInput("/job join " + job.name());
                                                        return;
                                                    }

                                                    ServerConnectUtils.connectPlayerToServer(
                                                        player,
                                                        server,
                                                        // Unable to use forwardToServer - because the command would be forwarded to the original server
                                                        targetServer -> player.spoofChatInput("/job join " + job.name())
                                                    );
                                                });
                                            })
                                        )
                                        .hoverEvent(
                                            HoverEvent.showText(
                                                Component.text("Click to join ", NamedTextColor.GRAY)
                                                    .append(Component.text(job.name(), NamedTextColor.GREEN))
                                                    .append(Component.text(" on "))
                                                    .append(Component.text(server, NamedTextColor.YELLOW))
                                                    .append(Component.text(" (single-use)")
                                                        .color(TextColor.color(0x5c5c5c))
                                                        .decorate(TextDecoration.ITALIC))
                                            )
                                        )
                                )
                                .append(Component.text(" to join", NamedTextColor.GRAY))
                                .append(Component.newline());

                        result.append(jobLine);
                    });

                result.append(Component.newline());
            });

        return result.build();
    }
}