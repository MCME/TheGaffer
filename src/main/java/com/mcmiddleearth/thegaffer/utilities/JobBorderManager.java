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
import org.bukkit.Bukkit;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;

import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Manages per-player world borders that visually mark a job's square build area.
 *
 * <p>Lifecycle:
 * <ul>
 *   <li>Border is shown automatically when a player joins a job (or relogs/changes world).</li>
 *   <li>Border is cleared when the player leaves the job, the job ends, or the player quits.</li>
 *   <li>Players may toggle the border on/off with {@code /job border}.</li>
 * </ul>
 *
 * <p>Two state sets track membership:
 * <ul>
 *   <li>{@code disabled} — UUIDs of players who opted out via {@code /job border}.</li>
 *   <li>{@code shown} — UUIDs of players currently holding a custom border from us,
 *       so that {@link #clear} never clobbers a border set by another plugin.</li>
 * </ul>
 */
public final class JobBorderManager {

    private JobBorderManager() {}

    /** Players who have opted out of the job border for this session. */
    private static final Set<UUID> disabled = new HashSet<>();

    /** Players that currently have a job border applied by us. */
    private static final Set<UUID> shown = new HashSet<>();

    // ---------------------------------------------------------------------------
    // Pure geometry — no Bukkit calls; unit-testable without a server.
    // ---------------------------------------------------------------------------

    /**
     * Computes the world-border geometry for a given job.
     *
     * @param job the job whose {@link Job#getBounds()} defines the build area
     * @return {@code [centerX, centerZ, size]} where size equals the side of the
     *         (square) build area (== {@code 2 * jobRadius})
     */
    public static double[] geometry(Job job) {
        Rectangle2D bounds = job.getBounds();
        // Rectangle2D uses X/Y; the job's bounds map X→X and Y→Z (horizontal plane).
        double centerX = bounds.getCenterX();
        double centerZ = bounds.getCenterY();          // Rectangle2D Y axis is our Z
        double size    = Math.max(bounds.getWidth(), bounds.getHeight());
        return new double[]{centerX, centerZ, size};
    }

    // ---------------------------------------------------------------------------
    // Bukkit-side lifecycle methods
    // ---------------------------------------------------------------------------

    /**
     * Applies a custom world border to {@code p} sized to {@code job}'s build area,
     * if the feature is enabled, the player has not opted out, and the player is in
     * the job's world.
     */
    public static void show(Player p, Job job) {
        if (!TheGaffer.isJobBorderEnabled()) {
            // Feature disabled server-wide; clear any previously shown border.
            clear(p);
            return;
        }
        if (disabled.contains(p.getUniqueId())) {
            return;
        }
        if (!p.getWorld().equals(job.getBukkitWorld())) {
            // Player is in a different world — no border to show here.
            return;
        }
        double[] geo = geometry(job);
        WorldBorder wb = Bukkit.createWorldBorder();
        wb.setCenter(geo[0], geo[1]);
        wb.setSize(geo[2]);
        wb.setDamageAmount(0);      // purely visual — no damage on crossing
        wb.setDamageBuffer(0);
        wb.setWarningDistance(0);
        p.setWorldBorder(wb);
        shown.add(p.getUniqueId());
    }

    /**
     * Removes the job border from {@code p} (if we applied one), resetting to the
     * real world border for their current world.
     */
    public static void clear(Player p) {
        if (shown.remove(p.getUniqueId())) {
            // Reset to the natural border of the player's current world.
            p.setWorldBorder(p.getWorld().getWorldBorder());
        }
    }

    /**
     * Finds the player's active job in their current world and calls {@link #show};
     * if no such job exists, calls {@link #clear}.  This is the central workhorse
     * called on join, world-change, and job-join events.
     */
    public static void refresh(Player p) {
        // Recognise the player's job whether they are the OWNER, a HELPER, or a WORKER
        // (getJobWorking checks all three) — not just workers, so a staff member who
        // started or helps a job still gets the boundary.
        Job job = JobDatabase.getJobWorking(p);
        if (job != null && p.getWorld().equals(job.getBukkitWorld())) {
            show(p, job);
        } else {
            // Not in a job in this world — remove any stale border.
            clear(p);
        }
    }

    /**
     * Toggles the border on/off for {@code p}.
     *
     * @return {@code true} if the border is now <em>visible</em> (on), {@code false} if hidden
     */
    public static boolean toggle(Player p) {
        UUID id = p.getUniqueId();
        if (shown.contains(id)) {
            // Currently visible → hide it and remember the opt-out.
            disabled.add(id);
            clear(p);
            return false;
        }
        // Not currently visible → opt back in and try to show it. Returns true ONLY if a
        // border was actually applied (i.e. the player is in a job in this world), so a
        // player who isn't in a job doesn't get a misleading "shown".
        disabled.remove(id);
        refresh(p);
        return shown.contains(id);
    }

    /**
     * Clears the border for every online member of {@code job}. Called when the job ends
     * (via {@code JobEndEvent}) so no stale borders remain after the job is over.
     */
    public static void clearAll(Job job) {
        for (Player p : job.getAllAsPlayersArray()) {
            clear(p);
        }
    }

    /**
     * Removes {@code uuid} from the {@code shown} set only (the client-side border is
     * already gone on disconnect). Called from {@code PlayerQuitEvent} so the set does not
     * grow unboundedly. The {@code disabled} preference is intentionally kept for the
     * session so a relog with the same connection restores the opt-out state.
     */
    public static void forget(UUID uuid) {
        shown.remove(uuid);
    }
}
