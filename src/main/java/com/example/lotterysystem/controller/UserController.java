package com.example.lotterysystem.controller;

import com.example.lotterysystem.annotation.RateLimit;
import com.example.lotterysystem.common.exception.BusinessException;
import com.example.lotterysystem.common.result.ApiResponse;
import com.example.lotterysystem.common.result.ResultCode;
import com.example.lotterysystem.entity.User;
import com.example.lotterysystem.entity.VerificationCode;
import com.example.lotterysystem.mapper.VerificationCodeMapper;
import com.example.lotterysystem.service.MailService;
import com.example.lotterysystem.service.UserService;
import com.example.lotterysystem.utils.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

/**
 * 用户端接口：注册、多渠道登录、验证码
 * <p>
 * 登录方式支持三种：手机号+密码、邮箱+密码、邮箱+验证码。
 * 关键接口均标注 {@code @RateLimit} 防刷。
 */
@RestController
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired private UserService userService;
    @Autowired private VerificationCodeMapper verificationCodeMapper;
    @Autowired private MailService mailService;

    // ==================== 注册 ====================

    @RateLimit(key = "register", time = 60, count = 3, message = "注册太频繁，请稍后再试")
    @PostMapping("/api/register")
    public ApiResponse<?> register(@RequestBody User user) {
        if (userService.isPhoneExist(user.getPhone())) {
            return ApiResponse.fail(ResultCode.BAD_REQUEST, "手机号已注册");
        }
        if (userService.isEmailExist(user.getEmail())) {
            return ApiResponse.fail(ResultCode.BAD_REQUEST, "邮箱已注册");
        }
        user.setPassword(PasswordUtil.encode(user.getPassword()));
        boolean ok = userService.register(user);
        return ok ? ApiResponse.success("注册成功") : ApiResponse.fail(ResultCode.ERROR, "注册失败");
    }

    // ==================== 登录 ====================

    @RateLimit(key = "login", time = 60, count = 10, message = "登录太频繁，请稍后再试")
    @PostMapping("/api/login/phone")
    public ApiResponse<?> loginByPhone(@RequestBody Map<String, String> params, HttpSession session) {
        String phone = params.get("phone");
        String password = params.get("password");
        if (phone == null || password == null) {
            return ApiResponse.fail(ResultCode.BAD_REQUEST, "请填写完整信息");
        }
        User user = userService.loginByPhone(phone, password);
        if (user == null) {
            return ApiResponse.fail(ResultCode.BAD_REQUEST, "手机号或密码错误");
        }
        session.setAttribute("user", user);
        return ApiResponse.success(Map.of("role", user.getRole()), "登录成功");
    }

    @RateLimit(key = "login", time = 60, count = 10, message = "登录太频繁，请稍后再试")
    @PostMapping("/api/login/email")
    public ApiResponse<?> loginByEmail(@RequestBody Map<String, String> params, HttpSession session) {
        String email = params.get("email");
        String password = params.get("password");
        if (email == null || password == null) {
            return ApiResponse.fail(ResultCode.BAD_REQUEST, "请填写完整信息");
        }
        User user = userService.loginByEmail(email, password);
        if (user == null) {
            return ApiResponse.fail(ResultCode.BAD_REQUEST, "邮箱或密码错误");
        }
        session.setAttribute("user", user);
        return ApiResponse.success(Map.of("role", user.getRole()), "登录成功");
    }

    // ==================== 验证码登录 ====================

    @RateLimit(key = "sendCode", time = 60, count = 3, message = "验证码发送太频繁，请稍后再试")
    @PostMapping("/api/sendCode")
    public ApiResponse<?> sendCode(@RequestBody Map<String, String> params) {
        String email = params.get("email");
        if (!userService.isEmailExist(email)) {
            return ApiResponse.fail(ResultCode.BAD_REQUEST, "邮箱未注册");
        }
        String code = String.format("%06d", new Random().nextInt(999999));

        VerificationCode vc = new VerificationCode();
        vc.setEmail(email);
        vc.setCode(code);
        vc.setType("LOGIN");
        vc.setExpireTime(LocalDateTime.now().plusMinutes(5));
        vc.setUsed(0);
        verificationCodeMapper.insert(vc);

        try {
            mailService.sendVerificationCode(email, code);
            return ApiResponse.success("验证码已发送");
        } catch (Exception e) {
            log.error("邮件发送失败", e);
            return ApiResponse.fail(ResultCode.ERROR, "验证码发送失败，请稍后重试");
        }
    }

    @RateLimit(key = "login", time = 60, count = 10, message = "登录太频繁，请稍后再试")
    @PostMapping("/api/login/emailCode")
    public ApiResponse<?> loginByEmailCode(@RequestBody Map<String, String> params, HttpSession session) {
        String email = params.get("email");
        String code = params.get("code");
        VerificationCode vc = verificationCodeMapper.findValidCode(email, code, "LOGIN");
        if (vc == null) {
            return ApiResponse.fail(ResultCode.BAD_REQUEST, "验证码无效或已过期");
        }
        User user = userService.findByEmail(email);
        if (user == null) {
            return ApiResponse.fail(ResultCode.BAD_REQUEST, "邮箱未注册");
        }
        verificationCodeMapper.markUsed(vc.getId());
        session.setAttribute("user", user);
        return ApiResponse.success(Map.of("role", user.getRole()), "登录成功");
    }

    // ==================== 登出 / 当前用户 ====================

    @GetMapping("/api/logout")
    public ApiResponse<?> logout(HttpSession session) {
        session.invalidate();
        return ApiResponse.success();
    }

    @GetMapping("/api/currentUser")
    public ApiResponse<?> getCurrentUser(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ApiResponse.fail(ResultCode.UNAUTHORIZED);
        }
        return ApiResponse.success(user);
    }
}
