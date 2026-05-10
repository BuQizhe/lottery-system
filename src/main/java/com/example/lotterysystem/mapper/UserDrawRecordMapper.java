package com.example.lotterysystem.mapper;

import com.example.lotterysystem.entity.UserDrawRecord;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;

@Mapper
public interface UserDrawRecordMapper {

    @Select("SELECT * FROM user_draw_record WHERE user_id = #{userId} AND activity_id = #{activityId} AND draw_date = #{drawDate}")
    UserDrawRecord findByUserAndActivityAndDate(@Param("userId") Integer userId,
                                                @Param("activityId") Integer activityId,
                                                @Param("drawDate") LocalDate drawDate);

    @Insert("INSERT INTO user_draw_record(user_id, activity_id, draw_date, draw_count) VALUES(#{userId}, #{activityId}, #{drawDate}, #{drawCount})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserDrawRecord record);

    @Update("UPDATE user_draw_record SET draw_count = draw_count + 1 WHERE id = #{id}")
    int incrementCount(Integer id);

    /**
     * 原子操作：插入或递增抽奖计数
     * 使用 INSERT ... ON DUPLICATE KEY UPDATE 避免并发竞态条件
     * 两个并发请求不会出现"都查到 null 然后都插入"的问题
     */
    @Insert("INSERT INTO user_draw_record(user_id, activity_id, draw_date, draw_count) " +
            "VALUES(#{userId}, #{activityId}, #{drawDate}, 1) " +
            "ON DUPLICATE KEY UPDATE draw_count = draw_count + 1")
    int incrementOrInsert(@Param("userId") Integer userId,
                          @Param("activityId") Integer activityId,
                          @Param("drawDate") LocalDate drawDate);

    @Select("SELECT SUM(draw_count) FROM user_draw_record WHERE user_id = #{userId} AND activity_id = #{activityId}")
    Integer getTotalDrawCount(@Param("userId") Integer userId, @Param("activityId") Integer activityId);

    @Delete("DELETE FROM user_draw_record WHERE activity_id = #{activityId}")
    int deleteByActivityId(Integer activityId);
}