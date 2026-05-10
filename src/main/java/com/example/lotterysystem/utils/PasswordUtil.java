package com.example.lotterysystem.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordUtil {

    private static final PasswordEncoder encoder = new BCryptPasswordEncoder();

    // 加密密码
    public static String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    // 验证密码
    public static boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }

    // 生成加密密码（用于初始化数据）
    public static void main(String[] args) {
        System.out.println("admin123加密后: " + encode("admin123"));
        System.out.println("claimer123加密后: " + encode("claimer123"));
        System.out.println("user123加密后: " + encode("user123"));
    }
}