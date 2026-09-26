package com.mcmiddleearth.thegaffer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Pure-logic tests for the job-creation rules. These exercise the decision helpers
 * that back the {@code /createjob} Dialog without standing up a server — the live
 * {@code create()} path (filesystem + JobDatabase + event firing) is covered by
 * in-game QA.
 */
class JobCreationServiceTest {

    @Test
    void normalizeNameReplacesSpacesWithUnderscores() {
        assertEquals("My_Build_Job", JobCreationService.normalizeName("My Build Job"));
    }

    @Test
    void normalizeNameTrimsSurroundingWhitespace() {
        assertEquals("job", JobCreationService.normalizeName("  job  "));
    }

    @Test
    void normalizeNameLeavesAlreadyCleanNamesUntouched() {
        assertEquals("already_clean", JobCreationService.normalizeName("already_clean"));
    }

    @Test
    void normalizeNameTreatsNullAsEmpty() {
        assertEquals("", JobCreationService.normalizeName(null));
    }

    @Test
    void clampRadiusRaisesBelowMinimumToOne() {
        assertEquals(1, JobCreationService.clampRadius(0));
        assertEquals(1, JobCreationService.clampRadius(-25));
    }

    @Test
    void clampRadiusCapsAboveMaximumAtOneThousand() {
        assertEquals(1000, JobCreationService.clampRadius(1001));
        assertEquals(1000, JobCreationService.clampRadius(50000));
    }

    @Test
    void clampRadiusLeavesInRangeValuesUntouched() {
        assertEquals(1, JobCreationService.clampRadius(1));
        assertEquals(50, JobCreationService.clampRadius(50));
        assertEquals(1000, JobCreationService.clampRadius(1000));
    }

    @Test
    void resolveFreeNameReturnsBaseWhenFree() {
        assertEquals("Wall", JobCreationService.resolveFreeName("Wall", n -> false));
    }

    @Test
    void resolveFreeNameAppendsTwoWhenBaseTaken() {
        Set<String> taken = Set.of("Wall");
        assertEquals("Wall-2", JobCreationService.resolveFreeName("Wall", taken::contains));
    }

    @Test
    void resolveFreeNameSkipsTakenSuffixes() {
        Set<String> taken = Set.of("Wall", "Wall-2", "Wall-3");
        assertEquals("Wall-4", JobCreationService.resolveFreeName("Wall", taken::contains));
    }

    @Test
    void resolveFreeNameFillsTheFirstGap() {
        // base and -2 taken, -3 free → -3 (smallest free suffix, not the largest+1).
        Set<String> taken = Set.of("Wall", "Wall-2");
        assertEquals("Wall-3", JobCreationService.resolveFreeName("Wall", taken::contains));
    }

    @Test
    void resolveFreeNameReturnsBlankBaseUnchanged() {
        assertEquals("", JobCreationService.resolveFreeName("", n -> true));
    }
}
