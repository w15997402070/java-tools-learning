# Day 39: Mockito — Java 单元测试模拟框架

## 1. 工具简介

**Mockito** 是 Java 生态中最流行的 mocking 框架之一，配合 JUnit 5 与 AssertJ 可写出高可读性、高维护性的单元测试。它专注于：

- **模拟外部依赖**（数据库、HTTP 客户端、消息队列等），让单元测试只关注当前类。
- **验证行为**（方法是否被调用、调用次数、调用顺序、传入参数）。
- **控制返回值**（stubbing），包括连续返回值、异常抛出等。

- **GitHub**: https://github.com/mockito/mockito
- **官网文档**: https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html
- **星标**: 15k+
- **版本**: 4.11.0（Java 8 兼容；Mockito 5.x 需要 Java 11+）

## 2. Maven 依赖

```xml
<properties>
    <mockito.version>4.11.0</mockito.version>
    <junit.version>5.10.2</junit.version>
    <assertj.version>3.24.2</assertj.version>
</properties>

<dependencies>
    <!-- Mockito 核心 -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <version>${mockito.version}</version>
        <scope>test</scope>
    </dependency>

    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>${junit.version}</version>
        <scope>test</scope>
    </dependency>

    <!-- AssertJ 流式断言 -->
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <version>${assertj.version}</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## 3. 核心 API 速查

| 场景 | 写法 |
|------|------|
| 创建 mock | `Mockito.mock(OrderRepository.class)` |
| 注解方式 | `@Mock`、`@InjectMocks`、`@Spy` |
| 打桩返回值 | `when(mock.findById(1L)).thenReturn(Optional.of(order))` |
| 连续返回值 | `.thenReturn(a).thenReturn(b).thenThrow(...)` |
| 参数匹配器 | `anyLong()`、`any(Order.class)`、`eq(1L)` |
| void 方法验证 | `verify(mock, times(2)).save(order)` |
| 从不出调用 | `verify(mock, never()).updateStatus(...)` |
| 捕获参数 | `ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class)` |
| BDD 风格 | `given(mock.foo()).willReturn(x)` / `then(mock).should().foo()` |
| 验证顺序 | `InOrder inOrder = inOrder(mockA, mockB)` |
| 部分 mock | `spy(realObject)` |

## 4. Spring Boot 集成方式

Spring Boot 2.7+ 已经内置了 `spring-boot-starter-test`，其中包含 Mockito + JUnit 5 + AssertJ，通常不需要额外引入。

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

### 4.1 使用 `@ExtendWith(MockitoExtension.class)`

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void shouldPayOrderSuccessfully() {
        // given
        Order order = new Order(1L, 100L, new BigDecimal("99.00"), OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentService.charge(order))
                .thenReturn(new PaymentResult(true, "TXN-001", "ok"));

        // when
        boolean result = orderService.payOrder(1L);

        // then
        assertThat(result).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository).save(order);
    }
}
```

### 4.2 与 Spring 上下文结合：`@MockBean`

如果需要替换 Spring 容器中的真实 Bean，可用 `@MockBean`：

```java
@SpringBootTest
class OrderControllerTest {

    @MockBean
    private OrderService orderService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnOrder() throws Exception {
        when(orderService.findById(1L))
                .thenReturn(new Order(1L, 100L, new BigDecimal("99.00"), OrderStatus.PAID));

        mockMvc.perform(get("/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }
}
```

## 5. 注意事项

### 5.1 混合使用原始值与匹配器会报错

错误写法：

```java
when(repository.findById(eq(1L), anyString())).thenReturn(...); // OK
when(repository.findById(1L, anyString())).thenReturn(...);     // 报错
```

**所有参数要么全部用匹配器，要么全部用真实值。**

### 5.2 `when(...).thenReturn(...)` 对 void 方法无效

void 方法不能用 `when(...)` 打桩返回值，应使用 `doNothing()` / `doThrow()` / `doAnswer()`：

```java
doNothing().when(notificationService).notifyPaid(any(Order.class));
doThrow(new RuntimeException("sms failed"))
    .when(notificationService).notifyPaid(any(Order.class));
```

### 5.3 Spy 对象慎用 `when(...)`

`when(spy.method()).thenReturn(...)` 会先执行真实方法再返回 stub 值，可能触发副作用。推荐：

```java
doReturn("stubbed").when(spyList).get(1);
```

### 5.4 `verify` 验证的是 mock 行为，不是真实结果

`verify` 只能验证**被 mock 的对象**的方法调用；对于真实对象或静态方法，需要使用 `Mockito.mockStatic()`（Mockito 3.4+）或 PowerMock。

### 5.5 Mockito 版本与 Java 版本

| Mockito 版本 | 最低 JDK |
|-------------|---------|
| 4.x         | Java 8  |
| 5.x         | Java 11 |

**Spring Boot 2.7.x 默认带 Mockito 4.x；Spring Boot 3.x 默认带 Mockito 5.x。**

### 5.6 不要 mock 值对象（DTO/VO/Entity）

值对象通常没有复杂依赖，直接用 `new` 构造即可。mock 值对象会导致 equals/hashCode 行为异常，且增加测试复杂度。

### 5.7 `verifyNoMoreInteractions` 过度使用

`verifyNoInteractions()` 和 `verifyNoMoreInteractions()` 会让测试变得脆弱，建议只在关键流程中使用，不要每个测试都验证。

## 6. 运行方法

### 6.1 编译打包（跳过测试运行）

```bash
cd d:/ai/workbuddy/java-tools-learning/mockito-demo
mvn clean package -DskipTests
```

### 6.2 运行所有测试

```bash
mvn test
```

### 6.3 运行单个测试类

```bash
mvn test -Dtest=MockitoBasicDemo
mvn test -Dtest=MockitoAdvancedDemo
mvn test -Dtest=MockitoPracticalDemo
```

## 7. 本 Demo 结构

```
mockito-demo/
├── pom.xml
├── src/main/java/com/example/mockito/
│   ├── domain/
│   │   ├── Order.java
│   │   ├── OrderStatus.java
│   │   ├── PaymentResult.java
│   │   └── User.java
│   ├── repository/
│   │   └── OrderRepository.java
│   └── service/
│       ├── InventoryClient.java
│       ├── NotificationService.java
│       ├── OrderService.java
│       ├── PaymentService.java
│       └── UserService.java
└── src/test/java/com/example/mockito/
    ├── MockitoBasicDemo.java      # 基础：mock / stub / verify / 参数匹配器
    ├── MockitoAdvancedDemo.java   # 进阶：注解 / Spy / ArgumentCaptor / BDD / InOrder
    └── MockitoPracticalDemo.java  # 实战：订单支付/取消/折扣完整流程
```

## 8. 推荐学习路径

1. 先跑通 `MockitoBasicDemo`，理解 mock / when / verify。
2. 再看 `MockitoAdvancedDemo`，掌握注解注入、Spy、ArgumentCaptor、BDD 风格。
3. 最后看 `MockitoPracticalDemo`，学习如何用 Mockito 隔离依赖并测试完整业务流。
4. 在 Spring Boot 项目中使用 `@ExtendWith(MockitoExtension.class)` 或 `@MockBean`。
