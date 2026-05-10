package com.example.lotterysystem.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SMSUtil {
    private static final Logger logger = LoggerFactory.getLogger(SMSUtil.class);

    @Value(value = "${sms.sign-name:test-sign}")
    private String signName;
    @Value(value = "${sms.access-key-id:test-key}")
    private String accessKeyId;
    @Value(value = "${sms.access-key-secret:test-secret}")
    private String accessKeySecret;

    /**
     * 发送短信（临时版本：只打印日志，不实际发送）
     *
     * @param templateCode 模板号
     * @param phoneNumbers 手机号
     * @param templateParam 模板参数 {"key":"value"}
     */
    public void sendMessage(String templateCode, String phoneNumbers, String templateParam) {
        logger.info("========== 【短信功能已临时禁用】==========");
        logger.info("如果要发送短信，需要：");
        logger.info("1. 注册阿里云账号");
        logger.info("2. 开通短信服务");
        logger.info("3. 申请签名和模板");
        logger.info("========================================");
        logger.info("当前收到的短信请求：");
        logger.info("  手机号: {}", phoneNumbers);
        logger.info("  模板号: {}", templateCode);
        logger.info("  模板参数: {}", templateParam);
        logger.info("========================================");

        // 实际发送短信的代码已注释，如需启用请配置阿里云短信服务
    }
}