package com.example.lotterysystem.service;

import com.example.lotterysystem.controller.param.DrawPrizeParam;
import com.example.lotterysystem.controller.param.ShowWinningRecordsParam;
import com.example.lotterysystem.dao.dataobject.WinningRecordDO;
import com.example.lotterysystem.service.dto.PrizeDTO;
import com.example.lotterysystem.service.dto.WinningRecordDTO;

import java.util.List;
import java.util.Map;

public interface DrawPrizeService {

    /**
     * 异步抽奖接口
     *
     * @param param
     */
    void drawPrize(DrawPrizeParam param);

    /**
     * 校验抽奖请求
     *
     * @param param
     */
    Boolean checkDrawPrizeParam(DrawPrizeParam param);

    /**
     * 保存中奖者名单
     *
     * @param param
     */
    List<WinningRecordDO> saveWinnerRecords(DrawPrizeParam param);

    /**
     * 删除活动/奖品下的中奖记录
     *
     * @param activityId
     * @param prizeId
     */
    void deleteRecords(Long activityId, Long prizeId);

    /**
     * 获取中奖记录
     *
     * @param param
     * @return
     */
    List<WinningRecordDTO> getRecords(ShowWinningRecordsParam param);

    /**
     * 用户抽奖（普通用户）
     *
     * @param token 用户token
     * @return 抽中的奖品
     */
    PrizeDTO userDraw(String token);

    /**
     * 获取当前用户的中奖记录
     *
     * @param token 用户token
     * @return 中奖记录列表
     */
    List<WinningRecordDTO> getMyRecords(String token);

    /**
     * 获取用户抽奖统计
     *
     * @param token 用户token
     * @return 统计信息（总抽奖次数、中奖次数、中奖率）
     */
    Map<String, Object> getUserStats(String token);
}