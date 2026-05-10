package com.example.lotterysystem.dao.mapper;

import com.example.lotterysystem.dao.dataobject.WinningRecordDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface WinningRecordMapper {

    @Insert("<script>" +
            " insert into winning_record (activity_id, activity_name," +
            " prize_id, prize_name, prize_tier," +
            " winner_id, winner_name, winner_email, winner_phone_number, winning_time)" +
            " values <foreach collection = 'items' item='item' index='index' separator=','>" +
            " (#{item.activityId}, #{item.activityName}," +
            " #{item.prizeId}, #{item.prizeName}, #{item.prizeTier}," +
            " #{item.winnerId}, #{item.winnerName}, #{item.winnerEmail}, #{item.winnerPhoneNumber}, #{item.winningTime})" +
            " </foreach>" +
            " </script>")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int batchInsert(@Param("items") List<WinningRecordDO> winningRecordDOList);

    /**
     * 插入单条中奖记录
     */
    @Insert("insert into winning_record (activity_id, activity_name," +
            " prize_id, prize_name, prize_tier," +
            " winner_id, winner_name, winner_email, winner_phone_number, winning_time)" +
            " values (#{activityId}, #{activityName}," +
            " #{prizeId}, #{prizeName}, #{prizeTier}," +
            " #{winnerId}, #{winnerName}, #{winnerEmail}, #{winnerPhoneNumber}, #{winningTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(WinningRecordDO winningRecordDO);

    @Select("select * from winning_record where activity_id = #{activityId}")
    List<WinningRecordDO> selectByActivityId(@Param("activityId") Long activityId);

    @Select("select count(1) from winning_record where activity_id = #{activityId} and prize_id = #{prizeId}")
    int countByAPId(@Param("activityId") Long activityId, @Param("prizeId") Long prizeId);

    /**
     * 根据中奖者ID查询中奖记录
     */
    @Select("select * from winning_record where winner_id = #{winnerId} order by winning_time desc")
    List<WinningRecordDO> selectByWinnerId(@Param("winnerId") Long winnerId);

    /**
     * 根据中奖者ID和奖品ID查询中奖记录（检查是否重复中奖）
     */
    @Select("select * from winning_record where winner_id = #{winnerId} and prize_id = #{prizeId}")
    List<WinningRecordDO> selectByWinnerIdAndPrizeId(@Param("winnerId") Long winnerId,
                                                     @Param("prizeId") Long prizeId);

    /**
     * 删除活动 或 奖品下的中奖记录
     */
    @Delete("<script>" +
            " delete from winning_record" +
            " where activity_id = #{activityId}" +
            " <if test=\"prizeId != null\">" +
            "   and prize_id = #{prizeId}" +
            " </if>" +
            " </script>")
    void deleteRecords(@Param("activityId") Long activityId, @Param("prizeId") Long prizeId);

    @Select("<script>" +
            " select * from winning_record" +
            " where activity_id = #{activityId}" +
            " <if test=\"prizeId != null\">" +
            "   and prize_id = #{prizeId}" +
            " </if>" +
            " </script>")
    List<WinningRecordDO> selectByActivityIdOrPrizeId(@Param("activityId") Long activityId,
                                                      @Param("prizeId")  Long prizeId);
}