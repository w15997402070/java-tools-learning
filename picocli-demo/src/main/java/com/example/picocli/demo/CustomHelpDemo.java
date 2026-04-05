package com.example.picocli.demo;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Help.ColorScheme;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

/**
 * Picocli自定义帮助格式示例
 *
 * 展示如何自定义:
 * 1. 帮助信息的颜色和样式
 * 2. 自定义使用消息格式
 * 3. ANSI彩色输出
 *
 * 运行方式:
 *   mvn compile exec:java -Dexec.mainClass="com.example.picocli.demo.CustomHelpDemo"
 */
public class CustomHelpDemo implements Callable<Integer>, Runnable {

    @Spec
    CommandSpec spec;

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "显示帮助")
    private boolean helpRequested;

    @Option(names = {"-V", "--version"}, versionHelp = true, description = "显示版本")
    private boolean versionRequested;

    @Option(names = {"--name"}, description = "项目名称")
    private String projectName = "MyProject";

    @Option(names = {"--env"}, description = "运行环境", paramLabel = "ENV")
    private String environment;

    @Override
    public void run() {
        System.out.println("Running " + projectName + " in " + environment + " mode");
    }

    @Override
    public Integer call() {
        System.out.println("项目: " + projectName + ", 环境: " + environment);
        return 0;
    }

    // ==================== 自定义颜色方案 ====================

    @Command(name = "colors", description = "自定义颜色方案示例",
             subcommands = {HelpCommand.class})
    static class ColorsCommand implements Callable<Integer> {

        @Option(names = {"-h", "--help"}, usageHelp = true, description = "显示帮助")
        private boolean helpRequested;

        @Option(names = {"--info"}, description = "信息消息")
        private String info;

        @Option(names = {"--warn"}, description = "警告消息")
        private String warning;

        @Option(names = {"--error"}, description = "错误消息")
        private String error;

        @Option(names = {"--success"}, description = "成功消息")
        private String success;

        @Override
        public Integer call() {
            System.out.println("=== 自定义颜色示例 ===");

            if (info != null) {
                System.out.println("[INFO] " + info);
            }
            if (warning != null) {
                System.out.println("[WARNING] " + warning);
            }
            if (error != null) {
                System.out.println("[ERROR] " + error);
            }
            if (success != null) {
                System.out.println("[SUCCESS] " + success);
            }

            return 0;
        }
    }

    // ==================== 自定义使用消息格式 ====================

    @Command(name = "format",
             header = {
                 "========================================",
                 "       自定义头部标题",
                 "========================================"
             },
             description = "这是一个带有自定义头部和描述的命令示例\n用于展示如何使用自定义header来定制帮助输出",
             optionListHeading = "【选项列表】\n",
             parameterListHeading = "【参数列表】\n",
             footer = "========================================\n" +
                      "© 2024 团队技术学习项目\n",
             subcommands = HelpCommand.class)
    static class FormatCommand implements Callable<Integer> {

        @Option(names = {"-h", "--help"}, usageHelp = true, description = "显示帮助")
        private boolean helpRequested;

        @Option(names = {"-n", "--name"}, description = "项目名称")
        private String name;

        @Option(names = {"--verbose"}, description = "详细输出")
        private boolean verbose;

        @Override
        public Integer call() {
            System.out.println("Format command executed with name: " + name);
            return 0;
        }
    }

    // ==================== ANSI风格帮助 ====================

    @Command(name = "ansi",
             header = {
                 "========================================",
                 "       彩色帮助信息示例",
                 "========================================"
             },
             footer = {
                 "",
                 "提示: 使用 --help 查看更多帮助信息",
                 "文档: https://docs.example.com",
                 ""
             })
    static class AnsiCommand implements Callable<Integer> {

        @Option(names = {"-h", "--help"}, usageHelp = true, description = "显示帮助")
        private boolean helpRequested;

        @Option(names = {"-n", "--name"},
                description = "显示的名称")
        private String name;

        @Override
        public Integer call() {
            System.out.println("=== ANSI彩色样式测试 ===");
            System.out.println();

            if (name != null) {
                System.out.println("名称: " + name);
            }

            return 0;
        }
    }

    // ==================== 主程序 ====================

    public static void main(String[] args) {
        System.out.println("Picocli 自定义帮助格式示例\n");

        CustomHelpDemo demo = new CustomHelpDemo();
        CommandLine cmd = new CommandLine(demo);

        // 添加子命令
        cmd.addSubcommand("colors", new ColorsCommand())
           .addSubcommand("format", new FormatCommand())
           .addSubcommand("ansi", new AnsiCommand());

        // 测试各种帮助
        // java -jar target/picocli-demo-1.0.0.jar colors --info "测试信息" --warn "测试警告" --error "测试错误" --success "测试成功"
        // java -jar target/picocli-demo-1.0.0.jar format --help
        // java -jar target/picocli-demo-1.0.0.jar ansi --help

        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }
}
