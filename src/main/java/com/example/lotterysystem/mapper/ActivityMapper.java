package com.example.lotterysystem.mapper;

import com.example.lotterysystem.entity.Activity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ActivityMapper {

    /**
     * 查询所有活动
     */
    @Select("SELECT * FROM activity ORDER BY create_time DESC")
    List<Activity> findAll();

    /**
     * 根据ID查询活动
     */
    @Select("SELECT * FROM activity WHERE id = #{id}")
    Activity findById(Integer id);

    /**
     * 查询进行中的活动（状态=1 且 当前时间在时间范围内）
     */
    @Select("SELECT * FROM activity WHERE status = 1 AND start_time <= NOW() AND end_time >= NOW() ORDER BY create_time DESC")
    List<Activity> findActiveActivities();

    /**
     * 新增活动
     */
    @Insert("INSERT INTO activity(name, description, start_time, end_time, status) VALUES(#{name}, #{description}, #{startTime}, #{endTime}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Activity activity);

    /**
     * 更新活动
     */
    @Update("UPDATE activity SET name = #{name}, description = #{description}, start_time = #{startTime}, end_time = #{endTime}, status = #{status} WHERE id = #{id}")
    int update(Activity activity);

    /**
     * 更新活动状态
     */
    @Update("UPDATE activity SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Integer id, @Param("status") Integer status);

    /**
     * 删除活动
     */
    @Delete("DELETE FROM activity WHERE id = #{id}")
    int deleteById(Integer id);
}