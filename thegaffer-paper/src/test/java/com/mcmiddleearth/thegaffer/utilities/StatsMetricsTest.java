package com.mcmiddleearth.thegaffer.utilities;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
class StatsMetricsTest {
    @Test void longestStreakFindsConsecutiveRun() {
        assertEquals(3, StatsMetrics.longestStreak(Set.of(10, 11, 12, 20, 21)));
    }
    @Test void currentStreakCountsBackFromToday() {
        assertEquals(3, StatsMetrics.currentStreak(Set.of(98, 99, 100), 100));
    }
    @Test void tierByPlacedThresholds() {
        assertEquals("Apprentice", StatsMetrics.tierForPlaced(0));
        assertEquals("Journeyman", StatsMetrics.tierForPlaced(10_000));
        assertEquals("Grandmaster", StatsMetrics.tierForPlaced(2_000_000));
    }
}
