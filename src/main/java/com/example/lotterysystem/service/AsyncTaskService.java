package com.example.lotterysystem.service;

import com.example.lotterysystem.mapper.UserDrawRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 异步任务服务 — 非关键路径操作异步执行
 *
 * 修复了并发竞态条件：
 * 之前用 check-then-insert（先查再插），两个并发请求可能同时查到 null 然后都插入
 * 现在用 INSERT ... ON DUPLICATE KEY UPDATE，数据库层面保证原子性
 */
@Service
public class AsyncTaskService {

    private static final Logger log = LoggerFactory.getLogger(AsyncTaskService.class);

    @Autowired
    private UserDrawRecordMapper userDrawRecordMapper;

    /**
     * 异步更新用户每日抽奖计数
     * 使用原子的 upsert 操作，不存在并发竞态
     */
    @Async("taskExecutor")
    public void updateDrawCount(Integer userId, Integer activityId) {
        try {
            // 原子操作：记录不存在则插入（count=1），已存在则 draw_count + 1
            userDrawRecordMapper.incrementOrInsert(userId, activityId, LocalDate.now());
            log.debug("异步更新抽奖计数成功: userId={}, activityId={}", userId, activityId);
        } catch (Exception e) {
            log.error("异步更新抽奖计数失败: userId={}, activityId={}", userId, activityId, e);
        }
    }
}
