package com.mcmiddleearth.thegaffer.storage;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ProjectModelTest {

    @Test
    void canonicalIsCaseAndWhitespaceInsensitive() {
        assertEquals(Project.canonical("Minas Tirith"), Project.canonical("  minas tirith "));
        assertEquals("minas tirith", Project.canonical("Minas Tirith"));
        assertEquals("", Project.canonical(null));
    }

    @Test
    void ownershipPredicates() {
        UUID lead = UUID.randomUUID();
        UUID mgr = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        Project p = new Project("Minas Tirith", lead, 1000L);
        p.addManager(mgr);

        assertTrue(p.isLead(lead));
        assertFalse(p.isLead(mgr));
        assertTrue(p.isManager(mgr));
        assertTrue(p.canManage(lead));
        assertTrue(p.canManage(mgr));
        assertFalse(p.canManage(other));
        assertFalse(p.canManage(null));
    }

    @Test
    void addManagerIsIdempotentAndRemovable() {
        UUID mgr = UUID.randomUUID();
        Project p = new Project("P", UUID.randomUUID(), 0L);
        p.addManager(mgr);
        p.addManager(mgr);
        assertEquals(1, p.getManagers().size());
        p.removeManager(mgr);
        assertTrue(p.getManagers().isEmpty());
    }

    @org.junit.jupiter.api.Test
    void dirtyFlagTracksPersistentMutations() {
        Project p = new Project("P", java.util.UUID.randomUUID(), 0L);
        org.junit.jupiter.api.Assertions.assertFalse(p.isDirty(), "fresh project should not be dirty");
        p.setDescription("x");
        org.junit.jupiter.api.Assertions.assertTrue(p.isDirty(), "setDescription should mark dirty");
        p.setDirty(false);
        p.setCreatedTime(5L);
        org.junit.jupiter.api.Assertions.assertFalse(p.isDirty(), "setCreatedTime should not mark dirty");
        java.util.UUID m = java.util.UUID.randomUUID();
        p.addManager(m);
        org.junit.jupiter.api.Assertions.assertTrue(p.isDirty(), "addManager should mark dirty");
        p.setDirty(false);
        p.removeManager(m);
        org.junit.jupiter.api.Assertions.assertTrue(p.isDirty(), "removeManager should mark dirty");
    }
}
