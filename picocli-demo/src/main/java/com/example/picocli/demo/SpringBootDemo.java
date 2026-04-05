package com.example.picocli.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeExceptionMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

/**
 * Spring Boot集成Picocli示例
 *
 * 演示三种集成方式:
 * 1. @SpringBootApplication + CommandLineRunner
 * 2. CommandLineRunner作为Bean
 * 3. 与Spring Dependency Injection结合
 *
 * 运行方式:
 *   mvn spring-boot:run
 *   或者
 *   java -jar target/picocli-demo-1.0.0.jar [命令参数]
 */
@SpringBootApplication
public class SpringBootDemo {

    // ==================== Spring Boot集成的主命令 ====================

    @Command(name = "spring-picocli",
             description = "Spring Boot集成Picocli示例",
             mixinStandardHelpOptions = true,
             version = "1.0.0",
             subcommands = {
                 UserCommand.class,
                 ConfigCommand.class,
                 HelpCommand.class
             })
    static class ApplicationCommand implements Runnable {

        @Spec
        CommandSpec spec;

        @Option(names = {"-c", "--config"},
                description = "Spring配置文件")
        String config;

        @Override
        public void run() {
            System.out.println("请使用子命令。使用 --help 查看帮助。");
        }
    }

    // ==================== 用户管理子命令 ====================

    @Command(name = "user",
             description = "用户管理",
             subcommands = {
                 UserListCommand.class,
                 UserGetCommand.class,
                 UserCreateCommand.class,
                 HelpCommand.class
             })
    static class UserCommand implements Runnable {
        @Override
        public void run() {
            System.out.println("用户管理子命令: list, get, create");
            System.out.println("使用 'user <子命令> --help' 查看帮助");
        }
    }

    @Command(name = "list",
             description = "列出所有用户")
    static class UserListCommand implements Runnable {

        @Option(names = {"-p", "--page"},
                description = "页码",
                defaultValue = "1")
        int page;

        @Option(names = {"-s", "--size"},
                description = "每页大小",
                defaultValue = "10")
        int pageSize;

        @Override
        public void run() {
            System.out.println("用户列表 (第 " + page + " 页，每页 " + pageSize + " 条):");
            System.out.println("+----+--------------+---------------------+");
            System.out.println("| ID | 用户名        | 邮箱                |");
            System.out.println("+----+--------------+---------------------+");
            System.out.println("| 1  | admin        | admin@example.com   |");
            System.out.println("| 2  | user001      | user001@example.com |");
            System.out.println("| 3  | user002      | user002@example.com |");
            System.out.println("+----+--------------+---------------------+");
        }
    }

    @Command(name = "get",
             description = "获取用户详情")
    static class UserGetCommand implements Runnable {

        @Option(names = {"--id"},
                description = "用户ID",
                required = true)
        Long userId;

        @Override
        public void run() {
            System.out.println("用户详情:");
            System.out.println("  ID: " + userId);
            System.out.println("  用户名: user" + userId);
            System.out.println("  邮箱: user" + userId + "@example.com");
            System.out.println("  状态: 活跃");
        }
    }

    @Command(name = "create",
             description = "创建新用户")
    static class UserCreateCommand implements Runnable {

        @Option(names = {"--name"},
                description = "用户名",
                required = true)
        String name;

        @Option(names = {"--email"},
                description = "邮箱",
                required = true)
        String email;

        @Option(names = {"--role"},
                description = "角色",
                defaultValue = "user")
        String role;

        @Override
        public void run() {
            System.out.println("创建用户:");
            System.out.println("  用户名: " + name);
            System.out.println("  邮箱: " + email);
            System.out.println("  角色: " + role);
            System.out.println();
            System.out.println("用户创建成功!");
        }
    }

    // ==================== 配置管理子命令 ====================

    @Command(name = "config",
             description = "配置管理",
             subcommands = {
                 ConfigGetCommand.class,
                 ConfigSetCommand.class,
                 ConfigListCommand.class,
                 HelpCommand.class
             })
    static class ConfigCommand implements Runnable {
        @Override
        public void run() {
            System.out.println("配置管理子命令: get, set, list");
        }
    }

    @Command(name = "get",
             description = "获取配置项")
    static class ConfigGetCommand implements Runnable {

        @Option(names = {"--key"},
                description = "配置键",
                required = true)
        String key;

        @Override
        public void run() {
            // 模拟从Spring Environment获取配置
            System.out.println(key + " = " + "value_of_" + key);
        }
    }

    @Command(name = "set",
             description = "设置配置项")
    static class ConfigSetCommand implements Runnable {

        @Option(names = {"--key"},
                description = "配置键",
                required = true)
        String key;

        @Option(names = {"--value"},
                description = "配置值",
                required = true)
        String value;

        @Override
        public void run() {
            System.out.println("设置配置: " + key + " = " + value);
            System.out.println("配置已保存");
        }
    }

    @Command(name = "list",
             description = "列出所有配置")
    static class ConfigListCommand implements Runnable {

        @Option(names = {"--prefix"},
                description = "配置前缀过滤")
        String prefix;

        @Override
        public void run() {
            System.out.println("配置列表:");
            if (prefix == null) {
                prefix = "";
            }
            System.out.println("  " + prefix + "app.name = SpringPicocliDemo");
            System.out.println("  " + prefix + "app.version = 1.0.0");
            System.out.println("  " + prefix + "spring.profiles.active = default");
        }
    }

    // ==================== Spring Boot入口和配置 ====================

    public static void main(String[] args) {
        System.out.println();
        System.out.println("========================================");
        System.out.println("  Spring Boot + Picocli 集成示例");
        System.out.println("========================================");
        System.out.println();

        SpringApplication.run(SpringBootDemo.class, args);
    }

    /**
     * Picocli命令工厂
     */
    @Bean
    public CommandLine picocliCommandLine() {
        ApplicationCommand command = new ApplicationCommand();
        CommandLine cmd = new CommandLine(command);

        // 配置子命令
        cmd.addSubcommand("user", new UserCommand())
           .addSubcommand("config", new ConfigCommand());

        return cmd;
    }

    /**
     * 自定义命令运行器 - Spring Boot推荐方式
     */
    @Bean
    public CommandLineRunner picocliCommandLineRunner(CommandLine commandLine) {
        return args -> {
            int exitCode = commandLine.execute(args);
            if (exitCode != 0 && !(exitCode == Integer.MIN_VALUE + 1)) {
                // Integer.MIN_VALUE + 1 通常表示 --help 退出
                System.exit(exitCode);
            }
        };
    }

    /**
     * 异常退出码映射
     */
    @Bean
    public ExitCodeExceptionMapper exitCodeExceptionMapper() {
        return exception -> {
            System.err.println("错误: " + exception.getMessage());
            System.err.println("使用 --help 查看帮助");
            return 1;
        };
    }
}
