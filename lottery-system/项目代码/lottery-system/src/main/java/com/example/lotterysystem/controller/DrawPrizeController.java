package com.example.lotterysystem.controller;

import cn.hutool.core.date.DateUtil;
import com.example.lotterysystem.common.pojo.CommonResult;
import com.example.lotterysystem.common.utils.ExcelUtil;
import com.example.lotterysystem.common.utils.JacksonUtil;
import com.example.lotterysystem.controller.param.DrawPrizeParam;
import com.example.lotterysystem.controller.param.ShowWinningRecordsParam;
import com.example.lotterysystem.controller.result.WinningRecordResult;
import com.example.lotterysystem.service.DrawPrizeService;
import com.example.lotterysystem.service.dto.PrizeDTO;
import com.example.lotterysystem.service.dto.WinningRecordDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class DrawPrizeController {

    private static final Logger logger = LoggerFactory.getLogger(DrawPrizeController.class);

    @Autowired
    private DrawPrizeService drawPrizeService;
    @Autowired
    private ExcelUtil excelUtil;

    @RequestMapping("/draw-prize")
    public CommonResult<Boolean> drawPrize(
            @Validated @RequestBody DrawPrizeParam param) {
        logger.info("drawPrize DrawPrizeParam:{}", param);
        drawPrizeService.drawPrize(param);
        return CommonResult.success(true);
    }


    @RequestMapping("/winning-records/show")
    public CommonResult<List<WinningRecordResult>> showWinningRecords(
            @Validated @RequestBody ShowWinningRecordsParam param) {
        logger.info("showWinningRecords ShowWinningRecordsParam:{}",
                JacksonUtil.writeValueAsString(param));
        List<WinningRecordDTO> winningRecordDTOList = drawPrizeService.getRecords(param);
        return CommonResult.success(
                convertToWinningRecordResultList(winningRecordDTOList));
    }

    /**
     * 导出中奖名单为 Excel（根据活动ID）
     */
    @RequestMapping("/winning-records/export")
    public void exportWinningRecords(@RequestParam Long activityId, HttpServletResponse response) {
        logger.info("exportWinningRecords activityId:{}", activityId);
        try {
            ShowWinningRecordsParam param = new ShowWinningRecordsParam();
            param.setActivityId(activityId);
            List<WinningRecordDTO> recordList = drawPrizeService.getRecords(param);

            if (CollectionUtils.isEmpty(recordList)) {
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().write("暂无中奖记录");
                return;
            }

            List<Map<String, Object>> data = recordList.stream().map(record -> {
                Map<String, Object> map = new HashMap<>();
                map.put("winnerId", record.getWinnerId());
                map.put("winnerName", record.getWinnerName());
                map.put("prizeName", record.getPrizeName());
                map.put("prizeTier", record.getPrizeTier().getMessage());
                map.put("winningTime", DateUtil.formatDateTime(record.getWinningTime()));
                return map;
            }).collect(Collectors.toList());

            String activityName = recordList.isEmpty() ? "抽奖活动" : recordList.get(0).getActivityName();
            excelUtil.exportWinningRecords(response, data, activityName);

        } catch (Exception e) {
            logger.error("导出中奖名单失败", e);
            try {
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().write("导出失败：" + e.getMessage());
            } catch (Exception ex) {
                // ignore
            }
        }
    }

    /**
     * 导出中奖数据（POST方式，支持导出当前筛选结果）
     */
    @PostMapping("/winning-records/export-data")
    public void exportWinningData(@RequestBody List<WinningRecordDTO> records, HttpServletResponse response) {
        logger.info("导出中奖数据，共{}条", records == null ? 0 : records.size());
        try {
            if (CollectionUtils.isEmpty(records)) {
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().write("暂无中奖记录");
                return;
            }

            List<Map<String, Object>> data = records.stream().map(record -> {
                Map<String, Object> map = new HashMap<>();
                map.put("winnerId", record.getWinnerId());
                map.put("winnerName", record.getWinnerName());
                map.put("prizeName", record.getPrizeName());
                String tierName = record.getPrizeTier() != null ? record.getPrizeTier().getMessage() : "参与奖";
                map.put("prizeTier", tierName);
                map.put("winningTime", DateUtil.formatDateTime(record.getWinningTime()));
                return map;
            }).collect(Collectors.toList());

            String activityName = "中奖记录";
            excelUtil.exportWinningRecords(response, data, activityName);

        } catch (Exception e) {
            logger.error("导出中奖名单失败", e);
            try {
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().write("导出失败：" + e.getMessage());
            } catch (Exception ex) {
                // ignore
            }
        }
    }

    /**
     * 用户抽奖（普通用户）
     */
    @PostMapping("/draw/user-draw")
    public CommonResult<PrizeDTO> userDraw(@RequestHeader("user_token") String token) {
        logger.info("用户抽奖，token: {}", token);
        PrizeDTO prize = drawPrizeService.userDraw(token);
        return CommonResult.success(prize);
    }

    /**
     * 我的中奖记录
     */
    @PostMapping("/winning-records/my")
    public CommonResult<List<WinningRecordResult>> myWinningRecords(@RequestHeader("user_token") String token) {
        logger.info("我的中奖记录，token: {}", token);
        List<WinningRecordDTO> recordList = drawPrizeService.getMyRecords(token);
        return CommonResult.success(convertToWinningRecordResultList(recordList));
    }

    /**
     * 抽奖统计
     */
    @GetMapping("/winning-records/stats")
    public CommonResult<Map<String, Object>> stats(@RequestHeader("user_token") String token) {
        logger.info("抽奖统计，token: {}", token);
        Map<String, Object> stats = drawPrizeService.getUserStats(token);
        return CommonResult.success(stats);
    }

    private List<WinningRecordResult> convertToWinningRecordResultList(
            List<WinningRecordDTO> winningRecordDTOList) {
        if (CollectionUtils.isEmpty(winningRecordDTOList)) {
            return Arrays.asList();
        }
        return winningRecordDTOList.stream()
                .map(winningRecordDTO -> {
                    WinningRecordResult result = new WinningRecordResult();
                    result.setWinnerId(winningRecordDTO.getWinnerId());
                    result.setWinnerName(winningRecordDTO.getWinnerName());
                    result.setPrizeName(winningRecordDTO.getPrizeName());
                    result.setPrizeTier(winningRecordDTO.getPrizeTier().getMessage());
                    result.setWinningTime(winningRecordDTO.getWinningTime());
                    return result;
                }).collect(Collectors.toList());
    }
}