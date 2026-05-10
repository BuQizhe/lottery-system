package com.example.lotterysystem.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户每日抽奖记录
 * <p>
 * 用于追踪每个用户在每个活动中每天的抽奖次数。
 * 与 {@code ActivityLimitConfig.dailyLimit} 配合实现每日次数限制。
 * Redis 中另有一份实时计数器，数据库记录为异步持久化的最终结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDrawRecord {
    private Integer id;
    private Integer userId;
    private Integer activityId;
    /** 抽奖日期 */
    private LocalDate drawDate;
    /** 该日抽奖次数 */
    private Integer drawCount;
    private LocalDateTime updateTime;
}
