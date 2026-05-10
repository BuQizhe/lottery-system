package com.example.lotterysystem.controller;

import com.example.lotterysystem.common.result.ApiResponse;
import com.example.lotterysystem.common.result.ResultCode;
import com.example.lotterysystem.entity.Activity;
import com.example.lotterysystem.entity.Prize;
import com.example.lotterysystem.entity.User;
import com.example.lotterysystem.entity.WinningRecord;
import com.example.lotterysystem.mapper.ActivityMapper;
import com.example.lotterysystem.mapper.WinningRecordMapper;
import com.example.lotterysystem.service.AIService;
import com.example.lotterysystem.service.LotteryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

/**
 * 用户端抽奖接口
 * <p>
 * 提供：活动列表、抽奖执行、中奖记录查询、AI 客服。
 * 登录校验通过 HttpSession 获取当前用户，未登录返回 401。
 */
@RestController
public class LotteryController {

    private static final Logger log = LoggerFactory.getLogger(LotteryController.class);

    @Autowired private LotteryService lotteryService;
    @Autowired private ActivityMapper activityMapper;
    @Autowired private WinningRecordMapper winningRecordMapper;
    @Autowired private AIService aiService;
    @Autowired private com.example.lotterysystem.mapper.PrizePoolMapper prizePoolMapper;
    @Autowired private com.example.lotterysystem.mapper.PrizeMapper prizeMapper;

    /** 获取进行中的活动列表 */
    @GetMapping("/api/activities")
    public ApiResponse<List<Activity>> getActivities() {
        return ApiResponse.success(activityMapper.findActiveActivities());
    }

    /**
     * 抽奖接口
     * <p>
     * 流程：活动校验 → 次数检查 → 概率抽奖 → 库存扣减 → AI祝福语 → 返回结果。
     * AI 祝福语生成失败不影响抽奖结果，以兜底文字替代。
     */
    @PostMapping("/api/draw")
    public ApiResponse<?> draw(@RequestBody Map<String, Integer> params, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ApiResponse.fail(ResultCode.UNAUTHORIZED);
        }

        Integer activityId = params.get("activityId");
        if (activityId == null) {
            return ApiResponse.fail(ResultCode.BAD_REQUEST, "请选择活动");
        }

        Prize prize = lotteryService.draw(user.getId(), activityId);
        if (prize == null) {
            return ApiResponse.fail(ResultCode.BAD_REQUEST, "很遗憾，未中奖");
        }

        // AI 祝福语（容错，失败不影响主流程）
        String blessing;
        try {
            blessing = aiService.generateBlessing(user.getName(), prize.getName());
            if (blessing == null || blessing.isEmpty()) blessing = "好运连连！";
        } catch (Exception e) {
            log.warn("AI祝福语生成失败", e);
            blessing = "恭喜中奖，好运常伴！";
        }

        Map<String, Object> data = Map.of(
                "prizeName", prize.getName(),
                "prizeImageUrl", prize.getImageUrl() == null ? "" : prize.getImageUrl(),
                "blessing", blessing
        );
        return ApiResponse.success(data, "恭喜中奖！");
    }

    /** 获取用户中奖记录（含奖品图片和二维码） */
    @GetMapping("/api/user/winningRecords")
    public ApiResponse<List<WinningRecord>> getUserWinningRecords(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ApiResponse.fail(ResultCode.UNAUTHORIZED);
        }
        return ApiResponse.success(
                winningRecordMapper.findUserRecordsWithImage(user.getId()));
    }

    /** 欢迎信息 */
    @GetMapping("/api/welcome")
    public ApiResponse<?> welcome(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ApiResponse.fail(ResultCode.UNAUTHORIZED);
        }
        List<Activity> activities = activityMapper.findActiveActivities();
        return ApiResponse.success(Map.of(
                "userName", user.getName(),
                "activityCount", activities.size(),
                "message", "欢迎回来，" + user.getName() + "！今天也要元气满满哦 🎉"
        ));
    }

    /** AI 客服问答 */
    @PostMapping("/api/ai/chat")
    public ApiResponse<?> aiChat(@RequestBody Map<String, String> params) {
        String question = params.get("question");
        if (question == null || question.trim().isEmpty()) {
            return ApiResponse.fail(ResultCode.BAD_REQUEST, "请输入您的问题");
        }
        String answer = aiService.customerService(question);
        return ApiResponse.success(Map.of("answer", answer));
    }

    /**
     * 获取活动奖品池（普通用户可用）
     * 转盘页面需要加载奖品数据来绘制转盘
     */
    @GetMapping("/api/prizePool/{activityId}")
    public ApiResponse<?> getPrizePool(@PathVariable Integer activityId) {
        List<com.example.lotterysystem.entity.PrizePool> pools = prizePoolMapper.findByActivityId(activityId);
        for (com.example.lotterysystem.entity.PrizePool p : pools) {
            com.example.lotterysystem.entity.Prize prize = prizeMapper.findById(p.getPrizeId());
            if (prize != null) p.setPrizeName(prize.getName());
        }
        return ApiResponse.success(pools);
    }

    // ==================== 排行榜接口 ====================

    /**
     * GET /api/ranking — 中奖排行榜
     * 返回中奖次数最多的前10名用户
     */
    @GetMapping("/api/ranking")
    public ApiResponse<?> getRanking() {
        return ApiResponse.success(winningRecordMapper.getWinRanking());
    }

    /**
     * GET /api/recentWins — 最近中奖记录
     * 返回最近20条中奖记录，用于页面顶部的滚动公告
     */
    @GetMapping("/api/recentWins")
    public ApiResponse<?> getRecentWins() {
        return ApiResponse.success(winningRecordMapper.getRecentWins());
    }

    /**
     * GET /api/user/stats — 个人战绩
     * 返回当前登录用户的中奖统计
     */
    @GetMapping("/api/user/stats")
    public ApiResponse<?> getUserStats(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ApiResponse.fail(ResultCode.UNAUTHORIZED);
        }
        int winCount = winningRecordMapper.getUserWinCount(user.getId());
        String lastPrize = winningRecordMapper.getUserLastPrize(user.getId());
        return ApiResponse.success(Map.of(
                "winCount", winCount,
                "lastPrize", lastPrize != null ? lastPrize : "暂无"
        ));
    }
}
