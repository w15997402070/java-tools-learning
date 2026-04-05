package com.example.picocli.demo;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Picocli验证与自定义类型示例
 *
 * 展示:
 * 1. 自定义类型转换器
 * 2. 参数验证
 * 3. 自定义参数消费者
 *
 * 运行方式:
 *   mvn compile exec:java -Dexec.mainClass="com.example.picocli.demo.ValidationDemo"
 */
public class ValidationDemo implements Callable<Integer> {

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "显示帮助")
    private boolean helpRequested;

    @Override
    public Integer call() {
        System.out.println("使用 --help 查看各个子命令的帮助");
        return 0;
    }

    // ==================== 自定义类型转换器示例 ====================

    @Command(name = "convert", description = "自定义类型转换示例",
             subcommands = {HelpCommand.class})
    static class ConvertCommand implements Callable<Integer> {

        // 使用自定义EmailConverter
        @Option(names = {"--email"}, converter = EmailConverter.class,
                description = "邮箱地址 (自定义验证)")
        private String email;

        // 使用自定义DateConverter
        @Option(names = {"--date"}, converter = DateConverter.class,
                description = "日期 (yyyy-MM-dd格式)")
        private LocalDate date;

        // 使用自定义FileSizeConverter
        @Option(names = {"--size"}, converter = FileSizeConverter.class,
                description = "文件大小 (如 1KB, 10MB)")
        private long fileSize;

        // 使用自定义DurationConverter
        @Option(names = {"--timeout"}, converter = DurationConverter.class,
                description = "超时时间 (如 30s, 5m, 1h)")
        private long durationMs;

        @Option(names = {"--phone"}, converter = PhoneConverter.class,
                description = "手机号 (+86格式)")
        private String phone;

        @Override
        public Integer call() {
            System.out.println("类型转换结果:");
            if (email != null) System.out.println("Email: " + email);
            if (date != null) System.out.println("Date: " + date);
            if (fileSize > 0) System.out.println("File Size: " + fileSize + " bytes");
            if (durationMs > 0) System.out.println("Duration: " + durationMs + " ms");
            if (phone != null) System.out.println("Phone: " + phone);
            return 0;
        }
    }

    // Email转换器
    public static class EmailConverter implements ITypeConverter<String> {
        @Override
        public String convert(String value) throws Exception {
            if (value == null || value.isEmpty()) {
                throw new ParameterException(new CommandLine(new ValidationDemo()),
                    "邮箱不能为空");
            }
            if (!value.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                throw new ParameterException(new CommandLine(new ValidationDemo()),
                    "无效的邮箱格式: " + value);
            }
            return value;
        }
    }

    // 日期转换器
    public static class DateConverter implements ITypeConverter<LocalDate> {
        @Override
        public LocalDate convert(String value) throws Exception {
            try {
                return LocalDate.parse(value,
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (DateTimeParseException e) {
                throw new ParameterException(new CommandLine(new ValidationDemo()),
                    "无效的日期格式，请使用 yyyy-MM-dd 格式");
            }
        }
    }

    // 文件大小转换器 (如 1KB, 10MB, 1GB)
    public static class FileSizeConverter implements ITypeConverter<Long> {
        @Override
        public Long convert(String value) throws Exception {
            if (value == null || value.isEmpty()) {
                return 0L;
            }

            String upper = value.toUpperCase().trim();
            long multiplier = 1;

            if (upper.endsWith("KB")) {
                multiplier = 1024;
                upper = upper.substring(0, upper.length() - 2);
            } else if (upper.endsWith("MB")) {
                multiplier = 1024 * 1024;
                upper = upper.substring(0, upper.length() - 2);
            } else if (upper.endsWith("GB")) {
                multiplier = 1024 * 1024 * 1024;
                upper = upper.substring(0, upper.length() - 2);
            } else if (upper.endsWith("B")) {
                upper = upper.substring(0, upper.length() - 1);
            }

            try {
                return (long) (Double.parseDouble(upper.trim()) * multiplier);
            } catch (NumberFormatException e) {
                throw new ParameterException(new CommandLine(new ValidationDemo()),
                    "无效的文件大小格式: " + value);
            }
        }
    }

    // 持续时间转换器 (如 30s, 5m, 1h)
    public static class DurationConverter implements ITypeConverter<Long> {
        @Override
        public Long convert(String value) throws Exception {
            if (value == null || value.isEmpty()) {
                return 0L;
            }

            String upper = value.toUpperCase().trim();
            long multiplier = 1000; // 默认秒

            if (upper.endsWith("MS")) {
                multiplier = 1;
                upper = upper.substring(0, upper.length() - 2);
            } else if (upper.endsWith("S")) {
                multiplier = 1000;
                upper = upper.substring(0, upper.length() - 1);
            } else if (upper.endsWith("M")) {
                multiplier = 60 * 1000;
                upper = upper.substring(0, upper.length() - 1);
            } else if (upper.endsWith("H")) {
                multiplier = 60 * 60 * 1000;
                upper = upper.substring(0, upper.length() - 1);
            }

            try {
                return (long) (Double.parseDouble(upper.trim()) * multiplier);
            } catch (NumberFormatException e) {
                throw new ParameterException(new CommandLine(new ValidationDemo()),
                    "无效的持续时间格式: " + value);
            }
        }
    }

    // 手机号转换器
    public static class PhoneConverter implements ITypeConverter<String> {
        @Override
        public String convert(String value) throws Exception {
            if (value == null || value.isEmpty()) {
                return null;
            }

            // 移除常见分隔符
            String cleaned = value.replaceAll("[\\s-()]", "");

            // 验证格式
            if (!cleaned.matches("^\\+?1?\\d{6,14}$")) {
                throw new ParameterException(new CommandLine(new ValidationDemo()),
                    "无效的手机号格式: " + value);
            }

            return cleaned;
        }
    }

    // ==================== 参数验证示例 ====================

    @Command(name = "validate", description = "参数验证示例",
             subcommands = {HelpCommand.class})
    static class ValidateCommand implements Callable<Integer> {

        @Option(names = {"--name"},
                description = "用户名 (2-20个字符)")
        private String name;

        @Option(names = {"--age"},
                description = "年龄 (0-150)",
                converter = AgeConverter.class)
        private int age;

        @Option(names = {"--port"},
                description = "端口号 (1-65535)",
                converter = PortConverter.class)
        private int port;

        @Override
        public Integer call() {
            // 验证用户名长度
            if (name != null && (name.length() < 2 || name.length() > 20)) {
                throw new ParameterException(new CommandLine(new ValidationDemo()),
                    "用户名长度必须在2-20个字符之间，当前: " + name);
            }

            System.out.println("验证通过:");
            System.out.println("  姓名: " + (name != null ? name : "未提供"));
            System.out.println("  年龄: " + age);
            System.out.println("  端口: " + port);
            return 0;
        }
    }

    // 年龄转换器 (带验证)
    public static class AgeConverter implements ITypeConverter<Integer> {
        @Override
        public Integer convert(String value) throws Exception {
            int age = Integer.parseInt(value);
            if (age < 0 || age > 150) {
                throw new ParameterException(new CommandLine(new ValidationDemo()),
                    "年龄必须在0-150之间，当前: " + age);
            }
            return age;
        }
    }

    // 端口转换器 (带验证)
    public static class PortConverter implements ITypeConverter<Integer> {
        @Override
        public Integer convert(String value) throws Exception {
            int port = Integer.parseInt(value);
            if (port < 1 || port > 65535) {
                throw new ParameterException(new CommandLine(new ValidationDemo()),
                    "端口号必须在1-65535之间，当前: " + port);
            }
            return port;
        }
    }

    // ==================== 主程序 ====================

    public static void main(String[] args) {
        System.out.println("Picocli 验证与自定义类型示例\n");

        ValidationDemo demo = new ValidationDemo();
        CommandLine cmd = new CommandLine(demo);

        // 注册子命令
        cmd.addSubcommand("convert", new ConvertCommand())
           .addSubcommand("validate", new ValidateCommand());

        // 测试自定义转换器
        // java -jar target/picocli-demo-1.0.0.jar convert --email test@example.com --date 2024-01-15 --size 10MB --timeout 30s --phone "+86 138-0013-8000"

        // 测试验证
        // java -jar target/picocli-demo-1.0.0.jar validate --name Tom --age 25 --port 8080

        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }
}
