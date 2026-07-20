# Day 26 - Apache Commons IO 2.15.1

## 工具简介

Apache Commons IO 是 Apache 基金会维护的 IO 工具库，填补了 JDK 标准 IO API 的大量空白。它提供了对文件读写、流操作、文件名处理、文件过滤、目录遍历、文件监控等常见操作的简洁封装，是 Java 后端开发中使用最广泛的底层工具库之一。

- **GitHub**: https://github.com/apache/commons-io
- **官网**: https://commons.apache.org/proper/commons-io/
- **Stars**: 1.1k+（Apache 官方，实际下载量极大，Maven Central 月下载超 5 亿次）
- **版本**: 2.15.1（Java 8 兼容）
- **License**: Apache 2.0

### 核心模块

| 包/类 | 功能 |
|---|---|
| `FileUtils` | 文件读写、复制、移动、删除、大小统计 |
| `FilenameUtils` | 路径解析、扩展名提取、跨平台规范化 |
| `IOUtils` | 流/Reader/Writer 工具方法、流转换 |
| `LineIterator` | 大文件按行迭代（低内存） |
| `filefilter.*` | 组合式文件过滤器（后缀/通配符/时间/逻辑组合） |
| `comparator.*` | 文件排序工具（按大小、修改时间） |
| `input.*` | 增强输入流（BoundedInputStream、ReversedLinesFileReader、CloseShieldInputStream） |
| `output.*` | 增强输出流（TeeOutputStream、ByteArrayOutputStream、StringBuilderWriter） |
| `monitor.*` | 文件变更监控（FileAlterationMonitor） |

---

## Maven 依赖配置

```xml
<!-- Apache Commons IO -->
<dependency>
    <groupId>commons-io</groupId>
    <artifactId>commons-io</artifactId>
    <version>2.15.1</version>
</dependency>
```

> **注意**：与 `commons-io:commons-io` 不同，旧版坐标是 `org.apache.commons:commons-io`（已废弃），请使用上面的坐标。

---

## 核心 API 速查

### FileUtils

```java
// 读写文件
String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8);
FileUtils.writeStringToFile(file, append, StandardCharsets.UTF_8, true);  // 追加

List<String> lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
byte[] bytes = FileUtils.readFileToByteArray(file);
FileUtils.writeByteArrayToFile(file, bytes);

// 复制/移动
FileUtils.copyFile(src, dest);
FileUtils.copyFileToDirectory(file, dir);
FileUtils.copyDirectory(srcDir, destDir);
FileUtils.moveFile(src, dest);
FileUtils.moveFileToDirectory(file, destDir, createDestDir);
FileUtils.moveDirectory(srcDir, destDir);

// 目录操作
FileUtils.forceMkdir(dir);               // 递归创建目录
FileUtils.forceMkdirParent(file);        // 创建父目录
FileUtils.deleteDirectory(dir);          // 递归删除目录
FileUtils.cleanDirectory(dir);           // 清空目录（保留目录本身）
FileUtils.deleteQuietly(file);           // 删除，失败不抛异常

// 统计
long size = FileUtils.sizeOf(file);
long dirSize = FileUtils.sizeOfDirectory(dir);
String human = FileUtils.byteCountToDisplaySize(size);  // "1.5 MB"

// 比较
boolean same = FileUtils.contentEquals(file1, file2);

// 遍历
Collection<File> files = FileUtils.listFiles(dir, new SuffixFileFilter(".log"),
                                              TrueFileFilter.INSTANCE);  // 递归
LineIterator it = FileUtils.lineIterator(file, "UTF-8");
```

### FilenameUtils

```java
FilenameUtils.getName("/home/user/report.pdf")    // "report.pdf"
FilenameUtils.getBaseName("/home/user/report.pdf") // "report"
FilenameUtils.getExtension("/home/user/report.pdf")// "pdf"
FilenameUtils.getFullPath("/home/user/report.pdf") // "/home/user/"
FilenameUtils.normalize("C:\\..\\dir\\file.txt")   // 规范化路径
FilenameUtils.concat("/home/", "user/file.txt")    // 拼接路径
FilenameUtils.wildcardMatch("file.txt", "*.txt")   // true
FilenameUtils.removeExtension("file.tar.gz")       // "file.tar"
```

### IOUtils

```java
// 流 → 字符串/字节
String str = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
byte[] bytes = IOUtils.toByteArray(inputStream);
List<String> lines = IOUtils.readLines(reader);

// 字符串/字节 → 流
InputStream in = IOUtils.toInputStream("hello", StandardCharsets.UTF_8);

// 流复制
IOUtils.copy(inputStream, outputStream);
long copied = IOUtils.copyLarge(inputStream, outputStream);

// 关闭（不抛异常）
IOUtils.closeQuietly(closeable);
```

### 文件过滤器组合

```java
// 内置过滤器
SuffixFileFilter   suffix   = new SuffixFileFilter(new String[]{".txt", ".log"});
WildcardFileFilter wildcard = new WildcardFileFilter("*.csv");
AgeFileFilter      recent   = new AgeFileFilter(cutoffDate, false);  // 比 cutoff 新的
DirectoryFileFilter dirs    = DirectoryFileFilter.DIRECTORY;

// 组合过滤（AND / OR / NOT）
AndFileFilter combined = new AndFileFilter(suffix, recent);
OrFileFilter  eitherOr = new OrFileFilter(suffix, wildcard);
NotFileFilter notDirs  = new NotFileFilter(DirectoryFileFilter.DIRECTORY);
```

---

## Spring Boot 集成方式

Commons IO 是纯工具库，不需要 Spring Boot 专用配置，直接注入使用即可。

### 文件上传处理 Service

```java
@Service
@Slf4j
public class FileStorageService {

    @Value("${storage.base-dir:/data/uploads}")
    private String baseDir;

    /**
     * 保存上传文件，按 yyyy/MM/dd 归类目录
     */
    public String saveUpload(String originalFilename, InputStream inputStream) throws IOException {
        // 构建目录：/data/uploads/2026/05/29/
        String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
        File dir = new File(baseDir, datePath);
        FileUtils.forceMkdir(dir);

        // 防重名：加上时间戳前缀
        String ext = FilenameUtils.getExtension(originalFilename);
        String safeName = System.currentTimeMillis() + "."
                + (ext.isEmpty() ? "bin" : ext.toLowerCase());
        File dest = new File(dir, safeName);

        FileUtils.copyInputStreamToFile(inputStream, dest);  // 一行搞定写盘
        log.info("文件已保存: {}, 大小: {}", dest.getAbsolutePath(),
                FileUtils.byteCountToDisplaySize(dest.length()));
        return datePath + "/" + safeName;
    }

    /**
     * 按租户 ID 递归统计磁盘用量
     */
    public Map<String, String> diskUsageByTenant() {
        Map<String, String> result = new LinkedHashMap<>();
        File root = new File(baseDir);
        if (!root.isDirectory()) return result;
        for (File tenantDir : root.listFiles(File::isDirectory)) {
            long size = FileUtils.sizeOfDirectory(tenantDir);
            int count = FileUtils.listFiles(tenantDir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE).size();
            result.put(tenantDir.getName(),
                    count + " 个文件，" + FileUtils.byteCountToDisplaySize(size));
        }
        return result;
    }
}
```

### 文件变更监控 Spring Bean

```java
@Configuration
public class FileMonitorConfig {

    @Value("${monitor.watch-dir:/data/uploads}")
    private String watchDir;

    @Bean(destroyMethod = "stop")
    public FileAlterationMonitor fileMonitor() throws Exception {
        FileAlterationObserver observer = new FileAlterationObserver(new File(watchDir));
        observer.addListener(new FileAlterationListenerAdaptor() {
            @Override
            public void onFileCreate(File file) {
                log.info("新文件: {}", file.getName());
                // 触发业务逻辑：如推送通知、触发ETL等
            }
        });
        FileAlterationMonitor monitor = new FileAlterationMonitor(1000, observer);
        monitor.start();
        return monitor;
    }
}
```

### 定时日志清理 Spring Scheduled Task

```java
@Component
@Slf4j
public class LogCleanupTask {

    @Value("${log.dir:/var/log/app}")
    private String logDir;

    @Value("${log.retention-days:7}")
    private int retentionDays;

    /**
     * 每天凌晨 2 点清理 {retentionDays} 天前的日志
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanup() throws IOException {
        File dir = new File(logDir);
        if (!dir.isDirectory()) return;

        long cutoff = System.currentTimeMillis() - (long) retentionDays * 86400_000L;
        AgeFileFilter oldFiles = new AgeFileFilter(new Date(cutoff), true);  // 比 cutoff 旧的
        Collection<File> toDelete = FileUtils.listFiles(dir, oldFiles, TrueFileFilter.INSTANCE);

        long freed = 0;
        for (File f : toDelete) {
            freed += f.length();
            FileUtils.deleteQuietly(f);
        }
        log.info("日志清理完成：删除 {} 个文件，释放 {}", toDelete.size(),
                FileUtils.byteCountToDisplaySize(freed));
    }
}
```

---

## 注意事项

### 1. LineIterator 必须关闭
```java
// ❌ 忘记关闭会导致文件句柄泄漏
LineIterator it = FileUtils.lineIterator(file, "UTF-8");
while (it.hasNext()) { ... }

// ✅ 用 try-finally 或 try-with-resources 关闭
LineIterator it = FileUtils.lineIterator(file, "UTF-8");
try {
    while (it.hasNext()) { ... }
} finally {
    LineIterator.closeQuietly(it);
}
```

### 2. FileUtils.copyDirectory 行为差异（vs JDK）
```java
// Apache Commons IO：将 src 目录的内容复制到 dest（如果 dest 不存在则创建）
FileUtils.copyDirectory(src, dest);  // dest 是目标根目录

// 注意：dest 若已存在，内容会合并，不会先清空！
// 若需要全量覆盖，先 cleanDirectory，再 copyDirectory
FileUtils.cleanDirectory(dest);
FileUtils.copyDirectory(src, dest);
```

### 3. FileUtils.deleteDirectory vs deleteQuietly
```java
FileUtils.deleteDirectory(dir);   // 如果删除失败则抛 IOException
FileUtils.deleteQuietly(file);    // 失败静默忽略，适合最终清理

// ⚠️ 生产代码中谨慎使用 deleteQuietly，关键数据删除失败应该被感知到
```

### 4. BoundedInputStream 防止内存爆炸
```java
// 处理用户上传文件时，限制最大读取字节（防止 OOM）
long MAX_SIZE = 10 * 1024 * 1024L;   // 10 MB
try (BoundedInputStream bounded = new BoundedInputStream(rawInputStream, MAX_SIZE)) {
    byte[] data = IOUtils.toByteArray(bounded);
    if (data.length >= MAX_SIZE) {
        throw new IllegalArgumentException("文件超过 10MB 限制");
    }
}
```

### 5. FileUtils.readFileToString 不适合超大文件
```java
// ❌ 读 500MB 日志文件 → 一次性加载进内存
String all = FileUtils.readFileToString(bigFile, StandardCharsets.UTF_8);

// ✅ 大文件用 LineIterator 或 Java NIO Files.lines()
LineIterator it = FileUtils.lineIterator(bigFile, "UTF-8");
```

### 6. Windows 路径分隔符
```java
// FilenameUtils.normalize 在 Windows 下返回 \，在 Unix 下返回 /
// 如需统一为 Unix 风格（用于 URL、数据库存储）：
String unixPath = FilenameUtils.separatorsToUnix(FilenameUtils.normalize(path));
```

### 7. FileAlterationMonitor 依赖轮询，非 inotify
```java
// Commons IO Monitor 是轮询实现，最小间隔由构造参数决定（毫秒）
// 不适合高频变更场景（高频建议用 Java 7 WatchService）
// 适合：配置文件刷新、日志文件收集、低频目录同步
new FileAlterationMonitor(2000, observer);  // 每 2 秒轮询一次
```

---

## 运行方法

```bash
cd d:/ai/workbuddy/java-tools-learning/commons-io-demo

# 编译打包
JAVA_HOME=D:/jdk/jdk17 mvn clean package -DskipTests

# 运行基础演示
JAVA_HOME=D:/jdk/jdk17 java -cp target/commons-io-demo-1.0-SNAPSHOT.jar \
    com.example.commonsio.CommonsIOBasicDemo

# 运行进阶演示
JAVA_HOME=D:/jdk/jdk17 java -cp target/commons-io-demo-1.0-SNAPSHOT.jar \
    com.example.commonsio.CommonsIOAdvancedDemo

# 运行实战演示
JAVA_HOME=D:/jdk/jdk17 java -cp target/commons-io-demo-1.0-SNAPSHOT.jar \
    com.example.commonsio.CommonsIOPracticalDemo
```

---

## 与 Java NIO (Files/Path) 的选择建议

| 场景 | 推荐 |
|---|---|
| 读写小文件（< 10MB）、字符编码、追加写 | **Commons IO FileUtils**（代码更简洁） |
| 超大文件、异步 IO、内存映射 | **Java NIO (Files + FileChannel)** |
| 路径拼接、跨平台扩展名 | **Commons IO FilenameUtils** |
| 递归目录遍历 + 过滤 | **Commons IO FileUtils.listFiles** |
| 文件变更监控（低频） | **Commons IO FileAlterationMonitor** |
| 文件变更监控（高频/生产级） | **Java 7 WatchService** |
| Stream 流操作、函数式遍历 | **Java NIO Files.walk** (Java 8+) |

> Spring Boot 已将 Commons IO 作为隐式传递依赖，项目中通常不需要额外声明，但建议显式声明版本以锁定兼容性。
