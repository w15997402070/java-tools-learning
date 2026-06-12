package com.example.hutool;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.*;

import java.io.File;
import java.math.BigDecimal;
import java.util.*;

/**
 * Hutool 基础功能演示
 * 
 * 覆盖：日期时间、字符串、集合、类型转换、唯一ID、文件操作
 * 
 * @author java-tools-learning
 */
public class HutoolBasicDemo {

    public static void main(String[] args) {
        Console.log("========== Hutool 基础功能演示 ==========\n");

        // === 1. 日期时间工具 (DateUtil) ===
        Console.log("--- 1. 日期时间工具 ---");
        demoDateUtil();

        // === 2. 字符串工具 (StrUtil) ===
        Console.log("\n--- 2. 字符串工具 ---");
        demoStrUtil();

        // === 3. 集合工具 (CollUtil) ===
        Console.log("\n--- 3. 集合工具 ---");
        demoCollUtil();

        // === 4. 类型转换 (Convert) ===
        Console.log("\n--- 4. 类型转换 ---");
        demoConvert();

        // === 5. 唯一ID生成 (IdUtil / Snowflake) ===
        Console.log("\n--- 5. 唯一ID生成 ---");
        demoIdUtil();

        // === 6. 文件操作 (FileUtil) ===
        Console.log("\n--- 6. 文件操作 ---");
        demoFileUtil();

        Console.log("\n========== 基础演示完成 ==========");
    }

    /**
     * 日期时间工具演示
     */
    private static void demoDateUtil() {
        // 当前日期时间
        DateTime now = DateUtil.date();
        Console.log("当前时间: {}", DateUtil.formatDateTime(now));

        // 日期加减
        DateTime tomorrow = DateUtil.offset(now, DateField.DAY_OF_MONTH, 1);
        Console.log("明天: {}", DateUtil.formatDate(tomorrow));

        DateTime lastMonth = DateUtil.offset(now, DateField.MONTH, -1);
        Console.log("上个月: {}", DateUtil.formatDate(lastMonth));

        // 日期解析（支持多种格式自动识别）
        String dateStr1 = "2025-06-12";
        String dateStr2 = "2025/06/12 15:30:00";
        String dateStr3 = "20250612";
        Console.log("解析 '{}': {}", dateStr1, DateUtil.parse(dateStr1));
        Console.log("解析 '{}': {}", dateStr2, DateUtil.parse(dateStr2));
        Console.log("解析 '{}': {}", dateStr3, DateUtil.parse(dateStr3));

        // 日期差计算
        DateTime start = DateUtil.parse("2025-01-01");
        DateTime end = DateUtil.parse("2025-06-12");
        long days = DateUtil.between(start, end, DateUnit.DAY);
        Console.log("2025-01-01 到 2025-06-12 相差 {} 天", days);

        // 获取日期的开始/结束
        Console.log("今天开始: {}", DateUtil.beginOfDay(now));
        Console.log("今天结束: {}", DateUtil.endOfDay(now));
        Console.log("本月开始: {}", DateUtil.beginOfMonth(now));
        Console.log("本周开始(周一): {}", DateUtil.beginOfWeek(now));
    }

    /**
     * 字符串工具演示
     */
    private static void demoStrUtil() {
        // 判空（同时判断null和空字符串）
        Console.log("StrUtil.isEmpty(null): {}", StrUtil.isEmpty(null));
        Console.log("StrUtil.isEmpty(''): {}", StrUtil.isEmpty(""));
        Console.log("StrUtil.isEmpty(' '): {}", StrUtil.isEmpty(" "));
        Console.log("StrUtil.isBlank(' '): {}", StrUtil.isBlank(" "));

        // 格式化（类似slf4j风格）
        String formatted = StrUtil.format("Hello, {}! Today is {}.", "汪正朋", DateUtil.today());
        Console.log("格式化结果: {}", formatted);

        // 下划线与驼峰互转
        Console.log("toCamelCase(user_name): {}", StrUtil.toCamelCase("user_name"));
        Console.log("toUnderline(userName): {}", StrUtil.toUnderlineCase("userName"));

        // 截取与填充
        String text = "这是一段很长很长很长很长很长很长很长的文本";
        Console.log("截取前10个字符: {}", StrUtil.sub(text, 0, 10));
        Console.log("左填充: [{}]", StrUtil.padPre("123", 8, '0'));
        Console.log("右填充: [{}]", StrUtil.padAfter("abc", 8, '-'));

        // 掩码处理（手机号/身份证脱敏）
        Console.log("手机号掩码: {}", StrUtil.hide("13812345678", 3, 7));
        Console.log("身份证掩码: {}", StrUtil.hide("340823199001011234", 6, 14));
    }

    /**
     * 集合工具演示
     */
    private static void demoCollUtil() {
        // 快速创建集合
        List<String> list = CollUtil.newArrayList("Java", "Spring", "MyBatis", "Redis", "Kafka");
        Console.log("原始列表: {}", list);

        // List转Map（按字符长度分组 - 手动实现）
        Map<Integer, List<String>> groupByLen = new HashMap<>();
        for (String s : list) {
            groupByLen.computeIfAbsent(s.length(), k -> new ArrayList<>()).add(s);
        }
        Console.log("按长度分组: {}", groupByLen);

        // 取交集/并集/差集
        List<String> list2 = CollUtil.newArrayList("Java", "Python", "Go", "Redis");
        Console.log("交集: {}", CollUtil.intersection(list, list2));
        Console.log("并集: {}", CollUtil.union(list, list2));
        Console.log("差集(list-list2): {}", CollUtil.subtract(list, list2));

        // 安全获取子列表（索引越界返回空列表）
        List<String> subList = CollUtil.sub(list, 1, 3);
        Console.log("子列表[1,3): {}", subList);
        List<String> subList2 = CollUtil.sub(list, 100, 200);
        Console.log("子列表[100,200) - 越界安全: {}", subList2);

        // 空Map安全操作
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Hutool");
        Console.log("Map设置值: {}", map);
    }

    /**
     * 类型转换演示
     */
    private static void demoConvert() {
        // 字符串转数字（自动处理空白和默认值）
        int intVal = Convert.toInt("123", 0);
        double doubleVal = Convert.toDouble("3.14", 0.0);
        Console.log("'123' -> int: {}", intVal);
        Console.log("'3.14' -> double: {}", doubleVal);
        Console.log("null -> int(default 0): {}", Convert.toInt(null, 0));
        Console.log("'abc' -> int(default -1): {}", Convert.toInt("abc", -1));

        // 数字格式化
        Console.log("千位分隔: {}", Convert.toStr(123456789, null));
        
        // 字符串与字节数组互转
        byte[] bytes = Convert.toPrimitiveByteArray("Hello Hutool");
        String strFromBytes = Convert.toStr(bytes);
        Console.log("字节数组长度: {}, 还原: {}", bytes.length, strFromBytes);

        // BigDecimal转换
        BigDecimal price = Convert.toBigDecimal("199.99");
        Console.log("'199.99' -> BigDecimal: {}", price);

        // 汉字金额转换
        Console.log("100.50 -> 中文大写: {}", Convert.digitToChinese(new BigDecimal("100.50")));
    }

    /**
     * 唯一ID生成演示（Snowflake雪花算法）
     */
    private static void demoIdUtil() {
        // UUID（无横线）
        Console.log("UUID(简): {}", IdUtil.simpleUUID());
        Console.log("UUID(标准): {}", IdUtil.randomUUID());

        // Snowflake雪花算法（分布式唯一ID，趋势递增）
        Snowflake snowflake = IdUtil.getSnowflake(1, 1);
        for (int i = 0; i < 5; i++) {
            long id = snowflake.nextId();
            Console.log("Snowflake[{}]: {} ({})", i + 1, id, id);
        }

        // ObjectId（MongoDB风格）
        Console.log("ObjectId: {}", IdUtil.objectId());

        // NanoId
        Console.log("NanoId: {}", IdUtil.nanoId());
    }

    /**
     * 文件操作演示
     */
    private static void demoFileUtil() {
        // 获取用户目录
        String userDir = System.getProperty("user.dir");
        Console.log("当前工作目录: {}", userDir);

        // 创建临时文件并写入
        File tempFile = FileUtil.file(userDir, "target", "hutool-demo-temp.txt");
        FileUtil.writeUtf8String("Hello Hutool!\n这是测试内容。", tempFile);
        Console.log("写入文件: {}", tempFile.getAbsolutePath());

        // 读取文件
        String content = FileUtil.readUtf8String(tempFile);
        Console.log("读取内容: {}", content);

        // 读取文件行
        List<String> lines = FileUtil.readUtf8Lines(tempFile);
        Console.log("文件行数: {}", lines.size());

        // 获取文件信息
        Console.log("文件名: {}", FileUtil.getName(tempFile));
        Console.log("主文件名: {}", FileUtil.getPrefix(tempFile));
        Console.log("扩展名: {}", FileUtil.getSuffix(tempFile));
        Console.log("文件大小: {} bytes", FileUtil.size(tempFile));

        // 文件类型检测
        Console.log("是否为文件: {}", FileUtil.isFile(tempFile));

        // 尾缀名
        String path = "/home/user/project/config.yml";
        Console.log("路径后缀: {}", FileUtil.extName(path));
    }
}
