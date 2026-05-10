package com.example.lotterysystem.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 邮箱验证码实体
 * <p>
 * 验证码有效期 5 分钟，使用后标记为 used=1。
 * type 字段区分用途（LOGIN / REGISTER），支持未来扩展。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationCode {
    private Integer id;
    private String email;
    private String code;
    /** 验证码类型：LOGIN / REGISTER */
    private String type;
    private LocalDateTime expireTime;
    /** 0-未使用, 1-已使用 */
    private Integer used;
    private LocalDateTime createTime;
}
