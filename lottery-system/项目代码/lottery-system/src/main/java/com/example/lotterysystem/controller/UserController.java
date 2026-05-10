package com.example.lotterysystem.controller;

import com.example.lotterysystem.common.errorcode.ControllerErrorCodeConstants;
import com.example.lotterysystem.common.exception.ControllerException;
import com.example.lotterysystem.common.pojo.CommonResult;
import com.example.lotterysystem.common.utils.JacksonUtil;
import com.example.lotterysystem.common.utils.JWTUtil;
import com.example.lotterysystem.common.utils.RedisUtil;
import com.example.lotterysystem.controller.param.EmailLoginParam;
import com.example.lotterysystem.controller.param.UserPasswordLoginParam;
import com.example.lotterysystem.controller.param.UserRegisterParam;
import com.example.lotterysystem.controller.result.BaseUserInfoResult;
import com.example.lotterysystem.controller.result.PageResult;
import com.example.lotterysystem.controller.result.UserInfoResult;
import com.example.lotterysystem.controller.result.UserLoginResult;
import com.example.lotterysystem.controller.result.UserRegisterResult;
import com.example.lotterysystem.service.UserService;
import com.example.lotterysystem.service.VerificationCodeService;
import com.example.lotterysystem.service.dto.UserDTO;
import com.example.lotterysystem.service.dto.UserLoginDTO;
import com.example.lotterysystem.service.dto.UserRegisterDTO;
import com.example.lotterysystem.service.enums.UserIdentityEnum;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;
    @Autowired
    private VerificationCodeService verificationCodeService;
    @Autowired
    private RedisUtil redisUtil;

    /**
     * 注册
     */
    @RequestMapping("/register")
    public CommonResult<UserRegisterResult> userRegister(
            @Validated @RequestBody UserRegisterParam param) {
        logger.info("userRegister UserRegisterParam:{}", JacksonUtil.writeValueAsString(param));
        UserRegisterDTO userRegisterDTO = userService.register(param);
        return CommonResult.success(convertToUserRegisterResult(userRegisterDTO));
    }

    /**
     * 发送邮箱验证码
     */
    @GetMapping("/email-code/send")
    public CommonResult<Boolean> sendEmailCode(@RequestParam String email) {
        logger.info("sendEmailCode email:{}", email);
        verificationCodeService.sendVerificationCode(email);
        return CommonResult.success(Boolean.TRUE);
    }

    /**
     * 密码登录（无验证码）
     */
    @PostMapping("/password/login")
    public CommonResult<UserLoginResult> userPasswordLogin(
            @Validated @RequestBody UserPasswordLoginParam param) {

        logger.info("userPasswordLogin UserPasswordLoginParam:{}",
                JacksonUtil.writeValueAsString(param));

        UserLoginDTO userLoginDTO = userService.login(param);
        return CommonResult.success(convertToUserLoginResult(userLoginDTO));
    }

    /**
     * 邮箱验证码登录
     */
    @PostMapping("/email-code/login")
    public CommonResult<UserLoginResult> emailCodeLogin(
            @Validated @RequestBody EmailLoginParam param) {
        logger.info("emailCodeLogin EmailLoginParam:{}",
                JacksonUtil.writeValueAsString(param));
        UserLoginDTO userLoginDTO = userService.login(param);
        return CommonResult.success(convertToUserLoginResult(userLoginDTO));
    }

    /**
     * 查询用户列表
     */
    @RequestMapping("/base-user/find-list")
    public CommonResult<List<BaseUserInfoResult>> findBaseUserInfo(String identity) {
        logger.info("findBaseUserInfo identity:{}", identity);
        List<UserDTO> userDTOList = userService.findUserInfo(
                UserIdentityEnum.forName(identity));
        return CommonResult.success(convertToList(userDTOList));
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/user/current")
    public CommonResult<UserInfoResult> getCurrentUser(@RequestHeader("user_token") String token) {
        logger.info("getCurrentUser token:{}", token);

        // 解析token获取用户ID
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            throw new ControllerException(ControllerErrorCodeConstants.LOGIN_ERROR);
        }

        // 获取用户信息
        UserDTO userDTO = userService.getUserById(userId);
        if (userDTO == null) {
            throw new ControllerException(ControllerErrorCodeConstants.LOGIN_ERROR);
        }

        UserInfoResult result = new UserInfoResult();
        result.setUserId(userDTO.getUserId());
        result.setUserName(userDTO.getUserName());
        result.setEmail(userDTO.getEmail());
        result.setPhoneNumber(userDTO.getPhoneNumber());
        result.setIdentity(userDTO.getIdentity().name());

        return CommonResult.success(result);
    }

    // ========== 删除用户接口 ==========

    @DeleteMapping("/user/delete")
    public CommonResult<Boolean> deleteUser(@RequestParam Long userId) {
        logger.info("deleteUser userId:{}", userId);
        userService.deleteUser(userId);
        return CommonResult.success(true);
    }

    @DeleteMapping("/user/delete/batch")
    public CommonResult<Boolean> deleteUsers(@RequestBody List<Long> userIds) {
        logger.info("deleteUsers userIds:{}", userIds);
        userService.deleteUsers(userIds);
        return CommonResult.success(true);
    }

    // ========== 分页查询用户接口 ==========

    @GetMapping("/base-user/page-list")
    public CommonResult<PageResult<BaseUserInfoResult>> findUserPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String identity) {
        logger.info("findUserPage page:{}, size:{}, identity:{}", page, size, identity);

        int offset = (page - 1) * size;
        List<UserDTO> userDTOList = userService.findUserPage(identity, offset, size);
        int total = userService.countUsers(identity);
        List<BaseUserInfoResult> records = convertToList(userDTOList);

        PageResult<BaseUserInfoResult> pageResult = new PageResult<>();
        pageResult.setTotal(total);
        pageResult.setRecords(records);
        pageResult.setCurrentPage(page);
        pageResult.setPageSize(size);

        return CommonResult.success(pageResult);
    }

    private List<BaseUserInfoResult> convertToList(List<UserDTO> userDTOList) {
        if (CollectionUtils.isEmpty(userDTOList)) {
            return Arrays.asList();
        }
        return userDTOList.stream()
                .map(userDTO -> {
                    BaseUserInfoResult result = new BaseUserInfoResult();
                    result.setUserId(userDTO.getUserId());
                    result.setUserName(userDTO.getUserName());
                    result.setIdentity(userDTO.getIdentity().name());
                    return result;
                }).collect(Collectors.toList());
    }

    private UserLoginResult convertToUserLoginResult(UserLoginDTO userLoginDTO) {
        if (null == userLoginDTO) {
            throw new ControllerException(ControllerErrorCodeConstants.LOGIN_ERROR);
        }
        UserLoginResult result = new UserLoginResult();
        result.setToken(userLoginDTO.getToken());
        result.setIdentity(userLoginDTO.getIdentity().name());
        return result;
    }

    private UserRegisterResult convertToUserRegisterResult(UserRegisterDTO userRegisterDTO) {
        UserRegisterResult result = new UserRegisterResult();
        if (null == userRegisterDTO) {
            throw new ControllerException(ControllerErrorCodeConstants.REGISTER_ERROR);
        }
        result.setUserId(userRegisterDTO.getUserId());
        return result;
    }

    /**
     * 从token中获取用户ID
     */
    private Long getUserIdFromToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        try {
            Claims claims = JWTUtil.parseJWT(token);
            if (claims == null) {
                return null;
            }
            Object userId = claims.get("id");
            if (userId == null) {
                userId = claims.get("userId");
            }
            if (userId instanceof Integer) {
                return ((Integer) userId).longValue();
            }
            if (userId instanceof Long) {
                return (Long) userId;
            }
            if (userId instanceof String) {
                return Long.parseLong((String) userId);
            }
            return null;
        } catch (Exception e) {
            logger.error("解析token失败", e);
            return null;
        }
    }
}