package com.mcmiddleearth.thegaffer.commands;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.storage.Project;
import com.mcmiddleearth.thegaffer.storage.ProjectDatabase;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProjectCommandTest {

    @TempDir File tmp;
    private ServerMock server;
    private final ProjectCommand cmd = new ProjectCommand();

    @BeforeEach
    void setUp() throws Exception {
        server = MockBukkit.mock();
        Field f = TheGaffer.class.getDeclaredField("serverInstance");
        f.setAccessible(true); f.set(null, server);
        setStorageField("projectsDirOverride", tmp);
        ProjectDatabase.getProjects().clear();
    }

    @AfterEach
    void tearDown() throws Exception {
        setStorageField("projectsDirOverride", null);
        Field f = TheGaffer.class.getDeclaredField("serverInstance");
        f.setAccessible(true); f.set(null, null);
        MockBukkit.unmock();
    }

    private static void setStorageField(String name, Object value) throws Exception {
        Field f = ProjectDatabase.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(null, value);
    }

    @Test
    void multiWordNameMatchedAsPrefixLeavingTrailingArgs() {
        ProjectDatabase.create(new Project("Minas Tirith", UUID.randomUUID(), 1L));
        ProjectCommand.Match m = cmd.matchProject(
                new String[]{"setdescription", "Minas", "Tirith", "the", "white", "city"}, 1);
        assertNotNull(m);
        assertEquals("Minas Tirith", m.project.getName());
        assertEquals(3, m.next); // trailing text "the white city" begins at index 3
    }

    @Test
    void longestRegisteredNameWinsWhenNamesNest() {
        ProjectDatabase.create(new Project("Minas", UUID.randomUUID(), 1L));
        ProjectDatabase.create(new Project("Minas Tirith", UUID.randomUUID(), 1L));
        ProjectCommand.Match m = cmd.matchProject(new String[]{"x", "Minas", "Tirith", "foo"}, 1);
        assertNotNull(m);
        assertEquals("Minas Tirith", m.project.getName());
        assertEquals(3, m.next);
    }

    @Test
    void noRegisteredMatchReturnsNull() {
        assertNull(cmd.matchProject(new String[]{"x", "Unknown", "Place"}, 1));
    }
}
