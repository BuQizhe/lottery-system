package com.example.lotterysystem.service;

import com.example.lotterysystem.entity.*;
import com.example.lotterysystem.mapper.*;
import com.example.lotterysystem.utils.QRCodeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 抽奖核心服务
 * <p>
 * 实现"累计概率区间"抽奖算法：
 * <ol>
 *   <li>从奖品池加载所有配置（优先 Redis 缓存）</li>
 *   <li>计算总权重 totalProb，生成 [0, totalProb) 间随机数</li>
 *   <li>按配置顺序累加各奖品权重，随机数落在哪个区间就中哪个奖品</li>
 *   <li>原子扣减库存（SQL UPDATE WHERE remaining > 0），防止超发</li>
 *   <li>生成核销码 + 二维码，记录中奖信息</li>
 *   <li>异步更新抽奖计数（Redis 实时 + DB 异步持久化）</li>
 * </ol>
 * 支持 Redis 降级：Redis 不可用时自动回退到纯 DB 模式，不影响核心抽奖功能。
 */
@Service
public class LotteryService {

    private static final Logger log = LoggerFactory.getLogger(LotteryService.class);

    @Autowired private PrizePoolMapper prizePoolMapper;
    @Autowired private WinningRecordMapper winningRecordMapper;
    @Autowired private PrizeMapper prizeMapper;
    @Autowired private MailService mailService;
    @Autowired private UserService userService;
    @Autowired private ActivityMapper activityMapper;
    @Autowired private ActivityLimitConfigMapper activityLimitConfigMapper;
    @Autowired private UserDrawRecordMapper userDrawRecordMapper;
    @Autowired private AsyncTaskService asyncTaskService;
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    private final Random random = new Random();

    // ==================== Redis 工具方法 ====================

    // Redis 可用状态标记（volatile 保证多线程可见性）
    // 不再每次操作都 ping()，而是捕获异常后标记不可用
    private volatile boolean redisAvailable = true;

    /** 判断 Redis 是否可用，不可用时自动降级到纯 DB 模式 */
    private boolean isRedisAvailable() {
        if (redisTemplate == null) return false;
        if (!redisAvailable) return false;  // 之前已经判定不可用，直接跳过
        return true;
    }

    private void cachePrizePool(Integer activityId, List<PrizePool> pools) {
        if (!isRedisAvailable()) return;
        try {
            String key = "lottery:prize_pool:" + activityId;
            redisTemplate.opsForValue().set(key, pools, 1, TimeUnit.HOURS);
        } catch (Exception e) {
            redisAvailable = false;
            log.warn("Redis 缓存奖品池失败，已降级: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<PrizePool> getCachedPrizePool(Integer activityId) {
        if (!isRedisAvailable()) return null;
        try {
            String key = "lottery:prize_pool:" + activityId;
            return (List<PrizePool>) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            redisAvailable = false;
            log.warn("Redis 读取奖品池失败，已降级: {}", e.getMessage());
            return null;
        }
    }

    public void evictPrizePool(Integer activityId) {
        if (!isRedisAvailable()) return;
        try {
            redisTemplate.delete("lottery:prize_pool:" + activityId);
        } catch (Exception e) {
            redisAvailable = false;
        }
    }

    private Long getTodayDrawCountFromRedis(Integer userId, Integer activityId) {
        if (!isRedisAvailable()) return null;
        try {
            String key = "lottery:draw_count:" + activityId + ":" + userId + ":" + LocalDate.now();
            Object val = redisTemplate.opsForValue().get(key);
            return val == null ? null : ((Number) val).longValue();
        } catch (Exception e) {
            redisAvailable = false;
            return null;
        }
    }

    private void incrementTodayDrawCount(Integer userId, Integer activityId) {
        if (!isRedisAvailable()) return;
        try {
            String key = "lottery:draw_count:" + activityId + ":" + userId + ":" + LocalDate.now();
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, 1, TimeUnit.DAYS);
        } catch (Exception e) {
            redisAvailable = false;
        }
    }

    // ==================== 核心抽奖方法 ====================

    /**
     * 执行一次抽奖
     *
     * @param userId     用户ID
     * @param activityId 活动ID
     * @return 中奖奖品（未中奖返回 null）
     */
    public Prize draw(Integer userId, Integer activityId) {
        log.info("抽奖开始: userId={}, activityId={}", userId, activityId);

        // 1. 活动有效性校验
        Activity activity = activityMapper.findById(activityId);
        if (activity == null || activity.getStatus() != 1) {
            log.warn("活动无效: {}", activityId);
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        if (activity.getStartTime() != null && now.isBefore(activity.getStartTime())) {
            log.warn("活动尚未开始");
            return null;
        }
        if (activity.getEndTime() != null && now.isAfter(activity.getEndTime())) {
            log.warn("活动已结束");
            return null;
        }

        // 2. 抽奖次数限制检查
        ActivityLimitConfig limitConfig = activityLimitConfigMapper.findByActivityId(activityId);
        if (limitConfig == null) {
            limitConfig = new ActivityLimitConfig();
            limitConfig.setActivityId(activityId);
            limitConfig.setDailyLimit(0);
            limitConfig.setTotalLimit(0);
            activityLimitConfigMapper.insert(limitConfig);
        }

        // 今日次数（优先 Redis，回退 DB）
        Long todayCount = getTodayDrawCountFromRedis(userId, activityId);
        if (todayCount == null) {
            UserDrawRecord record = userDrawRecordMapper.findByUserAndActivityAndDate(
                    userId, activityId, LocalDate.now());
            todayCount = record != null ? (long) record.getDrawCount() : 0L;
        }
        if (limitConfig.getDailyLimit() > 0 && todayCount >= limitConfig.getDailyLimit()) {
            log.info("今日抽奖次数已达上限: userId={}, todayCount={}", userId, todayCount);
            return null;
        }
        Integer totalCount = userDrawRecordMapper.getTotalDrawCount(userId, activityId);
        if (totalCount == null) totalCount = 0;
        if (limitConfig.getTotalLimit() > 0 && totalCount >= limitConfig.getTotalLimit()) {
            log.info("总抽奖次数已达上限: userId={}, totalCount={}", userId, totalCount);
            return null;
        }

        // 3. 加载奖品池（优先 Redis 缓存，首次从 DB 加载并缓存）
        List<PrizePool> prizePools = getCachedPrizePool(activityId);
        if (prizePools == null) {
            prizePools = prizePoolMapper.findByActivityId(activityId);
            if (prizePools != null && !prizePools.isEmpty()) {
                cachePrizePool(activityId, prizePools);
            }
        }
        if (prizePools == null || prizePools.isEmpty()) {
            log.warn("奖品池为空，无法抽奖");
            return null;
        }

        // 4. 累计概率区间抽奖算法
        double totalProb = prizePools.stream()
                .mapToDouble(p -> p.getProbability().doubleValue()).sum();
        if (totalProb <= 0) {
            log.warn("奖品池总概率为0");
            return null;
        }

        double rand = random.nextDouble() * totalProb;
        double cumulative = 0.0;
        for (PrizePool pool : prizePools) {
            cumulative += pool.getProbability().doubleValue();
            if (rand <= cumulative) {
                Prize prize = prizeMapper.findById(pool.getPrizeId());
                if (prize == null || prize.getRemaining() <= 0) break;

                // 原子扣减库存（UPDATE SET remaining = remaining - 1 WHERE remaining > 0）
                int affected = prizeMapper.decreaseRemaining(prize.getId());
                if (affected == 0) {
                    log.warn("库存扣减失败（并发冲突）: prizeId={}", prize.getId());
                    break;
                }

                // 生成核销码和二维码
                String claimCode = QRCodeUtil.generateClaimCode();
                String qrContent = "http://localhost:8080/claim.html?code=" + claimCode;
                String qrBase64 = QRCodeUtil.generateQRCodeBase64(qrContent);

                WinningRecord record = new WinningRecord();
                record.setUserId(userId);
                record.setActivityId(activityId);
                record.setPrizeId(prize.getId());
                record.setPrizeName(prize.getName());
                record.setWinningCode(System.currentTimeMillis() + "" + random.nextInt(10000));
                record.setClaimCode(claimCode);
                record.setQrCode(qrBase64);
                record.setStatus(0);
                record.setDrawTime(LocalDateTime.now());
                winningRecordMapper.insert(record);

                // 更新奖品池的中奖计数（之前这个方法存在但从未调用）
                try {
                    prizePoolMapper.incrementWinCount(pool.getId());
                } catch (Exception e) {
                    log.warn("更新奖品池中奖计数失败: poolId={}", pool.getId(), e);
                }

                // 异步发送中奖邮件（不阻塞主流程）
                User user = userService.findById(userId);
                if (user != null && user.getEmail() != null) {
                    try {
                        mailService.sendWinningNotification(
                                user.getEmail(), user.getName(), prize.getName(), activity.getName());
                    } catch (Exception e) {
                        log.error("中奖邮件发送失败", e);
                    }
                }

                // 增加计数（Redis 实时 + @Async 持久化到 DB）
                incrementTodayDrawCount(userId, activityId);
                asyncTaskService.updateDrawCount(userId, activityId);

                log.info("中奖: userId={}, prize={}", userId, prize.getName());
                return prize;
            }
        }

        // 未中奖也增加计数
        incrementTodayDrawCount(userId, activityId);
        asyncTaskService.updateDrawCount(userId, activityId);
        log.debug("未中奖: userId={}", userId);
        return null;
    }

    // ==================== 查询方法 ====================

    /** 查询用户今日剩余抽奖次数（-1=不限） */
    public int getTodayRemainCount(Integer userId, Integer activityId) {
        ActivityLimitConfig config = activityLimitConfigMapper.findByActivityId(activityId);
        if (config == null || config.getDailyLimit() == 0) return -1;
        Long today = getTodayDrawCountFromRedis(userId, activityId);
        if (today == null) {
            UserDrawRecord record = userDrawRecordMapper.findByUserAndActivityAndDate(
                    userId, activityId, LocalDate.now());
            today = record != null ? (long) record.getDrawCount() : 0L;
        }
        return Math.max(0, config.getDailyLimit() - today.intValue());
    }

    /** 查询用户活动期间剩余抽奖次数（-1=不限） */
    public int getTotalRemainCount(Integer userId, Integer activityId) {
        ActivityLimitConfig config = activityLimitConfigMapper.findByActivityId(activityId);
        if (config == null || config.getTotalLimit() == 0) return -1;
        Integer total = userDrawRecordMapper.getTotalDrawCount(userId, activityId);
        return Math.max(0, config.getTotalLimit() - (total == null ? 0 : total));
    }

    /** 查询用户所有中奖记录 */
    public List<WinningRecord> getUserWinningRecords(Integer userId) {
        return winningRecordMapper.findByUserId(userId);
    }

    /** 查询用户中奖记录（含图片） */
    public List<WinningRecord> getUserWinningRecordsWithImage(Integer userId) {
        return winningRecordMapper.findUserRecordsWithImage(userId);
    }
}
