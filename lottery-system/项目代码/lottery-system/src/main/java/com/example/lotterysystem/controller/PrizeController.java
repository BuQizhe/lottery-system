package com.example.lotterysystem.controller;

import com.example.lotterysystem.common.errorcode.ControllerErrorCodeConstants;
import com.example.lotterysystem.common.exception.ControllerException;
import com.example.lotterysystem.common.pojo.CommonResult;
import com.example.lotterysystem.common.utils.JacksonUtil;
import com.example.lotterysystem.controller.param.CreatePrizeParam;
import com.example.lotterysystem.controller.param.PageParam;
import com.example.lotterysystem.controller.param.PrizeUpdateParam;
import com.example.lotterysystem.controller.result.FindPrizeListResult;
import com.example.lotterysystem.service.PictureService;
import com.example.lotterysystem.service.PrizeService;
import com.example.lotterysystem.service.dto.PageListDTO;
import com.example.lotterysystem.service.dto.PrizeDTO;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;


@RestController
public class PrizeController {

    private static final Logger logger = LoggerFactory.getLogger(PrizeController.class);

    @Autowired
    private PictureService pictureService;
    @Autowired
    private PrizeService prizeService;

    @RequestMapping("/pic/upload")
    public String uploadPic(MultipartFile file) {
        return pictureService.savePicture(file);
    }

    /**
     * 创建奖品
     */
    @RequestMapping("/prize/create")
    public CommonResult<Long> createPrize(@Valid @RequestPart("param") CreatePrizeParam param,
                                          @RequestPart("prizePic") MultipartFile picFile) {
        logger.info("createPrize CreatePrizeParam:{}",
                JacksonUtil.writeValueAsString(param));
        return CommonResult.success(
                prizeService.createPrize(param, picFile));
    }

    @RequestMapping("/prize/find-list")
    public CommonResult<FindPrizeListResult> findPrizeList(PageParam param) {
        logger.info("findPrizeList PageParam:{}",
                JacksonUtil.writeValueAsString(param));
        PageListDTO<PrizeDTO> pageListDTO = prizeService.findPrizeList(param);
        return CommonResult.success(convertToFindPrizeListResult(pageListDTO));
    }

    /**
     * 更新奖品概率
     */
    @PostMapping("/prize/update-probability")
    public CommonResult<Boolean> updateProbability(@RequestParam Long prizeId,
                                                   @RequestParam Double probability) {
        logger.info("updateProbability prizeId:{}, probability:{}", prizeId, probability);
        prizeService.updateProbability(prizeId, probability);
        return CommonResult.success(true);
    }

    /**
     * 更新奖品库存
     */
    @PostMapping("/prize/update-stock")
    public CommonResult<Boolean> updateStock(@RequestParam Long prizeId,
                                             @RequestParam Integer stock) {
        logger.info("updateStock prizeId:{}, stock:{}", prizeId, stock);
        prizeService.updateStock(prizeId, stock);
        return CommonResult.success(true);
    }

    /**
     * 批量更新奖品（概率和库存）
     */
    @PostMapping("/prize/batch-update")
    public CommonResult<Boolean> batchUpdate(@RequestBody List<PrizeUpdateParam> updates) {
        logger.info("batchUpdate updates:{}", JacksonUtil.writeValueAsString(updates));
        prizeService.batchUpdate(updates);
        return CommonResult.success(true);
    }

    /**
     * 获取所有奖品列表（用于会员中心）
     */
    @GetMapping("/prize/all-list")
    public CommonResult<List<PrizeDTO>> getAllPrizes() {
        logger.info("getAllPrizes");
        PageParam param = new PageParam();
        param.setCurrentPage(1);
        param.setPageSize(100);
        PageListDTO<PrizeDTO> pageListDTO = prizeService.findPrizeList(param);
        return CommonResult.success(pageListDTO.getRecords());
    }

    private FindPrizeListResult convertToFindPrizeListResult(PageListDTO<PrizeDTO> pageListDTO) {
        if (null == pageListDTO) {
            throw new ControllerException(ControllerErrorCodeConstants.FIND_PRIZE_LIST_ERROR);
        }

        FindPrizeListResult result = new FindPrizeListResult();
        result.setTotal(pageListDTO.getTotal());
        result.setRecords(
                pageListDTO.getRecords().stream()
                        .map(prizeDTO -> {
                            FindPrizeListResult.PrizeInfo prizeInfo = new FindPrizeListResult.PrizeInfo();
                            prizeInfo.setPrizeId(prizeDTO.getPrizeId());
                            prizeInfo.setPrizeName(prizeDTO.getName());
                            prizeInfo.setDescription(prizeDTO.getDescription());
                            prizeInfo.setImageUrl(prizeDTO.getImageUrl());
                            prizeInfo.setPrice(prizeDTO.getPrice());
                            return prizeInfo;
                        }).collect(Collectors.toList())
        );
        return result;
    }
}