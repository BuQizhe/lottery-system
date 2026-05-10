package com.example.lotterysystem.service.impl;

import com.example.lotterysystem.common.errorcode.ServiceErrorCodeConstants;
import com.example.lotterysystem.common.exception.ServiceException;
import com.example.lotterysystem.controller.param.CreatePrizeParam;
import com.example.lotterysystem.controller.param.PageParam;
import com.example.lotterysystem.controller.param.PrizeUpdateParam;
import com.example.lotterysystem.dao.dataobject.PrizeDO;
import com.example.lotterysystem.dao.mapper.PrizeMapper;
import com.example.lotterysystem.service.PictureService;
import com.example.lotterysystem.service.PrizeService;
import com.example.lotterysystem.service.dto.PageListDTO;
import com.example.lotterysystem.service.dto.PrizeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class PrizeServiceImpl implements PrizeService {

    private static final Logger log = LoggerFactory.getLogger(PrizeServiceImpl.class);

    @Autowired
    private PictureService pictureService;
    @Autowired
    private PrizeMapper prizeMapper;

    @Override
    public Long createPrize(CreatePrizeParam param, MultipartFile picFile) {
        // 上传图片
        String fileName = pictureService.savePicture(picFile);
        // 存库
        PrizeDO prizeDO = new PrizeDO();
        prizeDO.setName(param.getPrizeName());
        prizeDO.setDescription(param.getDescription());
        prizeDO.setImageUrl(fileName);
        prizeDO.setPrice(param.getPrice());
        // 默认概率和库存
        prizeDO.setProbability(10.0);
        prizeDO.setStock(100);
        prizeMapper.insert(prizeDO);
        return prizeDO.getId();
    }

    @Override
    public PageListDTO<PrizeDTO> findPrizeList(PageParam param) {
        // 总量
        int total = prizeMapper.count();
        // 查询当前页列表
        List<PrizeDTO> prizeDTOList = new ArrayList<>();
        List<PrizeDO> prizeDOList = prizeMapper.selectPrizeList(param.offset(), param.getPageSize());
        for (PrizeDO prizeDO : prizeDOList) {
            PrizeDTO prizeDTO = new PrizeDTO();
            prizeDTO.setPrizeId(prizeDO.getId());
            prizeDTO.setName(prizeDO.getName());
            prizeDTO.setDescription(prizeDO.getDescription());
            prizeDTO.setImageUrl(prizeDO.getImageUrl());
            prizeDTO.setPrice(prizeDO.getPrice());
            prizeDTO.setProbability(prizeDO.getProbability());
            prizeDTO.setStock(prizeDO.getStock());
            prizeDTOList.add(prizeDTO);
        }
        return new PageListDTO<>(total, prizeDTOList);
    }

    @Override
    public void updateProbability(Long prizeId, Double probability) {
        if (prizeId == null) {
            throw new ServiceException(ServiceErrorCodeConstants.PRIZE_ID_IS_EMPTY);
        }
        if (probability == null || probability < 0 || probability > 100) {
            throw new ServiceException(ServiceErrorCodeConstants.PROBABILITY_ERROR);
        }
        int result = prizeMapper.updateProbability(prizeId, probability);
        if (result == 0) {
            throw new ServiceException(ServiceErrorCodeConstants.PRIZE_NOT_EXIST);
        }
        log.info("更新奖品概率成功，prizeId: {}, probability: {}", prizeId, probability);
    }

    @Override
    public void updateStock(Long prizeId, Integer stock) {
        if (prizeId == null) {
            throw new ServiceException(ServiceErrorCodeConstants.PRIZE_ID_IS_EMPTY);
        }
        if (stock == null || stock < 0) {
            throw new ServiceException(ServiceErrorCodeConstants.STOCK_ERROR);
        }
        int result = prizeMapper.updateStock(prizeId, stock);
        if (result == 0) {
            throw new ServiceException(ServiceErrorCodeConstants.PRIZE_NOT_EXIST);
        }
        log.info("更新奖品库存成功，prizeId: {}, stock: {}", prizeId, stock);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdate(List<PrizeUpdateParam> updates) {
        if (updates == null || updates.isEmpty()) {
            return;
        }
        // 改为循环单个更新，避免MySQL多语句执行问题
        int successCount = 0;
        for (PrizeUpdateParam param : updates) {
            try {
                if (param.getProbability() != null) {
                    prizeMapper.updateProbability(param.getPrizeId(), param.getProbability());
                }
                if (param.getStock() != null) {
                    prizeMapper.updateStock(param.getPrizeId(), param.getStock());
                }
                successCount++;
            } catch (Exception e) {
                log.error("更新奖品失败，prizeId: {}", param.getPrizeId(), e);
                throw new ServiceException(ServiceErrorCodeConstants.PRIZE_UPDATE_ERROR);
            }
        }
        log.info("批量更新奖品成功，更新数量: {}", successCount);
    }

    @Override
    public boolean decrementStock(Long prizeId) {
        if (prizeId == null) {
            return false;
        }
        int result = prizeMapper.decrementStock(prizeId);
        return result > 0;
    }
}