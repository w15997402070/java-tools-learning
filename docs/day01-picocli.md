# Day 1: Picocli - 强大的Java命令行解析框架

## 📌 工具概览

| 属性 | 值 |
|------|-----|
| **名称** | Picocli |
| **GitHub** | https://github.com/remkop/picocli |
| **星标数** | 5.3k ⭐ |
| **最新版本** | 4.7.7 (2025-04-19) |
| **许可证** | Apache-2.0 |
| **Java版本** | Java 5+ |
| **作者** | Remko Popma |

## 🎯 工具简介

Picocli是一个现代化的命令行解析框架，主要特点：

- **注解驱动**：使用注解定义命令行参数和选项
- **零样板代码**：单行代码即可执行命令
- **子命令支持**：支持git风格的嵌套子命令
- **ANSI彩色输出**：自动生成美观的帮助文档
- **TAB自动补全**：支持bash和zsh自动补全
- **GraalVM支持**：可编译为原生可执行文件
- **依赖注入集成**：支持Spring、Guice、Dagger等

## 🏆 知名项目采用

- Groovy
- Micronaut
- Quarkus
- JUnit 5
- Apache Hadoop
- CheckStyle
- Debbian官方仓库

---

## 📦 Spring Boot集成

### 1. 添加Maven依赖

```xml
<dependency>
    <groupId>info.picocli</groupId>
    <artifactId>picocli</artifactId>
    <version>4.7.7</version>
</dependency>

<!-- 如果使用GraalVM原生镜像 -->
<dependency>
    <groupId>info.picocli</groupId>
    <artifactId>picocli-native-image-support</artifactId>
    <version>4.7.7</version>
</dependency>
```

### 2. Gradle配置

```groovy
// Gradle (Kotlin DSL)
implementation("info.picocli:picocli:4.7.7")
implementation("info.picocli:picocli-codegen:4.7.7")

// 配置注解处理器
annotationProcessor("info.picocli:picocli-codegen:4.7.7")
```

### 3. Spring Boot中使用Picocli

#### 方式一：作为命令行工具

```java
@SpringBootApplication
public class MyCliApplication implements Runnable, CommandLine.IExitCodeExceptionMapper {

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "显示帮助信息")
    private boolean help;

    @Option(names = {"-v", "--version"}, versionHelp = true, description = "显示版本")
    private boolean version;

    @Option(names = {"-c", "--config"}, description = "配置文件路径")
    private File configFile;

    @Parameters(paramLabel = "<command>", description = "要执行的命令")
    private String command;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new MyCliApplication())
            .setExitCodeExceptionMapper(new MyCliApplication())
            .execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        // 执行逻辑
        System.out.println("执行命令: " + command);
    }

    @Override
    public int getExitCode(ExecutionException ex) {
        return 1;
    }
}
```

#### 方式二：作为Bean注入使用

```java
@Component
public class CliService {

    private final MyCommand command;

    public CliService() {
        this.command = new MyCommand();
    }

    public int execute(String[] args) {
        return new CommandLine(command).execute(args);
    }
}

@Command(name = "myapp", description = "我的应用命令")
public class MyCommand implements Runnable {

    @Option(names = {"-n", "--name"}, description = "用户名")
    private String name;

    @Override
    public void run() {
        System.out.println("Hello, " + name);
    }
}
```

#### 方式三：与Spring Boot CLI Runner结合

```java
@Component
@Order(1)
public class StartupCommandRunner implements CommandLineRunner {

    @Value("${cli.args:}", split = "")
    private String[] cliArgs;

    @Autowired
    private CliService cliService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (cliArgs.length > 0) {
            cliService.execute(cliArgs);
        }
    }
}
```

### 4. application.yml配置

```yaml
# 启动时传入CLI参数
cli:
  args: ["--name", "World"]
```

---

## ⚠️ 注意事项

### 🔴 潜在Bug

#### 1. 参数顺序敏感问题
```java
// ❌ 错误：位置参数必须在选项之前
@Command
public class BadExample implements Runnable {
    @Parameters(paramLabel = "<input>")
    private String input;

    @Option(names = {"-o", "--output"})
    private String output;
}
// 命令: app -o file.txt input.txt  // 可能解析错误
```

**解决方案**：使用 `@ArgGroup` 或明确指定参数顺序
```java
// ✅ 正确：明确参数顺序
CommandLine cmd = new CommandLine(new GoodExample())
    .setPosixClusteredShortOptionsAllowed(false)
    .setUnmatchedArgumentsAllowed(true);
```

#### 2. 布尔选项默认值混淆
```java
// ❌ 问题：-x 默认值是什么？
@Option(names = "-x")
private boolean flag;

// ✅ 明确指定
@Option(names = "-x", negatable = true)
private boolean flag;
```

#### 3. 子命令帮助信息不显示
```java
// ❌ 子命令没有正确继承
@Command(subcommands = {SubCommand.class})
public class MainCommand {}

// ✅ 需要显式添加HelpCommand
@Command(subcommands = {SubCommand.class, CommandLine.HelpCommand.class})
public class MainCommand {}
```

### 🟡 性能问题

#### 1. 大量参数解析时的性能
```java
// ❌ 每次解析都创建新实例
public void process(String[] args) {
    CommandLine cmd = new CommandLine(new MyCommand());
    cmd.execute(args);
}

// ✅ 复用CommandLine实例
private static final CommandLine COMMAND = new CommandLine(new MyCommand());

public void process(String[] args) {
    COMMAND.execute(args);
}
```

#### 2. 自动补全脚本生成
```java
// 生成自动补全脚本可能较慢，大型命令建议缓存
// 考虑使用 @CompletionCandidates 限制候选项
@Option(names = "--type", completionCandidates = TypeCandidates.class)
private String type;

public class TypeCandidates extends ArrayList<String> implements Iterable<String> {
    public TypeCandidates() {
        super(Arrays.asList("type1", "type2", "type3"));
    }
}
```

### 🟢 使用限制

#### 1. Java版本兼容
- 4.x版本需要Java 8+
- 如需Java 5支持，使用3.x版本

#### 2. 参数长度限制
- 单个参数值不能超过JVM的`java.io.File.read()`限制
- 超长参数建议使用文件输入

#### 3. 特殊字符处理
```java
// ❌ 含空格的参数需要引号
// 命令行: app --name "John Doe"  // 必须加引号

// ✅ 使用@ option避免歧义
@Option(names = "--name", split = ",")
private List<String> names;
```

#### 4. Windows兼容性问题
- 某些ANSI颜色代码在Windows CMD不支持
- 生产环境建议关闭颜色：`setterm -rgb off`

```java
// 检测并禁用颜色
ColorScheme scheme = ColorScheme.create_ANSI()
    .escapeCode("");  // 禁用颜色
```

---

## 📊 与同类工具对比

| 特性 | Picocli | JCommander | Commons CLI |
|------|---------|------------|------------|
| 注解支持 | ✅ | ❌ | ❌ |
| 子命令 | ✅ | ❌ | ❌ |
| 自动补全 | ✅ | ❌ | ❌ |
| 帮助生成 | ✅ | ✅ | ✅ |
| 类型转换 | ✅ | ✅ | ❌ |
| 文件参数 | ✅ | ❌ | ❌ |
| 零依赖 | ✅ | ✅ | ❌ |
| 学习曲线 | 低 | 低 | 中 |

---

## 🔗 相关资源

- [官方文档](https://picocli.info/)
- [用户手册](https://picocli.info/picocli_quick_guide.html)
- [最佳实践](https://picocli.info/#_tips_and_tricks)
- [示例代码](https://github.com/remkop/picocli/tree/main/picocli-examples)
