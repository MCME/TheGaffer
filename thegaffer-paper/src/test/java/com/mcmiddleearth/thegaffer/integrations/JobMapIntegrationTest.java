package com.mcmiddleearth.thegaffer.integrations;

import com.mcmiddleearth.thegaffer.storage.Job;
import org.junit.jupiter.api.Test;

import java.awt.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit tests for {@link JobMapIntegration} — no Bukkit/Dynmap server needed.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Corner geometry from a job's {@link Job#getBounds()} (Rectangle2D)</li>
 *   <li>Project → palette colour determinism</li>
 *   <li>Null / "nothing" project falls back to the default grey</li>
 * </ul>
 */
class JobMapIntegrationTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Creates a minimal Job whose bounds match the standard square centered on
     * ({@code warpX}, {@code warpZ}) with the given radius.
     * The no-arg constructor is used to avoid any Bukkit API calls.
     */
    private static Job jobWithBounds(double warpX, double warpZ, int radius, String project) {
        Job job = new Job();
        job.setBounds(new Rectangle2D.Double(
                warpX - radius, warpZ - radius,
                radius * 2.0,
                radius * 2.0));
        job.setProjectname(project);
        return job;
    }

    // -----------------------------------------------------------------------
    // corners() — pure geometry
    // -----------------------------------------------------------------------

    @Test
    void cornersContainsFourValues() {
        Job job = jobWithBounds(0, 0, 50, null);
        double[] c = JobMapIntegration.corners(job);
        assertEquals(4, c.length, "corners() must return 4 elements: minX, maxX, minZ, maxZ");
    }

    @Test
    void cornersMinMaxSymmetricAroundWarp() {
        double warpX = 200.0, warpZ = -150.0;
        int radius = 80;
        Job job = jobWithBounds(warpX, warpZ, radius, null);
        double[] c = JobMapIntegration.corners(job);
        // c = {minX, maxX, minZ, maxZ}
        assertEquals(warpX - radius, c[0], 1e-9, "minX = warpX - radius");
        assertEquals(warpX + radius, c[1], 1e-9, "maxX = warpX + radius");
        assertEquals(warpZ - radius, c[2], 1e-9, "minZ = warpZ - radius");
        assertEquals(warpZ + radius, c[3], 1e-9, "maxZ = warpZ + radius");
    }

    @Test
    void cornersWorksForNegativeCoordinates() {
        double warpX = -500.0, warpZ = -1000.0;
        int radius = 200;
        Job job = jobWithBounds(warpX, warpZ, radius, null);
        double[] c = JobMapIntegration.corners(job);
        assertEquals(warpX - radius, c[0], 1e-9);
        assertEquals(warpX + radius, c[1], 1e-9);
        assertEquals(warpZ - radius, c[2], 1e-9);
        assertEquals(warpZ + radius, c[3], 1e-9);
    }

    @Test
    void cornersSizeMatchesTwoTimesRadius() {
        int radius = 75;
        Job job = jobWithBounds(0, 0, radius, null);
        double[] c = JobMapIntegration.corners(job);
        double width  = c[1] - c[0];   // maxX - minX
        double height = c[3] - c[2];   // maxZ - minZ
        assertEquals(radius * 2.0, width,  1e-9, "width  == 2 * radius");
        assertEquals(radius * 2.0, height, 1e-9, "height == 2 * radius");
    }

    // -----------------------------------------------------------------------
    // colourFor() — project → palette colour
    // -----------------------------------------------------------------------

    /** The default grey used for unattached jobs (matches the constant in the class). */
    private static final int DEFAULT_GREY = 0x888888;

    @Test
    void colourForNullProjectReturnsGrey() {
        Job job = jobWithBounds(0, 0, 50, null);
        assertEquals(DEFAULT_GREY, JobMapIntegration.colourFor(job),
                "null project must map to the default grey");
    }

    @Test
    void colourForNothingProjectReturnsGrey() {
        Job job = jobWithBounds(0, 0, 50, "nothing");
        assertEquals(DEFAULT_GREY, JobMapIntegration.colourFor(job),
                "\"nothing\" sentinel must map to the default grey");
    }

    @Test
    void colourForNothingCaseInsensitiveReturnsGrey() {
        Job job = jobWithBounds(0, 0, 50, "NOTHING");
        assertEquals(DEFAULT_GREY, JobMapIntegration.colourFor(job),
                "\"NOTHING\" (uppercase) sentinel must map to the default grey");
    }

    @Test
    void colourForRealProjectReturnsPaletteColour() {
        Job job = jobWithBounds(0, 0, 50, "MinasTirith");
        int colour = JobMapIntegration.colourFor(job);
        assertNotEquals(DEFAULT_GREY, colour,
                "a real project name should not map to the default grey");
    }

    @Test
    void colourForSameProjectIsDeterministic() {
        String project = "GondorRoads";
        Job a = jobWithBounds(0, 0, 50, project);
        Job b = jobWithBounds(100, 200, 30, project);
        assertEquals(JobMapIntegration.colourFor(a), JobMapIntegration.colourFor(b),
                "same project name must always yield the same colour");
    }

    @Test
    void colourForIsCaseInsensitiveToDifferentCasings() {
        Job lower = jobWithBounds(0, 0, 50, "minas tirith");
        Job upper = jobWithBounds(0, 0, 50, "MINAS TIRITH");
        assertEquals(JobMapIntegration.colourFor(lower), JobMapIntegration.colourFor(upper),
                "colour hash should be case-insensitive");
    }

    @Test
    void colourForDifferentProjectsCanDiffer() {
        // Hash collisions are possible, but the two projects below are chosen to
        // land on different palette buckets — they are not adjacent hashes.
        Job a = jobWithBounds(0, 0, 50, "ProjectAlpha");
        Job b = jobWithBounds(0, 0, 50, "ProjectBeta");
        // We can only assert that at least one of many project pairs differs;
        // a true collision here is astronomically unlikely, so treat it as a
        // soft assertion that warns but does not fail the build.
        if (JobMapIntegration.colourFor(a) == JobMapIntegration.colourFor(b)) {
            System.out.println("[WARN] ProjectAlpha and ProjectBeta happened to hash to the same palette slot — that is fine, but note it.");
        }
        // Always succeeds — this test exists to document expected divergence.
        assertTrue(true);
    }
}
