package com.example.lotterysystem.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AI 服务（基于大语言模型API）
 * <p>
 * 提供三大能力：
 * <ul>
 *   <li>中奖祝福语生成 — 抽中奖品时调用，增加仪式感</li>
 *   <li>AI 客服问答 — 用户可向系统提问活动相关问题</li>
 *   <li>活动数据分析 — 管理员查看活动运营数据并获取优化建议</li>
 * </ul>
 * 使用 OkHttp 连接池（复用TCP连接）替代原生 HttpURLConnection，
 * 使用 Fastjson2 进行序列化/反序列化替代手工字符串拼接。
 */
@Service
public class AIService {

    private static final Logger log = LoggerFactory.getLogger(AIService.class);

    @Value("${ai.api.key}")
    private String apiKey;
    @Value("${ai.api.url}")
    private String apiUrl;
    @Value("${ai.model}")
    private String model;

    /** OkHttp 客户端（连接池复用，默认5个空闲连接、5分钟保活） */
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))
            .build();

    // ==================== 核心调用方法 ====================

    /**
     * 调用 AI 接口，发送 system prompt + user prompt，返回模型文本回复
     *
     * @param systemPrompt 系统提示词（设定角色、语气、字数限制）
     * @param userPrompt   用户输入内容
     * @return AI回复文本，失败返回兜底文字
     */
    private String callAI(String systemPrompt, String userPrompt) {
        // 1. 用 Fastjson2 构建请求体（替代手工字符串拼接）
        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("temperature", 0.7);
        body.put("max_tokens", 300);

        JSONArray messages = new JSONArray();
        JSONObject sysMsg = new JSONObject();
        sysMsg.put("role", "system");
        sysMsg.put("content", systemPrompt);
        messages.add(sysMsg);

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);
        messages.add(userMsg);
        body.put("messages", messages);

        // 2. 构建 OkHttp 请求
        Request request = new Request.Builder()
                .url(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body.toJSONString(), MediaType.get("application/json")))
                .build();

        // 3. 同步调用（当前业务场景无需流式响应）
        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("AI接口返回错误: {} {}", response.code(), response.message());
                return null;
            }
            String respBody = response.body() != null ? response.body().string() : "";
            // 用 Fastjson2 解析路径: choices[0].message.content
            return extractContent(respBody);
        } catch (IOException e) {
            log.error("AI接口调用失败", e);
            return null;
        }
    }

    /**
     * 从 OpenAI 兼容格式的返回 JSON 中提取 content 字段
     */
    private String extractContent(String respBody) {
        try {
            JSONObject json = JSON.parseObject(respBody);
            JSONArray choices = json.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) return null;
            JSONObject first = choices.getJSONObject(0);
            JSONObject message = first.getJSONObject("message");
            if (message == null) return null;
            return message.getString("content");
        } catch (Exception e) {
            log.error("AI响应解析失败", e);
            return null;
        }
    }

    // ==================== 对外暴露的方法 ====================

    /**
     * 生成中奖祝福语
     *
     * @param userName  中奖用户名
     * @param prizeName 奖品名称
     * @return 祝福语文本
     */
    public String generateBlessing(String userName, String prizeName) {
        String system = "你是一个热情幽默的抽奖活动主持人，为用户生成简短的中奖祝福语，20字以内。";
        String prompt = "用户" + userName + "抽中了" + prizeName + "，请生成祝福语";
        String result = callAI(system, prompt);
        return result != null ? result : "恭喜中奖，好运常伴！";
    }

    /**
     * AI 客服问答
     *
     * @param question 用户问题
     * @return AI 回复
     */
    public String customerService(String question) {
        String system = "你是一个抽奖活动的客服助手，回答用户关于抽奖规则、中奖查询、兑奖流程、奖品等问题。回答简洁友好，不超过100字。";
        String result = callAI(system, question);
        return result != null ? result : "AI 服务暂时不可用，请稍后再试。";
    }

    /**
     * 分析活动数据并给出优化建议
     *
     * @param totalDraw    总抽奖次数
     * @param totalWin     总中奖次数
     * @param prizeStatText 各奖品中奖统计
     * @return 分析报告
     */
    public String analyzeActivityData(int totalDraw, int totalWin, List<String> prizeStatText) {
        int winRate = totalDraw == 0 ? 0 : totalWin * 100 / totalDraw;
        String system = "你是一个数据分析师，分析抽奖活动数据并给出优化建议。200字以内。";
        String prompt = "总抽奖次数：" + totalDraw
                + "，总中奖次数：" + totalWin
                + "，中奖率：" + winRate + "%。\n奖品统计："
                + String.join("、", prizeStatText);
        String result = callAI(system, prompt);
        return result != null ? result : "暂无法生成分析报告。";
    }
}
