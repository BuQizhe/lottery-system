package com.example.lotterysystem.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lottery probability algorithm test
 * Tests the cumulative probability interval algorithm for distribution correctness.
 * Pure unit test, no Spring context needed.
 */
class LotteryAlgorithmTest {

    /**
     * Cumulative probability interval draw algorithm
     * (extracted from LotteryService.draw() for isolated testing)
     */
    private String drawPrize(List<PrizePoolEntry> pools, Random random) {
        double totalProb = pools.stream().mapToDouble(p -> p.probability).sum();
        if (totalProb <= 0) return null;

        double rand = random.nextDouble() * totalProb;
        double cumulative = 0.0;
        for (PrizePoolEntry pool : pools) {
            cumulative += pool.probability;
            if (rand <= cumulative) {
                return pool.name;
            }
        }
        return null;
    }

    // ==================== Test Cases ====================

    @Test
    @DisplayName("Probability distribution: 10000 draws, actual frequency within +/-3% of expected")
    void probabilityDistribution_shouldMatchExpected() {
        // Given: 3 prizes with 30%, 50%, 20% probability
        List<PrizePoolEntry> pools = List.of(
                new PrizePoolEntry("Prize A", 0.30),
                new PrizePoolEntry("Prize B", 0.50),
                new PrizePoolEntry("Prize C", 0.20)
        );

        // When: draw 10000 times
        Map<String, Integer> counts = new HashMap<>();
        int total = 10000;
        Random random = new Random(42); // fixed seed for reproducibility
        for (int i = 0; i < total; i++) {
            String prize = drawPrize(pools, random);
            counts.merge(prize, 1, Integer::sum);
        }

        // Then: actual frequency within +/-3% of theoretical probability
        double tolerance = 0.03;
        assertEquals(total, counts.values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(0.30, (double) counts.get("Prize A") / total, tolerance, "Prize A frequency deviation too large");
        assertEquals(0.50, (double) counts.get("Prize B") / total, tolerance, "Prize B frequency deviation too large");
        assertEquals(0.20, (double) counts.get("Prize C") / total, tolerance, "Prize C frequency deviation too large");
    }

    @Test
    @DisplayName("Empty prize pool returns null")
    void drawPrize_emptyPool_shouldReturnNull() {
        List<PrizePoolEntry> emptyPools = Collections.emptyList();
        assertNull(drawPrize(emptyPools, new Random()));
    }

    @Test
    @DisplayName("Single prize pool always returns the same prize")
    void drawPrize_singlePool_shouldAlwaysReturnSame() {
        List<PrizePoolEntry> pools = List.of(
                new PrizePoolEntry("Only Prize", 1.0)
        );

        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            assertEquals("Only Prize", drawPrize(pools, random));
        }
    }

    @Test
    @DisplayName("Zero total probability returns null")
    void drawPrize_zeroProbability_shouldReturnNull() {
        List<PrizePoolEntry> pools = List.of(
                new PrizePoolEntry("Prize X", 0.0),
                new PrizePoolEntry("Prize Y", 0.0)
        );
        assertNull(drawPrize(pools, new Random()));
    }

    @Test
    @DisplayName("Many equal-probability prizes distribute evenly")
    void drawPrize_manyPrizes_shouldDistributeCorrectly() {
        int n = 10;
        List<PrizePoolEntry> pools = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            pools.add(new PrizePoolEntry("Prize_" + i, 1.0));
        }

        Map<String, Integer> counts = new HashMap<>();
        int total = 10000;
        Random random = new Random(42);
        for (int i = 0; i < total; i++) {
            String prize = drawPrize(pools, random);
            counts.merge(prize, 1, Integer::sum);
        }

        // Each prize should be ~10% (+/-3%)
        double tolerance = 0.03;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            assertEquals(0.10, (double) entry.getValue() / total, tolerance,
                    entry.getKey() + " frequency deviation too large");
        }
    }

    /** Simplified prize pool entry for pure algorithm testing */
    static class PrizePoolEntry {
        String name;
        double probability;
        PrizePoolEntry(String name, double probability) {
            this.name = name;
            this.probability = probability;
        }
    }
}
