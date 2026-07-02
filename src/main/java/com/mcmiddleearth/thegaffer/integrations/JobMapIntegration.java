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

import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.utilities.Util;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

import java.awt.geom.Rectangle2D;
import java.util.logging.Logger;

/**
 * Soft-dependency integration with <a href="https://github.com/webbukkit/dynmap">Dynmap</a>,
 * whose web map is served by <a href="https://github.com/JLyne/LiveAtlas">LiveAtlas</a>.
 *
 * <p>All methods are static and no-op silently when Dynmap is absent or its
 * MarkerAPI is unavailable. Exceptions from the Dynmap API are caught and logged
 * so they can never propagate into job lifecycle code.
 *
 * <h3>Dependency strategy</h3>
 * {@code DynmapCoreAPI} is declared as a {@code provided} compile dependency
 * (resolved from {@code https://repo.mikeprimm.com/} and cached in the local
 * {@code .m2} repository). The offline {@code mvn -o} build works because the
 * artifact is cached; a one-off online build was performed first to populate the
 * cache. Dynmap stays a Bukkit soft-dependency — it is never required at runtime.
 *
 * <h3>Colour palette</h3>
 * Projects are hashed to one of the 8 colours below. Unattached jobs (project
 * {@code null} or {@code "nothing"}) use the default grey.
 */
public final class JobMapIntegration {

    /** MarkerSet id registered with Dynmap. */
    private static final String MARKER_SET_ID = "thegaffer.jobs";

    /** Human-readable label shown in the LiveAtlas/Dynmap layer selector. */
    private static final String MARKER_SET_LABEL = "Jobs";

    /** Marker id prefix — actual id is the job name (sanitised). */
    private static final String AREA_MARKER_SUFFIX = "_job";

    /** Null when Dynmap is absent/disabled; non-null means ready. */
    private static MarkerAPI markerAPI = null;

    /** The MarkerSet that holds all job AreaMarkers. */
    private static MarkerSet markerSet = null;

    /**
     * A small fixed palette for project colours (fill, in 0xRRGGBB).
     * The default grey is used for jobs with no project.
     */
    private static final int[] PALETTE = {
            0x4A90D9,   // blue
            0x7ED321,   // green
            0xF5A623,   // orange
            0xD0021B,   // red
            0x9013FE,   // purple
            0x50E3C2,   // teal
            0xFFD700,   // gold
            0xFF69B4    // pink
    };

    /** Default colour for jobs with no project (grey). */
    private static final int DEFAULT_COLOUR = 0x888888;

    private JobMapIntegration() { /* utility class */ }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Initialises the Dynmap integration. Call from
     * {@link com.mcmiddleearth.thegaffer.TheGaffer#onEnable()} after all
     * other setup is done. If Dynmap is absent or its MarkerAPI is unavailable
     * the integration is marked disabled and all other methods become no-ops.
     *
     * @param plugin  the TheGaffer plugin instance (used only for logging)
     */
    public static void init(Plugin plugin) {
        markerAPI = null;
        markerSet  = null;
        try {
            Plugin dyn = Bukkit.getPluginManager().getPlugin("dynmap");
            if (dyn == null || !dyn.isEnabled()) {
                Util.info("Dynmap not present — web-map integration disabled.");
                return;
            }
            if (!(dyn instanceof DynmapCommonAPI)) {
                Util.info("Dynmap plugin found but does not implement DynmapCommonAPI — web-map integration disabled.");
                return;
            }
            DynmapCommonAPI api = (DynmapCommonAPI) dyn;
            markerAPI = api.getMarkerAPI();
            if (markerAPI == null) {
                Util.info("Dynmap MarkerAPI is null — web-map integration disabled.");
                return;
            }
            // Reuse an existing MarkerSet or create a new one.
            markerSet = markerAPI.getMarkerSet(MARKER_SET_ID);
            if (markerSet == null) {
                markerSet = markerAPI.createMarkerSet(
                        MARKER_SET_ID, MARKER_SET_LABEL,
                        null,   // allowed icons (null = all)
                        false); // not persistent (we rebuild on each enable)
            } else {
                markerSet.setMarkerSetLabel(MARKER_SET_LABEL);
            }
            if (markerSet == null) {
                Logger.getLogger("TheGaffer").warning(
                        "[JobMapIntegration] Could not create/retrieve MarkerSet — web-map integration disabled.");
                markerAPI = null;
                return;
            }
            Util.info("Dynmap web-map integration enabled (MarkerSet: " + MARKER_SET_ID + ").");
        } catch (Exception ex) {
            Logger.getLogger("TheGaffer").warning(
                    "[JobMapIntegration] init() threw an exception — web-map integration disabled: " + ex);
            markerAPI = null;
            markerSet  = null;
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Creates or replaces the AreaMarker for an active job. Safe to call even
     * if the job's world is not loaded yet (silently no-ops if so).
     */
    public static void showJob(Job job) {
        if (!isEnabled()) { return; }
        try {
            String markerId = markerId(job);
            World world = job.getBukkitWorld();
            if (world == null) { return; } // world not loaded

            // Delete any stale marker first (e.g. if radius/warp changed).
            AreaMarker existing = markerSet.findAreaMarker(markerId);
            if (existing != null) {
                existing.deleteMarker();
            }

            double[] corners = corners(job);
            double[] xCorners = { corners[0], corners[1], corners[1], corners[0] };
            double[] zCorners = { corners[2], corners[2], corners[3], corners[3] };

            AreaMarker marker = markerSet.createAreaMarker(
                    markerId,
                    job.getName(),      // label (shown in popup header)
                    true,               // markup (allow HTML in description)
                    world.getName(),
                    xCorners,
                    zCorners,
                    false               // not persistent
            );
            if (marker == null) { return; }

            int colour = colourFor(job);
            // Fill: semi-transparent (opacity 0.3 = ~77 out of 255, encoded as 0-1 float)
            marker.setFillStyle(0.3, colour);
            // Outline: fully opaque, 2px weight
            marker.setLineStyle(2, 1.0, colour);
            // HTML popup description
            marker.setDescription(buildDescription(job));

        } catch (Exception ex) {
            Logger.getLogger("TheGaffer").warning(
                    "[JobMapIntegration] showJob(" + job.getName() + ") failed: " + ex);
        }
    }

    /**
     * Removes the AreaMarker for a job that has ended.
     */
    public static void removeJob(Job job) {
        if (!isEnabled()) { return; }
        try {
            AreaMarker marker = markerSet.findAreaMarker(markerId(job));
            if (marker != null) {
                marker.deleteMarker();
            }
        } catch (Exception ex) {
            Logger.getLogger("TheGaffer").warning(
                    "[JobMapIntegration] removeJob(" + job.getName() + ") failed: " + ex);
        }
    }

    /**
     * Recomputes the corners and colour for a job whose radius or warp has changed.
     * Delegates to {@link #showJob(Job)} which already handles create-or-replace.
     */
    public static void updateJob(Job job) {
        if (!isEnabled()) { return; }
        showJob(job); // showJob deletes existing and recreates
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Returns {@code true} when Dynmap is present and the MarkerSet is ready. */
    public static boolean isEnabled() {
        return markerSet != null;
    }

    /**
     * Computes the 4 corner X/Z values for the job's square area.
     *
     * @return double[4]: {minX, maxX, minZ, maxZ}
     */
    static double[] corners(Job job) {
        Rectangle2D bounds = job.getBounds();
        return new double[] {
                bounds.getMinX(),   // minX
                bounds.getMaxX(),   // maxX
                bounds.getMinY(),   // minZ  (Rectangle2D Y == Minecraft Z)
                bounds.getMaxY()    // maxZ
        };
    }

    /**
     * Resolves a display colour for the job based on its project name.
     * The hash is case-insensitive and "nothing"/null maps to the default grey.
     */
    static int colourFor(Job job) {
        String project = job.getProjectname();
        if (project == null || project.equalsIgnoreCase("nothing") || project.isBlank()) {
            return DEFAULT_COLOUR;
        }
        // Stable, non-negative index into the palette
        int idx = Math.abs(project.toLowerCase().hashCode()) % PALETTE.length;
        return PALETTE[idx];
    }

    /**
     * Builds the HTML popup description shown when a user clicks on the marker
     * in LiveAtlas / Dynmap.
     */
    static String buildDescription(Job job) {
        String project = job.getProjectname();
        boolean hasProject = project != null && !project.equalsIgnoreCase("nothing") && !project.isBlank();

        return "<b>" + htmlEscape(job.getName()) + "</b><br>"
                + "Owner: " + htmlEscape(Util.nameOf(job.getOwner())) + "<br>"
                + "Project: " + (hasProject ? htmlEscape(project) : "&mdash;") + "<br>"
                + "Radius: " + job.getJobRadius() + "<br>"
                + "<i>/job join " + htmlEscape(job.getName()) + "</i>";
    }

    private static String markerId(Job job) {
        // Dynmap marker ids must be short and not contain special characters.
        // Sanitise: lowercase, strip everything except letters/digits/hyphen/underscore.
        return job.getName().toLowerCase().replaceAll("[^a-z0-9_\\-]", "_") + AREA_MARKER_SUFFIX;
    }

    private static String htmlEscape(String s) {
        if (s == null) { return ""; }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
