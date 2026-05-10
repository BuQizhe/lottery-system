package com.example.lotterysystem.service;

import com.example.lotterysystem.controller.param.CreatePrizeParam;
import com.example.lotterysystem.controller.param.PageParam;
import com.example.lotterysystem.controller.param.PrizeUpdateParam;
import com.example.lotterysystem.service.dto.PageListDTO;
import com.example.lotterysystem.service.dto.PrizeDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PrizeService {

    /**
     * 创建单个奖品
     *
     * @param param 奖品属性
     * @param picFile  上传的奖品图
     * @return  奖品id
     */
    Long createPrize(CreatePrizeParam param, MultipartFile picFile);

    /**
     * 翻页查询列表
     *
     * @param param
     * @return
     */
    PageListDTO<PrizeDTO> findPrizeList(PageParam param);

    /**
     * 更新奖品概率
     *
     * @param prizeId 奖品ID
     * @param probability 概率
     */
    void updateProbability(Long prizeId, Double probability);

    /**
     * 更新奖品库存
     *
     * @param prizeId 奖品ID
     * @param stock 库存
     */
    void updateStock(Long prizeId, Integer stock);

    /**
     * 批量更新奖品（概率和库存）
     *
     * @param updates 更新列表
     */
    void batchUpdate(List<PrizeUpdateParam> updates);

    /**
     * 扣减库存
     *
     * @param prizeId 奖品ID
     * @return 是否扣减成功
     */
    boolean decrementStock(Long prizeId);
}