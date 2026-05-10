package com.example.lotterysystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 抽奖系统启动类
 * <p>
 * 技术栈：Spring Boot 2.7 + MyBatis + MySQL + Redis + WebSocket + BCrypt
 * <p>
 * 系统包含三大角色：
 * <ul>
 *   <li>用户端 — 注册/登录/抽奖/查看中奖记录</li>
 *   <li>管理端 — 活动配置/奖品管理/数据统计/Excel导出</li>
 *   <li>核销端 — 扫码核销/核销码查询</li>
 * </ul>
 * {@code @EnableScheduling} 已移至 {@code ScheduleService}，避免重复声明。
 */
@SpringBootApplication
public class LotterySystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(LotterySystemApplication.class, args);
        System.out.println("========================================");
        System.out.println("抽奖系统启动成功！");
        System.out.println("访问地址: http://localhost:8080/blogin.html");
        System.out.println("========================================");
    }
}