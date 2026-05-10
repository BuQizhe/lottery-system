package com.example.lotterysystem.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 抽奖活动实体
 * <p>
 * status 取值：
 * <ul>
 *   <li>0 — 未开始</li>
 *   <li>1 — 进行中</li>
 *   <li>2 — 已结束</li>
 * </ul>
 * 定时任务 {@code ScheduleService} 每小时检测时间范围，自动切换状态。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Activity {
    private Integer id;
    private String name;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /** 状态：0-未开始, 1-进行中, 2-已结束 */
    private Integer status;
    private LocalDateTime createTime;
    /** 前端展示用的状态文本，非数据库字段 */
    private String statusText;
}
