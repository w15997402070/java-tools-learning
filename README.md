# Java工具学习项目 (java-tools-learning)

本项目用于团队技术能力提升，每周/每天学习一个实用的开源Java工具。

## 📚 工具列表

### Day 1: Picocli - 命令行解析框架
- **GitHub**: https://github.com/remkop/picocli
- **星标**: 5.3k
- **版本**: 4.7.7
- **文档**: https://picocli.info
- **状态**: ✅ 已完成

### Day 2: Gson - Google JSON处理库
- **GitHub**: https://github.com/google/gson
- **星标**: 23k+
- **版本**: 2.10.1
- **文档**: [docs/day02-gson.md](docs/day02-gson.md)
- **状态**: ✅ 已完成

### Day 3: OkHttp - 高性能 HTTP 客户端
- **GitHub**: https://github.com/square/okhttp
- **星标**: 45k+
- **版本**: 4.12.0
- **文档**: [docs/day03-okhttp.md](docs/day03-okhttp.md)
- **状态**: ✅ 已完成

### Day 4: Apache POI - Office 文档处理库
- **GitHub**: https://github.com/apache/poi
- **星标**: 3.8k+
- **版本**: 5.2.5
- **文档**: [docs/day04-poi.md](docs/day04-poi.md)
- **状态**: ✅ 已完成

### Day 5: Google Guava - Java 工具类库
- **GitHub**: https://github.com/google/guava
- **星标**: 48k+
- **版本**: 33.1.0-jre
- **文档**: [docs/day05-guava.md](docs/day05-guava.md)
- **状态**: ✅ 已完成

### Day 6: ZXing - 二维码/条形码处理库
- **GitHub**: https://github.com/zxing/zxing
- **星标**: 32k+
- **版本**: 3.5.3
- **文档**: [docs/day06-zxing.md](docs/day06-zxing.md)
- **状态**: ✅ 已完成

---

## 🎯 学习目标

1. 掌握主流Java开源工具的使用
2. 了解工具与Spring Boot的集成方式
3. 避免使用过程中的常见坑
4. 建立团队共享的技术知识库

## 📁 项目结构

```
java-tools-learning/
├── picocli-demo/                    # Day 1: Picocli Demo
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/com/example/picocli/
│           └── resources/
├── gson-demo/                       # Day 2: Gson Demo
│   ├── pom.xml
│   └── src/
│       └── main/
│           └── java/com/example/gson/
│               ├── GsonBasicDemo.java      # 基础序列化/反序列化
│               ├── GsonAdvancedDemo.java   # 注解/适配器/命名策略
│               └── GsonPracticalDemo.java  # 泛型响应/流式API/JSON合并
├── okhttp-demo/                     # Day 3: OkHttp Demo
│   ├── pom.xml
│   └── src/
│       └── main/
│           └── java/com/example/okhttp/
│               ├── OkHttpBasicDemo.java    # 同步GET/带请求头/带参数
│               ├── OkHttpAdvancedDemo.java # POST/拦截器/异步请求
│               └── OkHttpPracticalDemo.java # REST API/文件上传/Cookie
├── poi-demo/                        # Day 4: Apache POI Demo
│   ├── pom.xml
│   └── src/
│       └── main/
│           └── java/com/example/poi/
│               ├── PoiExcelBasicDemo.java  # Excel基础：创建/读取/多Sheet/样式
│               ├── PoiExcelAdvancedDemo.java # Excel高级：公式/合并/冻结/日期
│               └── PoiPracticalDemo.java   # 实战：数据导出/Word文档/大数据流式
├── guava-demo/                      # Day 5: Google Guava Demo
│   ├── pom.xml
│   └── src/
│       └── main/
│           └── java/com/example/guava/
│               ├── GuavaCollectionsDemo.java    # 集合工具演示
│               ├── GuavaStringUtilsDemo.java    # 字符串工具演示
│               └── GuavaPracticalDemo.java      # 高级工具实战（缓存/限流/事件总线）
├── zxing-demo/                      # Day 6: ZXing Demo
│   ├── pom.xml
│   └── src/
│       └── main/
│           └── java/com/example/zxing/
│               ├── ZXingBasicDemo.java          # 基础：生成/解析/条形码
│               ├── ZXingAdvancedDemo.java       # 高级：彩色码/Logo/批量生成
│               └── ZXingPracticalDemo.java       # 实战：vCard/WiFi/海报/多码识别
└── docs/                           # 学习文档
    ├── day01-picocli.md
    ├── day02-gson.md
    ├── day03-okhttp.md
    ├── day04-poi.md
    ├── day05-guava.md
    └── day06-zxing.md
```

## 🚀 如何使用

每个Demo都包含完整的可运行代码和详细的集成文档。

## 📝 添加新工具

如需添加新的工具学习，请创建对应的子模块并更新本文档。
