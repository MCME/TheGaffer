package com.mcmiddleearth.thegaffer;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the reflective build-permission API contract.
 *
 * <p>External plugins call into TheGaffer's protection API <em>by reflection</em>:
 * <ul>
 *   <li><b>MCME-Architect</b> and <b>PlotBuild</b> call
 *       {@code TheGaffer.hasBuildPermission(Player, Location)} via
 *       {@code getMethod("hasBuildPermission", Player.class, Location.class)}.</li>
 * </ul>
 * If that signature changes, the external lookup throws {@code NoSuchMethodException} and at
 * least PlotBuild treats the failure as "allowed" — silently bypassing build protection
 * (a fail-<em>open</em> security hole). These tests fail the build the moment the signature
 * drifts, so the contract can't be broken accidentally. If a change here is ever genuinely
 * required, coordinate it with the PlotBuild / MCME-Architect maintainers first.
 */
class ProtectionApiContractTest {

    @Test
    void hasBuildPermissionSignatureIsStable() throws NoSuchMethodException {
        Method m = TheGaffer.class.getMethod("hasBuildPermission", Player.class, Location.class);
        assertEquals(boolean.class, m.getReturnType(),
                "hasBuildPermission must return boolean (reflective contract with PlotBuild/MCME-Architect)");
        assertTrue(Modifier.isPublic(m.getModifiers()) && Modifier.isStatic(m.getModifiers()),
                "hasBuildPermission must remain public static");
    }

    @Test
    void getBuildProtectionMessageSignatureIsStable() throws NoSuchMethodException {
        Method m = TheGaffer.class.getMethod("getBuildProtectionMessage", Player.class, Location.class);
        assertEquals(String.class, m.getReturnType(),
                "getBuildProtectionMessage must return String");
        assertTrue(Modifier.isPublic(m.getModifiers()) && Modifier.isStatic(m.getModifiers()),
                "getBuildProtectionMessage must remain public static");
    }
}
