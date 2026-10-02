package com.mcmiddleearth.thegaffer.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which characters a project name may contain.
 *
 * <p>The underscore is the point of this test. Job names are full of them --
 * {@code JobCreationService.normalizeName} turns every space in a job name into one -- so a project
 * was unable to share the name of its own job.
 */
class ProjectNameCharactersTest {

    @Test
    void allowsTheUnderscoreThatJobNamesAreFullOf() {
        assertTrue(ProjectCommand.hasAllowedNameCharacters("Minas_Tirith"));
    }

    @Test
    void allowsSpacesHyphensAndApostrophes() {
        assertTrue(ProjectCommand.hasAllowedNameCharacters("Minas Tirith"));
        assertTrue(ProjectCommand.hasAllowedNameCharacters("Helm's Deep-2"));
    }

    @Test
    void rejectsPunctuationThatWouldConfuseTheCommandParser() {
        assertFalse(ProjectCommand.hasAllowedNameCharacters("Minas#Tirith"));
        assertFalse(ProjectCommand.hasAllowedNameCharacters("drop/table"));
    }

    @Test
    void rejectsAnEmptyName() {
        assertFalse(ProjectCommand.hasAllowedNameCharacters(""));
    }
}
