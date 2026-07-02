package com.mcmiddleearth.thegaffer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mcmiddleearth.thegaffer.utilities.JobCreationService.Outcome;
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
    void validateNameRejectsEmptyName() {
        assertEquals(Outcome.EMPTY_NAME, JobCreationService.validateName("", false, false));
        assertEquals(Outcome.EMPTY_NAME, JobCreationService.validateName(null, false, false));
    }

    @Test
    void validateNameRejectsHistoricalName() {
        assertEquals(Outcome.NAME_TAKEN_HISTORY, JobCreationService.validateName("old", true, false));
    }

    @Test
    void validateNameRunningTakesPrecedenceOverHistory() {
        // An active job always has a saved file too, so the clearer "already running" wins.
        assertEquals(Outcome.NAME_RUNNING, JobCreationService.validateName("live", true, true));
    }

    @Test
    void validateNameRejectsRunningName() {
        assertEquals(Outcome.NAME_RUNNING, JobCreationService.validateName("live", false, true));
    }

    @Test
    void validateNameAcceptsFreshName() {
        assertEquals(Outcome.OK, JobCreationService.validateName("brand_new", false, false));
    }
}
