package com.example.lotterysystem;

public class TestStartup {
    public static void main(String[] args) {
        try {
            System.err.println("=== 开始启动 Spring Boot ===");
            LotterySystemApplication.main(args);
            System.err.println("=== 启动成功 ===");
        } catch (Exception e) {
            System.err.println("=== 启动失败，错误信息如下 ===");
            e.printStackTrace(System.err);
        }
    }
}