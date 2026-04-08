package com.example.poi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Apache POI 实战演示类
 *
 * 功能覆盖：
 * 1. Excel 数据导出（将Java对象列表导出为 .xlsx 文件）
 * 2. Excel 数据导入（从 .xlsx 文件读取数据为 Java 对象）
 * 3. Word 文档创建（.docx格式，使用 XWPF API）
 * 4. Word 文档操作（段落、表格、图片、页眉页脚）
 * 5. 大数据量 Excel 处理（使用 SXSSFWorkbook 流式写入）
 *
 * @author JavaTools Team
 */
public class PoiPracticalDemo {

    private static final Logger logger = LogManager.getLogger(PoiPracticalDemo.class);
    private static final String OUTPUT_DIR = "src/main/resources/";

    // 模拟数据模型
    static class Employee {
        String id;
        String name;
        String department;
        double salary;
        java.util.Date joinDate;

        Employee(String id, String name, String department, double salary) {
            this.id = id;
            this.name = name;
            this.department = department;
            this.salary = salary;
            this.joinDate = new java.util.Date();
        }
    }

    /**
     * 演示1: 将 Java 对象列表导出到 Excel
     * 这是最常见的业务场景，用于数据报表导出
     */
    public static void exportDataToExcel() throws IOException {
        logger.info("=== 导出数据到 Excel ===");

        // 准备测试数据
        List<Employee> employees = new ArrayList<>();
        employees.add(new Employee("E001", "张三", "研发部", 15000.00));
        employees.add(new Employee("E002", "李四", "产品部", 12000.00));
        employees.add(new Employee("E003", "王五", "运营部", 10000.00));
        employees.add(new Employee("E004", "赵六", "市场部", 11000.00));
        employees.add(new Employee("E005", "钱七", "研发部", 18000.00));

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("员工信息");

            // 创建样式
            CreationHelper createHelper = workbook.getCreationHelper();
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd"));

            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.setDataFormat(createHelper.createDataFormat().getFormat("#,##0.00"));

            // 创建表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            // 写入表头
            String[] headers = {"工号", "姓名", "部门", "月薪(元)", "入职日期"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 写入数据行
            for (int i = 0; i < employees.size(); i++) {
                Employee emp = employees.get(i);
                Row row = sheet.createRow(i + 1);

                row.createCell(0).setCellValue(emp.id);
                row.createCell(1).setCellValue(emp.name);
                row.createCell(2).setCellValue(emp.department);

                // 薪资列应用货币格式
                Cell salaryCell = row.createCell(3);
                salaryCell.setCellValue(emp.salary);
                salaryCell.setCellStyle(currencyStyle);

                // 日期列应用日期格式
                Cell dateCell = row.createCell(4);
                dateCell.setCellValue(emp.joinDate);
                dateCell.setCellStyle(dateStyle);
            }

            // 汇总行
            int summaryRow = employees.size() + 1;
            Row totalRow = sheet.createRow(summaryRow);
            totalRow.createCell(2).setCellValue("部门合计");
            Cell totalCell = totalRow.createCell(3);
            // 使用公式计算总薪资
            totalCell.setCellFormula("SUM(D2:D" + (employees.size() + 1) + ")");
            totalCell.setCellStyle(currencyStyle);

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 冻结表头
            sheet.createFreezePane(0, 1);

            String filePath = OUTPUT_DIR + "employees_export.xlsx";
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }

            logger.info("成功导出 {} 条员工数据到: {}", employees.size(), filePath);
        }
    }

    /**
     * 演示2: 创建 Word 文档 (.docx)
     * 使用 XWPFDocument API，类似于 XSSF 是对 OOXML 格式的封装
     */
    public static void createWordDocument() throws IOException {
        logger.info("=== 创建 Word 文档 ===");

        // XWPFDocument 用于操作 .docx 格式的 Word 文件
        try (XWPFDocument document = new XWPFDocument()) {

            // 添加标题段落
            XWPFParagraph titleParagraph = document.createParagraph();
            titleParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setText("员工绩效考核报告");
            titleRun.setBold(true);
            titleRun.setFontSize(20);
            titleRun.setFontFamily("宋体");

            // 空行
            document.createParagraph();

            // 添加正文内容
            XWPFParagraph introParagraph = document.createParagraph();
            XWPFRun introRun = introParagraph.createRun();
            introRun.setText("本报告总结了2026年第一季度员工绩效考核情况，考核周期为2026年1月至3月。");
            introRun.setFontFamily("宋体");
            introRun.setFontSize(12);

            // 添加带格式的段落
            XWPFParagraph sectionParagraph = document.createParagraph();
            XWPFRun sectionRun = sectionParagraph.createRun();
            sectionRun.setText("一、考核结果摘要");
            sectionRun.setBold(true);
            sectionRun.setFontSize(14);

            // 添加列表项（使用编号段落模拟）
            String[] points = {
                "优秀员工共计12名（占比24%）",
                "良好员工共计28名（占比56%）",
                "待提高员工共计10名（占比20%）"
            };

            for (String point : points) {
                XWPFParagraph listPara = document.createParagraph();
                listPara.setIndentationLeft(500); // 缩进
                XWPFRun listRun = listPara.createRun();
                listRun.setText("• " + point);
                listRun.setFontSize(12);
            }

            // 添加分页空行
            document.createParagraph();

            // 创建表格
            XWPFParagraph tableParagraph = document.createParagraph();
            XWPFRun tableHeaderRun = tableParagraph.createRun();
            tableHeaderRun.setText("二、部门绩效明细");
            tableHeaderRun.setBold(true);
            tableHeaderRun.setFontSize(14);

            // 创建4行5列的表格
            XWPFTable table = document.createTable(4, 5);

            // 设置表格宽度
            table.setWidth(9000);

            // 表头
            String[] tableHeaders = {"部门", "人数", "优秀", "良好", "待提高"};
            XWPFTableRow headerRow = table.getRow(0);
            for (int i = 0; i < tableHeaders.length; i++) {
                XWPFTableCell cell = headerRow.getCell(i);
                cell.setText(tableHeaders[i]);
                // 设置表头加粗
                cell.getParagraphs().get(0).getRuns().get(0).setBold(true);
            }

            // 表格数据
            String[][] tableData = {
                {"研发部", "20", "6", "12", "2"},
                {"产品部", "15", "4", "8", "3"},
                {"运营部", "15", "2", "8", "5"}
            };

            for (int r = 0; r < tableData.length; r++) {
                XWPFTableRow row = table.getRow(r + 1);
                for (int c = 0; c < tableData[r].length; c++) {
                    row.getCell(c).setText(tableData[r][c]);
                }
            }

            // 保存文件
            String filePath = OUTPUT_DIR + "performance_report.docx";
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                document.write(fos);
            }

            logger.info("成功创建 Word 文档: {}", filePath);
        }
    }

    /**
     * 演示3: 大数据量 Excel 处理（流式写入）
     *
     * 当数据量超过10万行时，使用 SXSSFWorkbook（Streaming XSSF）避免内存溢出
     * SXSSFWorkbook 会将超过窗口大小的行写入临时磁盘文件
     *
     * 注意事项：
     * - SXSSFWorkbook 不支持读取，仅用于写入
     * - 写完后必须调用 dispose() 清理临时文件
     * - 已写入的行无法回溯修改
     */
    public static void largeDataExportDemo() throws IOException {
        logger.info("=== 大数据量 Excel 导出演示（SXSSFWorkbook）===");

        int totalRows = 50000; // 模拟5万行数据

        // SXSSFWorkbook 每次只在内存中保留最新的 rowAccessWindowSize 行
        // 超出窗口的行会被自动刷到磁盘临时文件
        // 参数100表示内存中最多保存100行
        try (org.apache.poi.xssf.streaming.SXSSFWorkbook workbook =
                new org.apache.poi.xssf.streaming.SXSSFWorkbook(100)) {

            org.apache.poi.xssf.streaming.SXSSFSheet sheet =
                    (org.apache.poi.xssf.streaming.SXSSFSheet) workbook.createSheet("大数据导出");

            // 注意: SXSSFSheet 需要手动开启行访问追踪（如果需要自动调整列宽）
            // sheet.trackAllColumnsForAutoSizing();  // 开启后才能调用 autoSizeColumn

            // 写入表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {"序号", "订单号", "产品", "数量", "单价", "金额"};
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            long startTime = System.currentTimeMillis();

            // 写入大量数据
            Random random = new Random(42); // 固定种子，保证可重现
            for (int i = 1; i <= totalRows; i++) {
                Row row = sheet.createRow(i);
                row.createCell(0).setCellValue(i);
                row.createCell(1).setCellValue("ORD-" + String.format("%08d", i));
                row.createCell(2).setCellValue("产品" + (i % 20 + 1));
                int qty = random.nextInt(100) + 1;
                double price = Math.round(random.nextDouble() * 1000 + 10) / 10.0;
                row.createCell(3).setCellValue(qty);
                row.createCell(4).setCellValue(price);
                row.createCell(5).setCellValue(qty * price);

                // 每1万行打印进度
                if (i % 10000 == 0) {
                    logger.info("已写入 {} 行...", i);
                }
            }

            long writeTime = System.currentTimeMillis() - startTime;
            logger.info("数据写入完成, 用时: {}ms", writeTime);

            String filePath = OUTPUT_DIR + "large_data_export.xlsx";
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }

            // 重要：必须清理临时文件
            workbook.dispose();

            long totalTime = System.currentTimeMillis() - startTime;
            logger.info("成功导出 {} 行数据，总用时: {}ms，文件: {}", totalRows, totalTime, filePath);
        }
    }

    /**
     * 演示4: Excel 模板填充
     * 基于现有文件读取并修改（模板填充场景）
     *
     * 注意: 此演示不需要真实文件，通过代码模拟模板填充逻辑
     */
    public static void templateFillDemo() throws IOException {
        logger.info("=== Excel 模板填充演示 ===");

        // 先创建一个"模板"文件
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("工资单");

            // 模拟模板内容（实际使用时应从文件加载）
            sheet.createRow(0).createCell(0).setCellValue("员工姓名:");
            sheet.createRow(1).createCell(0).setCellValue("工号:");
            sheet.createRow(2).createCell(0).setCellValue("部门:");
            sheet.createRow(3).createCell(0).setCellValue("基本工资:");
            sheet.createRow(4).createCell(0).setCellValue("绩效奖金:");
            sheet.createRow(5).createCell(0).setCellValue("实发工资:");

            // 填充数据（模拟从数据库查询后填充）
            Map<String, Object> data = new HashMap<>();
            data.put("name", "张三");
            data.put("id", "E001");
            data.put("dept", "研发部");
            data.put("base", 15000.0);
            data.put("bonus", 3000.0);

            // 填充值到B列
            sheet.getRow(0).createCell(1).setCellValue((String) data.get("name"));
            sheet.getRow(1).createCell(1).setCellValue((String) data.get("id"));
            sheet.getRow(2).createCell(1).setCellValue((String) data.get("dept"));
            sheet.getRow(3).createCell(1).setCellValue((Double) data.get("base"));
            sheet.getRow(4).createCell(1).setCellValue((Double) data.get("bonus"));

            // 实发工资使用公式
            sheet.getRow(5).createCell(1).setCellFormula("B4+B5");

            // 自动调整列宽
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

            String filePath = OUTPUT_DIR + "salary_slip.xlsx";
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }

            logger.info("成功生成工资单: {}", filePath);
        }
    }

    /**
     * 主方法 - 运行所有实战演示
     */
    public static void main(String[] args) {
        try {
            // 1. 数据导出
            exportDataToExcel();

            // 2. Word 文档
            createWordDocument();

            // 3. 大数据量导出
            largeDataExportDemo();

            // 4. 模板填充
            templateFillDemo();

            logger.info("=== 所有实战演示完成 ===");

        } catch (Exception e) {
            logger.error("实战演示执行失败", e);
        }
    }
}
