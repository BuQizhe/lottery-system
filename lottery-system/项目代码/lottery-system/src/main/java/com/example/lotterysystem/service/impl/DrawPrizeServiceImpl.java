package com.example.lotterysystem.service.impl;

import com.example.lotterysystem.common.errorcode.ServiceErrorCodeConstants;
import com.example.lotterysystem.common.exception.ServiceException;
import com.example.lotterysystem.common.utils.JacksonUtil;
import com.example.lotterysystem.common.utils.JWTUtil;
import com.example.lotterysystem.common.utils.RedisUtil;
import com.example.lotterysystem.controller.param.DrawPrizeParam;
import com.example.lotterysystem.controller.param.ShowWinningRecordsParam;
import com.example.lotterysystem.dao.dataobject.*;
import com.example.lotterysystem.dao.mapper.*;
import com.example.lotterysystem.service.DrawPrizeService;
import com.example.lotterysystem.service.dto.PrizeDTO;
import com.example.lotterysystem.service.dto.WinningRecordDTO;
import com.example.lotterysystem.service.enums.ActivityPrizeStatusEnum;
import com.example.lotterysystem.service.enums.ActivityPrizeTiersEnum;
import com.example.lotterysystem.service.enums.ActivityStatusEnum;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.example.lotterysystem.common.config.DirectRabbitConfig.EXCHANGE_NAME;
import static com.example.lotterysystem.common.config.DirectRabbitConfig.ROUTING;

@Service
public class DrawPrizeServiceImpl implements DrawPrizeService {

    private static final Logger logger = LoggerFactory.getLogger(DrawPrizeServiceImpl.class);

    private final String WINNING_RECORDS_PREFIX = "WINNING_RECORDS_";
    private final Long WINNING_RECORDS_TIMEOUT = 60 * 60 * 24 * 2L;

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private ActivityMapper activityMapper;
    @Autowired
    private ActivityPrizeMapper activityPrizeMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PrizeMapper prizeMapper;
    @Autowired
    private WinningRecordMapper winningRecordMapper;
    @Autowired
    private RedisUtil redisUtil;

    // ========== 原有方法 ==========

    @Override
    public void drawPrize(DrawPrizeParam param) {
        Map<String, String> map = new HashMap<>();
        map.put("messageId", String.valueOf(UUID.randomUUID()));
        map.put("messageData", JacksonUtil.writeValueAsString(param));
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, ROUTING, map);
        logger.info("mq消息发送成功：map={}", JacksonUtil.writeValueAsString(map));
    }

    @Override
    public Boolean checkDrawPrizeParam(DrawPrizeParam param) {
        ActivityDO activityDO = activityMapper.selectById(param.getActivityId());
        ActivityPrizeDO activityPrizeDO = activityPrizeMapper.selectByAPId(
                param.getActivityId(), param.getPrizeId());

        if (null == activityDO || null == activityPrizeDO) {
            logger.info("校验抽奖请求失败！失败原因：{}",
                    ServiceErrorCodeConstants.ACTIVITY_OR_PRIZE_IS_EMPTY.getMsg());
            return false;
        }

        if (activityDO.getStatus().equalsIgnoreCase(ActivityStatusEnum.COMPLETED.name())) {
            logger.info("校验抽奖请求失败！失败原因：{}",
                    ServiceErrorCodeConstants.ACTIVITY_COMPLETED.getMsg());
            return false;
        }

        if (activityPrizeDO.getStatus().equalsIgnoreCase(ActivityPrizeStatusEnum.COMPLETED.name())) {
            logger.info("校验抽奖请求失败！失败原因：{}",
                    ServiceErrorCodeConstants.ACTIVITY_PRIZE_COMPLETED.getMsg());
            return false;
        }

        if (activityPrizeDO.getPrizeAmount() != param.getWinnerList().size()) {
            logger.info("校验抽奖请求失败！失败原因：{}",
                    ServiceErrorCodeConstants.WINNER_PRIZE_AMOUNT_ERROR.getMsg());
            return false;
        }
        return true;
    }

    @Override
    public List<WinningRecordDO> saveWinnerRecords(DrawPrizeParam param) {
        ActivityDO activityDO = activityMapper.selectById(param.getActivityId());
        List<UserDO> userDOList = userMapper.batchSelectByIds(
                param.getWinnerList().stream()
                        .map(DrawPrizeParam.Winner::getUserId)
                        .collect(Collectors.toList()));
        PrizeDO prizeDO = prizeMapper.selectById(param.getPrizeId());
        ActivityPrizeDO activityPrizeDO = activityPrizeMapper.selectByAPId(param.getActivityId(), param.getPrizeId());

        List<WinningRecordDO> winningRecordDOList = userDOList.stream()
                .map(userDO -> {
                    WinningRecordDO winningRecordDO = new WinningRecordDO();
                    winningRecordDO.setActivityId(activityDO.getId());
                    winningRecordDO.setActivityName(activityDO.getActivityName());
                    winningRecordDO.setPrizeId(prizeDO.getId());
                    winningRecordDO.setPrizeName(prizeDO.getName());
                    winningRecordDO.setPrizeTier(activityPrizeDO.getPrizeTiers());
                    winningRecordDO.setWinnerId(userDO.getId());
                    winningRecordDO.setWinnerName(userDO.getUserName());
                    winningRecordDO.setWinnerEmail(userDO.getEmail());
                    winningRecordDO.setWinnerPhoneNumber(userDO.getPhoneNumber());
                    winningRecordDO.setWinningTime(param.getWinningTime());
                    return winningRecordDO;
                }).collect(Collectors.toList());
        winningRecordMapper.batchInsert(winningRecordDOList);

        cacheWinningRecords(param.getActivityId() + "_" + param.getPrizeId(),
                winningRecordDOList, WINNING_RECORDS_TIMEOUT);

        if (activityDO.getStatus().equalsIgnoreCase(ActivityStatusEnum.COMPLETED.name())) {
            List<WinningRecordDO> allList = winningRecordMapper.selectByActivityId(param.getActivityId());
            cacheWinningRecords(String.valueOf(param.getActivityId()), allList, WINNING_RECORDS_TIMEOUT);
        }

        return winningRecordDOList;
    }

    @Override
    public void deleteRecords(Long activityId, Long prizeId) {
        if (null == activityId) {
            logger.warn("要删除中奖记录相关的活动id为空！");
            return;
        }
        winningRecordMapper.deleteRecords(activityId, prizeId);
        if (null != prizeId) {
            deleteWinningRecords(activityId + "_" + prizeId);
        }
        deleteWinningRecords(String.valueOf(activityId));
    }

    @Override
    public List<WinningRecordDTO> getRecords(ShowWinningRecordsParam param) {
        String key = null == param.getPrizeId()
                ? String.valueOf(param.getActivityId())
                : param.getActivityId() + "_" + param.getPrizeId();
        List<WinningRecordDO> winningRecordDOList = getWinningRecords(key);
        if (!CollectionUtils.isEmpty(winningRecordDOList)) {
            return convertToWinningRecordDTOList(winningRecordDOList);
        }
        winningRecordDOList = winningRecordMapper.selectByActivityIdOrPrizeId(
                param.getActivityId(), param.getPrizeId());
        if (CollectionUtils.isEmpty(winningRecordDOList)) {
            logger.info("查询的中奖记录为空！param:{}", JacksonUtil.writeValueAsString(param));
            return Arrays.asList();
        }
        cacheWinningRecords(key, winningRecordDOList, WINNING_RECORDS_TIMEOUT);
        return convertToWinningRecordDTOList(winningRecordDOList);
    }

    // ========== 用户抽奖相关方法 ==========

    @Override
    public PrizeDTO userDraw(String token) {
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            throw new ServiceException(ServiceErrorCodeConstants.USER_NOT_LOGIN);
        }

        // 获取用户真实姓名并存储到 localStorage
        UserDO user = userMapper.selectById(userId);
        if (user != null && user.getUserName() != null) {
            // 这里可以通过其他方式返回用户名，或者在前端单独请求
        }

        List<PrizeDO> allPrizes = prizeMapper.selectPrizeList(0, 100);
        if (CollectionUtils.isEmpty(allPrizes)) {
            throw new ServiceException(ServiceErrorCodeConstants.NO_PRIZE);
        }

        List<PrizeDO> availablePrizes = allPrizes.stream()
                .filter(p -> p.getStock() != null && p.getStock() > 0)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(availablePrizes)) {
            throw new ServiceException(ServiceErrorCodeConstants.PRIZE_OUT_OF_STOCK);
        }

        PrizeDO winPrize = drawByProbability(availablePrizes);

        // 扣减库存
        boolean success = prizeMapper.decrementStock(winPrize.getId()) > 0;
        if (!success) {
            throw new ServiceException(ServiceErrorCodeConstants.PRIZE_OUT_OF_STOCK);
        }

        // 保存中奖记录（检查是否重复）
        saveUserWinningRecord(userId, winPrize);

        PrizeDTO prizeDTO = new PrizeDTO();
        prizeDTO.setPrizeId(winPrize.getId());
        prizeDTO.setName(winPrize.getName());
        prizeDTO.setDescription(winPrize.getDescription());
        prizeDTO.setImageUrl(winPrize.getImageUrl());
        prizeDTO.setPrice(winPrize.getPrice());
        prizeDTO.setProbability(winPrize.getProbability());
        prizeDTO.setStock(winPrize.getStock() - 1);

        logger.info("用户抽奖成功，userId: {}, prizeId: {}, prizeName: {}", userId, winPrize.getId(), winPrize.getName());
        return prizeDTO;
    }

    @Override
    public List<WinningRecordDTO> getMyRecords(String token) {
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            throw new ServiceException(ServiceErrorCodeConstants.USER_NOT_LOGIN);
        }

        List<WinningRecordDO> recordDOList = winningRecordMapper.selectByWinnerId(userId);
        if (CollectionUtils.isEmpty(recordDOList)) {
            return Arrays.asList();
        }

        return recordDOList.stream()
                .map(record -> {
                    WinningRecordDTO dto = new WinningRecordDTO();
                    dto.setWinnerId(record.getWinnerId());
                    dto.setWinnerName(record.getWinnerName());
                    dto.setPrizeName(record.getPrizeName());
                    dto.setPrizeTier(ActivityPrizeTiersEnum.forName(record.getPrizeTier()));
                    dto.setWinningTime(record.getWinningTime());
                    dto.setActivityName(record.getActivityName());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getUserStats(String token) {
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            throw new ServiceException(ServiceErrorCodeConstants.USER_NOT_LOGIN);
        }

        List<WinningRecordDO> records = winningRecordMapper.selectByWinnerId(userId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDraws", records.size());
        stats.put("totalWins", records.size());
        stats.put("winRate", records.isEmpty() ? 0 : 100.0);

        Map<String, Long> prizeCount = records.stream()
                .collect(Collectors.groupingBy(WinningRecordDO::getPrizeName, Collectors.counting()));
        stats.put("prizeCount", prizeCount);

        return stats;
    }

    // ========== 私有方法 ==========

    private List<WinningRecordDTO> convertToWinningRecordDTOList(List<WinningRecordDO> winningRecordDOList) {
        if (CollectionUtils.isEmpty(winningRecordDOList)) {
            return Arrays.asList();
        }
        return winningRecordDOList.stream()
                .map(winningRecordDO -> {
                    WinningRecordDTO winningRecordDTO = new WinningRecordDTO();
                    winningRecordDTO.setWinnerId(winningRecordDO.getWinnerId());
                    winningRecordDTO.setWinnerName(winningRecordDO.getWinnerName());
                    winningRecordDTO.setPrizeName(winningRecordDO.getPrizeName());
                    winningRecordDTO.setPrizeTier(
                            ActivityPrizeTiersEnum.forName(winningRecordDO.getPrizeTier()));
                    winningRecordDTO.setWinningTime(winningRecordDO.getWinningTime());
                    return winningRecordDTO;
                }).collect(Collectors.toList());
    }

    private void deleteWinningRecords(String key) {
        try {
            if (redisUtil.hasKey(WINNING_RECORDS_PREFIX + key)) {
                redisUtil.del(WINNING_RECORDS_PREFIX + key);
            }
        } catch (Exception e) {
            logger.error("删除中奖记录缓存异常，key:{}", key);
        }
    }

    private void cacheWinningRecords(String key, List<WinningRecordDO> winningRecordDOList, Long time) {
        String str = "";
        try {
            if (!StringUtils.hasText(key) || CollectionUtils.isEmpty(winningRecordDOList)) {
                logger.warn("要缓存的内容为空！key:{}, value:{}",
                        key, JacksonUtil.writeValueAsString(winningRecordDOList));
                return;
            }
            str = JacksonUtil.writeValueAsString(winningRecordDOList);
            redisUtil.set(WINNING_RECORDS_PREFIX + key, str, time);
        } catch (Exception e) {
            logger.error("缓存中奖记录异常！key:{}, value:{}", WINNING_RECORDS_PREFIX + key, str);
        }
    }

    private List<WinningRecordDO> getWinningRecords(String key) {
        try {
            if (!StringUtils.hasText(key)) {
                logger.warn("要从缓存中查询中奖记录的key为空！");
                return Arrays.asList();
            }
            String str = redisUtil.get(WINNING_RECORDS_PREFIX + key);
            if (!StringUtils.hasText(str)) {
                return Arrays.asList();
            }
            return JacksonUtil.readListValue(str, WinningRecordDO.class);
        } catch (Exception e) {
            logger.error("从缓存中查询中奖记录异常！key:{}", WINNING_RECORDS_PREFIX + key);
            return Arrays.asList();
        }
    }

    /**
     * 根据概率抽奖
     */
    private PrizeDO drawByProbability(List<PrizeDO> prizes) {
        double totalProbability = prizes.stream()
                .mapToDouble(p -> p.getProbability() != null ? p.getProbability() : 0)
                .sum();

        if (totalProbability <= 0) {
            return prizes.get(0);
        }

        double random = Math.random() * totalProbability;
        double cumulative = 0;

        for (PrizeDO prize : prizes) {
            double prob = prize.getProbability() != null ? prize.getProbability() : 0;
            cumulative += prob;
            if (random <= cumulative) {
                return prize;
            }
        }
        return prizes.get(0);
    }

    /**
     * 保存用户中奖记录（检查是否重复）
     */
    private void saveUserWinningRecord(Long userId, PrizeDO prize) {
        UserDO user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }

        // 检查是否已经中过这个奖品（避免重复插入）
        List<WinningRecordDO> existing = winningRecordMapper.selectByWinnerIdAndPrizeId(userId, prize.getId());
        if (existing != null && !existing.isEmpty()) {
            logger.info("用户已中过该奖品，不再重复记录，userId: {}, prizeId: {}", userId, prize.getId());
            return;
        }

        WinningRecordDO record = new WinningRecordDO();
        record.setWinnerId(userId);
        record.setWinnerName(user.getUserName());
        record.setWinnerEmail(user.getEmail());
        record.setWinnerPhoneNumber(user.getPhoneNumber());
        record.setPrizeId(prize.getId());
        record.setPrizeName(prize.getName());
        record.setPrizeTier("PARTICIPATION");
        record.setWinningTime(new Date());
        record.setActivityId(0L);
        record.setActivityName("幸运大转盘");

        winningRecordMapper.insert(record);
    }

    /**
     * 从token中获取用户ID
     */
    private Long getUserIdFromToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            Claims claims = JWTUtil.parseJWT(token);
            if (claims == null) {
                return null;
            }
            Object userId = claims.get("id");
            if (userId == null) {
                userId = claims.get("userId");
            }
            if (userId instanceof Integer) {
                return ((Integer) userId).longValue();
            }
            if (userId instanceof Long) {
                return (Long) userId;
            }
            if (userId instanceof String) {
                return Long.parseLong((String) userId);
            }
            return null;
        } catch (Exception e) {
            logger.error("解析token失败", e);
            return null;
        }
    }
}