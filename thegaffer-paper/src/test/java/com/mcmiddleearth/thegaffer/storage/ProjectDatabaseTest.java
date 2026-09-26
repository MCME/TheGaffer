package com.mcmiddleearth.thegaffer.storage;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import com.mcmiddleearth.thegaffer.TheGaffer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProjectDatabaseTest {

    @TempDir File tmp;
    private ServerMock server;

    @BeforeEach
    void setUp() throws Exception {
        server = MockBukkit.mock();
        setField("serverInstance", server);
        ProjectDatabase.projectsDirOverride = tmp;
        ProjectDatabase.getProjects().clear();
    }

    @AfterEach
    void tearDown() throws Exception {
        ProjectDatabase.projectsDirOverride = null;
        setField("serverInstance", null);
        MockBukkit.unmock();
    }

    private static void setField(String name, Object value) throws Exception {
        Field f = TheGaffer.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(null, value);
    }

    @Test
    void createRejectsDuplicateCanonicalName() {
        assertTrue(ProjectDatabase.create(new Project("Minas Tirith", UUID.randomUUID(), 1L)));
        assertFalse(ProjectDatabase.create(new Project("  minas tirith ", UUID.randomUUID(), 2L)));
        assertEquals(1, ProjectDatabase.all().size());
    }

    @Test
    void getIsCaseInsensitive() {
        ProjectDatabase.create(new Project("Minas Tirith", UUID.randomUUID(), 1L));
        assertNotNull(ProjectDatabase.get("MINAS tirith"));
        assertNull(ProjectDatabase.get("Pelargir"));
    }

    @Test
    void persistsAndReloadsFromDisk() {
        Project p = new Project("Pelargir", UUID.randomUUID(), 9L);
        p.setDescription("Harbour");
        ProjectDatabase.create(p);
        ProjectDatabase.getProjects().clear();
        int n = ProjectDatabase.loadProjects();
        assertEquals(1, n);
        Project r = ProjectDatabase.get("pelargir");
        assertNotNull(r);
        assertEquals("Harbour", r.getDescription());
        assertFalse(r.isDirty());
    }

    @Test
    void byStatusAndHasActive() {
        Project a = new Project("A", UUID.randomUUID(), 1L);
        Project b = new Project("B", UUID.randomUUID(), 1L);
        b.setStatus(Project.Status.ARCHIVED);
        ProjectDatabase.create(a);
        ProjectDatabase.create(b);
        assertEquals(1, ProjectDatabase.byStatus(Project.Status.ACTIVE).size());
        assertEquals(1, ProjectDatabase.byStatus(Project.Status.ARCHIVED).size());
        assertTrue(ProjectDatabase.hasActiveProjects());
    }

    @Test
    void deleteRemovesEntryAndFile() {
        ProjectDatabase.create(new Project("Gone", UUID.randomUUID(), 1L));
        File f = new File(tmp, "gone.yml");
        assertTrue(f.exists());
        ProjectDatabase.delete("GONE");
        assertNull(ProjectDatabase.get("gone"));
        assertFalse(f.exists());
    }

    @Test
    void loadSkipsBlankNamedFiles() throws Exception {
        ProjectDatabase.create(new Project("Real", UUID.randomUUID(), 1L));
        org.bukkit.configuration.file.YamlConfiguration y = new org.bukkit.configuration.file.YamlConfiguration();
        y.set("name", "   ");
        y.set("status", "ACTIVE");
        y.save(new File(tmp, "blank.yml"));
        ProjectDatabase.getProjects().clear();
        int n = ProjectDatabase.loadProjects();
        assertEquals(1, n); // only "Real" loaded; blank-named file skipped
        assertNotNull(ProjectDatabase.get("Real"));
    }
}
