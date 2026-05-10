package com.example.lotterysystem.mapper;

import com.example.lotterysystem.entity.PrizePool;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PrizePoolMapper {

    /**
     * 根据活动ID查询奖品池配置
     */
    @Select("SELECT * FROM prize_pool WHERE activity_id = #{activityId}")
    List<PrizePool> findByActivityId(Integer activityId);

    /**
     * 根据活动ID和奖品ID查询
     */
    @Select("SELECT * FROM prize_pool WHERE activity_id = #{activityId} AND prize_id = #{prizeId}")
    PrizePool findByActivityAndPrize(@Param("activityId") Integer activityId, @Param("prizeId") Integer prizeId);

    /**
     * 根据ID查询
     */
    @Select("SELECT * FROM prize_pool WHERE id = #{id}")
    PrizePool findById(Integer id);

    /**
     * 新增奖品池配置
     */
    @Insert("INSERT INTO prize_pool(activity_id, prize_id, probability, daily_limit, total_limit, win_count) VALUES(#{activityId}, #{prizeId}, #{probability}, #{dailyLimit}, #{totalLimit}, #{winCount})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PrizePool prizePool);

    /**
     * 更新概率
     */
    @Update("UPDATE prize_pool SET probability = #{probability} WHERE id = #{id}")
    int updateProbability(PrizePool prizePool);

    /**
     * 更新整个配置
     */
    @Update("UPDATE prize_pool SET activity_id = #{activityId}, prize_id = #{prizeId}, probability = #{probability}, daily_limit = #{dailyLimit}, total_limit = #{totalLimit}, win_count = #{winCount} WHERE id = #{id}")
    int update(PrizePool prizePool);

    /**
     * 增加中奖次数
     */
    @Update("UPDATE prize_pool SET win_count = win_count + 1 WHERE id = #{id}")
    int incrementWinCount(Integer id);

    /**
     * 根据ID删除配置
     */
    @Delete("DELETE FROM prize_pool WHERE id = #{id}")
    int deleteById(Integer id);

    /**
     * 根据活动ID删除所有配置
     */
    @Delete("DELETE FROM prize_pool WHERE activity_id = #{activityId}")
    int deleteByActivityId(Integer activityId);

    /**
     * 根据奖品ID删除所有配置（删除奖品时使用）
     */
    @Delete("DELETE FROM prize_pool WHERE prize_id = #{prizeId}")
    int deleteByPrizeId(Integer prizeId);

    /**
     * 查询所有配置
     */
    @Select("SELECT * FROM prize_pool")
    List<PrizePool> findAll();
}