package com.example.lotterysystem.service;

public interface VerificationCodeService {

    /**
     * 发送邮箱验证码
     *
     * @param email 邮箱地址
     */
    void sendVerificationCode(String email);

    /**
     * 从缓存中获取验证码
     *
     * @param email 邮箱地址
     * @return 验证码
     */
    String getVerificationCode(String email);

}