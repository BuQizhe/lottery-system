package com.example.lotterysystem.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.example.lotterysystem.common.errorcode.ServiceErrorCodeConstants;
import com.example.lotterysystem.common.exception.ServiceException;
import com.example.lotterysystem.common.utils.JWTUtil;
import com.example.lotterysystem.common.utils.RedisUtil;
import com.example.lotterysystem.common.utils.RegexUtil;
import com.example.lotterysystem.controller.param.EmailLoginParam;
import com.example.lotterysystem.controller.param.UserLoginParam;
import com.example.lotterysystem.controller.param.UserPasswordLoginParam;
import com.example.lotterysystem.controller.param.UserRegisterParam;
import com.example.lotterysystem.dao.dataobject.Encrypt;
import com.example.lotterysystem.dao.dataobject.UserDO;
import com.example.lotterysystem.dao.mapper.UserMapper;
import com.example.lotterysystem.service.UserService;
import com.example.lotterysystem.service.VerificationCodeService;
import com.example.lotterysystem.service.dto.UserDTO;
import com.example.lotterysystem.service.dto.UserLoginDTO;
import com.example.lotterysystem.service.dto.UserRegisterDTO;
import com.example.lotterysystem.service.enums.UserIdentityEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private VerificationCodeService verificationCodeService;
    @Autowired
    private RedisUtil redisUtil;

    @Override
    public UserRegisterDTO register(UserRegisterParam param) {

        // 校验注册信息
        checkRegisterInfo(param);

        // 加密私密数据（构造dao层请求）
        UserDO userDO = new UserDO();
        userDO.setUserName(param.getName());
        userDO.setEmail(param.getMail());
        userDO.setPhoneNumber(new Encrypt(param.getPhoneNumber()));
        userDO.setIdentity(param.getIdentity());
        if (StringUtils.hasText(param.getPassword())) {
            userDO.setPassword(DigestUtil.sha256Hex(param.getPassword()));
        }
        // 保存数据
        userMapper.insert(userDO);

        // 构造返回
        UserRegisterDTO userRegisterDTO = new UserRegisterDTO();
        userRegisterDTO.setUserId(userDO.getId());
        return userRegisterDTO;
    }

    @Override
    public UserLoginDTO login(UserLoginParam param) {
        UserLoginDTO userLoginDTO;

        // 类型检查与类型转换，java 14及以上版本
        if (param instanceof UserPasswordLoginParam loginParam) {
            // 密码登录流程
            userLoginDTO = loginByUserPassword(loginParam);
        } else if (param instanceof EmailLoginParam loginParam) {
            // 邮箱验证码登录流程
            userLoginDTO = loginByEmailCode(loginParam);
        } else {
            throw new ServiceException(ServiceErrorCodeConstants.LOGIN_INFO_NOT_EXIST);
        }

        return userLoginDTO;
    }

    @Override
    public List<UserDTO> findUserInfo(UserIdentityEnum identity) {
        String identityString =  null == identity ? null : identity.name();
        // 查表
        List<UserDO> userDOList = userMapper.selectUserListByIdentity(identityString);
        List<UserDTO> userDTOList = userDOList.stream()
                .map(userDO -> {
                    UserDTO userDTO = new UserDTO();
                    userDTO.setUserId(userDO.getId());
                    userDTO.setUserName(userDO.getUserName());
                    userDTO.setEmail(userDO.getEmail());
                    userDTO.setPhoneNumber(userDO.getPhoneNumber().getValue());
                    userDTO.setIdentity(UserIdentityEnum.forName(userDO.getIdentity()));
                    return userDTO;
                }).collect(Collectors.toList());
        return userDTOList;
    }

    /**
     * 邮箱验证码登录
     *
     * @param loginParam
     * @return
     */
    private UserLoginDTO loginByEmailCode(EmailLoginParam loginParam) {
        String email = loginParam.getEmail();
        String inputCode = loginParam.getVerificationCode();

        log.info("========== 邮箱验证码登录 ==========");
        log.info("邮箱: {}", email);
        log.info("输入的验证码: {}", inputCode);

        // 校验邮箱格式
        if (!RegexUtil.checkMail(email)) {
            log.error("邮箱格式错误: {}", email);
            throw new ServiceException(ServiceErrorCodeConstants.MAIL_ERROR);
        }

        // 获取用户数据
        UserDO userDO = userMapper.selectByMail(email);
        if (null == userDO) {
            log.error("用户不存在: {}", email);
            throw new ServiceException(ServiceErrorCodeConstants.USER_INFO_IS_EMPTY);
        } else if (StringUtils.hasText(loginParam.getMandatoryIdentity())
                && !loginParam.getMandatoryIdentity()
                .equalsIgnoreCase(userDO.getIdentity())) {
            log.error("身份不匹配: 需要={}, 实际={}", loginParam.getMandatoryIdentity(), userDO.getIdentity());
            throw new ServiceException(ServiceErrorCodeConstants.IDENTITY_ERROR);
        }

        // 校验验证码
        String savedCode = verificationCodeService.getVerificationCode(email);
        log.info("Redis中保存的验证码: {}", savedCode);

        if (!inputCode.equals(savedCode)) {
            log.error("验证码不匹配: 输入={}, 保存={}", inputCode, savedCode);
            throw new ServiceException(ServiceErrorCodeConstants.VERIFICATION_CODE_ERROR);
        }

        // 删除已使用的验证码
        redisUtil.del("EMAIL_CODE_" + email);
        log.info("验证码已删除");

        // 塞入返回值（JWT）
        Map<String, Object> claim = new HashMap<>();
        claim.put("id", userDO.getId());
        claim.put("identity", userDO.getIdentity());
        String token = JWTUtil.genJwt(claim);

        UserLoginDTO userLoginDTO = new UserLoginDTO();
        userLoginDTO.setToken(token);
        userLoginDTO.setIdentity(UserIdentityEnum.forName(userDO.getIdentity()));

        log.info("登录成功，用户: {}", email);
        log.info("========== 登录完成 ==========");
        return userLoginDTO;
    }

    /**
     * 密码登录
     *
     * @param loginParam
     * @return
     */
    private UserLoginDTO loginByUserPassword(UserPasswordLoginParam loginParam) {

        UserDO userDO = null;
        // 判断手机登录还是邮箱登录
        if (RegexUtil.checkMail(loginParam.getLoginName())) {
            // 邮箱登录
            // 根据邮箱查询用户表
            userDO = userMapper.selectByMail(loginParam.getLoginName());
        } else if (RegexUtil.checkMobile(loginParam.getLoginName())) {
            // 手机号登录
            // 根据手机号查询用户表
            userDO = userMapper.selectByPhoneNumber(new Encrypt(loginParam.getLoginName()));
        } else {
            throw new ServiceException(ServiceErrorCodeConstants.LOGIN_NOT_EXIST);
        }

        // 校验登录信息
        if (null == userDO) {
            throw new ServiceException(ServiceErrorCodeConstants.USER_INFO_IS_EMPTY);
        } else if (StringUtils.hasText(loginParam.getMandatoryIdentity())
                && !loginParam.getMandatoryIdentity()
                .equalsIgnoreCase(userDO.getIdentity())) {
            // 强制身份登录，身份校验不通过
            throw new ServiceException(ServiceErrorCodeConstants.IDENTITY_ERROR);
        } else if (!DigestUtil.sha256Hex(loginParam.getPassword())
                .equals(userDO.getPassword())) {
            // 校验密码不同
            throw new ServiceException(ServiceErrorCodeConstants.PASSWORD_ERROR);
        }

        // 塞入返回值（JWT）
        Map<String, Object> claim = new HashMap<>();
        claim.put("id", userDO.getId());
        claim.put("identity", userDO.getIdentity());
        String token = JWTUtil.genJwt(claim);

        UserLoginDTO userLoginDTO = new UserLoginDTO();
        userLoginDTO.setToken(token);
        userLoginDTO.setIdentity(UserIdentityEnum.forName(userDO.getIdentity()));
        return userLoginDTO;

    }

    private void checkRegisterInfo(UserRegisterParam param) {
        if (null == param) {
            throw new ServiceException(ServiceErrorCodeConstants.REGISTER_INFO_IS_EMPTY);
        }
        // 校验邮箱格式 xxx@xxx.xxx
        if (!RegexUtil.checkMail(param.getMail())) {
            throw new ServiceException(ServiceErrorCodeConstants.MAIL_ERROR);
        }
        // 校验手机号格式
        if (!RegexUtil.checkMobile(param.getPhoneNumber())) {
            throw new ServiceException(ServiceErrorCodeConstants.PHONE_NUMBER_ERROR);
        }

        // 校验身份信息
        if (null == UserIdentityEnum.forName(param.getIdentity())) {
            throw new ServiceException(ServiceErrorCodeConstants.IDENTITY_ERROR);
        }

        // 校验管理员密码必填
        if (param.getIdentity().equalsIgnoreCase(UserIdentityEnum.ADMIN.name())
                && !StringUtils.hasText(param.getPassword())) {
            throw new ServiceException(ServiceErrorCodeConstants.PASSWORD_IS_EMPTY);
        }

        // 密码校验，至少6位
        if (StringUtils.hasText(param.getPassword())
                && !RegexUtil.checkPassword(param.getPassword())) {
            throw new ServiceException(ServiceErrorCodeConstants.PASSWORD_ERROR);
        }

        // 校验邮箱是否被使用
        if (checkMailUsed(param.getMail())) {
            throw new ServiceException(ServiceErrorCodeConstants.MAIL_USED);
        }

        // 校验手机号是否被使用
        if (checkPhoneNumberUsed(param.getPhoneNumber())) {
            throw new ServiceException(ServiceErrorCodeConstants.PHONE_NUMBER_USED);
        }
    }

    /**
     * 校验手机号是否被使用
     *
     * @param phoneNumber
     * @return
     */
    private boolean checkPhoneNumberUsed(String phoneNumber) {
        int count = userMapper.countByPhone(new Encrypt(phoneNumber));
        return count > 0;

    }

    /**
     * 校验邮箱是否被使用
     *
     * @param mail
     * @return
     */
    private boolean checkMailUsed(String mail) {
        int count = userMapper.countByMail(mail);
        return count > 0;
    }

    // ========== 删除方法实现 ==========

    @Override
    public void deleteUser(Long userId) {
        if (userId == null) {
            throw new ServiceException(ServiceErrorCodeConstants.USER_ID_IS_EMPTY);
        }
        int result = userMapper.deleteById(userId);
        if (result == 0) {
            throw new ServiceException(ServiceErrorCodeConstants.USER_NOT_EXIST);
        }
        log.info("删除用户成功，userId: {}", userId);
    }

    @Override
    public void deleteUsers(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            throw new ServiceException(ServiceErrorCodeConstants.USER_ID_IS_EMPTY);
        }
        int result = userMapper.deleteByIds(userIds);
        log.info("批量删除用户成功，删除数量: {}", result);
    }

    // ========== 分页查询方法实现 ==========

    @Override
    public List<UserDTO> findUserPage(String identity, int offset, int size) {
        List<UserDO> userDOList = userMapper.selectUserPage(identity, offset, size);
        if (CollectionUtils.isEmpty(userDOList)) {
            return List.of();
        }
        return userDOList.stream()
                .map(this::convertToUserDTO)
                .collect(Collectors.toList());
    }

    @Override
    public int countUsers(String identity) {
        return userMapper.countUsers(identity);
    }

    /**
     * 将 UserDO 转换为 UserDTO
     */
    private UserDTO convertToUserDTO(UserDO userDO) {
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(userDO.getId());
        userDTO.setUserName(userDO.getUserName());
        userDTO.setEmail(userDO.getEmail());
        userDTO.setPhoneNumber(userDO.getPhoneNumber().getValue());
        userDTO.setIdentity(UserIdentityEnum.forName(userDO.getIdentity()));
        return userDTO;
    }

    // ========== 获取当前用户信息方法 ==========

    @Override
    public UserDTO getUserById(Long userId) {
        if (userId == null) {
            return null;
        }
        UserDO userDO = userMapper.selectById(userId);
        if (userDO == null) {
            return null;
        }
        return convertToUserDTO(userDO);
    }
}