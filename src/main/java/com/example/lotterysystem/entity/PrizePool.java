package com.example.lotterysystem.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 奖品池配置（活动-奖品关联）
 * <p>
 * 每个活动独立配置奖品池，支持：
 * <ul>
 *   <li>probability — 中奖概率（权重值，非百分比，总和无需等于1）</li>
 *   <li>dailyLimit / totalLimit — 单个奖品每日/总体的中奖上限</li>
 *   <li>winCount — 已中奖计数，用于上限控制</li>
 * </ul>
 * 抽奖算法：累计概率区间法，按 probability 加权随机，落在哪个区间就中哪个奖品。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrizePool {
    private Integer id;
    private Integer activityId;
    private Integer prizeId;
    /** 中奖权重（累加概率区间算法） */
    private BigDecimal probability;
    /** 该奖品每日中奖上限（0=不限） */
    private Integer dailyLimit;
    /** 该奖品总中奖上限（0=不限） */
    private Integer totalLimit;
    /** 已中奖次数 */
    private Integer winCount;
    /** 奖品名称（JOIN 查询填充，非数据库字段） */
    private String prizeName;
}
