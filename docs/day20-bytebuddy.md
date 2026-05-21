# Day 20 - Byte Buddy：Java 运行时字节码生成与操作库

## 工具简介

**Byte Buddy** 是一个在 Java 运行时动态生成和修改字节码的库，被广泛用于 AOP 框架、Mock 工具、代码生成和 Java Agent 开发等领域。

- **GitHub**: https://github.com/raphael/byte-buddy（官方：https://bytebuddy.net）
- **真实地址**: https://github.com/raphael/byte-buddy
- **Stars**: 11k+（持续增长）
- **版本**: 1.14.18（本文档使用版本）
- **维护状态**: 活跃维护（2024年仍有更新）
- **授权协议**: Apache License 2.0
- **知名使用者**: Mockito（底层使用 Byte Buddy 生成 Mock 对象）、Hibernate（懒加载代理）、Spring Boot（部分代理场景）

### 核心能力

| 能力 | 说明 |
|------|------|
| `subclass` | 运行时生成目标类的子类，并覆写/拦截指定方法 |
| `redefine` | 直接修改已有类的字节码（需配合 Java Agent） |
| `rebase` | 保留原方法副本并覆写，可同时调用原逻辑 |
| `defineField` / `defineMethod` | 动态添加字段或方法 |
| `MethodDelegation` | 将方法调用委托给另一个类（拦截器） |
| `FixedValue` | 让方法始终返回固定值（快速 Mock） |
| `FieldAccessor` | 自动生成 getter/setter |

---

## Maven 依赖配置

```xml
<dependencies>
    <!-- Byte Buddy 核心库 -->
    <dependency>
        <groupId>net.bytebuddy</groupId>
        <artifactId>byte-buddy</artifactId>
        <version>1.14.18</version>
    </dependency>

    <!-- Byte Buddy Agent（Java Agent 场景才需要） -->
    <dependency>
        <groupId>net.bytebuddy</groupId>
        <artifactId>byte-buddy-agent</artifactId>
        <version>1.14.18</version>
    </dependency>
</dependencies>
```

> **Java 8 兼容性**：Byte Buddy 1.14.x 完全支持 Java 8+，无需额外配置。

---

## 核心 API 速查

### 1. 基础结构

```java
// 三种操作模式
new ByteBuddy().subclass(MyClass.class)   // 生成子类（最常用）
new ByteBuddy().redefine(MyClass.class)   // 重定义（需 Java Agent）
new ByteBuddy().rebase(MyClass.class)     // 重基（保留原方法副本）

// 通用流程：创建 → 配置 → make → load → getLoaded
Class<?> clazz = new ByteBuddy()
    .subclass(Object.class)
    .method(ElementMatchers.named("toString"))
    .intercept(FixedValue.value("Hello ByteBuddy!"))
    .make()
    .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
    .getLoaded();
```

### 2. ElementMatchers（方法选择器）

```java
// 按名称
ElementMatchers.named("methodName")
ElementMatchers.nameStartsWith("get")
ElementMatchers.nameContains("User")

// 按签名
ElementMatchers.returns(String.class)
ElementMatchers.takesArguments(String.class, int.class)
ElementMatchers.isPublic()
ElementMatchers.isAnnotatedWith(Transactional.class)

// 组合
ElementMatchers.named("foo").or(ElementMatchers.named("bar"))
ElementMatchers.isPublic().and(ElementMatchers.not(ElementMatchers.named("equals")))
```

### 3. Implementation（实现策略）

```java
// 固定返回值
FixedValue.value("MOCKED")           // 返回具体值
FixedValue.nullValue()               // 返回 null

// 方法委托（最强大）
MethodDelegation.to(MyInterceptor.class)          // 委托给静态方法
MethodDelegation.to(new MyInterceptor())          // 委托给实例方法

// 字段访问器（生成 getter/setter）
FieldAccessor.ofField("fieldName")

// 调用父类
SuperMethodCall.INSTANCE

// 什么都不做（void 方法）
StubMethod.INSTANCE
```

### 4. MethodDelegation 拦截器注解

```java
public static class MyInterceptor {
    @RuntimeType                         // 必须有，标记返回值类型不检查
    public static Object intercept(
        @This Object self,               // 当前实例（this）
        @Origin Method method,           // 被拦截的方法对象
        @AllArguments Object[] args,     // 全部参数
        @SuperCall Callable<?> superCall // 调用父类方法的 Callable
    ) throws Exception {
        // 前置逻辑
        Object result = superCall.call(); // 调用原始方法
        // 后置逻辑
        return result;
    }
}
```

---

## Spring Boot 集成方式

### 方式一：BeanPostProcessor（AOP 代理增强）

```java
@Component
public class ByteBuddyAopPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean.getClass().isAnnotationPresent(Monitored.class)) {
            try {
                return wrapWithMonitoring(bean);
            } catch (Exception e) {
                throw new BeanCreationException("ByteBuddy wrap failed", e);
            }
        }
        return bean;
    }

    @SuppressWarnings("unchecked")
    private <T> T wrapWithMonitoring(T bean) throws Exception {
        Class<? extends T> enhanced = (Class<? extends T>) new ByteBuddy()
            .subclass(bean.getClass())
            .method(ElementMatchers.isPublic())
            .intercept(MethodDelegation.to(MetricsInterceptor.class))
            .make()
            .load(bean.getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
            .getLoaded();
        // 注意：Spring 管理的 Bean 需要特殊处理构造函数注入
        return enhanced.newInstance();
    }
}
```

### 方式二：Java Agent 模式（推荐用于生产环境全局 AOP）

```java
// META-INF/MANIFEST.MF 中配置：
// Agent-Class: com.example.MyAgent
// Premain-Class: com.example.MyAgent

public class MyAgent {
    public static void premain(String arguments, Instrumentation instrumentation) {
        new AgentBuilder.Default()
            .type(ElementMatchers.nameStartsWith("com.example.service"))
            .transform((builder, type, classLoader, module, protectionDomain) ->
                builder
                    .method(ElementMatchers.isPublic())
                    .intercept(MethodDelegation.to(LogInterceptor.class))
            )
            .installOn(instrumentation);
    }
}
```

```bash
# 启动时添加 JVM 参数
java -javaagent:my-agent.jar -jar app.jar
```

### 方式三：与 Spring AOP 对比选择

| 场景 | 推荐方案 |
|------|---------|
| Spring 管理的 Bean 方法增强 | Spring AOP（更成熟，更简单） |
| 非 Spring 类动态生成/代理 | Byte Buddy |
| 跨 JVM 进程 / Java Agent | Byte Buddy AgentBuilder |
| 测试 Mock（非 Spring 上下文） | Byte Buddy 或 Mockito（Mockito 底层就是 Byte Buddy） |

---

## 演示类说明

### `ByteBuddyBasicDemo.java` - 基础演示

| Demo | 演示内容 |
|------|---------|
| Demo 1 | 动态生成接口实现类（运行时无需写实现类） |
| Demo 2 | FixedValue 覆写方法（快速 Mock 场景） |
| Demo 3 | 方法拦截 AOP 日志（前置 + 后置 + 计时） |

### `ByteBuddyAdvancedDemo.java` - 进阶演示

| Demo | 演示内容 |
|------|---------|
| Demo 1 | 动态添加字段 + 自动生成 getter/setter（FieldAccessor） |
| Demo 2 | 动态添加注解（类型注解 + 方法注解） |
| Demo 3 | 按方法名条件分发拦截器（读写分离） |
| Demo 4 | rebase 用法（保留原方法副本） |

### `ByteBuddyPracticalDemo.java` - 实战演示

| 场景 | 演示内容 |
|------|---------|
| 场景一 | 轻量级 AOP 性能监控框架（统计调用次数/耗时/平均值） |
| 场景二 | 动态代理路由工厂（同一接口根据参数路由到不同实现） |
| 场景三 | 测试 Mock 工厂（无 Mockito，快速生成 Stub） |

---

## 注意事项

### Bug 风险 / 陷阱

#### 1. ClassLoader 冲突（最常见问题）
```java
// ❌ 错误：新类与原类使用相同 ClassLoader 加载同名类会冲突
.load(targetClass.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)

// ✅ 正确：用 WRAPPER 创建子 ClassLoader，避免冲突
.load(targetClass.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
```

#### 2. 重复创建类导致 ClassLoader 内存泄漏
```java
// ❌ 每次请求都生成新类 → ClassLoader 不断增长 → OOM
public Object createProxy() {
    return new ByteBuddy()
        .subclass(MyService.class)
        .make().load(...).getLoaded().newInstance(); // 每次都创建新 Class
}

// ✅ 正确：将生成的 Class 对象缓存起来
private static final Class<? extends MyService> PROXY_CLASS = buildProxyClass();
public Object createProxy() throws Exception {
    return PROXY_CLASS.newInstance();
}
```

#### 3. final 类和 final 方法无法被 subclass/拦截
```java
// ❌ 无法继承 final 类
new ByteBuddy().subclass(String.class)   // 抛出 IllegalArgumentException

// ✅ 只能 redefine（需要 Java Agent）
// 或使用接口方式绕过
```

#### 4. 构造函数注入的 Spring Bean 不能直接 newInstance()
```java
// ❌ 如果原类有带参构造函数，newInstance() 会失败
enhancedClass.newInstance();   // NoSuchMethodException

// ✅ 使用 objenesis（Byte Buddy 内置）
Objenesis objenesis = new ObjenesisStd();
T instance = objenesis.newInstance(enhancedClass);
```

### 性能注意事项

- **首次类生成有开销**（几十 ms），后续 `newInstance()` 与普通反射相当
- **建议缓存生成的 Class 对象**，不要在热路径中重复调用 `make().load()`
- **字节码操作本身**（方法调用拦截）性能开销约为普通调用的 1.1~1.5 倍，可接受

### 使用限制

| 限制 | 说明 |
|------|------|
| Java 9+ 模块系统 | 需要额外 `--add-opens` 参数才能访问封装模块 |
| Android | 不支持（Android 用 dexmaker/subzero） |
| final 类/方法 | `subclass/rebase` 无法处理，需 `redefine` + Java Agent |
| OSGi 环境 | ClassLoader 隔离问题，需显式指定 ClassLoader |
| GraalVM native-image | 编译期不执行字节码生成，需注册反射元数据 |

---

## 运行方法

```bash
# 进入项目目录
cd bytebuddy-demo

# 编译（需要 JDK 8+，推荐 JDK 11/17）
JAVA_HOME=D:/jdk/jdk17 mvn clean package -DskipTests

# 运行基础演示
JAVA_HOME=D:/jdk/jdk17 mvn exec:java -Dexec.mainClass="com.example.bytebuddy.ByteBuddyBasicDemo"

# 运行进阶演示
JAVA_HOME=D:/jdk/jdk17 mvn exec:java -Dexec.mainClass="com.example.bytebuddy.ByteBuddyAdvancedDemo"

# 运行实战演示
JAVA_HOME=D:/jdk/jdk17 mvn exec:java -Dexec.mainClass="com.example.bytebuddy.ByteBuddyPracticalDemo"
```

---

## 与同类工具对比

| 工具 | 易用性 | 功能 | 适用场景 |
|------|--------|------|---------|
| **Byte Buddy** | ★★★★☆ | 极强 | AOP、代理、Agent、Mock框架底层 |
| Cglib | ★★★☆☆ | 强 | Spring AOP 历史方案（逐渐被取代） |
| Javassist | ★★★☆☆ | 强 | 直接操作字节码字符串（学习成本高） |
| ASM | ★★☆☆☆ | 最强 | 底层字节码框架（专业领域使用） |
| JDK Proxy | ★★★★★ | 弱 | 仅支持接口代理，无需依赖 |

> **结论**：Byte Buddy 是目前 Java 动态代理和字节码操作的最优选择，比 Cglib 更现代，比 ASM 更易用。
