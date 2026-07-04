package com.mcmiddleearth.thegaffer.utilities;

import com.mcmiddleearth.thegaffer.storage.JobStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatsManagerFoldTest {

    private final UUID player = UUID.randomUUID();
    private final UUID coPlayer = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        StatsManager.reset();
    }

    @Test
    void activeDaysCountsDistinctDaysAndCoBuilderExcludesSelf() {
        // Job 1: endTime = day 10 (10 * 86_400_000L), both player and coPlayer participate
        long endTime1 = 10L * 86_400_000L;
        JobStats s1 = new JobStats("job1", player, "proj", "world", 0, 0, 10,
                endTime1 - 3_600_000L, endTime1);
        s1.addParticipant(player);
        s1.addParticipant(coPlayer);
        s1.recordPlace(player, 5);

        // Job 2: endTime = day 12 (12 * 86_400_000L), same two participants
        long endTime2 = 12L * 86_400_000L;
        JobStats s2 = new JobStats("job2", player, "proj", "world", 0, 0, 10,
                endTime2 - 3_600_000L, endTime2);
        s2.addParticipant(player);
        s2.addParticipant(coPlayer);
        s2.recordPlace(player, 3);

        StatsManager.ingest(s1);
        StatsManager.ingest(s2);

        StatsManager.PlayerAggregate agg = StatsManager.getPlayerTotals(player);

        // Two distinct end-days (10 and 12)
        assertEquals(2, agg.getActiveDays().size());

        // coBuilders for player should contain coPlayer but NOT player itself
        assertFalse(agg.getCoBuilders().contains(player), "coBuilders must not contain the player itself");
        assertTrue(agg.getCoBuilders().contains(coPlayer), "coBuilders must contain the co-participant");
        assertEquals(1, agg.getCoBuilders().size());
    }
}
