package com.example.lotterysystem.service.impl;

import com.example.lotterysystem.common.errorcode.ServiceErrorCodeConstants;
import com.example.lotterysystem.common.exception.ServiceException;
import com.example.lotterysystem.common.utils.*;
import com.example.lotterysystem.service.VerificationCodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VerificationCodeServiceImpl implements VerificationCodeService {

    private static final Logger log = LoggerFactory.getLogger(VerificationCodeServiceImpl.class);

    private static final String VERIFICATION_CODE_PREFIX = "EMAIL_CODE_";
    private static final Long VERIFICATION_CODE_TIMEOUT = 300L; // 5分钟有效期

    @Autowired
    private MailUtil mailUtil;
    @Autowired
    private RedisUtil redisUtil;

    @Override
    public void sendVerificationCode(String email) {
        log.info("========== 开始发送邮箱验证码 ==========");
        log.info("目标邮箱: {}", email);

        // 校验邮箱格式
        if (!RegexUtil.checkMail(email)) {
            log.error("邮箱格式错误: {}", email);
            throw new ServiceException(ServiceErrorCodeConstants.MAIL_ERROR);
        }

        // 生成6位随机验证码
        String code = CaptchaUtil.getCaptcha(6);
        log.info("生成的验证码: {}", code);

        // 发送邮件
        String subject = "抽奖系统登录验证码";
        String content = "您好！您的登录验证码是：" + code + "，有效期5分钟，请勿泄露给他人。";
        boolean success = mailUtil.sendSampleMail(email, subject, content);
        log.info("邮件发送结果: {}", success);

        if (!success) {
            log.error("邮件发送失败");
            throw new ServiceException(ServiceErrorCodeConstants.SEND_EMAIL_ERROR);
        }

        // 缓存验证码（有效期5分钟）
        redisUtil.set(VERIFICATION_CODE_PREFIX + email, code, VERIFICATION_CODE_TIMEOUT);
        log.info("验证码已存入Redis，key: {}", VERIFICATION_CODE_PREFIX + email);
        log.info("========== 发送完成 ==========");
    }

    @Override
    public String getVerificationCode(String email) {
        log.info("========== 获取验证码 ==========");
        log.info("查询邮箱: {}", email);

        // 校验邮箱格式
        if (!RegexUtil.checkMail(email)) {
            log.error("邮箱格式错误: {}", email);
            throw new ServiceException(ServiceErrorCodeConstants.MAIL_ERROR);
        }

        String key = VERIFICATION_CODE_PREFIX + email;
        String code = redisUtil.get(key);
        log.info("Redis key: {}", key);
        log.info("从Redis获取到的验证码: {}", code);
        log.info("========== 获取完成 ==========");

        return code;
    }
}