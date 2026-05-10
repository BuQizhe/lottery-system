package com.example.lotterysystem.controller.param;

import lombok.Data;

import java.io.Serializable;

@Data
public class PrizeUpdateParam implements Serializable {
    /**
     * 奖品ID
     */
    private Long prizeId;

    /**
     * 中奖概率（百分比）
     */
    private Double probability;

    /**
     * 剩余库存
     */
    private Integer stock;
}