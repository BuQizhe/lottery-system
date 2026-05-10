package com.example.lotterysystem.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 系统用户实体
 * <p>
 * 角色分为三类：
 * <ul>
 *   <li>USER   — 普通用户，可参与抽奖</li>
 *   <li>ADMIN  — 管理员，可配置活动和奖品</li>
 *   <li>CLAIMER — 核销员，可扫码核销奖品</li>
 * </ul>
 * 密码字段使用 BCrypt 加密存储，不可逆。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Integer id;
    private String name;
    private String phone;
    private String email;
    /** BCrypt 加密后的密码，不可明文存储 */
    private String password;
    /** 角色：ADMIN / USER / CLAIMER */
    private String role;
    private LocalDateTime createTime;
}
