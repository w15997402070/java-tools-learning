package com.example.easyexcel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.util.ListUtils;
import com.alibaba.excel.write.handler.SheetWriteHandler;
import com.alibaba.excel.write.handler.context.SheetWriteHandlerContext;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;

import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * EasyExcel 高级特性演示
 *
 * 演示内容：
 * 1. 自定义表头样式（字体、颜色、居中等）
 * 2. 动态表头写入（不用预定义 POJO，使用 List<List<Object>> 写入）
 * 3. 读取时的数据类型转换与异常处理
 * 4. 自定义 SheetWriteHandler：冻结首行、自动筛选器
 * 5. 模板填充（fill 模式，用于报表模板）
 */
public class EasyExcelAdvancedDemo {

    // ==================== 数据模型 ====================

    /**
     * 订单数据模型
     */
    @HeadRowHeight(24)
    static class OrderModel {
        @ExcelProperty(value = "订单号", index = 0)
        @ColumnWidth(20)
        private String orderId;

        @ExcelProperty(value = "商品名称", index = 1)
        @ColumnWidth(25)
        private String productName;

        @ExcelProperty(value = "数量", index = 2)
        @ColumnWidth(10)
        private Integer quantity;

        @ExcelProperty(value = "单价(元)", index = 3)
        @ColumnWidth(14)
        private Double unitPrice;

        @ExcelProperty(value = "总金额(元)", index = 4)
        @ColumnWidth(16)
        private Double totalAmount;

        @ExcelProperty(value = "状态", index = 5)
        @ColumnWidth(12)
        private String status;

        public OrderModel() {}

        public OrderModel(String orderId, String productName, int quantity,
                          double unitPrice, String status) {
            this.orderId = orderId;
            this.productName = productName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.totalAmount = quantity * unitPrice;
            this.status = status;
        }

        @Override
        public String toString() {
            return "OrderModel{orderId='" + orderId + "', product='" + productName
                    + "', qty=" + quantity + ", total=" + totalAmount + ", status='" + status + "'}";
        }
    }

    // ==================== 1. 自定义表头样式 ====================

    /**
     * 示例1：自定义表头和内容单元格样式
     *
     * 使用 HorizontalCellStyleStrategy 分别设置表头/内容的样式，
     * 无需复杂的 WriteHandler，一行配置即可。
     */
    static void demo1_customStyle(String filePath) {
        System.out.println("=== 示例1：自定义表头样式 ===");

        // 表头样式：蓝色背景 + 白色加粗字体 + 居中
        WriteCellStyle headStyle = new WriteCellStyle();
        headStyle.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
        headStyle.setFillPatternType(FillPatternType.SOLID_FOREGROUND);
        WriteFont headFont = new WriteFont();
        headFont.setBold(true);
        headFont.setColor(IndexedColors.WHITE.getIndex());
        headFont.setFontHeightInPoints((short) 12);
        headStyle.setWriteFont(headFont);
        headStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);

        // 内容样式：浅灰色背景 + 居中
        WriteCellStyle contentStyle = new WriteCellStyle();
        contentStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);

        // HorizontalCellStyleStrategy：将样式策略应用到写入器
        HorizontalCellStyleStrategy styleStrategy =
                new HorizontalCellStyleStrategy(headStyle, contentStyle);

        List<OrderModel> orders = buildOrderData(6);

        EasyExcel.write(filePath, OrderModel.class)
                .registerWriteHandler(styleStrategy)   // 注册样式策略
                .sheet("订单列表")
                .doWrite(orders);

        System.out.println("自定义样式写入成功：" + filePath);
    }

    // ==================== 2. 自定义 SheetWriteHandler（冻结 + 筛选器） ====================

    /**
     * 示例2：通过 SheetWriteHandler 添加冻结行和自动筛选器
     *
     * SheetWriteHandler 在 Sheet 创建完成后执行自定义逻辑，
     * 可以直接操作原生 POI 的 Sheet 对象。
     */
    static void demo2_freezeAndAutoFilter(String filePath) {
        System.out.println("\n=== 示例2：冻结首行 + 自动筛选器 ===");

        List<OrderModel> orders = buildOrderData(10);
        final int colCount = 6; // 列数

        EasyExcel.write(filePath, OrderModel.class)
                .registerWriteHandler(new SheetWriteHandler() {
                    /**
                     * Sheet 创建完成后调用，此时表头已写入
                     */
                    @Override
                    public void afterSheetCreate(SheetWriteHandlerContext context) {
                        Sheet sheet = context.getWriteSheetHolder().getSheet();

                        // 冻结首行（参数：冻结列数, 冻结行数, 起始列, 起始行）
                        sheet.createFreezePane(0, 1);

                        // 设置自动筛选器（覆盖表头行所有列）
                        // 区域：A1 到最后一列的第1行
                        sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                                0, 0, 0, colCount - 1));

                        System.out.println("  已添加：冻结首行 + 自动筛选器");
                    }
                })
                .sheet("订单（冻结+筛选）")
                .doWrite(orders);

        System.out.println("写入成功：" + filePath);
    }

    // ==================== 3. 动态表头（无 POJO 模型） ====================

    /**
     * 示例3：动态表头写入
     *
     * 当列结构在运行时才确定时（如用户自定义报表），
     * 可以使用 List<List<Object>> 格式写入，
     * 无需预定义 POJO 模型类。
     */
    static void demo3_dynamicHead(String filePath) {
        System.out.println("\n=== 示例3：动态表头写入 ===");

        // 构建动态表头（每个 List<String> 代表一列的多级标题）
        List<List<String>> head = new ArrayList<>();
        String[] columns = {"月份", "销售额(元)", "成本(元)", "利润(元)", "利润率(%)"};
        for (String col : columns) {
            List<String> colHead = new ArrayList<>();
            colHead.add("2026年销售报表"); // 第一级（合并表头）
            colHead.add(col);              // 第二级
            head.add(colHead);
        }

        // 构建动态数据（每个 List<Object> 代表一行）
        List<List<Object>> data = new ArrayList<>();
        String[] months = {"1月", "2月", "3月", "4月", "5月", "6月"};
        double[] sales   = {120000, 98000, 135000, 142000, 156000, 168000};
        double[] costs   = {80000,  65000, 88000,  92000,  100000, 105000};
        for (int i = 0; i < months.length; i++) {
            double profit = sales[i] - costs[i];
            double profitRate = profit / sales[i] * 100;
            List<Object> row = new ArrayList<>();
            row.add(months[i]);
            row.add(sales[i]);
            row.add(costs[i]);
            row.add(profit);
            row.add(String.format("%.1f%%", profitRate));
            data.add(row);
        }

        EasyExcel.write(filePath)
                .head(head)       // 传入动态表头
                .sheet("销售报表")
                .doWrite(data);

        System.out.println("动态表头写入成功：" + filePath);
    }

    // ==================== 4. 读取时处理异常（健壮性读取） ====================

    /**
     * 示例4：读取时处理数据类型转换异常
     *
     * 实际业务中 Excel 数据常常脏乱，必须处理异常行，
     * 否则一行错误会导致整个文件读取失败。
     */
    static void demo4_robustRead(String filePath) {
        System.out.println("\n=== 示例4：健壮性读取（异常处理）===");

        List<OrderModel> successList = new ArrayList<>();
        List<String> errorList = new ArrayList<>();

        EasyExcel.read(filePath, OrderModel.class, new ReadListener<OrderModel>() {
            @Override
            public void invoke(OrderModel data, AnalysisContext context) {
                successList.add(data);
            }

            /**
             * 读取某行发生异常时调用此方法
             * 默认实现会抛出异常终止读取，重写后可以跳过错误行
             */
            @Override
            public void onException(Exception exception, AnalysisContext context) {
                int rowIndex = context.readRowHolder().getRowIndex();
                errorList.add("第 " + rowIndex + " 行解析失败：" + exception.getMessage());
                // 不抛出异常，继续读取下一行
                System.out.println("  [WARN] 跳过错误行 " + rowIndex + ": " + exception.getMessage());
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                System.out.println("  读取完成：成功 " + successList.size() + " 行，错误 " + errorList.size() + " 行");
            }
        }).sheet().doRead();
    }

    // ==================== 工具方法 ====================

    static List<OrderModel> buildOrderData(int count) {
        String[] products = {"苹果手机", "笔记本电脑", "蓝牙耳机", "机械键盘", "显示器", "鼠标垫"};
        String[] statuses = {"已完成", "待发货", "已取消", "退款中", "已发货"};
        List<OrderModel> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(new OrderModel(
                    "ORD2026" + String.format("%04d", i),
                    products[(i - 1) % products.length],
                    (i % 5) + 1,
                    99.0 + i * 50,
                    statuses[(i - 1) % statuses.length]
            ));
        }
        return list;
    }

    // ==================== 主入口 ====================

    public static void main(String[] args) {
        File outputDir = new File("output");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        demo1_customStyle("output/order_styled.xlsx");
        demo2_freezeAndAutoFilter("output/order_freeze_filter.xlsx");
        demo3_dynamicHead("output/dynamic_head.xlsx");
        demo4_robustRead("output/order_styled.xlsx"); // 读取示例1生成的文件

        System.out.println("\n=== EasyExcel 高级演示完成 ===");
    }
}
