package com.example.lotterysystem.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 邮件服务 — 发送验证码和中奖通知
 *
 * 修复：
 * 1. System.out.println 改为 SLF4J logger
 * 2. HTML 内容中对用户输入做转义，防止 XSS 注入
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * 发送验证码邮件
     */
    public void sendVerificationCode(String toEmail, String code) {
        log.info("发送验证码邮件: to={}", toEmail);
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("抽奖系统 - 登录验证码");

            String htmlContent = "<div style='font-family: Microsoft YaHei; padding: 20px;'>" +
                    "<h2 style='color: #667eea;'>抽奖系统</h2>" +
                    "<p>您好！</p>" +
                    "<p>您的登录验证码是：</p>" +
                    "<h1 style='color: #ff6b6b; font-size: 32px; letter-spacing: 5px;'>" +
                    escapeHtml(code) + "</h1>" +
                    "<p>验证码有效期为 <strong>5分钟</strong>，请尽快使用。</p>" +
                    "<hr><p style='color: #999; font-size: 12px;'>此邮件由系统自动发送，请勿回复。</p>" +
                    "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            log.info("验证码邮件发送成功: {}", toEmail);
        } catch (Exception e) {
            log.error("验证码邮件发送失败: {}", toEmail, e);
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    /**
     * 发送中奖通知邮件
     */
    public void sendWinningNotification(String toEmail, String userName,
                                         String prizeName, String activityName) {
        log.info("发送中奖邮件: to={}, prize={}", toEmail, prizeName);
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("恭喜中奖！- 抽奖系统");

            // 对所有用户输入做 HTML 转义，防止 XSS 注入
            String safeUserName = escapeHtml(userName);
            String safePrizeName = escapeHtml(prizeName);
            String safeActivityName = escapeHtml(activityName);

            String htmlContent = "<div style='font-family: Microsoft YaHei; padding: 20px;'>" +
                    "<h2 style='color: #667eea;'>恭喜中奖！</h2>" +
                    "<p>尊敬的 " + safeUserName + "：</p>" +
                    "<p>恭喜您在「<strong>" + safeActivityName + "</strong>」中抽中：</p>" +
                    "<h1 style='color: #ff6b6b; font-size: 28px;'>" + safePrizeName + "</h1>" +
                    "<p>请登录系统领取奖品。</p>" +
                    "<hr><p style='color: #999; font-size: 12px;'>此邮件由系统自动发送，请勿回复。</p>" +
                    "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            log.info("中奖邮件发送成功: {}", toEmail);
        } catch (Exception e) {
            log.error("中奖邮件发送失败: {}", toEmail, e);
        }
    }

    /**
     * HTML 转义：将特殊字符转为 HTML 实体，防止 XSS 注入
     * 例如 <script> 会变成 &lt;script&gt;，浏览器会当作文本显示而不会执行
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }
}
