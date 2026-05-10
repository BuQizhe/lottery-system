package com.example.lotterysystem.dao.dataobject;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class PrizeDO extends BaseDO{

    /**
     * 奖品名
     */
    private String name;

    /**
     * 图片索引
     */
    private String imageUrl;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 描述
     */
    private String description;

    /**
     * 中奖概率（百分比，如 10.0 表示 10%）
     */
    private Double probability;

    /**
     * 剩余库存
     */
    private Integer stock;
}