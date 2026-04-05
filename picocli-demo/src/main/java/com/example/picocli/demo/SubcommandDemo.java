package com.example.picocli.demo;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Picocli子命令示例 - Git风格的多级命令
 *
 * 运行方式:
 *   mvn compile exec:java -Dexec.mainClass="com.example.picocli.demo.SubcommandDemo"
 *
 * 测试命令:
 *   java -jar target/picocli-demo-1.0.0.jar --help
 *   java -jar target/picocli-demo-1.0.0.jar user --help
 *   java -jar target/picocli-demo-1.0.0.jar user create --name 张三 --email test@example.com
 *   java -jar target/picocli-demo-1.0.0.jar user delete --id 123 --force
 *   java -jar target/picocli-demo-1.0.0.jar config --set debug=true
 *   java -jar target/picocli-demo-1.0.0.jar deploy --env production --region us-east
 */
public class SubcommandDemo implements Callable<Integer> {

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "显示帮助")
    private boolean helpRequested;

    @Option(names = {"-V", "--version"}, versionHelp = true, description = "显示版本")
    private boolean versionRequested;

    @Option(names = {"--verbose"}, description = "详细输出模式")
    private boolean verbose;

    @Override
    public Integer call() {
        System.out.println("请指定子命令，使用 --help 查看帮助");
        return 0;
    }

    // ==================== User子命令 ====================

    @Command(name = "user", description = "用户管理命令",
             subcommands = {UserCreateCommand.class, UserDeleteCommand.class, HelpCommand.class})
    static class UserCommand implements Callable<Integer> {

        @Option(names = {"-h", "--help"}, usageHelp = true, description = "显示帮助")
        private boolean helpRequested;

        @Override
        public Integer call() {
            System.out.println("用户子命令可用: create, delete");
            System.out.println("使用 'user <子命令> --help' 查看详细帮助");
            return 0;
        }
    }

    // 用户创建子命令
    @Command(name = "create", description = "创建新用户")
    static class UserCreateCommand implements Callable<Integer> {

        @Option(names = {"-h", "--help"}, usageHelp = true, description = "显示帮助")
        private boolean helpRequested;

        @Option(names = {"--name"}, description = "用户名", required = true)
        private String name;

        @Option(names = {"--email"}, description = "邮箱地址", required = true)
        private String email;

        @Option(names = {"--role"}, description = "用户角色 (admin/user/guest)",
                defaultValue = "user")
        private String role;

        @Option(names = {"--password"}, description = "密码 (可选)")
        private String password;

        @Override
        public Integer call() {
            System.out.println("========================================");
            System.out.println("              创建用户");
            System.out.println("========================================");
            System.out.println("  用户名: " + name);
            System.out.println("  邮箱:   " + email);
            System.out.println("  角色:   " + role);
            if (password != null) {
                System.out.println("  密码:   [已设置]");
            }
            System.out.println();
            System.out.println("用户创建成功! ID: " + System.currentTimeMillis() % 10000);
            return 0;
        }
    }

    // 用户删除子命令
    @Command(name = "delete", description = "删除用户")
    static class UserDeleteCommand implements Callable<Integer> {

        @Option(names = {"-h", "--help"}, usageHelp = true, description = "显示帮助")
        private boolean helpRequested;

        @Option(names = {"--id"}, description = "用户ID", required = true)
        private Long userId;

        @Option(names = {"-f", "--force"}, description = "强制删除，不确认")
        private boolean force;

        @Option(names = {"-b", "--backup"}, description = "删除前备份用户数据")
        private boolean backup;

        @Override
        public Integer call() {
            System.out.println("准备删除用户 ID: " + userId);
            if (backup) {
                System.out.println("  正在备份用户数据...");
            }
            if (force) {
                System.out.println("  强制模式: 直接删除");
            } else {
                System.out.println("  请确认删除操作");
            }
            System.out.println("用户删除成功!");
            return 0;
        }
    }

    // ==================== Config子命令 ====================

    @Command(name = "config", description = "配置管理命令",
             subcommands = {ConfigSetCommand.class, ConfigGetCommand.class, ConfigListCommand.class, HelpCommand.class})
    static class ConfigCommand implements Callable<Integer> {

        @Option(names = {"-h", "--help"}, usageHelp = true, description = "显示帮助")
        private boolean helpRequested;

        @Override
        public Integer call() {
            System.out.println("配置子命令可用: set, get, list");
            return 0;
        }
    }

    @Command(name = "set", description = "设置配置项")
    static class ConfigSetCommand implements Callable<Integer> {

        @Option(names = {"--set"}, description = "配置项 (key=value)", required = true)
        private String setting;

        @Option(names = {"-g", "--global"}, description = "设置全局配置")
        private boolean global;

        @Override
        public Integer call() {
            System.out.println("配置设置:");
            String[] parts = setting.split("=", 2);
            if (parts.length == 2) {
                System.out.println("  " + (global ? "[全局] " : "") + parts[0] + " = " + parts[1]);
            } else {
                System.out.println("  无效格式: " + setting + " (需要 key=value)");
            }
            return 0;
        }
    }

    @Command(name = "get", description = "获取配置项")
    static class ConfigGetCommand implements Callable<Integer> {

        @Option(names = {"--key"}, description = "配置项名称", required = true)
        private String key;

        @Option(names = {"-d", "--default"}, description = "默认值")
        private String defaultValue = "null";

        @Override
        public Integer call() {
            // 模拟获取配置
            System.out.println(key + " = " + defaultValue);
            return 0;
        }
    }

    @Command(name = "list", description = "列出所有配置项")
    static class ConfigListCommand implements Callable<Integer> {

        @Option(names = {"-a", "--all"}, description = "显示所有配置，包含默认值")
        private boolean showAll;

        @Override
        public Integer call() {
            System.out.println("配置列表:");
            System.out.println("  app.name = MyApplication");
            System.out.println("  app.version = 1.0.0");
            if (showAll) {
                System.out.println("  app.timeout = 30000");
                System.out.println("  app.debug = false");
            }
            return 0;
        }
    }

    // ==================== Deploy子命令 ====================

    @Command(name = "deploy", description = "部署应用程序",
             subcommands = {HelpCommand.class})
    static class DeployCommand implements Callable<Integer> {

        @Option(names = {"-h", "--help"}, usageHelp = true, description = "显示帮助")
        private boolean helpRequested;

        @Option(names = {"--env"}, description = "部署环境 (dev/staging/production)",
                required = true)
        private String environment;

        @Option(names = {"--region"}, description = "部署区域")
        private String region = "cn-beijing";

        @Option(names = {"--instances"}, description = "实例数量", defaultValue = "1")
        private int instances;

        @Option(names = {"--dry-run"}, description = "演练模式，不实际部署")
        private boolean dryRun;

        @Option(names = {"--rollback"}, description = "回滚到指定版本")
        private String rollbackVersion;

        @Override
        public Integer call() {
            System.out.println("========================================");
            System.out.println("              部署信息");
            System.out.println("========================================");
            System.out.println("  环境:    " + environment);
            System.out.println("  区域:    " + region);
            System.out.println("  实例数:  " + instances);
            System.out.println("  模式:    " + (dryRun ? "演练" : "正式"));

            if (rollbackVersion != null) {
                System.out.println("  回滚到:  " + rollbackVersion);
            }

            if (dryRun) {
                System.out.println();
                System.out.println("演练模式: 部署计划预览完成");
            } else {
                System.out.println();
                System.out.println("部署完成!");
            }
            return 0;
        }
    }

    // ==================== Main方法 ====================

    public static void main(String[] args) {
        System.out.println("Picocli 子命令示例程序 (Git风格)\n");

        SubcommandDemo demo = new SubcommandDemo();
        CommandLine cmd = new CommandLine(demo);

        // 添加全局子命令
        cmd.addSubcommand("user", new UserCommand())
           .addSubcommand("config", new ConfigCommand())
           .addSubcommand("deploy", new DeployCommand());

        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }
}
