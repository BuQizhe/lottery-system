package com.example.lotterysystem.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.example.lotterysystem.common.pojo.CommonResult;
import com.example.lotterysystem.common.utils.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@RestController
public class CaptchaController {

    private static final Logger log = LoggerFactory.getLogger(CaptchaController.class);

    @Autowired
    private RedisUtil redisUtil;

    private static final String CAPTCHA_PREFIX = "CAPTCHA_";
    private static final long CAPTCHA_EXPIRE = 300; // 5分钟

    /**
     * 获取验证码图片，同时返回UUID在响应头中
     */
    @GetMapping("/captcha")
    public void getCaptcha(HttpServletResponse response) throws IOException {
        // 生成UUID
        String uuid = UUID.randomUUID().toString();

        // 生成验证码
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 20);
        String code = lineCaptcha.getCode();

        log.info("========== 生成验证码 ==========");
        log.info("UUID: {}", uuid);
        log.info("验证码: {}", code);

        // 存入 Redis
        redisUtil.set(CAPTCHA_PREFIX + uuid, code.toLowerCase(), CAPTCHA_EXPIRE);
        log.info("验证码已存入Redis，key: {}{}", CAPTCHA_PREFIX, uuid);

        // 将UUID放入响应头
        response.setHeader("Captcha-UUID", uuid);
        response.setContentType("image/png");

        // 输出图片
        lineCaptcha.write(response.getOutputStream());
        log.info("验证码图片已输出");
    }

    /**
     * 获取验证码文本（用于自动填入，测试用）
     */
    @GetMapping("/captcha-text")
    public CommonResult<String> getCaptchaText(@RequestParam String uuid) {
        String code = redisUtil.get(CAPTCHA_PREFIX + uuid);
        log.info("获取验证码文本 - UUID: {}, 验证码: {}", uuid, code);
        return CommonResult.success(code);
    }
}