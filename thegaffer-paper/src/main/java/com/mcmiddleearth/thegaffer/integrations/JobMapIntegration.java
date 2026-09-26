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
import org.bukkit.plugin.Plugin;

import java.awt.geom.Rectangle2D;
import java.util.logging.Logger;

/**
 * Soft-dependency integration with <a href="https://github.com/webbukkit/dynmap">Dynmap</a>,
 * whose web map is served by <a href="https://github.com/JLyne/LiveAtlas">LiveAtlas</a>.
 *
 * <p>All methods are static and no-op silently when Dynmap is absent or its
 * MarkerAPI is unavailable. Throwables from the Dynmap API are caught and logged
 * so they can never propagate into job lifecycle code.
 *
 * <h3>Soft-dependency class-load isolation (important)</h3>
 * This OUTER class references <b>no Dynmap type</b> in any field, parameter, or
 * return type. That guarantees loading {@code JobMapIntegration} — which happens
 * as soon as {@code TheGaffer.onEnable()} calls {@link #init(Plugin)} — never
 * triggers resolution of any {@code org.dynmap.*} class descriptor. On a server
 * <i>without</i> Dynmap, such resolution would throw {@link NoClassDefFoundError}
 * (an {@link Error}, not caught by {@code catch (Exception)}, and it would fire
 * during class-load before any method body runs), crashing the plugin on enable.
 *
 * <p>All Dynmap-typed state and every Dynmap API call live in the nested
 * {@link DynmapMarkers} class. That class is only <b>class-loaded</b> the first
 * time it is instantiated ({@code new DynmapMarkers()} in {@link #init}), which
 * happens only <i>after</i> {@code Bukkit.getPluginManager().getPlugin("dynmap")}
 * confirms Dynmap is present and enabled. Every boundary that touches the inner
 * class is wrapped in {@code catch (Throwable)} as a backstop.
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

    /** Marker id suffix — actual id is the sanitised job name + this. */
    private static final String AREA_MARKER_SUFFIX = "_job";

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

    /**
     * Handle to the Dynmap-touching state. Typed as the nested class (NOT any
     * Dynmap type), so the outer class descriptor never references org.dynmap.*.
     * Null when Dynmap is absent/disabled → all methods no-op.
     */
    private static DynmapMarkers handle = null;

    private JobMapIntegration() { /* utility class */ }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Initialises the Dynmap integration. Call from
     * {@link com.mcmiddleearth.thegaffer.TheGaffer#onEnable()} after all
     * other setup is done. If Dynmap is absent or its MarkerAPI is unavailable
     * the integration is left disabled and all other methods become no-ops.
     *
     * @param plugin  the TheGaffer plugin instance (used only for logging)
     */
    public static void init(Plugin plugin) {
        handle = null;
        // Gate on the Bukkit plugin lookup — no Dynmap class is touched here.
        Plugin dyn = Bukkit.getPluginManager().getPlugin("dynmap");
        if (dyn == null || !dyn.isEnabled()) {
            Util.info("Dynmap not present — web-map integration disabled.");
            return;
        }
        // First (and only) touch of any org.dynmap.* class is inside DynmapMarkers,
        // instantiated here only after Dynmap is confirmed present. catch Throwable
        // covers NoClassDefFoundError / LinkageError from an API mismatch.
        try {
            DynmapMarkers markers = new DynmapMarkers();
            if (markers.setup(dyn)) {
                handle = markers;
                Util.info("Dynmap web-map integration enabled (MarkerSet: " + MARKER_SET_ID + ").");
            } else {
                Util.info("Dynmap present but MarkerAPI/MarkerSet unavailable — web-map integration disabled.");
            }
        } catch (Throwable t) {
            Logger.getLogger("TheGaffer").warning(
                    "[JobMapIntegration] init() failed — web-map integration disabled: " + t);
            handle = null;
        }
    }

    // -------------------------------------------------------------------------
    // Public API — all signatures are Dynmap-free; delegate to the handle.
    // -------------------------------------------------------------------------

    /**
     * Creates or replaces the AreaMarker for an active job. Safe to call even
     * if the job's world is not loaded yet (silently no-ops if so).
     */
    public static void showJob(Job job) {
        if (handle == null || job == null) { return; }
        try {
            String world = worldName(job);
            if (world == null) { return; } // world not loaded
            handle.show(markerId(job), job.getName(), world,
                    corners(job), colourFor(job), buildDescription(job));
        } catch (Throwable t) {
            Util.debug("[JobMapIntegration] showJob(" + job.getName() + ") failed: " + t);
        }
    }

    /**
     * Removes the AreaMarker for a job that has ended.
     */
    public static void removeJob(Job job) {
        if (handle == null || job == null) { return; }
        try {
            handle.remove(markerId(job));
        } catch (Throwable t) {
            Util.debug("[JobMapIntegration] removeJob(" + job.getName() + ") failed: " + t);
        }
    }

    /**
     * Recomputes the corners and colour for a job whose radius or warp has changed.
     * Delegates to {@link #showJob(Job)} which already handles create-or-replace.
     */
    public static void updateJob(Job job) {
        showJob(job); // show() deletes any existing marker first, then recreates
    }

    // -------------------------------------------------------------------------
    // Pure helpers (NO Dynmap types) — exercised by JobMapIntegrationTest.
    // -------------------------------------------------------------------------

    /** Returns {@code true} when Dynmap is present and the MarkerSet is ready. */
    public static boolean isEnabled() {
        return handle != null;
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

    /** The job's world name, or null if the world is not loaded. Bukkit-only, no Dynmap. */
    private static String worldName(Job job) {
        org.bukkit.World w = job.getBukkitWorld();
        return (w == null) ? null : w.getName();
    }

    static String markerId(Job job) {
        // Dynmap marker ids must be short and not contain special characters.
        // Sanitise: lowercase, strip everything except letters/digits/hyphen/underscore.
        return job.getName().toLowerCase().replaceAll("[^a-z0-9_\\-]", "_") + AREA_MARKER_SUFFIX;
    }

    private static String htmlEscape(String s) {
        if (s == null) { return ""; }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // -------------------------------------------------------------------------
    // Dynmap-typed inner class — LOADED ONLY WHEN INSTANTIATED (Dynmap present).
    //
    // Every org.dynmap.* reference in this whole file is confined here. Because
    // the JVM resolves a class's referenced-type descriptors lazily at first
    // active use, and the outer class never names any org.dynmap type, this
    // nested class is not resolved until `new DynmapMarkers()` runs in init() —
    // which only happens after getPlugin("dynmap") confirms Dynmap is present.
    // -------------------------------------------------------------------------
    private static final class DynmapMarkers {

        private org.dynmap.markers.MarkerAPI markerAPI;
        private org.dynmap.markers.MarkerSet markerSet;

        /**
         * Obtains the Dynmap MarkerAPI and the shared "Jobs" MarkerSet.
         *
         * @param dyn  the Dynmap plugin (already confirmed present + enabled)
         * @return true if the MarkerAPI and MarkerSet are ready; false otherwise
         */
        boolean setup(Plugin dyn) {
            if (!(dyn instanceof org.dynmap.DynmapCommonAPI)) {
                return false;
            }
            org.dynmap.DynmapCommonAPI api = (org.dynmap.DynmapCommonAPI) dyn;
            markerAPI = api.getMarkerAPI();
            if (markerAPI == null) {
                return false;
            }
            markerSet = markerAPI.getMarkerSet(MARKER_SET_ID);
            if (markerSet == null) {
                markerSet = markerAPI.createMarkerSet(
                        MARKER_SET_ID, MARKER_SET_LABEL,
                        null,   // allowed icons (null = all)
                        false); // not persistent (we rebuild on each enable)
            } else {
                markerSet.setMarkerSetLabel(MARKER_SET_LABEL);
            }
            return markerSet != null;
        }

        /** Creates or replaces the AreaMarker for a job. */
        void show(String markerId, String label, String world,
                  double[] corners, int colour, String description) {
            // Delete any stale marker first (e.g. if radius/warp changed).
            org.dynmap.markers.AreaMarker existing = markerSet.findAreaMarker(markerId);
            if (existing != null) {
                existing.deleteMarker();
            }
            double[] xCorners = { corners[0], corners[1], corners[1], corners[0] };
            double[] zCorners = { corners[2], corners[2], corners[3], corners[3] };
            org.dynmap.markers.AreaMarker marker = markerSet.createAreaMarker(
                    markerId,
                    label,   // shown in popup header
                    true,    // markup (allow HTML in description)
                    world,
                    xCorners,
                    zCorners,
                    false);  // not persistent
            if (marker == null) { return; }
            // Fill: semi-transparent (opacity 0.3); outline: fully opaque, 2px weight.
            marker.setFillStyle(0.3, colour);
            marker.setLineStyle(2, 1.0, colour);
            marker.setDescription(description);
        }

        /** Deletes the AreaMarker for a job by its id, if present. */
        void remove(String markerId) {
            org.dynmap.markers.AreaMarker marker = markerSet.findAreaMarker(markerId);
            if (marker != null) {
                marker.deleteMarker();
            }
        }
    }
}
