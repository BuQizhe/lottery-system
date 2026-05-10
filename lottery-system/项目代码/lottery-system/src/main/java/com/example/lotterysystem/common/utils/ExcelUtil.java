package com.example.lotterysystem.common.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
public class ExcelUtil {

    /**
     * 导出中奖名单到 Excel
     */
    public void exportWinningRecords(HttpServletResponse response, List<Map<String, Object>> data, String activityName) {
        try (Workbook workbook = new XSSFWorkbook()) {
            // 创建 sheet
            Sheet sheet = workbook.createSheet("中奖名单");

            // 创建标题行样式
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // 创建内容样式
            CellStyle contentStyle = workbook.createCellStyle();
            contentStyle.setAlignment(HorizontalAlignment.CENTER);

            // 创建表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {"序号", "中奖者ID", "中奖者姓名", "奖品名称", "奖品等级", "中奖时间"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }

            // 填充数据
            int rowNum = 1;
            for (Map<String, Object> record : data) {
                Row row = sheet.createRow(rowNum);
                row.createCell(0).setCellValue(rowNum);
                row.createCell(1).setCellValue(record.get("winnerId") != null ?
                        String.valueOf(record.get("winnerId")) : "");
                row.createCell(2).setCellValue(record.get("winnerName") != null ?
                        String.valueOf(record.get("winnerName")) : "");
                row.createCell(3).setCellValue(record.get("prizeName") != null ?
                        String.valueOf(record.get("prizeName")) : "");
                row.createCell(4).setCellValue(record.get("prizeTier") != null ?
                        String.valueOf(record.get("prizeTier")) : "");
                row.createCell(5).setCellValue(record.get("winningTime") != null ?
                        String.valueOf(record.get("winningTime")) : "");
                rowNum++;
            }

            // 设置响应头
            String fileName = activityName + "_中奖名单_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition",
                    "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20"));

            // 写入响应
            try (OutputStream outputStream = response.getOutputStream()) {
                workbook.write(outputStream);
                outputStream.flush();
            }

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("导出Excel失败", e);
        }
    }
}