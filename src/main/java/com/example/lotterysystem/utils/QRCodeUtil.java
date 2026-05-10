package com.example.lotterysystem.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

public class QRCodeUtil {

    private static final int WIDTH = 300;
    private static final int HEIGHT = 300;

    /**
     * 生成二维码图片（Base64格式）
     */
    public static String generateQRCodeBase64(String content) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, WIDTH, HEIGHT);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            byte[] bytes = baos.toByteArray();

            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (WriterException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 生成核销码（6位随机码）
     * 使用 SecureRandom 而非 Math.random()，防止被预测
     */
    private static final SecureRandom secureRandom = new SecureRandom();

    public static String generateClaimCode() {
        return String.format("%06d", secureRandom.nextInt(1000000));
    }
}