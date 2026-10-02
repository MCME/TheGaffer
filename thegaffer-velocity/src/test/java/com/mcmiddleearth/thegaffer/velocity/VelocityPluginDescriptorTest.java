package com.mcmiddleearth.thegaffer.velocity;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * velocity-plugin.json is generated at compile time from the {@code @Plugin} annotation, whose
 * version must be a compile-time constant and therefore cannot be filled in by Maven.
 *
 * <p>Left unguarded, the pom and the descriptor drift and the proxy reports a version the jar does
 * not have. MCME-Connect shipped two different production jars both claiming 2.0.1 this way, and
 * they were indistinguishable from the proxy's plugin list.
 */
class VelocityPluginDescriptorTest {

    @Test
    void testTheGeneratedDescriptorDeclaresThePomVersion() throws Exception {
        String pomVersion = System.getProperty("project.version");
        assertNotNull(pomVersion, "surefire must pass project.version through; see the pom");

        String json;
        try (InputStream in = getClass().getResourceAsStream("/velocity-plugin.json")) {
            assertNotNull(in, "velocity-plugin.json is generated into target/classes at compile time."
                    + " If it is missing, the velocity-api annotation processor did not run --"
                    + " check annotationProcessorPaths in this module's pom.");
            json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        Matcher version = Pattern.compile("\"version\"\s*:\s*\"([^\"]+)\"").matcher(json);
        assertTrue(version.find(), "descriptor declares no version: " + json);
        assertEquals(pomVersion, version.group(1),
                "the @Plugin annotation's version must be bumped alongside the pom");
    }
}
