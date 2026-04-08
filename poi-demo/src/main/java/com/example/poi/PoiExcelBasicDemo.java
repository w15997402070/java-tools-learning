package com.example.poi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Apache POI Excel 基础演示类
 *
 * 功能覆盖：
 * 1. 创建新的 Excel 工作簿 (.xlsx格式)
 * 2. 创建/重命名/复制工作表
 * 3. 写入各种类型的数据（字符串、数字、日期、布尔值）
 * 4. 读取 Excel 文件内容
 * 5. 单元格样式设置基础
 *
 * @author JavaTools Team
 */
public class PoiExcelBasicDemo {

    private static final Logger logger = LogManager.getLogger(PoiExcelBasicDemo.class);

    // 输出目录
    private static final String OUTPUT_DIR = "src/main/resources/";

    /**
     * 演示1: 创建新的 Excel 工作簿
     * 使用 XSSFWorkbook 创建 .xlsx 格式（支持较大数据量，2007+格式）
     */
    public static void createNewWorkbook() throws IOException {
        logger.info("=== 创建新的 Excel 工作簿 ===");

        // 创建新的工作簿
        try (Workbook workbook = new XSSFWorkbook()) {
            // 创建工作表
            Sheet sheet = workbook.createSheet("学生信息");

            // 创建表头行
            Row headerRow = sheet.createRow(0);
            String[] headers = {"学号", "姓名", "年龄", "班级", "成绩"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // 创建数据行
            Object[][] data = {
                {"001", "张三", 18, "高一(1)班", 95.5},
                {"002", "李四", 17, "高一(2)班", 88.0},
                {"003", "王五", 18, "高一(1)班", 92.5},
                {"004", "赵六", 17, "高一(3)班", 78.0}
            };

            for (int rowIdx = 0; rowIdx < data.length; rowIdx++) {
                Row row = sheet.createRow(rowIdx + 1);
                for (int colIdx = 0; colIdx < data[rowIdx].length; colIdx++) {
                    Cell cell = row.createCell(colIdx);
                    Object value = data[rowIdx][colIdx];

                    if (value instanceof String) {
                        cell.setCellValue((String) value);
                    } else if (value instanceof Integer) {
                        cell.setCellValue((Integer) value);
                    } else if (value instanceof Double) {
                        cell.setCellValue((Double) value);
                    }
                }
            }

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 写入文件
            String filePath = OUTPUT_DIR + "students_basic.xlsx";
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }

            logger.info("成功创建 Excel 文件: {}", filePath);
        }
    }

    /**
     * 演示2: 读取 Excel 文件
     * 展示如何遍历工作表、读取各种类型的单元格值
     */
    public static void readExcelFile() throws IOException {
        String filePath = OUTPUT_DIR + "students_basic.xlsx";
        logger.info("=== 读取 Excel 文件: {} ===", filePath);

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            // 获取第一个工作表
            Sheet sheet = workbook.getSheetAt(0);

            // 遍历所有行
            for (Row row : sheet) {
                StringBuilder rowData = new StringBuilder();
                for (Cell cell : row) {
                    rowData.append(getCellValue(cell)).append(" | ");
                }
                logger.info("行 {}: {}", row.getRowNum(), rowData.toString());
            }
        }
    }

    /**
     * 演示3: 工作表操作
     * 创建、重命名、删除、复制工作表
     */
    public static void manageSheets() throws IOException {
        logger.info("=== 工作表管理操作 ===");

        try (Workbook workbook = new XSSFWorkbook()) {
            // 创建多个工作表
            Sheet sheet1 = workbook.createSheet("一月");
            Sheet sheet2 = workbook.createSheet("二月");
            Sheet sheet3 = workbook.createSheet("三月");

            // 在工作表中写入一些数据
            Row row = sheet1.createRow(0);
            row.createCell(0).setCellValue("销售报表");

            // 重命名工作表
            workbook.setSheetName(0, "Q1_Jan");

            // 创建输出文件
            String filePath = OUTPUT_DIR + "multi_sheets.xlsx";
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }

            logger.info("成功创建包含多个工作表的文件: {}", filePath);

            // 遍历所有工作表名称
            List<String> sheetNames = new ArrayList<>();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                sheetNames.add(workbook.getSheetName(i));
            }
            logger.info("工作表列表: {}", sheetNames);
        }
    }

    /**
     * 演示4: 单元格样式设置
     * 设置字体、颜色、边框、对齐方式等
     */
    public static void styleCells() throws IOException {
        logger.info("=== 单元格样式设置 ===");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("格式化报表");

            // 创建表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 14);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // 设置边框
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // 创建表头行
            Row headerRow = sheet.createRow(0);
            String[] headers = {"产品", "销量", "销售额", "增长率"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 数据行样式
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setAlignment(HorizontalAlignment.CENTER);

            // 添加数据
            Object[][] data = {
                {"手机", 1200, 6000000.00, "15%"},
                {"电脑", 500, 3500000.00, "8%"},
                {"平板", 300, 1200000.00, "-3%"}
            };

            for (int rowIdx = 0; rowIdx < data.length; rowIdx++) {
                Row row = sheet.createRow(rowIdx + 1);
                for (int colIdx = 0; colIdx < data[rowIdx].length; colIdx++) {
                    Cell cell = row.createCell(colIdx);
                    cell.setCellStyle(dataStyle);
                    Object value = data[rowIdx][colIdx];
                    if (value instanceof String) {
                        cell.setCellValue((String) value);
                    } else if (value instanceof Integer) {
                        cell.setCellValue((Integer) value);
                    } else if (value instanceof Double) {
                        cell.setCellValue((Double) value);
                    }
                }
            }

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            String filePath = OUTPUT_DIR + "styled_report.xlsx";
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }

            logger.info("成功创建格式化报表: {}", filePath);
        }
    }

    /**
     * 辅助方法: 获取单元格的值
     */
    private static String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return "公式: " + cell.getCellFormula();
            default:
                return "";
        }
    }

    /**
     * 主方法 - 运行所有演示
     */
    public static void main(String[] args) {
        try {
            // 1. 创建新的 Excel 工作簿
            createNewWorkbook();

            // 2. 读取 Excel 文件
            readExcelFile();

            // 3. 工作表管理
            manageSheets();

            // 4. 单元格样式设置
            styleCells();

            logger.info("=== 所有 Excel 基础演示完成 ===");

        } catch (Exception e) {
            logger.error("演示执行失败", e);
        }
    }
}
