package com.example.lotterysystem.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 奖品实体
 * <p>
 * stock 为总库存，remaining 为剩余可抽数量。
 * 每次中奖时通过 SQL {@code UPDATE ... SET remaining = remaining - 1 WHERE remaining > 0}
 * 原子扣减，避免超发。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Prize {
    private Integer id;
    private String name;
    private String description;
    /** 总库存 */
    private Integer stock;
    /** 剩余库存（原子扣减） */
    private Integer remaining;
    /** 奖品等级（1最高），用于排序 */
    private Integer level;
    /** 奖品图片相对路径，如 /uploads/prizes/xxx.jpg */
    private String imageUrl;
    private LocalDateTime createTime;
}
