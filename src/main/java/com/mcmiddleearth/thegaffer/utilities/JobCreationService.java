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
import org.bukkit.entity.Player;

/**
 * The single job-creation path shared by every entry point (currently the
 * {@code /createjob} Dialog). Extracting it here keeps one authoritative
 * "build and activate a job" routine and lets the name/radius rules be
 * unit-tested without standing up a server.
 *
 * <p>The pure decision helpers ({@link #normalizeName}, {@link #clampRadius},
 * {@link #validateName}) carry the logic worth testing; {@link #create} wires
 * them to the live filesystem / {@link JobDatabase} and is exercised by
 * in-game QA.
 */
public final class JobCreationService {

    private JobCreationService() {}

    /** Result of a creation attempt, so callers can render an appropriate message. */
    public enum Outcome {
        /** Job was created and activated. */
        OK,
        /** The name was blank after normalisation. */
        EMPTY_NAME,
        /** A job file with this name exists — the name was used by a past job. */
        NAME_TAKEN_HISTORY,
        /** A job with this name is currently running. */
        NAME_RUNNING
    }

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
     * Pure name validation: given whether a name already exists in history (a saved
     * job file) and whether it is currently active, decide the outcome. Kept free of
     * I/O so it can be tested exhaustively.
     */
    public static Outcome validateName(String normalizedName, boolean existsInHistory, boolean isActive) {
        if (normalizedName == null || normalizedName.isEmpty()) {
            return Outcome.EMPTY_NAME;
        }
        // A currently-running name takes precedence so the clearer "already running"
        // message wins: an active job also has a saved file (activateJob writes it), so
        // checking history first would always mask a running-name clash as "run before".
        if (isActive) {
            return Outcome.NAME_RUNNING;
        }
        if (existsInHistory) {
            return Outcome.NAME_TAKEN_HISTORY;
        }
        return Outcome.OK;
    }

    /** Location of the saved-job file for a (already normalised) name. */
    public static File jobFile(String normalizedName) {
        return new File(TheGaffer.getPluginDataFolder() + TheGaffer.getFileSeperator() + "jobs"
                + TheGaffer.getFileSeperator() + normalizedName + TheGaffer.getFileExtension());
    }

    /**
     * Validate, build and activate a job from the collected fields. On {@link Outcome#OK}
     * the job is live and {@code activateJob} has fired the start event (broadcast,
     * Discord, border, map). Any non-OK outcome makes no changes.
     *
     * @param owner       the creating player (becomes owner; their location seeds the warp)
     * @param rawName     user-entered name (spaces are converted to underscores)
     * @param description short description, or "" when descriptions are disabled
     * @param priv        private (invite-only) job
     * @param radius      build-area radius (clamped to 1–1000)
     * @param discordSend announce on Discord
     * @param project     canonical project name, or "nothing"
     * @param glowing     give workers/helpers a glow outline
     */
    public static Outcome create(Player owner, String rawName, String description, boolean priv,
            int radius, boolean discordSend, String project, boolean glowing) {
        String name = normalizeName(rawName);
        Outcome verdict = validateName(name, jobFile(name).exists(),
                JobDatabase.getActiveJobs().containsKey(name));
        if (verdict != Outcome.OK) {
            return verdict;
        }

        JobWarp warp = new JobWarp(owner.getLocation());
        Job job = new Job(name, description == null ? "" : description, owner.getUniqueId(), true,
                warp, warp.getWorld(), priv, clampRadius(radius), discordSend,
                (project == null || project.isEmpty()) ? "nothing" : project);
        if (glowing) {
            job.setGlowing();
        }
        JobDatabase.activateJob(job);
        return Outcome.OK;
    }
}
