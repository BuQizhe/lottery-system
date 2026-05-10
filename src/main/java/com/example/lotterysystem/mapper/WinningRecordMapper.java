package com.example.lotterysystem.mapper;

import com.example.lotterysystem.entity.WinningRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface WinningRecordMapper {

    @Insert("INSERT INTO winning_record(user_id, activity_id, prize_id, prize_name, winning_code, qr_code, claim_code, status, draw_time) VALUES(#{userId}, #{activityId}, #{prizeId}, #{prizeName}, #{winningCode}, #{qrCode}, #{claimCode}, #{status}, #{drawTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(WinningRecord record);

    @Select("SELECT * FROM winning_record WHERE user_id = #{userId} ORDER BY draw_time DESC")
    List<WinningRecord> findByUserId(Integer userId);

    @Select("SELECT * FROM winning_record WHERE activity_id = #{activityId} ORDER BY draw_time DESC")
    List<WinningRecord> findByActivityId(Integer activityId);

    @Select("SELECT w.*, u.name as userName, u.phone, u.email, p.image_url as prizeImageUrl FROM winning_record w LEFT JOIN user u ON w.user_id = u.id LEFT JOIN prize p ON w.prize_id = p.id ORDER BY w.draw_time DESC")
    List<WinningRecord> findAllWithUser();

    @Select("SELECT w.*, u.name as userName, u.phone, u.email, p.image_url as prizeImageUrl FROM winning_record w LEFT JOIN user u ON w.user_id = u.id LEFT JOIN prize p ON w.prize_id = p.id WHERE w.user_id = #{userId} ORDER BY w.draw_time DESC")
    List<WinningRecord> findUserRecordsWithImage(@Param("userId") Integer userId);

    @Update("UPDATE winning_record SET status = #{status}, claim_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Integer id, @Param("status") Integer status);

    @Select("SELECT * FROM winning_record WHERE claim_code = #{claimCode}")
    WinningRecord findByClaimCode(String claimCode);

    @Select("SELECT COUNT(*) FROM winning_record WHERE activity_id = #{activityId} AND prize_id = #{prizeId}")
    int countWinByActivityAndPrize(@Param("activityId") Integer activityId, @Param("prizeId") Integer prizeId);

    // ==================== 统计方法 ====================

    @Select("SELECT COUNT(DISTINCT user_id) FROM winning_record WHERE DATE(draw_time) = CURDATE()")
    int getTodayParticipantCount();

    @Select("SELECT COUNT(*) FROM winning_record WHERE DATE(draw_time) = CURDATE()")
    int getTodayDrawCount();

    @Select("SELECT COUNT(DISTINCT user_id) FROM winning_record")
    int getTotalParticipantCount();

    @Select("SELECT COUNT(*) FROM winning_record")
    int getTotalDrawCount();

    @Select("SELECT prize_id, prize_name, COUNT(*) as count FROM winning_record GROUP BY prize_id, prize_name")
    List<Map<String, Object>> getPrizeWinStats();

    @Select("SELECT DATE(draw_time) as date, COUNT(*) as count FROM winning_record WHERE draw_time >= DATE_SUB(NOW(), INTERVAL 7 DAY) GROUP BY DATE(draw_time) ORDER BY date")
    List<Map<String, Object>> getLast7DaysStats();

    @Select("SELECT a.id, a.name, COUNT(DISTINCT w.user_id) as user_count FROM activity a LEFT JOIN winning_record w ON a.id = w.activity_id GROUP BY a.id ORDER BY user_count DESC LIMIT 5")
    List<Map<String, Object>> getActivityRanking();

    // 根据活动ID获取总抽奖次数（从 user_draw_record 汇总，包含未中奖的抽奖）
    @Select("SELECT COALESCE(SUM(draw_count), 0) FROM user_draw_record WHERE activity_id = #{activityId}")
    int getTotalDrawCountByActivity(Integer activityId);

    // 根据活动ID获取总中奖次数（其实就是总记录数，因为每条记录都是中奖）
    @Select("SELECT COUNT(*) FROM winning_record WHERE activity_id = #{activityId}")
    int getWinCountByActivity(Integer activityId);

    // 根据活动ID获取奖品中奖统计
    @Select("SELECT prize_name, COUNT(*) as count FROM winning_record WHERE activity_id = #{activityId} GROUP BY prize_name")
    List<Map<String, Object>> getPrizeWinStatsByActivity(Integer activityId);

    // ==================== 排行榜方法 ====================

    /**
     * 中奖次数排行榜（前10名）
     * 按用户分组统计中奖次数，降序排列
     */
    @Select("SELECT u.name as user_name, COUNT(*) as win_count " +
            "FROM winning_record w JOIN user u ON w.user_id = u.id " +
            "GROUP BY w.user_id, u.name ORDER BY win_count DESC LIMIT 10")
    List<Map<String, Object>> getWinRanking();

    /**
     * 最近中奖记录（前20条，用于滚动公告）
     * 按中奖时间倒序
     */
    @Select("SELECT u.name as user_name, w.prize_name, w.draw_time " +
            "FROM winning_record w JOIN user u ON w.user_id = u.id " +
            "ORDER BY w.draw_time DESC LIMIT 20")
    List<Map<String, Object>> getRecentWins();

    /**
     * 用户个人战绩：总中奖次数
     */
    @Select("SELECT COUNT(*) FROM winning_record WHERE user_id = #{userId}")
    int getUserWinCount(@Param("userId") Integer userId);

    /**
     * 用户个人战绩：最近一次中奖的奖品名
     */
    @Select("SELECT prize_name FROM winning_record WHERE user_id = #{userId} ORDER BY draw_time DESC LIMIT 1")
    String getUserLastPrize(@Param("userId") Integer userId);
}