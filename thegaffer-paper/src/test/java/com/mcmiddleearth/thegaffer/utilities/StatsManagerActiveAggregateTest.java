package com.mcmiddleearth.thegaffer.utilities;
import com.mcmiddleearth.thegaffer.storage.JobStats;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
class StatsManagerActiveAggregateTest {
    @Test void ingestFoldsActiveBuildMillis() {
        StatsManager.reset();
        JobStats s = new JobStats("j", null, "nothing", "world", 0, 0, 10, 0L, 1000L);
        java.util.UUID u = java.util.UUID.randomUUID();
        s.setBuilderStat(u, 5, 2, 30_000L);   // 4-arg overload from Task 2: placed, broke, activeMillis
        StatsManager.ingest(s);
        assertEquals(30_000L, StatsManager.getPlayerTotals(u).getActiveBuildMillis());
    }
}
