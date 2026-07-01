package com.mcmiddleearth.thegaffer.commands.AdminCommands;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the load-order fix in {@link JobAdminCommands}: the admin dispatch map must be
 * populated at class INITIALISATION (a static block), NOT in the instance constructor.
 *
 * <p>{@code /jobadmin} is registered as a {@code JobAdminConversation}, which forwards args
 * to the static {@code JobAdminCommands.executeOneLiner(...)} — so on a cold server a
 * {@code JobAdminCommands} instance may never be constructed. If the map were populated only
 * in the constructor it would be empty on that path and every {@code /jobadmin <job> <action>}
 * would silently fall through to the "unknown action" help. {@code Class.forName} triggers
 * static init without invoking the constructor, reproducing exactly that path.
 */
class JobAdminCommandsTest {

    @Test
    void dispatchMapPopulatedAtClassInitWithoutConstructingInstance() throws Exception {
        Class<?> c = Class.forName(
                "com.mcmiddleearth.thegaffer.commands.AdminCommands.JobAdminCommands");
        Field f = c.getDeclaredField("Methods");
        f.setAccessible(true);
        Map<?, ?> methods = (Map<?, ?>) f.get(null);

        assertFalse(methods.isEmpty(),
                "admin dispatch map must be populated at class-init, not only in the constructor");
        assertTrue(methods.containsKey("setradius"), "setradius must be a known admin action");
        assertTrue(methods.containsKey("addhelper"));
        assertTrue(methods.containsKey("clearworkerinven"));
        assertEquals(12, methods.size(), "all 12 admin actions must be registered");
    }
}
