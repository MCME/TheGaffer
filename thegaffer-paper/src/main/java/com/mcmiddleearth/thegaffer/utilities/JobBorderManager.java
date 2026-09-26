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
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Manages per-player particle walls that visually mark a job's square build area.
 *
 * <p>Lifecycle:
 * <ul>
 *   <li>Particle wall is shown automatically when a player joins a job (or relogs/changes world).</li>
 *   <li>Wall stops rendering when the player leaves the job, the job ends, or the player quits.</li>
 *   <li>Players may toggle the wall on/off with {@code /job border}.</li>
 * </ul>
 *
 * <p>Two data structures track membership:
 * <ul>
 *   <li>{@code disabled} — UUIDs of players who opted out via {@code /job border}.</li>
 *   <li>{@code active} — UUID → Job map for players currently showing the particle wall.</li>
 * </ul>
 *
 * <p>The particle wall is <strong>purely visual</strong> — it has zero effect on player movement.
 * Players can fly straight through it.
 */
public final class JobBorderManager {

    private JobBorderManager() {}

    /** Players who have opted out of the job border for this session. */
    private static final Set<UUID> disabled = new HashSet<>();

    /** Players currently showing the particle border, mapped to the job whose bounds to draw. */
    private static final Map<UUID, Job> active = new HashMap<>();

    /** Render range: only draw edge segments within this many blocks of the player. Large enough
     *  that a player standing at the job's centre (the warp, where /createjob drops them) still sees
     *  the walls of a typical job; bigger jobs reveal each wall as the player approaches it. */
    static final double RENDER_RANGE = 64.0;

    /** Sample step along each edge (blocks). */
    static final double STEP = 2.0;

    /** Height band below player Y. */
    static final double HEIGHT_BELOW = 3.0;

    /** Height band above player Y. */
    static final double HEIGHT_ABOVE = 5.0;

    /** Height step inside the band. */
    static final double HEIGHT_STEP = 2.0;

    // ---------------------------------------------------------------------------
    // Pure geometry — no Bukkit calls; unit-testable without a server.
    // ---------------------------------------------------------------------------

    /**
     * Computes the world-border geometry for a given job (used for display/sizing).
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

    /**
     * Computes the list of X/Z sample points along the four perimeter edges of
     * {@code bounds} that fall within {@code range} blocks of {@code px, pz},
     * stepping every {@code step} blocks.
     *
     * <p>The four edges are:
     * <ol>
     *   <li>West  wall: x = minX, z from minZ … maxZ</li>
     *   <li>East  wall: x = maxX, z from minZ … maxZ</li>
     *   <li>South wall: z = minZ, x from minX … maxX</li>
     *   <li>North wall: z = maxZ, x from minX … maxX</li>
     * </ol>
     *
     * <p>This method is pure (no Bukkit calls) and is unit-testable.
     *
     * @param bounds the job's build-area bounding box (Rectangle2D)
     * @param px     the player's X coordinate
     * @param pz     the player's Z coordinate
     * @param range  the maximum horizontal distance (from player) to include a point
     * @param step   the sample spacing along each edge
     * @return a list of {@code double[2]} arrays, each being {@code {x, z}}
     */
    public static List<double[]> nearEdgeSamples(Rectangle2D bounds,
                                                  double px, double pz,
                                                  double range, double step) {
        double minX = bounds.getMinX();
        double maxX = bounds.getMaxX();
        double minZ = bounds.getMinY();   // Rectangle2D Y == our Z
        double maxZ = bounds.getMaxY();

        double rangeSq = range * range;
        java.util.ArrayList<double[]> points = new java.util.ArrayList<>();

        // West wall: x = minX, z varies
        for (double z = minZ; z <= maxZ + step * 0.01; z += step) {
            double zc = Math.min(z, maxZ);
            double dx = minX - px;
            double dz = zc - pz;
            if (dx * dx + dz * dz <= rangeSq) {
                points.add(new double[]{minX, zc});
            }
        }

        // East wall: x = maxX, z varies
        for (double z = minZ; z <= maxZ + step * 0.01; z += step) {
            double zc = Math.min(z, maxZ);
            double dx = maxX - px;
            double dz = zc - pz;
            if (dx * dx + dz * dz <= rangeSq) {
                points.add(new double[]{maxX, zc});
            }
        }

        // South wall: z = minZ, x varies
        for (double x = minX; x <= maxX + step * 0.01; x += step) {
            double xc = Math.min(x, maxX);
            double dx = xc - px;
            double dz = minZ - pz;
            if (dx * dx + dz * dz <= rangeSq) {
                points.add(new double[]{xc, minZ});
            }
        }

        // North wall: z = maxZ, x varies
        for (double x = minX; x <= maxX + step * 0.01; x += step) {
            double xc = Math.min(x, maxX);
            double dx = xc - px;
            double dz = maxZ - pz;
            if (dx * dx + dz * dz <= rangeSq) {
                points.add(new double[]{xc, maxZ});
            }
        }

        return points;
    }

    // ---------------------------------------------------------------------------
    // Render task
    // ---------------------------------------------------------------------------

    /**
     * Starts the repeating render task that draws particles for all players in {@code active}.
     * Must be called from {@link TheGaffer#onEnable()}. The scheduler automatically cancels
     * the task when the plugin disables.
     *
     * @param plugin the owning plugin
     */
    public static void startRenderTask(Plugin plugin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (active.isEmpty()) return;

                // Snapshot entries to avoid CME if another thread modifies the map
                // (all Bukkit events are synchronous, but be defensive).
                for (Map.Entry<UUID, Job> entry : active.entrySet()) {
                    UUID id  = entry.getKey();
                    Job  job = entry.getValue();

                    Player p = plugin.getServer().getPlayer(id);
                    if (p == null || !p.isOnline()) continue;
                    if (!p.getWorld().equals(job.getBukkitWorld())) continue;

                    Rectangle2D bounds = job.getBounds();
                    double px = p.getLocation().getX();
                    double py = p.getLocation().getY();
                    double pz = p.getLocation().getZ();

                    List<double[]> samples = nearEdgeSamples(
                            bounds, px, pz, RENDER_RANGE, STEP);

                    for (double[] xz : samples) {
                        double wx = xz[0];
                        double wz = xz[1];
                        // Draw a vertical column through the height band
                        for (double wy = py - HEIGHT_BELOW;
                             wy <= py + HEIGHT_ABOVE;
                             wy += HEIGHT_STEP) {
                            // END_ROD: white glow, no data object, exists in all supported
                            // API versions (1.9+). Safe on 1.19-compiled / 26.2-runtime.
                            // Per-player render. NOTE: Player.spawnParticle has no "force" flag in
                            // the 1.19 API, so a client running reduced ("Minimal"/"Decreased")
                            // particle settings may not see END_ROD — default/"All" settings do.
                            p.spawnParticle(Particle.END_ROD,
                                    new Location(p.getWorld(), wx, wy, wz),
                                    1,              // count
                                    0.0, 0.0, 0.0,  // no spread
                                    0.0);           // no extra speed
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }

    // ---------------------------------------------------------------------------
    // Bukkit-side lifecycle methods
    // ---------------------------------------------------------------------------

    /**
     * Starts showing the particle wall for {@code p} sized to {@code job}'s build area,
     * if the feature is enabled, the player has not opted out, and the player is in
     * the job's world.
     */
    public static void show(Player p, Job job) {
        if (!TheGaffer.isJobBorderEnabled()) {
            clear(p);
            return;
        }
        UUID id = p.getUniqueId();
        if (disabled.contains(id)) {
            return;
        }
        if (!p.getWorld().equals(job.getBukkitWorld())) {
            return;
        }
        active.put(id, job);
    }

    /**
     * Stops the particle wall for {@code p} (removes from the active render set).
     */
    public static void clear(Player p) {
        active.remove(p.getUniqueId());
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
            // Not in a job in this world — stop rendering any stale wall.
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
        if (active.containsKey(id)) {
            // Currently visible → hide it and remember the opt-out.
            disabled.add(id);
            clear(p);
            return false;
        }
        // Not currently visible → opt back in and try to show it. Returns true ONLY if a
        // wall was actually started (i.e. the player is in a job in this world), so a
        // player who isn't in a job doesn't get a misleading "shown".
        disabled.remove(id);
        refresh(p);
        return active.containsKey(id);
    }

    /**
     * Clears the border for every online member of {@code job}. Called when the job ends
     * (via {@code JobEndEvent}) so no stale particle walls remain after the job is over.
     */
    public static void clearAll(Job job) {
        for (Player p : job.getAllAsPlayersArray()) {
            clear(p);
        }
    }

    /**
     * Removes {@code uuid} from the {@code active} map (the client-side particles are
     * already gone on disconnect). Called from {@code PlayerQuitEvent} so the map does not
     * grow unboundedly. The {@code disabled} preference is intentionally kept for the
     * session so a relog with the same connection restores the opt-out state.
     */
    public static void forget(UUID uuid) {
        active.remove(uuid);
    }
}
