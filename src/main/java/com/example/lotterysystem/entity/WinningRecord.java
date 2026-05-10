package com.example.lotterysystem.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 中奖记录实体
 * <p>
 * 每次抽中奖品生成一条记录，包含：
 * <ul>
 *   <li>qrCode — 领奖二维码（Base64），用户端展示</li>
 *   <li>claimCode — 6位核销码，核销员线下输入</li>
 *   <li>status — 0待领取 / 1已领取</li>
 * </ul>
 * 二维码内容为：http://host/claim.html?code={claimCode}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WinningRecord {
    private Integer id;
    private Integer userId;
    private Integer activityId;
    private Integer prizeId;
    private String prizeName;
    private String winningCode;
    /** 0-待领取, 1-已领取 */
    private Integer status;
    private LocalDateTime drawTime;
    private LocalDateTime claimTime;
    /** 领奖二维码 Base64 */
    private String qrCode;
    /** 6位核销码 */
    private String claimCode;
    /** 用户名（JOIN 查询填充） */
    private String userName;
    /** 手机号（JOIN 查询填充） */
    private String phone;
    /** 邮箱（JOIN 查询填充） */
    private String email;
    /** 奖品图片URL（JOIN 查询填充） */
    private String prizeImageUrl;
}
