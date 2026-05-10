package com.example.lotterysystem.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 活动抽奖限制配置
 * <p>
 * 每个活动可独立配置：
 * <ul>
 *   <li>dailyLimit — 每人每日最大抽奖次数（0=不限，依靠 Redis 计数器实现）</li>
 *   <li>totalLimit — 每人活动期间最大抽奖次数（0=不限）</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLimitConfig {
    private Integer id;
    private Integer activityId;
    /** 每人每日抽奖上限（0=不限） */
    private Integer dailyLimit;
    /** 每人活动期间总抽奖上限（0=不限） */
    private Integer totalLimit;
    private LocalDateTime updateTime;
    /** 活动名称（JOIN 查询填充） */
    private String activityName;
}
