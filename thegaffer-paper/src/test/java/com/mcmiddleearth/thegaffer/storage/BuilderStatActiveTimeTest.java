package com.mcmiddleearth.thegaffer.storage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
class BuilderStatActiveTimeTest {
    private static final long THRESH = 60_000L;
    @Test void firstEventAddsNothing() {
        JobStats.BuilderStat b = new JobStats.BuilderStat();
        b.recordActivity(1_000L, THRESH);
        assertEquals(0L, b.getActiveMillis());
    }
    @Test void gapWithinThresholdAccrues() {
        JobStats.BuilderStat b = new JobStats.BuilderStat();
        b.recordActivity(1_000L, THRESH); b.recordActivity(6_000L, THRESH); b.recordActivity(9_000L, THRESH);
        assertEquals(8_000L, b.getActiveMillis());
    }
    @Test void gapOverThresholdDoesNotAccrue() {
        JobStats.BuilderStat b = new JobStats.BuilderStat();
        b.recordActivity(1_000L, THRESH); b.recordActivity(200_000L, THRESH); b.recordActivity(203_000L, THRESH);
        assertEquals(3_000L, b.getActiveMillis());
    }
}
