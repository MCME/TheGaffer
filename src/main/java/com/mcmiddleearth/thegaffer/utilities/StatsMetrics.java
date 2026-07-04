package com.mcmiddleearth.thegaffer.utilities;

import java.util.*;

public final class StatsMetrics {
    private StatsMetrics() {}

    public static int longestStreak(Set<Integer> days) {
        if (days.isEmpty()) return 0;
        List<Integer> s = new ArrayList<>(days); Collections.sort(s);
        int best = 1, run = 1;
        for (int i = 1; i < s.size(); i++) {
            run = (s.get(i) == s.get(i - 1) + 1) ? run + 1 : 1;
            best = Math.max(best, run);
        }
        return best;
    }

    public static int currentStreak(Set<Integer> days, int today) {
        int n = 0, d = today;
        while (days.contains(d)) { n++; d--; }
        if (n == 0 && days.contains(today - 1)) { d = today - 1; while (days.contains(d)) { n++; d--; } }
        return n;
    }

    public static String tierForPlaced(long placed) {
        if (placed >= 1_000_000) return "Grandmaster";
        if (placed >= 250_000)  return "Master";
        if (placed >= 50_000)   return "Craftsman";
        if (placed >= 10_000)   return "Journeyman";
        return "Apprentice";
    }

    public static List<String> milestonesForPlaced(long placed) {
        List<String> out = new ArrayList<>();
        for (long t : new long[]{1_000, 10_000, 100_000, 1_000_000}) if (placed >= t) out.add("blocks_" + t);
        return out;
    }
}
