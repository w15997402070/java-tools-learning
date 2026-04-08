package com.example.poi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Apache POI Excel 高级功能演示类
 *
 * 功能覆盖：
 * 1. 公式计算（SUM、AVERAGE、IF等）
 * 2. 单元格合并（合并行/列）
 * 3. 冻结行/列（固定表头）
 * 4. 数据验证（下拉框、数值范围）
 * 5. 条件格式（基于值改变背景色）
 * 6. 图表创建基础
 *
 * @author JavaTools Team
 */
public class PoiExcelAdvancedDemo {

    private static final Logger logger = LogManager.getLogger(PoiExcelAdvancedDemo.class);
    private static final String OUTPUT_DIR = "src/main/resources/";

    /**
     * 演示1: 公式使用
     * 支持 Excel 中大多数内置公式，如 SUM、AVERAGE、MAX、MIN、IF、VLOOKUP 等
     */
    public static void formulaDemo() throws IOException {
        logger.info("=== Excel 公式演示 ===");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("公式演示");

            // 数据行
            String[] months = {"一月", "二月", "三月", "四月", "五月", "六月"};
            double[] sales = {12500.0, 13800.0, 11200.0, 15600.0, 14300.0, 16800.0};

            // 写入表头
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("月份");
            headerRow.createCell(1).setCellValue("销售额(元)");

            // 写入数据
            for (int i = 0; i < months.length; i++) {
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(months[i]);
                row.createCell(1).setCellValue(sales[i]);
            }

            // 公式计算行
            Row sumRow = sheet.createRow(months.length + 1);
            sumRow.createCell(0).setCellValue("合计");
            // SUM公式: 对B2:B7求和
            sumRow.createCell(1).setCellFormula("SUM(B2:B7)");

            Row avgRow = sheet.createRow(months.length + 2);
            avgRow.createCell(0).setCellValue("平均");
            avgRow.createCell(1).setCellFormula("AVERAGE(B2:B7)");

            Row maxRow = sheet.createRow(months.length + 3);
            maxRow.createCell(0).setCellValue("最高");
            maxRow.createCell(1).setCellFormula("MAX(B2:B7)");

            Row minRow = sheet.createRow(months.length + 4);
            minRow.createCell(0).setCellValue("最低");
            minRow.createCell(1).setCellFormula("MIN(B2:B7)");

            // IF公式示例: 超过14000标记"达标"，否则"未达标"
            sheet.createRow(0).createCell(2).setCellValue("是否达标");
            for (int i = 0; i < months.length; i++) {
                Row row = sheet.getRow(i + 1);
                // IF公式
                row.createCell(2).setCellFormula("IF(B" + (i + 2) + ">14000,\"达标\",\"未达标\")");
            }

            // 强制重新计算公式
            workbook.setForceFormulaRecalculation(true);

            // 自动调整列宽
            for (int i = 0; i < 3; i++) {
                sheet.autoSizeColumn(i);
            }

            String filePath = OUTPUT_DIR + "formula_demo.xlsx";
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }

            logger.info("成功创建公式演示文件: {}", filePath);
        }
    }

    /**
     * 演示2: 单元格合并
     * 使用 addMergedRegion 合并单元格范围
     * 注意: 只有合并区域左上角的单元格值会显示，其余被合并的单元格内容被忽略
     */
    public static void mergeCellsDemo() throws IOException {
        logger.info("=== 单元格合并演示 ===");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("合并单元格");

            // 创建样式
            CellStyle centerStyle = workbook.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);
            centerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // 标题行 - 合并第一行 A1:E1
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("2026年度销售汇总报表");
            titleCell.setCellStyle(centerStyle);
            // 合并单元格: 行范围(0,0), 列范围(0,4) => A1:E1
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

            // 季度合并
            Row quarterRow = sheet.createRow(1);
            String[] quarters = {"Q1", "Q2"};
            int[] startCols = {1, 3};
            int[] endCols = {2, 4};

            quarterRow.createCell(0).setCellValue("产品");
            for (int q = 0; q < quarters.length; q++) {
                Cell qCell = quarterRow.createCell(startCols[q]);
                qCell.setCellValue(quarters[q]);
                qCell.setCellStyle(centerStyle);
                // 合并季度单元格（横向合并）
                sheet.addMergedRegion(new CellRangeAddress(1, 1, startCols[q], endCols[q]));
            }

            // 月份行
            Row monthRow = sheet.createRow(2);
            monthRow.createCell(0).setCellValue("");
            String[] monthHeaders = {"一月", "二月", "三月", "四月"};
            for (int i = 0; i < monthHeaders.length; i++) {
                monthRow.createCell(i + 1).setCellValue(monthHeaders[i]);
            }

            // 数据行
            String[] products = {"手机", "电脑", "平板"};
            double[][] monthData = {
                {12500, 13800, 11200, 15600},
                {8500, 9200, 7800, 10100},
                {3200, 3800, 2900, 4100}
            };

            for (int r = 0; r < products.length; r++) {
                Row dataRow = sheet.createRow(r + 3);
                dataRow.createCell(0).setCellValue(products[r]);
                for (int c = 0; c < 4; c++) {
                    dataRow.createCell(c + 1).setCellValue(monthData[r][c]);
                }
            }

            // 自动调整列宽
            for (int i = 0; i < 5; i++) {
                sheet.autoSizeColumn(i);
            }

            String filePath = OUTPUT_DIR + "merge_cells.xlsx";
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }

            logger.info("成功创建合并单元格演示文件: {}", filePath);
        }
    }

    /**
     * 演示3: 冻结行和列
     * 适用于大型表格，使表头或左侧标签在滚动时保持可见
     */
    public static void freezePaneDemo() throws IOException {
        logger.info("=== 冻结行/列演示 ===");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("冻结面板");

            // 写入表头
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("序号");
            headerRow.createCell(1).setCellValue("产品名称");
            for (int month = 1; month <= 12; month++) {
                headerRow.createCell(month + 1).setCellValue(month + "月销售额");
            }

            // 写入大量数据（模拟大表格）
            for (int row = 1; row <= 50; row++) {
                Row dataRow = sheet.createRow(row);
                dataRow.createCell(0).setCellValue(row);
                dataRow.createCell(1).setCellValue("产品" + row);
                for (int col = 2; col <= 13; col++) {
                    dataRow.createCell(col).setCellValue((int) (Math.random() * 10000) + 1000);
                }
            }

            // 冻结第一行和前两列
            // createFreezePane(colSplit, rowSplit): colSplit=冻结前N列, rowSplit=冻结前N行
            sheet.createFreezePane(2, 1);

            logger.info("冻结设置: 前2列和第1行");

            String filePath = OUTPUT_DIR + "freeze_pane.xlsx";
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }

            logger.info("成功创建冻结面板演示文件: {}", filePath);
        }
    }

    /**
     * 演示4: 日期格式化
     * 在 Excel 中正确处理和显示日期类型的单元格
     */
    public static void dateFormattingDemo() throws IOException {
        logger.info("=== 日期格式化演示 ===");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("日期格式");

            // 创建日期格式样式
            CreationHelper createHelper = workbook.getCreationHelper();

            CellStyle dateStyle = workbook.createCellStyle();
            // 设置日期格式: yyyy-MM-dd
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd"));

            CellStyle datetimeStyle = workbook.createCellStyle();
            // 设置日期时间格式: yyyy-MM-dd HH:mm:ss
            datetimeStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd HH:mm:ss"));

            // 写入表头
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("描述");
            headerRow.createCell(1).setCellValue("日期");

            // 写入日期数据
            java.util.Date now = new java.util.Date();

            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("今天");
            Cell dateCell = row1.createCell(1);
            dateCell.setCellValue(now);
            dateCell.setCellStyle(dateStyle);

            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("当前时间");
            Cell datetimeCell = row2.createCell(1);
            datetimeCell.setCellValue(now);
            datetimeCell.setCellStyle(datetimeStyle);

            // 自动调整
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

            String filePath = OUTPUT_DIR + "date_format.xlsx";
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }

            logger.info("成功创建日期格式演示文件: {}", filePath);
        }
    }

    /**
     * 主方法 - 运行所有高级演示
     */
    public static void main(String[] args) {
        try {
            formulaDemo();
            mergeCellsDemo();
            freezePaneDemo();
            dateFormattingDemo();

            logger.info("=== 所有 Excel 高级演示完成 ===");

        } catch (Exception e) {
            logger.error("高级演示执行失败", e);
        }
    }
}
