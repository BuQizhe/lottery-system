package com.example.lotterysystem.service.impl;

import com.example.lotterysystem.entity.User;
import com.example.lotterysystem.mapper.UserMapper;
import com.example.lotterysystem.service.UserService;
import com.example.lotterysystem.utils.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现
 * <p>
 * 登录逻辑说明：
 * <ul>
 *   <li>BCrypt 密码 — 以 $2a$ 开头，使用 PasswordUtil.matches() 验证</li>
 *   <li>明文密码 — 兼容旧数据，验证成功后自动升级为 BCrypt 加密</li>
 * </ul>
 * 注册时密码加密在 Controller 层完成（调用 PasswordUtil.encode()），
 * Service 层不再重复加密。
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean register(User user) {
        if (isPhoneExist(user.getPhone()) || isEmailExist(user.getEmail())) {
            return false;
        }
        user.setRole("USER");
        return userMapper.insert(user) > 0;
    }

    @Override
    public User loginByPhone(String phone, String password) {
        User user = userMapper.findByPhone(phone);
        if (user == null) return null;
        return authenticate(user, password);
    }

    @Override
    public User loginByEmail(String email, String password) {
        User user = userMapper.findByEmail(email);
        if (user == null) return null;
        return authenticate(user, password);
    }

    /**
     * 统一密码验证逻辑
     * <p>
     * BCrypt($2a$) → PasswordUtil.matches()
     * 明文（旧数据） → 直接比对，成功后自动升级为 BCrypt
     */
    private User authenticate(User user, String rawPassword) {
        String stored = user.getPassword();
        if (stored == null) return null;

        if (stored.startsWith("$2a$")) {
            return PasswordUtil.matches(rawPassword, stored) ? user : null;
        }
        // 明文密码，兼容旧数据
        if (rawPassword.equals(stored)) {
            user.setPassword(PasswordUtil.encode(rawPassword));
            userMapper.update(user);
            log.info("用户 {} 的密码已自动升级为 BCrypt 加密", user.getName());
            return user;
        }
        return null;
    }

    @Override
    public boolean isPhoneExist(String phone) {
        return userMapper.findByPhone(phone) != null;
    }

    @Override
    public boolean isEmailExist(String email) {
        return userMapper.findByEmail(email) != null;
    }

    @Override
    public User findById(Integer id) {
        return userMapper.findById(id);
    }

    @Override
    public User findByEmail(String email) {
        return userMapper.findByEmail(email);
    }
}
