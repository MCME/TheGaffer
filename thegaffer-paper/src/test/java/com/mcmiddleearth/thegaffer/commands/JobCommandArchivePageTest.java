package com.mcmiddleearth.thegaffer.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit tests for {@link JobCommand#archivePageCount(int)} — no Bukkit needed.
 */
class JobCommandArchivePageTest {

    @Test
    void zeroJobsReturnsOnePage() {
        assertEquals(1, JobCommand.archivePageCount(0));
    }

    @Test
    void negativeCountReturnsOnePage() {
        assertEquals(1, JobCommand.archivePageCount(-5));
    }

    @Test
    void exactlyEightJobsIsOnePage() {
        assertEquals(1, JobCommand.archivePageCount(8));
    }

    @Test
    void nineJobsIsSecondPage() {
        assertEquals(2, JobCommand.archivePageCount(9));
    }

    @Test
    void sixteenJobsIsTwoPages() {
        assertEquals(2, JobCommand.archivePageCount(16));
    }

    @Test
    void seventeenJobsIsThreePages() {
        assertEquals(3, JobCommand.archivePageCount(17));
    }

    @Test
    void oneJobIsOnePage() {
        assertEquals(1, JobCommand.archivePageCount(1));
    }
}
