package com.example.easyexcel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.format.NumberFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.util.ListUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * EasyExcel 基础演示
 *
 * 演示内容：
 * 1. 使用注解模型写 Excel（写入）
 * 2. 使用注解模型读 Excel（读取）
 * 3. 多 Sheet 写入
 *
 * 核心特点：EasyExcel 通过注解 + 模型类，极简化 Excel 操作，
 *           且采用流式解析，内存占用远低于原生 Apache POI。
 */
public class EasyExcelBasicDemo {

    // ==================== 数据模型（POJO） ====================

    /**
     * 用户数据模型
     * 使用 EasyExcel 注解描述 Excel 列映射关系
     */
    @HeadRowHeight(20)      // 表头行高
    @ContentRowHeight(18)   // 内容行高
    @ColumnWidth(20)        // 默认列宽
    static class UserModel {

        /** @ExcelProperty：映射 Excel 列标题（index 指定列顺序） */
        @ExcelProperty(value = "用户ID", index = 0)
        @ColumnWidth(10)
        private Integer userId;

        @ExcelProperty(value = "用户名", index = 1)
        @ColumnWidth(15)
        private String username;

        @ExcelProperty(value = "邮箱", index = 2)
        @ColumnWidth(30)
        private String email;

        /**
         * @DateTimeFormat：日期格式化注解
         * 写入时自动将 Date 格式化为字符串
         */
        @ExcelProperty(value = "注册时间", index = 3)
        @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
        @ColumnWidth(22)
        private Date registerTime;

        /**
         * @NumberFormat：数值格式化注解
         * 写入时自动格式化数字（如保留2位小数）
         */
        @ExcelProperty(value = "账户余额(元)", index = 4)
        @NumberFormat("#,##0.00")
        @ColumnWidth(18)
        private Double balance;

        /** @ExcelIgnore：忽略该字段，不写入/读取 Excel */
        @ExcelIgnore
        private String password;

        // ---- 构造器 & Getter/Setter（不使用 Lombok，Java 8 手动写） ----
        public UserModel() {}

        public UserModel(Integer userId, String username, String email,
                         Date registerTime, Double balance) {
            this.userId = userId;
            this.username = username;
            this.email = email;
            this.registerTime = registerTime;
            this.balance = balance;
        }

        public Integer getUserId() { return userId; }
        public void setUserId(Integer userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public Date getRegisterTime() { return registerTime; }
        public void setRegisterTime(Date registerTime) { this.registerTime = registerTime; }
        public Double getBalance() { return balance; }
        public void setBalance(Double balance) { this.balance = balance; }

        @Override
        public String toString() {
            return "UserModel{userId=" + userId + ", username='" + username + "', email='" + email
                    + "', registerTime=" + registerTime + ", balance=" + balance + '}';
        }
    }

    // ==================== 1. 写 Excel ====================

    /**
     * 示例1：最简单的写 Excel
     * EasyExcel.write(文件路径, 模型类).sheet(sheet名).doWrite(数据列表)
     */
    static void demo1_simpleWrite(String filePath) {
        System.out.println("=== 示例1：写 Excel ===");

        // 准备数据
        List<UserModel> data = buildUserData(10);

        // 一行代码写 Excel，指定文件路径、模型类、Sheet 名
        EasyExcel.write(filePath, UserModel.class)
                .sheet("用户列表")
                .doWrite(data);

        System.out.println("写入成功：" + filePath);
        System.out.println("写入行数：" + data.size());
    }

    // ==================== 2. 读 Excel ====================

    /**
     * 示例2：读 Excel（同步读，数据量小时使用）
     *
     * 注意：EasyExcel 官方推荐使用监听器异步读取，
     *       但数据量小于 5000 行时可用 doReadSync() 同步模式。
     */
    static void demo2_syncRead(String filePath) {
        System.out.println("\n=== 示例2：同步读 Excel ===");

        // doReadSync() 直接返回 List，适合小数据量
        List<UserModel> result = EasyExcel.read(filePath)
                .head(UserModel.class)
                .sheet()
                .doReadSync();

        System.out.println("读取行数：" + result.size());
        result.forEach(user -> System.out.println("  " + user));
    }

    /**
     * 示例3：读 Excel（监听器模式，推荐！大数据量必用）
     *
     * 监听器逐行解析，不会将所有数据加载到内存，
     * 适合几十万行以上的大文件。
     */
    static void demo3_listenerRead(String filePath) {
        System.out.println("\n=== 示例3：监听器读 Excel（推荐方式）===");

        List<UserModel> resultList = new ArrayList<>();

        // 实现 ReadListener 匿名类，每读一行会调用 invoke()
        EasyExcel.read(filePath, UserModel.class, new ReadListener<UserModel>() {

            /** 每读取一行数据，就会调用此方法 */
            @Override
            public void invoke(UserModel data, AnalysisContext context) {
                resultList.add(data);
                // 实际项目中可在此处插入数据库，达到一定数量批量插入
                // 例如：if (resultList.size() >= 1000) { batchSave(resultList); resultList.clear(); }
            }

            /** 全部读取完成后调用 */
            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                System.out.println("全部读取完毕，总行数：" + resultList.size());
            }
        }).sheet().doRead();

        System.out.println("监听器读取完成，数据条数：" + resultList.size());
    }

    // ==================== 3. 多 Sheet 写入 ====================

    /**
     * 示例4：多 Sheet 写入
     * 使用 ExcelWriter 手动控制写入过程，可写多个 Sheet
     */
    static void demo4_multiSheetWrite(String filePath) {
        System.out.println("\n=== 示例4：多 Sheet 写入 ===");

        // ExcelWriter 用 try-with-resources 或手动 finish()，避免资源泄漏
        com.alibaba.excel.ExcelWriter excelWriter = null;
        try {
            excelWriter = EasyExcel.write(filePath, UserModel.class).build();

            // 第1个 Sheet：VIP 用户
            com.alibaba.excel.write.metadata.WriteSheet sheet1 =
                    EasyExcel.writerSheet(0, "VIP用户").build();
            excelWriter.write(buildUserData(5), sheet1);

            // 第2个 Sheet：普通用户
            com.alibaba.excel.write.metadata.WriteSheet sheet2 =
                    EasyExcel.writerSheet(1, "普通用户").build();
            excelWriter.write(buildUserData(8), sheet2);

            System.out.println("多 Sheet 写入成功：" + filePath);
        } finally {
            // 必须调用 finish()，否则 Excel 文件不会正确关闭
            if (excelWriter != null) {
                excelWriter.finish();
            }
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 构造测试数据
     */
    static List<UserModel> buildUserData(int count) {
        List<UserModel> list = ListUtils.newArrayList();
        for (int i = 1; i <= count; i++) {
            list.add(new UserModel(
                    i,
                    "用户_" + i,
                    "user" + i + "@example.com",
                    new Date(),
                    1000.0 * i + 0.99
            ));
        }
        return list;
    }

    // ==================== 主入口 ====================

    public static void main(String[] args) throws Exception {
        // 创建输出目录
        File outputDir = new File("output");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        String writeFile = "output/basic_write.xlsx";
        String multiSheetFile = "output/multi_sheet.xlsx";

        // 1. 写 Excel
        demo1_simpleWrite(writeFile);

        // 2. 同步读（读取刚写的文件）
        demo2_syncRead(writeFile);

        // 3. 监听器读
        demo3_listenerRead(writeFile);

        // 4. 多 Sheet 写入
        demo4_multiSheetWrite(multiSheetFile);

        System.out.println("\n=== EasyExcel 基础演示完成 ===");
        System.out.println("生成文件：");
        System.out.println("  " + new File(writeFile).getAbsolutePath());
        System.out.println("  " + new File(multiSheetFile).getAbsolutePath());
    }
}
