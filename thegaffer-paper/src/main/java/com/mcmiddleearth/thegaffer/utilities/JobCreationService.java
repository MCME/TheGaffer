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
package com.mcmiddleearth.thegaffer.utilities;

import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.storage.JobDatabase;
import com.mcmiddleearth.thegaffer.storage.JobWarp;
import java.io.File;
import java.util.function.Predicate;
import org.bukkit.entity.Player;

/**
 * The single job-creation path shared by every entry point (currently the
 * {@code /createjob} Dialog). Extracting it here keeps one authoritative
 * "build and activate a job" routine and lets the naming rules be unit-tested
 * without standing up a server.
 *
 * <p>Job names must stay globally unique (the name is the job's key, filename,
 * and stats grouping), but builders should never have to invent a fresh one:
 * a taken name is auto-numbered ({@code Wall} → {@code Wall-2} → …) rather than
 * rejected. The pure helpers ({@link #normalizeName}, {@link #clampRadius},
 * {@link #resolveFreeName}) carry the testable logic; {@link #create} wires them
 * to the live filesystem / {@link JobDatabase}.
 */
public final class JobCreationService {

    private JobCreationService() {}

    /** Highest suffix we'll try before giving up (effectively unreachable in practice). */
    private static final int MAX_SUFFIX = 10000;

    /** Trim and replace spaces with underscores, matching the old conversation's naming rule. */
    public static String normalizeName(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().replaceAll(" ", "_");
    }

    /** Keep the radius within the advertised 1–1000 range. */
    public static int clampRadius(int radius) {
        if (radius < 1) {
            return 1;
        }
        if (radius > 1000) {
            return 1000;
        }
        return radius;
    }

    /**
     * Return the first free variant of a normalised base name: the base itself if free,
     * otherwise {@code base-2}, {@code base-3}, … The {@code isTaken} predicate decides
     * availability, so the numbering is unit-testable without touching the filesystem.
     *
     * @return the free name, or {@code null} only if the base and {@value #MAX_SUFFIX}
     *         suffixes are all taken (not reachable under normal use).
     */
    public static String resolveFreeName(String normalizedBase, Predicate<String> isTaken) {
        if (normalizedBase == null || normalizedBase.isEmpty()) {
            return normalizedBase;
        }
        if (!isTaken.test(normalizedBase)) {
            return normalizedBase;
        }
        for (int n = 2; n <= MAX_SUFFIX; n++) {
            String candidate = normalizedBase + "-" + n;
            if (!isTaken.test(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /** Location of the saved-job file for a (already normalised) name. */
    public static File jobFile(String normalizedName) {
        return new File(TheGaffer.getPluginDataFolder() + TheGaffer.getFileSeperator() + "jobs"
                + TheGaffer.getFileSeperator() + normalizedName + TheGaffer.getFileExtension());
    }

    /** A name is taken if a job with it is running, or a past job saved a file under it. */
    private static boolean isNameTaken(String normalizedName) {
        return jobFile(normalizedName).exists()
                || JobDatabase.getActiveJobs().containsKey(normalizedName);
    }

    /**
     * Build and activate a job from the collected fields, auto-numbering the name if the
     * requested one is taken. On success the job is live and {@code activateJob} has fired
     * the start event (broadcast, Discord, border, map).
     *
     * @param owner       the creating player (becomes owner; their location seeds the warp)
     * @param rawName     user-entered name (spaces → underscores; auto-numbered if taken)
     * @param description short description, or "" when descriptions are disabled
     * @param priv        private (invite-only) job
     * @param radius      build-area radius (clamped to 1–1000)
     * @param discordSend announce on Discord
     * @param project     canonical project name, or "nothing"
     * @param glowing     give workers/helpers a glow outline
     * @return the actual job name created (may differ from the request if auto-numbered),
     *         or {@code null} if the name was blank (nothing is created).
     */
    public static String create(Player owner, String rawName, String description, boolean priv,
            int radius, boolean discordSend, String project, boolean glowing) {
        String base = normalizeName(rawName);
        if (base.isEmpty()) {
            return null;
        }
        String name = resolveFreeName(base, JobCreationService::isNameTaken);
        if (name == null) {
            return null;
        }

        JobWarp warp = new JobWarp(owner.getLocation());
        Job job = new Job(name, description == null ? "" : description, owner.getUniqueId(), true,
                warp, warp.getWorld(), priv, clampRadius(radius), discordSend,
                (project == null || project.isEmpty()) ? "nothing" : project);
        if (glowing) {
            job.setGlowing();
        }
        JobDatabase.activateJob(job);
        return name;
    }
}
