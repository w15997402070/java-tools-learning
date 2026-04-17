package com.example.easyexcel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.format.NumberFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.util.ListUtils;
import com.alibaba.excel.write.metadata.WriteSheet;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * EasyExcel 实战场景演示
 *
 * 演示内容：
 * 1. 大数据量分批写入（百万行场景，避免 OOM）
 * 2. 大数据量分批读取并批量入库（模拟数据导入）
 * 3. 多 Sheet 按条件分类写出（如按部门分 Sheet）
 * 4. 实战：用户数据导入导出完整闭环（写 → 读 → 统计）
 */
public class EasyExcelPracticalDemo {

    // ==================== 数据模型 ====================

    /**
     * 员工数据模型（模拟 HR 导入导出场景）
     */
    @HeadRowHeight(22)
    static class EmployeeModel {

        @ExcelProperty(value = "工号", index = 0)
        @ColumnWidth(12)
        private String empNo;

        @ExcelProperty(value = "姓名", index = 1)
        @ColumnWidth(12)
        private String name;

        @ExcelProperty(value = "部门", index = 2)
        @ColumnWidth(16)
        private String department;

        @ExcelProperty(value = "职位", index = 3)
        @ColumnWidth(16)
        private String position;

        @ExcelProperty(value = "月薪(元)", index = 4)
        @NumberFormat("#,##0.00")
        @ColumnWidth(14)
        private Double salary;

        @ExcelProperty(value = "入职日期", index = 5)
        @DateTimeFormat("yyyy-MM-dd")
        @ColumnWidth(14)
        private Date joinDate;

        public EmployeeModel() {}

        public EmployeeModel(String empNo, String name, String department,
                             String position, Double salary, Date joinDate) {
            this.empNo = empNo;
            this.name = name;
            this.department = department;
            this.position = position;
            this.salary = salary;
            this.joinDate = joinDate;
        }

        public String getDepartment() { return department; }
        public Double getSalary() { return salary; }
        public String getName() { return name; }

        @Override
        public String toString() {
            return "Employee{empNo='" + empNo + "', name='" + name
                    + "', dept='" + department + "', salary=" + salary + '}';
        }
    }

    // ==================== 1. 大数据量分批写入 ====================

    /**
     * 实战1：大数据量分批写入
     *
     * 背景：一次性将 50000 行数据写入 Excel。
     * 策略：使用 ExcelWriter + 分批 doWrite，每批 2000 行，
     *       配合 JVM 默认 GC，控制内存峰值在合理范围内。
     *
     * 注意：EasyExcel 内部使用 SXSSFWorkbook（POI 流式写入），
     *       默认只保留 100 行在内存，其余刷到磁盘，天然适合大数据。
     */
    static void practice1_largeDataWrite(String filePath) {
        System.out.println("=== 实战1：大数据量分批写入（50000 行）===");

        int totalRows = 50000;
        int batchSize = 2000;

        long startTime = System.currentTimeMillis();

        // ExcelWriter 需要手动 finish()
        com.alibaba.excel.ExcelWriter excelWriter = null;
        try {
            excelWriter = EasyExcel.write(filePath, EmployeeModel.class).build();
            WriteSheet writeSheet = EasyExcel.writerSheet("员工数据").build();

            // 分批写入，每批 batchSize 行
            int batches = (totalRows + batchSize - 1) / batchSize;
            for (int batch = 0; batch < batches; batch++) {
                int start = batch * batchSize + 1;
                int end = Math.min(start + batchSize - 1, totalRows);
                List<EmployeeModel> batchData = buildEmployeeData(start, end);
                excelWriter.write(batchData, writeSheet);

                if ((batch + 1) % 5 == 0) {
                    System.out.println("  已写入 " + end + " / " + totalRows + " 行...");
                }
            }
        } finally {
            if (excelWriter != null) {
                excelWriter.finish();
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("写入完成：" + totalRows + " 行，耗时 " + elapsed + "ms");
        System.out.println("文件大小：" + new File(filePath).length() / 1024 + " KB");
    }

    // ==================== 2. 大数据量分批读取 + 批量入库 ====================

    /**
     * 实战2：大数据量读取，分批入库
     *
     * 背景：用户上传 Excel 导入员工数据。
     * 策略：监听器每读取 500 行触发一次批量插入（模拟），
     *       避免一次性加载所有数据到内存。
     *
     * 这是 EasyExcel 最推荐的大文件读取方式。
     */
    static void practice2_batchImport(String filePath) {
        System.out.println("\n=== 实战2：分批读取 + 模拟批量入库 ===");

        final int BATCH_SIZE = 500; // 每批入库条数
        AtomicInteger totalImported = new AtomicInteger(0);
        AtomicInteger batchCount = new AtomicInteger(0);

        // 使用 final 包装 List，允许匿名内部类访问
        final List<EmployeeModel> batchList = new ArrayList<>(BATCH_SIZE);

        EasyExcel.read(filePath, EmployeeModel.class, new ReadListener<EmployeeModel>() {
            @Override
            public void invoke(EmployeeModel data, AnalysisContext context) {
                batchList.add(data);
                // 达到批量大小，执行一次批量入库
                if (batchList.size() >= BATCH_SIZE) {
                    saveToDatabase(batchList, batchCount.incrementAndGet());
                    totalImported.addAndGet(batchList.size());
                    batchList.clear(); // 清空，释放内存
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                // 处理最后不足一批的数据
                if (!batchList.isEmpty()) {
                    saveToDatabase(batchList, batchCount.incrementAndGet());
                    totalImported.addAndGet(batchList.size());
                    batchList.clear();
                }
                System.out.println("导入完成：共 " + totalImported.get()
                        + " 条，分 " + batchCount.get() + " 批入库");
            }

            @Override
            public void onException(Exception exception, AnalysisContext context) {
                int rowIndex = context.readRowHolder().getRowIndex();
                System.out.println("  [ERROR] 第 " + rowIndex + " 行解析失败，已跳过：" + exception.getMessage());
                // 不抛出，继续处理后续行
            }
        }).sheet().doRead();
    }

    /**
     * 模拟批量入库（实际项目中替换为 MyBatis、JPA 等的批量 insert）
     */
    private static void saveToDatabase(List<EmployeeModel> batch, int batchNo) {
        // 模拟入库耗时
        System.out.println("  [DB] 第 " + batchNo + " 批入库：" + batch.size() + " 条");
    }

    // ==================== 3. 按部门分 Sheet 导出 ====================

    /**
     * 实战3：按部门分 Sheet 分类导出
     *
     * 背景：HR 要求每个部门单独一个 Sheet 导出员工名单。
     * 策略：先对数据按部门分组，再逐 Sheet 写入。
     */
    static void practice3_multiSheetByDept(String filePath) {
        System.out.println("\n=== 实战3：按部门分 Sheet 导出 ===");

        List<EmployeeModel> allEmployees = buildEmployeeData(1, 30);

        // 按部门分组
        java.util.Map<String, List<EmployeeModel>> deptMap = new java.util.LinkedHashMap<>();
        for (EmployeeModel emp : allEmployees) {
            deptMap.computeIfAbsent(emp.getDepartment(), k -> new ArrayList<>()).add(emp);
        }

        com.alibaba.excel.ExcelWriter excelWriter = null;
        try {
            excelWriter = EasyExcel.write(filePath, EmployeeModel.class).build();

            int sheetIndex = 0;
            for (java.util.Map.Entry<String, List<EmployeeModel>> entry : deptMap.entrySet()) {
                String deptName = entry.getKey();
                List<EmployeeModel> deptEmployees = entry.getValue();

                WriteSheet sheet = EasyExcel.writerSheet(sheetIndex++, deptName).build();
                excelWriter.write(deptEmployees, sheet);
                System.out.println("  部门[" + deptName + "] 写入 " + deptEmployees.size() + " 条");
            }
        } finally {
            if (excelWriter != null) {
                excelWriter.finish();
            }
        }

        System.out.println("按部门分 Sheet 导出成功：" + filePath);
    }

    // ==================== 4. 导入导出完整闭环 ====================

    /**
     * 实战4：Excel 导入导出完整闭环
     *
     * 模拟业务场景：
     * ① 生成员工数据 Excel（模拟系统导出）
     * ② 读取 Excel 文件（模拟用户上传导入）
     * ③ 统计分析数据（部门人数、平均薪资）
     */
    static void practice4_fullCycle(String exportFile) {
        System.out.println("\n=== 实战4：导入导出完整闭环 ===");

        // Step 1：导出（生成 Excel）
        System.out.println("Step 1: 导出员工数据...");
        List<EmployeeModel> exportData = buildEmployeeData(1, 20);
        EasyExcel.write(exportFile, EmployeeModel.class)
                .sheet("员工名单")
                .doWrite(exportData);
        System.out.println("  导出 " + exportData.size() + " 条记录到：" + exportFile);

        // Step 2：读取（模拟用户上传）
        System.out.println("Step 2: 读取并导入...");
        List<EmployeeModel> importData = EasyExcel.read(exportFile)
                .head(EmployeeModel.class)
                .sheet()
                .doReadSync();
        System.out.println("  读取到 " + importData.size() + " 条记录");

        // Step 3：统计分析
        System.out.println("Step 3: 统计分析...");
        java.util.Map<String, List<EmployeeModel>> deptMap = new java.util.LinkedHashMap<>();
        for (EmployeeModel emp : importData) {
            deptMap.computeIfAbsent(emp.getDepartment(), k -> new ArrayList<>()).add(emp);
        }

        System.out.println("\n  ┌──────────┬──────┬──────────────┐");
        System.out.println("  │ 部门     │ 人数 │ 平均月薪(元) │");
        System.out.println("  ├──────────┼──────┼──────────────┤");
        for (java.util.Map.Entry<String, List<EmployeeModel>> entry : deptMap.entrySet()) {
            String dept = entry.getKey();
            List<EmployeeModel> emps = entry.getValue();
            double avgSalary = emps.stream()
                    .mapToDouble(e -> e.getSalary() == null ? 0 : e.getSalary())
                    .average()
                    .orElse(0);
            System.out.printf("  │ %-8s │ %4d │ %12.2f │%n",
                    dept, emps.size(), avgSalary);
        }
        System.out.println("  └──────────┴──────┴──────────────┘");
    }

    // ==================== 工具方法 ====================

    static final String[] DEPARTMENTS = {"技术部", "销售部", "市场部", "运营部", "财务部"};
    static final String[] POSITIONS = {"工程师", "高级工程师", "主管", "经理", "总监"};

    static List<EmployeeModel> buildEmployeeData(int startId, int endId) {
        List<EmployeeModel> list = new ArrayList<>();
        for (int i = startId; i <= endId; i++) {
            String dept = DEPARTMENTS[(i - 1) % DEPARTMENTS.length];
            String pos  = POSITIONS[(i - 1) % POSITIONS.length];
            double salary = 8000.0 + (i % 5) * 3000 + (i % 3) * 1500;
            list.add(new EmployeeModel(
                    "EMP" + String.format("%05d", i),
                    "员工_" + i,
                    dept,
                    pos,
                    salary,
                    new Date(System.currentTimeMillis() - (long)(i * 86400000L))
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

        // 大数据量写（50000 行）
        practice1_largeDataWrite("output/large_data_50k.xlsx");

        // 分批读取导入
        practice2_batchImport("output/large_data_50k.xlsx");

        // 按部门分 Sheet 导出
        practice3_multiSheetByDept("output/employees_by_dept.xlsx");

        // 完整闭环
        practice4_fullCycle("output/employee_cycle.xlsx");

        System.out.println("\n=== EasyExcel 实战演示完成 ===");
        System.out.println("生成文件：");
        System.out.println("  output/large_data_50k.xlsx");
        System.out.println("  output/employees_by_dept.xlsx");
        System.out.println("  output/employee_cycle.xlsx");
    }
}
