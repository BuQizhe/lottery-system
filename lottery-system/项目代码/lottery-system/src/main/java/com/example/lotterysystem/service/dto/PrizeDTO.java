package com.example.lotterysystem.service.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PrizeDTO {

    /**
     * 奖品Id
     */
    private Long prizeId;

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
     * 中奖概率（百分比）
     */
    private Double probability;

    /**
     * 剩余库存
     */
    private Integer stock;
}