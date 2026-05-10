package com.example.lotterysystem.mapper;

import com.example.lotterysystem.entity.ActivityLimitConfig;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ActivityLimitConfigMapper {

    @Select("SELECT * FROM activity_limit_config")
    List<ActivityLimitConfig> findAll();

    @Select("SELECT * FROM activity_limit_config WHERE activity_id = #{activityId}")
    ActivityLimitConfig findByActivityId(Integer activityId);

    @Insert("INSERT INTO activity_limit_config(activity_id, daily_limit, total_limit) VALUES(#{activityId}, #{dailyLimit}, #{totalLimit})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ActivityLimitConfig config);

    @Update("UPDATE activity_limit_config SET daily_limit = #{dailyLimit}, total_limit = #{totalLimit} WHERE activity_id = #{activityId}")
    int update(ActivityLimitConfig config);

    @Delete("DELETE FROM activity_limit_config WHERE activity_id = #{activityId}")
    int deleteByActivityId(Integer activityId);
}