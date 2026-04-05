package com.example.picocli.demo;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.HelpCommand;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Picocli基础示例 - 最简单的命令行程序
 *
 * 运行方式:
 *   mvn compile exec:java -Dexec.mainClass="com.example.picocli.demo.BasicDemo"
 *
 * 测试命令:
 *   java -jar target/picocli-demo-1.0.0.jar --help
 *   java -jar target/picocli-demo-1.0.0.jar -n 张三 -c 3
 *   java -jar target/picocli-demo-1.0.0.jar --name 李四 --count 5
 */
public class BasicDemo implements Callable<Integer> {

    // ==================== 选项定义 ====================

    // 布尔选项，默认false，使用-h显示帮助
    @Option(names = {"-h", "--help"}, usageHelp = true, description = "显示帮助信息")
    private boolean helpRequested;

    // 布尔选项，显示版本
    @Option(names = {"-v", "--version"}, versionHelp = true, description = "显示版本信息")
    private boolean versionRequested;

    // 带默认值的字符串选项
    @Option(names = {"-n", "--name"}, description = "用户名，默认: World", defaultValue = "World")
    private String userName;

    // 带必须标记的选项
    @Option(names = {"-e", "--email"}, description = "邮箱地址", required = true)
    private String email;

    // 整数选项
    @Option(names = {"-c", "--count"}, description = "重复次数，默认: 1", defaultValue = "1")
    private int repeatCount;

    // 数组选项
    @Option(names = {"-t", "--tags"}, description = "标签列表，用逗号分隔")
    private String[] tags;

    // 列表选项（更灵活）
    @Option(names = {"-d", "--data"},
            description = "附加数据项",
            split = ",")
    private List<String> dataItems;

    // 文件选项
    @Option(names = {"-o", "--output"},
            description = "输出文件路径")
    private File outputFile;

    // ==================== 位置参数定义 ====================

    // 位置参数（必需）
    @Parameters(index = "0", description = "第一个位置参数：命令名称")
    private String commandName;

    // 可选的第二个位置参数
    @Parameters(index = "1..*", description = "额外的位置参数")
    private List<String> extraArgs;

    // ==================== 命令实现 ====================

    @Override
    public Integer call() {
        if (helpRequested || versionRequested) {
            return 0;
        }

        System.out.println("========================================");
        System.out.println("         Picocli 基础示例运行结果");
        System.out.println("========================================");
        System.out.println();

        System.out.println("Email: " + email);
        System.out.println("Username: " + userName);
        System.out.println("Repeat count: " + repeatCount);
        if (tags != null) {
            System.out.println("Tags: " + Arrays.toString(tags));
        }
        if (dataItems != null) {
            System.out.println("Data items: " + dataItems);
        }

        if (commandName != null) {
            System.out.println("Command: " + commandName);
        }

        if (extraArgs != null && !extraArgs.isEmpty()) {
            System.out.println("Extra args: " + extraArgs);
        }

        System.out.println();
        System.out.println("Command executed successfully!");

        return 0; // 返回0表示成功
    }

    // ==================== Main方法 ====================

    public static void main(String[] args) {
        System.out.println("Picocli Basic Demo");
        System.out.println("Enter --help for help\n");

        // 创建命令解析器
        CommandLine cmd = new CommandLine(new BasicDemo());

        // 执行命令
        int exitCode = cmd.execute(args);

        // 退出
        System.exit(exitCode);
    }
}
