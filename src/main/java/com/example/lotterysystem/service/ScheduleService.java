package com.example.lotterysystem.service;

import com.example.lotterysystem.entity.Activity;
import com.example.lotterysystem.mapper.ActivityMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务服务
 * <p>
 * 负责活动状态的定时检测和过期数据清理。
 * 使用 Spring @Scheduled + cron 表达式驱动。
 */
@Service
@EnableScheduling
public class ScheduleService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleService.class);

    @Autowired
    private ActivityMapper activityMapper;

    /**
     * 每小时检查一次活动状态
     * <p>
     * 自动结束已过期的活动（status 1→2）
     * 自动开启到达起始时间的活动（status 0→1）
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void checkActivityStatus() {
        log.info("定时任务: 检查活动状态");
        try {
            List<Activity> activities = activityMapper.findAll();
            LocalDateTime now = LocalDateTime.now();
            int updated = 0;
            for (Activity act : activities) {
                boolean needUpdate = false;
                if (act.getStatus() == 1 && act.getEndTime() != null && now.isAfter(act.getEndTime())) {
                    act.setStatus(2);  // 自动结束
                    needUpdate = true;
                    log.info("活动「{}」已自动结束", act.getName());
                }
                if (act.getStatus() == 0 && act.getStartTime() != null
                        && now.isAfter(act.getStartTime())
                        && (act.getEndTime() == null || now.isBefore(act.getEndTime()))) {
                    act.setStatus(1);  // 自动开始
                    needUpdate = true;
                    log.info("活动「{}」已自动开始", act.getName());
                }
                if (needUpdate) {
                    activityMapper.update(act);
                    updated++;
                }
            }
            log.info("活动状态检查完成，更新 {} 个活动", updated);
        } catch (Exception e) {
            log.error("活动状态检查失败", e);
        }
    }

    /** 每天凌晨清理过期验证码 */
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanExpiredData() {
        log.info("定时任务: 清理过期数据");
        // 未来可添加：DELETE FROM verification_code WHERE expire_time < NOW()
    }
}
