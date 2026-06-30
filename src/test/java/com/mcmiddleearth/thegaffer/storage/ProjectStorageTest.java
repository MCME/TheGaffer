package com.mcmiddleearth.thegaffer.storage;

import be.seeseemelk.mockbukkit.MockBukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.*;
import java.util.Arrays;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ProjectStorageTest {

    @BeforeEach void setUp() { MockBukkit.mock(); }
    @AfterEach  void tearDown() { MockBukkit.unmock(); }

    @Test
    void roundTripPreservesAllFields() {
        UUID lead = UUID.randomUUID();
        UUID m1 = UUID.randomUUID();
        UUID m2 = UUID.randomUUID();
        Project p = new Project("Minas Tirith", lead, 1234L);
        p.setDescription("Rebuild the citadel");
        p.setGoal("Reach the seventh level");
        p.setManagers(new java.util.ArrayList<>(Arrays.asList(m1, m2)));
        p.setStatus(Project.Status.COMPLETED);
        p.setCompletedTime(5678L);

        YamlConfiguration y = ProjectStorage.toYaml(p);
        Project r = ProjectStorage.fromYaml(y);

        assertEquals("Minas Tirith", r.getName());
        assertEquals("Rebuild the citadel", r.getDescription());
        assertEquals("Reach the seventh level", r.getGoal());
        assertEquals(lead, r.getLead());
        assertEquals(Arrays.asList(m1, m2), r.getManagers());
        assertEquals(Project.Status.COMPLETED, r.getStatus());
        assertEquals(1234L, r.getCreatedTime());
        assertEquals(5678L, r.getCompletedTime());
    }

    @Test
    void unknownStatusDefaultsToActive() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("name", "P");
        y.set("status", "NONSENSE");
        Project r = ProjectStorage.fromYaml(y);
        assertEquals(Project.Status.ACTIVE, r.getStatus());
    }

    @Test
    void nullLeadSurvives() {
        Project p = new Project("P", null, 0L);
        Project r = ProjectStorage.fromYaml(ProjectStorage.toYaml(p));
        assertNull(r.getLead());
    }
}
