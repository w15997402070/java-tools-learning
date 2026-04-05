package com.example.picocli.demo;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IExitCodeGenerator;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Picocli完整主命令示例
 *
 * 整合所有功能，演示如何在实际项目中使用Picocli
 *
 * 运行方式:
 *   mvn compile exec:java -Dexec.mainClass="com.example.picocli.demo.MainCommandDemo"
 *
 * 或者打包后:
 *   java -jar target/picocli-demo-1.0.0.jar --help
 *   java -jar target/picocli-demo-1.0.0.jar greet --name 张三 --count 3
 *   java -jar target/picocli-demo-1.0.0.jar process --files file1.txt,file2.txt --parallel --timeout 60s
 */
public class MainCommandDemo implements Callable<Integer>, IExitCodeGenerator {

    @Spec
    CommandLine.Model.CommandSpec spec;

    // 全局选项
    @Option(names = {"-c", "--config"},
            description = "配置文件路径")
    java.io.File configFile;

    @Option(names = {"-d", "--debug"},
            description = "启用调试模式",
            showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    boolean debug;

    @Option(names = {"--dry-run"},
            description = "演练模式，不实际执行")
    boolean dryRun;

    @Override
    public Integer call() {
        // 默认命令，显示欢迎信息
        CommandLine cmd = spec.commandLine();
        cmd.getErr().println("请指定要执行的子命令。输入以下命令查看帮助:");
        cmd.getErr().println("  picocli-demo --help");
        cmd.getErr().println();
        cmd.getErr().println("可用子命令:");
        for (Map.Entry<String, CommandLine> entry : cmd.getSubcommands().entrySet()) {
            if (!entry.getKey().equals("help")) {
                cmd.getErr().println("  " + entry.getKey());
            }
        }
        return 0;
    }

    @Override
    public int getExitCode() {
        return debug ? 42 : 0;
    }

    // ==================== 主程序入口 ====================

    public static void main(String[] args) {
        System.out.println();
        System.out.println("========================================");
        System.out.println("  Picocli 完整示例 - Java工具学习 Day1");
        System.out.println("  GitHub: remkop/picocli (5.3k stars)");
        System.out.println("========================================");
        System.out.println();

        MainCommandDemo command = new MainCommandDemo();
        CommandLine cmd = new CommandLine(command);

        // 添加子命令
        cmd.addSubcommand("greet", new GreetCommand())
           .addSubcommand("process", new ProcessCommand())
           .addSubcommand("generate", new GenerateCommand())
           .addSubcommand("user", new SubcommandDemo.UserCommand())
           .addSubcommand("config", new SubcommandDemo.ConfigCommand())
           .addSubcommand("deploy", new SubcommandDemo.DeployCommand());

        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }

    // ==================== Greet子命令 ====================

    @Command(name = "greet",
             description = "问候命令 - 演示基本选项和参数")
    public static class GreetCommand implements Callable<Integer> {

        @Option(names = {"-n", "--name"},
                description = "问候的姓名",
                defaultValue = "World",
                showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
        String name;

        @Option(names = {"-c", "--count"},
                description = "重复次数",
                defaultValue = "1")
        int count;

        @Option(names = {"--formal"},
                description = "正式问候模式")
        boolean formal;

        @Option(names = {"--lang"},
                description = "语言 (zh/en/ja)",
                defaultValue = "zh")
        String language;

        @Override
        public Integer call() {
            String greeting;
            switch (language) {
                case "en":
                    greeting = formal ? "Good morning, " : "Hello, ";
                    break;
                case "ja":
                    greeting = formal ? "Ohayou gozaimasu, " : "Konnichiwa, ";
                    break;
                case "zh":
                default:
                    greeting = formal ? "Nin hao, " : "Ni hao, ";
                    break;
            }

            String message = greeting + name + "!";

            for (int i = 0; i < count; i++) {
                System.out.println(message);
            }

            return 0;
        }
    }

    // ==================== Process子命令 ====================

    @Command(name = "process",
             description = "处理文件 - 演示复杂选项和验证")
    public static class ProcessCommand implements Callable<Integer> {

        @Option(names = {"-f", "--files"},
                description = "要处理的文件列表（逗号分隔）",
                required = true,
                split = ",")
        List<String> files;

        @Option(names = {"-p", "--parallel"},
                description = "并行处理模式")
        boolean parallel;

        @Option(names = {"-t", "--timeout"},
                description = "超时时间 (如 30s, 5m, 1h)",
                converter = ValidationDemo.DurationConverter.class,
                defaultValue = "5m")
        long timeoutMs;

        @Option(names = {"--format"},
                description = "输出格式",
                defaultValue = "json",
                showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
        String format;

        @Option(names = {"-v", "--verbose"},
                description = "详细输出")
        boolean verbose;

        @Override
        public Integer call() {
            System.out.println("========================================");
            System.out.println("          文件处理任务");
            System.out.println("========================================");
            System.out.println("文件列表: " + files.size() + " 个文件");
            System.out.println("处理模式: " + (parallel ? "并行" : "串行"));
            System.out.println("超时时间: " + timeoutMs + " ms");
            System.out.println("输出格式: " + format);
            System.out.println();

            int index = 1;
            for (String file : files) {
                if (verbose) {
                    System.out.println("[" + index + "/" + files.size() + "] 处理: " + file);
                }
                // 模拟处理
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("已处理: " + file);
                index++;
            }

            System.out.println();
            System.out.println("处理完成!");
            return 0;
        }
    }

    // ==================== Generate子命令 ====================

    @Command(name = "generate",
             description = "代码生成 - 演示子命令嵌套",
             subcommands = {
                 GenerateEntityCommand.class,
                 GenerateServiceCommand.class,
                 GenerateControllerCommand.class
             })
    public static class GenerateCommand implements Callable<Integer> {

        @Override
        public Integer call() {
            System.out.println("代码生成器 - 请指定要生成的类型:");
            System.out.println("  entity     - 生成实体类");
            System.out.println("  service    - 生成服务类");
            System.out.println("  controller - 生成控制器类");
            return 0;
        }
    }

    @Command(name = "entity",
             description = "生成Entity实体类")
    public static class GenerateEntityCommand implements Callable<Integer> {

        @Option(names = {"--name"},
                description = "实体类名称",
                required = true)
        String name;

        @Option(names = {"--table"},
                description = "对应的数据库表名")
        String tableName;

        @Option(names = {"--package"},
                description = "包名",
                defaultValue = "com.example.entity")
        String packageName;

        @Option(names = {"--fields"},
                description = "字段定义 (格式: name:type,...)")
        String fields;

        @Override
        public Integer call() {
            System.out.println("========================================");
            System.out.println("         生成Entity类");
            System.out.println("========================================");
            System.out.println("类名: " + name);
            System.out.println("表名: " + (tableName != null ? tableName : name.toLowerCase()));
            System.out.println("包名: " + packageName);

            if (fields != null) {
                System.out.println("字段: " + Arrays.toString(fields.split(",")));
            }

            // 模拟生成代码
            System.out.println();
            System.out.println("生成代码预览:");
            System.out.println("package " + packageName + ";");
            System.out.println();
            System.out.println("public class " + name + " {");
            System.out.println("    // 字段定义...");
            System.out.println("}");

            return 0;
        }
    }

    @Command(name = "service",
             description = "生成Service服务类")
    public static class GenerateServiceCommand implements Callable<Integer> {

        @Option(names = {"--name"},
                description = "服务类名称",
                required = true)
        String name;

        @Option(names = {"--entity"},
                description = "关联的实体类名称",
                required = true)
        String entityName;

        @Option(names = {"--package"},
                description = "包名",
                defaultValue = "com.example.service")
        String packageName;

        @Override
        public Integer call() {
            System.out.println("========================================");
            System.out.println("         生成Service类");
            System.out.println("========================================");
            System.out.println("服务类: " + name);
            System.out.println("实体类: " + entityName);
            System.out.println("包名: " + packageName);

            return 0;
        }
    }

    @Command(name = "controller",
             description = "生成Controller控制器类")
    public static class GenerateControllerCommand implements Callable<Integer> {

        @Option(names = {"--name"},
                description = "控制器名称",
                required = true)
        String name;

        @Option(names = {"--service"},
                description = "关联的服务名称")
        String serviceName;

        @Option(names = {"--rest"},
                description = "生成REST控制器",
                showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
        boolean restApi = true;

        @Override
        public Integer call() {
            System.out.println("========================================");
            System.out.println("        生成Controller类");
            System.out.println("========================================");
            System.out.println("控制器: " + name);
            System.out.println("服务类: " + (serviceName != null ? serviceName : "未指定"));
            System.out.println("REST API: " + (restApi ? "是" : "否"));

            return 0;
        }
    }
}
