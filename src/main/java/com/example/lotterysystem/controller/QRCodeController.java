package com.example.lotterysystem.controller;

import com.example.lotterysystem.common.result.ApiResponse;
import com.example.lotterysystem.utils.QRCodeDecoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 二维码解析接口
 * <p>
 * 核销端上传二维码图片（Base64），解析出核销码。
 */
@RestController
@RequestMapping("/api/qr")
public class QRCodeController {

    @PostMapping("/decode")
    public ApiResponse<?> decodeQRCode(@RequestBody Map<String, String> params) {
        String code = QRCodeDecoder.decodeQRCode(params.get("image"));
        if (code == null) {
            return ApiResponse.success(Map.of("code", (String) null), "未识别到二维码");
        }
        return ApiResponse.success(Map.of("code", code));
    }
}
