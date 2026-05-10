package com.example.lotterysystem.service.impl;

import com.example.lotterysystem.common.errorcode.ServiceErrorCodeConstants;
import com.example.lotterysystem.common.exception.ServiceException;
import com.example.lotterysystem.service.PictureService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class PictureServiceImpl implements PictureService {

    @Value("${pic.local-path}")
    private String localPath;

    @Override
    public String savePicture(MultipartFile multipartFile) {
        try {
            // 确保目录存在
            Path path = Paths.get(localPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                System.out.println("创建目录: " + localPath);
            }

            // 获取原文件名和后缀
            String originalFilename = multipartFile.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                throw new ServiceException(ServiceErrorCodeConstants.PIC_UPLOAD_ERROR);
            }

            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = UUID.randomUUID() + suffix;

            // 保存文件
            Path filePath = path.resolve(filename);
            multipartFile.transferTo(filePath.toFile());

            System.out.println("图片保存成功: " + filePath.toString());
            return filename;

        } catch (IOException e) {
            System.err.println("图片保存失败: " + e.getMessage());
            e.printStackTrace();
            throw new ServiceException(ServiceErrorCodeConstants.PIC_UPLOAD_ERROR);
        }
    }
}