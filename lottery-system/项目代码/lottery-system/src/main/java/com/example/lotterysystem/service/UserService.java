package com.example.lotterysystem.service;

import com.example.lotterysystem.controller.param.UserLoginParam;
import com.example.lotterysystem.controller.param.UserRegisterParam;
import com.example.lotterysystem.service.dto.UserDTO;
import com.example.lotterysystem.service.dto.UserLoginDTO;
import com.example.lotterysystem.service.dto.UserRegisterDTO;
import com.example.lotterysystem.service.enums.UserIdentityEnum;

import java.util.List;

public interface UserService {

    /**
     * 用户注册
     */
    UserRegisterDTO register(UserRegisterParam param);

    /**
     * 用户登录
     *   1、 密码
     *   2、 验证码
     *
     * @param param
     * @return
     */
    UserLoginDTO login(UserLoginParam param);

    /**
     * 根据身份查询人员列表
     *
     * @param identity: 如果为空，查询各个身份人员列表
     * @return
     */
    List<UserDTO> findUserInfo(UserIdentityEnum identity);

    // ========== 新增删除方法 ==========

    /**
     * 删除用户
     *
     * @param userId 用户ID
     */
    void deleteUser(Long userId);

    /**
     * 批量删除用户
     *
     * @param userIds 用户ID列表
     */
    void deleteUsers(List<Long> userIds);

    // ========== 分页查询方法 ==========

    /**
     * 分页查询用户列表
     *
     * @param identity 身份（可为空）
     * @param offset 起始位置
     * @param size 每页大小
     * @return 用户列表
     */
    List<UserDTO> findUserPage(String identity, int offset, int size);

    /**
     * 统计用户数量
     *
     * @param identity 身份（可为空）
     * @return 用户总数
     */
    int countUsers(String identity);

    // ========== 新增获取当前用户信息方法 ==========

    /**
     * 根据ID获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    UserDTO getUserById(Long userId);
}