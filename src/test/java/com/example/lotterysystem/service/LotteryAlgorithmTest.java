package com.example.lotterysystem.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 抽奖概率算法测试
 * <p>
 * 测试核心逻辑：累计概率区间算法的分布正确性。
 * 不依赖 Spring 容器，纯单元测试。
 */
class LotteryAlgorithmTest {

    /** 累计概率区间抽奖算法（从 LotteryService.draw() 提取的纯计算部分） */
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

    // ==================== 测试用例 ====================

    @Test
    @DisplayName("概率分布验证：10000次抽奖，实际频率应在理论概率±3%内")
    void probabilityDistribution_shouldMatchExpected() {
        // Given: 3个奖品，概率分别为 30%, 50%, 20%
        List<PrizePoolEntry> pools = List.of(
                new PrizePoolEntry("一等奖", 0.30),
                new PrizePoolEntry("二等奖", 0.50),
                new PrizePoolEntry("三等奖", 0.20)
        );

        // When: 抽 10000 次
        Map<String, Integer> counts = new HashMap<>();
        int total = 10000;
        Random random = new Random(42); // 固定种子保证可复现
        for (int i = 0; i < total; i++) {
            String prize = drawPrize(pools, random);
            counts.merge(prize, 1, Integer::sum);
        }

        // Then: 实际频率应在理论概率±3%范围内
        double tolerance = 0.03;
        assertEquals(total, counts.values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(0.30, (double) counts.get("一等奖") / total, tolerance, "一等奖频率偏差过大");
        assertEquals(0.50, (double) counts.get("二等奖") / total, tolerance, "二等奖频率偏差过大");
        assertEquals(0.20, (double) counts.get("三等奖") / total, tolerance, "三等奖频率偏差过大");
    }

    @Test
    @DisplayName("空奖品池返回 null")
    void drawPrize_emptyPool_shouldReturnNull() {
        List<PrizePoolEntry> emptyPools = Collections.emptyList();
        assertNull(drawPrize(emptyPools, new Random()));
    }

    @Test
    @DisplayName("单奖品池始终中同一奖品")
    void drawPrize_singlePool_shouldAlwaysReturnSame() {
        List<PrizePoolEntry> pools = List.of(
                new PrizePoolEntry("唯一奖品", 1.0)
        );

        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            assertEquals("唯一奖品", drawPrize(pools, random));
        }
    }

    @Test
    @DisplayName("总概率为 0 时返回 null")
    void drawPrize_zeroProbability_shouldReturnNull() {
        List<PrizePoolEntry> pools = List.of(
                new PrizePoolEntry("奖品A", 0.0),
                new PrizePoolEntry("奖品B", 0.0)
        );
        assertNull(drawPrize(pools, new Random()));
    }

    @Test
    @DisplayName("大量奖品的分布验证")
    void drawPrize_manyPrizes_shouldDistributeCorrectly() {
        int n = 10;
        List<PrizePoolEntry> pools = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            pools.add(new PrizePoolEntry("奖品" + i, 1.0)); // 每个奖品概率相等
        }

        Map<String, Integer> counts = new HashMap<>();
        int total = 10000;
        Random random = new Random(42);
        for (int i = 0; i < total; i++) {
            String prize = drawPrize(pools, random);
            counts.merge(prize, 1, Integer::sum);
        }

        // 每个奖品应约 10%（±3%）
        double tolerance = 0.03;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            assertEquals(0.10, (double) entry.getValue() / total, tolerance,
                    entry.getKey() + " 频率偏差过大");
        }
    }

    /** 简化的奖品池条目（用于纯算法测试） */
    static class PrizePoolEntry {
        String name;
        double probability;
        PrizePoolEntry(String name, double probability) {
            this.name = name;
            this.probability = probability;
        }
    }
}
