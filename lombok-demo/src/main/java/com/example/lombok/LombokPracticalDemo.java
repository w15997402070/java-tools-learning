package com.example.lombok;

import lombok.*;
import javax.validation.constraints.AssertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Lombok 实战演示类
 *
 * 本类演示 Lombok 在实际项目中的典型使用场景：
 * 1. 实体类 (Entity/DTO/VO) 的最佳实践
 * 2. 响应结果封装 (Result/Response)
 * 3. Spring 组件类
 * 4. 配置属性类
 * 5. 异常类
 * 6. Builder 模式实战 - 复杂对象构建
 */
public class LombokPracticalDemo {

    public static void main(String[] args) {
        System.out.println("=== Lombok 实战演示 ===\n");

        // 1. DTO/VO 实战 - 使用 @Data + @Builder
        System.out.println("1. DTO/VO 实战:");
        UserVO userVO = UserVO.builder()
                .id(1L)
                .username("john_doe")
                .realName("约翰·杜")
                .phone("13800138000")
                .roles(List.of("ADMIN", "USER"))
                .build();
        System.out.println("   " + userVO);
        System.out.println();

        // 2. 统一响应封装
        System.out.println("2. 统一响应封装:");
        ApiResponse<List<String>> successResponse = ApiResponse.success(List.of("item1", "item2"));
        ApiResponse<Void> errorResponse = ApiResponse.error(500, "Internal Server Error");
        System.out.println("   成功响应: " + successResponse);
        System.out.println("   错误响应: " + errorResponse);
        System.out.println();

        // 3. 配置属性类 - @ConfigurationProperties
        System.out.println("3. 配置属性类:");
        AppConfig config = new AppConfig();
        config.setAppName("MyApplication");
        config.setMaxConnections(100);
        config.setTimeout(3000L);
        System.out.println("   " + config);
        System.out.println();

        // 4. 异常类设计
        System.out.println("4. 自定义异常类:");
        BusinessException ex = new BusinessException("USER001", "用户不存在");
        System.out.println("   异常: " + ex);
        System.out.println("   错误码: " + ex.getCode());
        System.out.println("   消息: " + ex.getMessage());
        System.out.println();

        // 5. 分页结果封装
        System.out.println("5. 分页结果封装:");
        PageResult<UserVO> pageResult = PageResult.<UserVO>builder()
                .records(List.of(userVO))
                .total(100L)
                .page(1)
                .size(10)
                .build();
        System.out.println("   " + pageResult);
        System.out.println("   是否有上一页: " + pageResult.hasPrevious());
        System.out.println("   是否有下一页: " + pageResult.hasNext());
        System.out.println("   总页数: " + pageResult.getTotalPages());
        System.out.println();

        // 6. 事件对象
        System.out.println("6. 领域事件对象:");
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .userId(1L)
                .username("new_user")
                .email("new@example.com")
                .registeredAt(LocalDateTime.now())
                .source("WEB")
                .build();
        System.out.println("   " + event);
        System.out.println();

        // 7. Lombok + Optional 处理
        System.out.println("7. Lombok + Optional 实战:");
        Optional<String> name = Optional.ofNullable(userVO.getRealName());
        String displayName = name.orElse(userVO.getUsername());
        System.out.println("   显示名称: " + displayName);
    }
}

// ==================== 实战类定义 ====================

/**
 * 用户视图对象 (VO)
 * 使用 @Data + @Builder 实现不可变 POJO
 * 使用 @Accessors 优化 setter 链式调用
 */
@Data
@Builder
@ToString
public class UserVO {
    private Long id;
    private String username;
    private String realName;
    private String phone;
    private List<String> roles;

    // 排除敏感字段不输出
    @ToString.Exclude
    private String password;

    // 自定义 getter - 显示名称
    public String getDisplayName() {
        return realName != null ? realName : username;
    }
}

/**
 * 统一 API 响应封装
 * 使用泛型 + Builder 模式
 */
@Data
@Builder
@ToString
public class ApiResponse<T> {
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;

    // 静态工厂方法 - 成功响应
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message("Success")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    // 静态工厂方法 - 错误响应
    public static <T> ApiResponse<T> error(Integer code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    // 便捷方法
    public boolean isSuccess() {
        return this.code != null && this.code == 200;
    }
}

/**
 * 应用配置属性类
 * 用于 Spring @ConfigurationProperties 绑定
 * 注意：实际使用时需要添加 @ConfigurationProperties(prefix = "app") 注解
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppConfig {
    private String appName;
    private String version;
    private Integer maxConnections;
    private Long timeout;
    private List<String> allowedOrigins;
    private Boolean enableCache;

    // 配置校验
    @AssertTrue(message = "maxConnections 必须大于 0")
    public boolean isMaxConnectionsValid() {
        return maxConnections == null || maxConnections > 0;
    }
}

/**
 * 自定义业务异常
 * 包含错误码和详细信息
 */
public class BusinessException extends RuntimeException {
    private final String code;
    private final String details;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.details = null;
    }

    public BusinessException(String code, String message, String details) {
        super(message);
        this.code = code;
        this.details = details;
    }

    public String getCode() {
        return code;
    }

    public String getDetails() {
        return details;
    }

    @Override
    public String toString() {
        return "BusinessException{" +
                "code='" + code + '\'' +
                ", message='" + getMessage() + '\'' +
                ", details='" + details + '\'' +
                '}';
    }
}

/**
 * 分页结果封装
 * 提供便捷的分页计算方法
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    private List<T> records;
    private Long total;
    private Integer page;
    private Integer size;

    // 计算总页数
    public int getTotalPages() {
        if (size == null || size == 0) {
            return 0;
        }
        return (int) Math.ceil((double) total / size);
    }

    // 是否有上一页
    public boolean hasPrevious() {
        return page != null && page > 1;
    }

    // 是否有下一页
    public boolean hasNext() {
        return page != null && page < getTotalPages();
    }

    // 起始行号
    public int getOffset() {
        return (page - 1) * size;
    }
}

/**
 * 领域事件
 * 使用 @Builder 和 @Wither 实现不可变事件
 */
@Data
@Builder
@Wither
public class UserRegisteredEvent {
    private Long userId;
    private String username;
    private String email;
    private LocalDateTime registeredAt;
    private String source;  // WEB, APP, API

    // 事件发生时间
    public LocalDateTime getOccurredOn() {
        return registeredAt != null ? registeredAt : LocalDateTime.now();
    }
}
