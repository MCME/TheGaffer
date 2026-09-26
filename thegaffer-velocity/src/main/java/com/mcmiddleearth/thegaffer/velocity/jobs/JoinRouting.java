package com.mcmiddleearth.thegaffer.velocity.jobs;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Decides whether the proxy has to step in for a typed {@code /job join <name>}.
 *
 * <p>The backend already handles that command correctly for a job on its own server, including the
 * permission check, the ban list and its own error messages. The one thing it cannot do is reach a
 * job living on a different backend: it answers "No jobs currently running" instead. So the proxy
 * intercepts <em>only</em> that case and otherwise keeps out of the way -- there is no second copy
 * of the lookup rules here that could drift away from the backend's.
 *
 * <p>Name matching is deliberately <strong>case-sensitive</strong>, because the backend resolves the
 * name with {@code TreeMap.containsKey} under the default comparator. Matching loosely here would
 * transfer a player across servers only for the backend to then reject the name -- strictly worse
 * than leaving them where they are. The command label and sub-command <em>are</em> matched loosely,
 * because Bukkit dispatches {@code /JOB JOIN} just fine.
 *
 * <p>Pure and static on purpose: no Velocity types, so every branch is unit-testable.
 */
public final class JoinRouting {

    // This runs for every command every player types on the proxy, so keep it cheap: String.split
    // with a multi-character regex recompiles the pattern on each call.
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private JoinRouting() {
    }

    /** The named job lives on {@code targetServer}, so the proxy must move the player and replay the command. */
    public record Transfer(String targetServer, String commandToReplay) {
    }

    /**
     * @param command       the command as Velocity reports it, with no leading slash
     * @param currentServer the backend the player is on right now
     * @param jobs          every job the proxy currently knows about, across all backends
     * @return the transfer to perform, or empty to let the command fall through untouched
     */
    public static Optional<Transfer> routeNamedJoin(String command, String currentServer, Collection<Job> jobs) {
        String trimmed = command.trim();
        if (!trimmed.regionMatches(true, 0, "job", 0, 3)) {
            return Optional.empty();
        }

        String[] tokens = WHITESPACE.split(trimmed);
        if (tokens.length < 3
                || !tokens[0].equalsIgnoreCase("job")
                || !tokens[1].equalsIgnoreCase("join")) {
            return Optional.empty();
        }
        // Extra arguments are ignored, matching the backend, which reads args[1] and drops the rest.
        String name = tokens[2];

        List<Job> matches = jobs.stream().filter(job -> job.name().equals(name)).toList();
        if (matches.size() != 1) {
            // Nothing by that name, or two backends run a job with the same name. Guessing would
            // teleport the player somewhere they did not ask for, so let the backend answer.
            return Optional.empty();
        }

        Job job = matches.get(0);
        if (job.server().equalsIgnoreCase(currentServer)) {
            return Optional.empty();
        }
        return Optional.of(new Transfer(job.server(), "/job join " + name));
    }
}
