package com.mcmiddleearth.thegaffer.utilities;

import be.seeseemelk.mockbukkit.MockBukkit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit tests for {@link Msg#jobChat(String, Component)} — verifies that the
 * shared formatter (#17) contains the sender name and message body so both the
 * sticky listener and the one-off /jc command produce an identical output.
 */
class MsgJobChatTest {

    @BeforeAll
    static void setup() {
        MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    /** Recursively collects all plain-text content from a Component tree. */
    private static String collectText(Component component) {
        StringBuilder sb = new StringBuilder();
        if (component instanceof TextComponent tc) {
            sb.append(tc.content());
        }
        for (Component child : component.children()) {
            sb.append(collectText(child));
        }
        return sb.toString();
    }

    @Test
    void formattedComponentContainsJobTag() {
        Component result = Msg.jobChat("TestPlayer", Component.text("some message"));
        assertTrue(collectText(result).contains("[Job]"),
                "formatted component should contain the [Job] channel tag");
    }

    @Test
    void formattedComponentContainsSenderName() {
        Component result = Msg.jobChat("TestPlayer", Component.text("hello world"));
        assertTrue(collectText(result).contains("TestPlayer"),
                "formatted component should contain the sender name");
    }

    @Test
    void formattedComponentContainsBody() {
        Component result = Msg.jobChat("TestPlayer", Component.text("hello world"));
        assertTrue(collectText(result).contains("hello world"),
                "formatted component should contain the message body");
    }
}
