package com.example.lotterysystem.controller;

import com.example.lotterysystem.common.result.ApiResponse;
import com.example.lotterysystem.common.result.ResultCode;
import com.example.lotterysystem.entity.*;
import com.example.lotterysystem.mapper.*;
import com.example.lotterysystem.service.AIService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理后台接口
 * <p>
 * 涵盖数据统计、AI分析、用户/奖品/活动 CRUD、奖品池配置、核销、Excel导出等。
 * 所有接口均需管理员权限（前端校验 session 中 role=ADMIN）。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    @Autowired private UserMapper userMapper;
    @Autowired private PrizeMapper prizeMapper;
    @Autowired private ActivityMapper activityMapper;
    @Autowired private PrizePoolMapper prizePoolMapper;
    @Autowired private WinningRecordMapper winningRecordMapper;
    @Autowired private ActivityLimitConfigMapper activityLimitConfigMapper;
    @Autowired private UserDrawRecordMapper userDrawRecordMapper;
    @Autowired private AIService aiService;

    // ==================== 数据统计 ====================

    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> getStatistics() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("todayParticipant", winningRecordMapper.getTodayParticipantCount());
        data.put("todayDrawCount", winningRecordMapper.getTodayDrawCount());
        data.put("totalParticipant", winningRecordMapper.getTotalParticipantCount());
        data.put("totalDrawCount", winningRecordMapper.getTotalDrawCount());
        data.put("prizeStats", winningRecordMapper.getPrizeWinStats());
        data.put("trendStats", winningRecordMapper.getLast7DaysStats());
        data.put("activityRanking", winningRecordMapper.getActivityRanking());
        data.put("lowStockPrizes", prizeMapper.findLowStockPrizes(10));
        data.put("soldOutPrizes", prizeMapper.findSoldOutPrizes());
        return ApiResponse.success(data);
    }

    // ==================== AI 活动分析 ====================

    @GetMapping("/ai/activityInsight/{activityId}")
    public ApiResponse<?> getActivityInsight(@PathVariable Integer activityId) {
        int totalDraw = winningRecordMapper.getTotalDrawCountByActivity(activityId);
        int totalWin = winningRecordMapper.getWinCountByActivity(activityId);
        List<Map<String, Object>> prizeStats = winningRecordMapper.getPrizeWinStatsByActivity(activityId);
        List<String> prizeText = prizeStats.stream()
                .map(m -> m.get("prize_name") + ":" + m.get("count") + "次")
                .collect(Collectors.toList());
        String insight = aiService.analyzeActivityData(totalDraw, totalWin, prizeText);
        return ApiResponse.success(Map.of("insight", insight));
    }

    // ==================== 用户管理 ====================

    @GetMapping("/users")
    public ApiResponse<List<User>> getAllUsers() {
        return ApiResponse.success(userMapper.findAll());
    }

    // ==================== 奖品管理 ====================

    @GetMapping("/prizes")
    public ApiResponse<List<Prize>> getAllPrizes() {
        return ApiResponse.success(prizeMapper.findAll());
    }

    @PostMapping("/createPrize")
    public ApiResponse<?> createPrize(@RequestBody Prize prize) {
        List<Prize> existing = prizeMapper.findByName(prize.getName());
        if (!existing.isEmpty()) {
            Prize ex = existing.get(0);
            ex.setStock(ex.getStock() + prize.getStock());
            ex.setRemaining(ex.getRemaining() + prize.getStock());
            prizeMapper.update(ex);
            return ApiResponse.success(Map.of("prizeId", ex.getId()),
                    "库存已合并，当前库存：" + ex.getStock());
        }
        prize.setRemaining(prize.getStock());
        prizeMapper.insert(prize);
        return ApiResponse.success(Map.of("prizeId", prize.getId()), "创建成功");
    }

    @PostMapping("/uploadPrizeImage")
    public ApiResponse<?> uploadPrizeImage(@RequestParam("file") MultipartFile file,
                                            @RequestParam("prizeId") Integer prizeId) {
        try {
            String uploadDir = System.getProperty("user.dir") + "/uploads/prizes/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();
            String ext = Objects.requireNonNull(file.getOriginalFilename())
                    .substring(file.getOriginalFilename().lastIndexOf("."));
            String filename = UUID.randomUUID() + ext;
            file.transferTo(new File(uploadDir + filename));
            String imageUrl = "/uploads/prizes/" + filename;
            prizeMapper.updateImage(prizeId, imageUrl);
            return ApiResponse.success(Map.of("imageUrl", imageUrl));
        } catch (IOException e) {
            return ApiResponse.fail(ResultCode.ERROR, e.getMessage());
        }
    }

    @DeleteMapping("/deletePrize/{id}")
    public ApiResponse<?> deletePrize(@PathVariable Integer id) {
        prizePoolMapper.deleteByPrizeId(id);
        prizeMapper.deleteById(id);
        return ApiResponse.success();
    }

    // ==================== 活动管理 ====================

    @GetMapping("/activities")
    public ApiResponse<List<Activity>> getAllActivities() {
        List<Activity> acts = activityMapper.findAll();
        LocalDateTime now = LocalDateTime.now();
        for (Activity a : acts) {
            String statusText;
            if (a.getEndTime() != null && now.isAfter(a.getEndTime())) statusText = "已过期";
            else if (a.getStartTime() != null && now.isBefore(a.getStartTime())) statusText = "未开始";
            else if (a.getStatus() == 1) statusText = "进行中";
            else statusText = "已关闭";
            a.setStatusText(statusText);
        }
        return ApiResponse.success(acts);
    }

    @PostMapping("/createActivity")
    public ApiResponse<?> createActivity(@RequestBody Activity activity) {
        activity.setStatus(1);
        activityMapper.insert(activity);
        ActivityLimitConfig config = new ActivityLimitConfig();
        config.setActivityId(activity.getId());
        config.setDailyLimit(0);
        config.setTotalLimit(0);
        activityLimitConfigMapper.insert(config);
        return ApiResponse.success(Map.of("activityId", activity.getId()));
    }

    @DeleteMapping("/deleteActivity/{id}")
    public ApiResponse<?> deleteActivity(@PathVariable Integer id) {
        prizePoolMapper.deleteByActivityId(id);
        activityLimitConfigMapper.deleteByActivityId(id);
        userDrawRecordMapper.deleteByActivityId(id);
        activityMapper.deleteById(id);
        return ApiResponse.success();
    }

    // ==================== 抽奖限制配置 ====================

    @GetMapping("/activityLimits")
    public ApiResponse<List<ActivityLimitConfig>> getActivityLimits() {
        List<ActivityLimitConfig> limits = activityLimitConfigMapper.findAll();
        for (ActivityLimitConfig l : limits) {
            Activity a = activityMapper.findById(l.getActivityId());
            if (a != null) l.setActivityName(a.getName());
        }
        return ApiResponse.success(limits);
    }

    @PostMapping("/updateActivityLimit")
    public ApiResponse<?> updateActivityLimit(@RequestBody ActivityLimitConfig config) {
        ActivityLimitConfig existing = activityLimitConfigMapper.findByActivityId(config.getActivityId());
        if (existing == null) activityLimitConfigMapper.insert(config);
        else activityLimitConfigMapper.update(config);
        return ApiResponse.success();
    }

    // ==================== 奖品池配置 ====================

    @GetMapping("/prizePool/{activityId}")
    public ApiResponse<List<PrizePool>> getPrizePool(@PathVariable Integer activityId) {
        List<PrizePool> pools = prizePoolMapper.findByActivityId(activityId);
        for (PrizePool p : pools) {
            Prize prize = prizeMapper.findById(p.getPrizeId());
            if (prize != null) p.setPrizeName(prize.getName());
        }
        return ApiResponse.success(pools);
    }

    @GetMapping("/availablePrizes")
    public ApiResponse<List<Prize>> getAvailablePrizes() {
        return ApiResponse.success(prizeMapper.findAll());
    }

    @PostMapping("/addPrizeToActivity")
    public ApiResponse<?> addPrizeToActivity(@RequestBody Map<String, Object> params) {
        Integer activityId = Integer.valueOf(params.get("activityId").toString());
        Integer prizeId = Integer.valueOf(params.get("prizeId").toString());
        Double probability = Double.valueOf(params.get("probability").toString());
        PrizePool existing = prizePoolMapper.findByActivityAndPrize(activityId, prizeId);
        if (existing != null) return ApiResponse.fail(ResultCode.BAD_REQUEST, "该奖品已在活动中");

        PrizePool pool = new PrizePool();
        pool.setActivityId(activityId);
        pool.setPrizeId(prizeId);
        pool.setProbability(BigDecimal.valueOf(probability / 100));
        prizePoolMapper.insert(pool);
        return ApiResponse.success();
    }

    @PostMapping("/updateProbability")
    public ApiResponse<?> updateProbability(@RequestBody Map<String, Object> params) {
        Integer id = Integer.valueOf(params.get("id").toString());
        Double probability = Double.valueOf(params.get("probability").toString());
        PrizePool pool = new PrizePool();
        pool.setId(id);
        pool.setProbability(BigDecimal.valueOf(probability / 100));
        prizePoolMapper.updateProbability(pool);
        return ApiResponse.success();
    }

    @DeleteMapping("/removePrizeFromActivity/{id}")
    public ApiResponse<?> removePrizeFromActivity(@PathVariable Integer id) {
        prizePoolMapper.deleteById(id);
        return ApiResponse.success();
    }

    // ==================== 中奖记录 ====================

    @GetMapping("/winningRecords")
    public ApiResponse<List<WinningRecord>> getAllWinningRecords() {
        return ApiResponse.success(winningRecordMapper.findAllWithUser());
    }

    // ==================== 核销功能 ====================

    @PostMapping("/claimPrize")
    public ApiResponse<?> claimPrize(@RequestBody Map<String, String> params) {
        String claimCode = params.get("claimCode");
        WinningRecord record = winningRecordMapper.findByClaimCode(claimCode);
        if (record == null) return ApiResponse.fail(ResultCode.BAD_REQUEST, "核销码无效");
        if (record.getStatus() == 1) return ApiResponse.fail(ResultCode.BAD_REQUEST, "奖品已被领取");
        winningRecordMapper.updateStatus(record.getId(), 1);
        return ApiResponse.success("核销成功");
    }

    @GetMapping("/checkClaimCode")
    public ApiResponse<?> checkClaimCode(@RequestParam String claimCode) {
        WinningRecord record = winningRecordMapper.findByClaimCode(claimCode);
        if (record == null) return ApiResponse.fail(ResultCode.BAD_REQUEST, "无效核销码");
        User user = userMapper.findById(record.getUserId());
        return ApiResponse.success(Map.of(
                "prizeName", record.getPrizeName(),
                "userName", user != null ? user.getName() : "未知",
                "status", record.getStatus()
        ));
    }

    // ==================== 导出Excel ====================

    @GetMapping("/exportWinningRecords")
    public void exportWinningRecords(HttpServletResponse response) throws IOException {
        List<WinningRecord> records = winningRecordMapper.findAllWithUser();
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("中奖记录");

        String[] columns = {"用户", "手机号", "邮箱", "奖品", "核销码", "中奖时间", "状态"};
        Row header = sheet.createRow(0);
        for (int i = 0; i < columns.length; i++) header.createCell(i).setCellValue(columns[i]);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        int rowNum = 1;
        for (WinningRecord r : records) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(r.getUserName());
            row.createCell(1).setCellValue(r.getPhone());
            row.createCell(2).setCellValue(r.getEmail());
            row.createCell(3).setCellValue(r.getPrizeName());
            row.createCell(4).setCellValue(r.getClaimCode());
            row.createCell(5).setCellValue(r.getDrawTime() == null ? "" : r.getDrawTime().format(fmt));
            row.createCell(6).setCellValue(r.getStatus() == 0 ? "待领取" : "已领取");
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode("中奖记录.xlsx", "UTF-8"));
        workbook.write(response.getOutputStream());
        workbook.close();
    }
}
