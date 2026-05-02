package com.example.fastjson2;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.TypeReference;
import com.alibaba.fastjson2.filter.ValueFilter;

import java.util.*;

/**
 * Fastjson2 实战场景演示
 * 涵盖：泛型响应封装、字段过滤/修改、大文件流式处理、与 Spring Boot 集成示例
 */
public class FastJson2PracticalDemo {

    // ========== 场景 1：泛型响应封装 ==========

    /**
     * 通用 API 响应封装类
     */
    static class ApiResponse<T> {
        private Integer code;
        private String message;
        private T data;
        private Long timestamp;

        public ApiResponse() {
            this.timestamp = System.currentTimeMillis();
        }

        public ApiResponse(Integer code, String message, T data) {
            this.code = code;
            this.message = message;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        // getter 和 setter
        public Integer getCode() { return code; }
        public void setCode(Integer code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    }

    /**
     * 分页结果封装
     */
    static class PageResult<T> {
        private List<T> records;
        private Long total;
        private Integer pageNum;
        private Integer pageSize;

        public PageResult() {}

        public PageResult(List<T> records, Long total, Integer pageNum, Integer pageSize) {
            this.records = records;
            this.total = total;
            this.pageNum = pageNum;
            this.pageSize = pageSize;
        }

        public List<T> getRecords() { return records; }
        public void setRecords(List<T> records) { this.records = records; }
        public Long getTotal() { return total; }
        public void setTotal(Long total) { this.total = total; }
        public Integer getPageNum() { return pageNum; }
        public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }
        public Integer getPageSize() { return pageSize; }
        public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    }

    static class UserDTO {
        private String username;
        private String email;
        private Integer age;

        public UserDTO() {}
        public UserDTO(String username, String email, Integer age) {
            this.username = username;
            this.email = email;
            this.age = age;
        }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
    }

    // ========== 场景 2：字段过滤和修改 ==========

    /**
     * 使用 Filter 在序列化时动态修改字段值
     */
    static void demoValueFilter() {
        System.out.println("--- 场景 2：使用 ValueFilter 过滤/修改字段值 ---");

        List<UserDTO> users = Arrays.asList(
                new UserDTO("alice", "alice@example.com", 25),
                new UserDTO("bob", "bob@example.com", 30)
        );

        // 定义 ValueFilter：将邮箱地址脱敏
        ValueFilter emailFilter = (object, name, value) -> {
            if ("email".equals(name) && value != null) {
                String email = value.toString();
                int atIndex = email.indexOf('@');
                if (atIndex > 1) {
                    return email.charAt(0) + "***" + email.substring(atIndex);
                }
            }
            return value;
        };

        String json = JSON.toJSONString(users,
                JSONWriter.Feature.PrettyFormat,
                JSONWriter.Feature.WriteMapNullValue);
        System.out.println("过滤前：");
        System.out.println(json);

        String jsonFiltered = JSON.toJSONString(users,
                // 传入 Filter
                (com.alibaba.fastjson2.filter.Filter) emailFilter,
                JSONWriter.Feature.PrettyFormat);
        System.out.println("\n过滤后（邮箱脱敏）：");
        System.out.println(jsonFiltered);
    }

    // ========== 场景 3：处理超大 JSON 文件（流式 API） ==========

    /**
     * 演示如何使用流式 API 处理大 JSON 文件（避免 OOM）
     * 注意：这里用字符串模拟，实际场景从文件流读取
     */
    static void demoStreamApi() {
        System.out.println("--- 场景 3：流式 API 处理大 JSON ---");
        System.out.println("说明：以下为代码示例，实际大文件处理应使用 JSONReader 的流式 API");

        // 构建模拟的大 JSON 数据
        StringBuilder largeJsonBuilder = new StringBuilder();
        largeJsonBuilder.append("[");
        for (int i = 0; i < 5; i++) {  // 实际场景可能是几十万条数据
            if (i > 0) largeJsonBuilder.append(",");
            largeJsonBuilder.append(String.format(
                    "{\"id\":%d,\"name\":\"用户%d\",\"score\":%d}",
                    i, i, (int) (Math.random() * 100)));
        }
        largeJsonBuilder.append("]");

        String largeJson = largeJsonBuilder.toString();
        System.out.println("\n模拟的大 JSON 数据（前 200 字符）：");
        System.out.println(largeJson.length() > 200
                ? largeJson.substring(0, 200) + "..."
                : largeJson);

        // 使用 parseArray 逐条处理（适合中等数据量）
        List<UserDTO> users = JSON.parseArray(largeJson, UserDTO.class);
        System.out.println("\n逐条处理结果（共 " + users.size() + " 条）：");
        users.forEach(u -> System.out.println("  " + u.getUsername() + " -> " + u.getAge()));

        System.out.println("\n提示：对于超大文件（GB 级别），应使用 JSONReader 的流式解析接口");
        System.out.println("示例：try (JSONReader reader = JSONReader.of(new FileReader(\"large.json\"))) { ... }");
    }

    // ========== 场景 4：常见坑和解决方案 ==========

    static void demoCommonPitfalls() {
        System.out.println("--- 场景 4：常见坑和解决方案 ---");

        // 坑 1：反序列化时类型擦除问题
        System.out.println("\n[坑 1] 反序列化泛型类型时需要使用 TypeReference");
        String json = "{\"code\":200,\"message\":\"成功\",\"data\":{\"username\":\"张三\",\"age\":28}}";

        // 错误方式：直接使用 ApiResponse.class，data 字段会变成 JSONObject
        ApiResponse<Object> wrong = JSON.parseObject(json, ApiResponse.class);
        System.out.println("错误方式：data 类型 = " + wrong.getData().getClass());

        // 正确方式：使用 TypeReference
        ApiResponse<UserDTO> correct = JSON.parseObject(
                json,
                new TypeReference<ApiResponse<UserDTO>>() {});
        System.out.println("正确方式：data 类型 = " + correct.getData().getClass());
        System.out.println("data.username = " + correct.getData().getUsername());

        // 坑 2：boolean 类型字段的序列化问题
        System.out.println("\n[坑 2] boolean 字段的 isXXX getter 问题");
        class Status {
            private boolean isActive;  // 注意：这会被识别为 'active' 而非 'isActive'
            public boolean isActive() { return isActive; }
            public void setActive(boolean active) { isActive = active; }
        }
        Status status = new Status();
        status.setActive(true);
        System.out.println("序列化结果：" + JSON.toJSONString(status));
        System.out.println("提示：boolean 字段建议不要用 'is' 开头命名");

        // 坑 3：BigDecimal 精度问题
        System.out.println("\n[坑 3] BigDecimal 序列化保持精度");
        class Amount {
            private java.math.BigDecimal value;
            public Amount() {}
            public Amount(java.math.BigDecimal value) { this.value = value; }
            public java.math.BigDecimal getValue() { return value; }
            public void setValue(java.math.BigDecimal value) { this.value = value; }
        }
        Amount amount = new Amount(new java.math.BigDecimal("123.456789"));
        // 使用 WriteBigDecimalAsPlain 保持精度
        String amountJson = JSON.toJSONString(amount,
                JSONWriter.Feature.WriteBigDecimalAsPlain);
        System.out.println("BigDecimal 序列化结果：" + amountJson);
    }

    public static void main(String[] args) {
        System.out.println("=== Fastjson2 实战场景演示 ===\n");

        // 场景 1：泛型响应封装
        demoGenericResponse();

        // 场景 2：字段过滤和修改
        demoValueFilter();

        // 场景 3：流式 API 处理大 JSON
        demoStreamApi();

        // 场景 4：常见坑和解决方案
        demoCommonPitfalls();
    }

    /**
     * 场景 1：泛型响应封装（REST API 常用）
     */
    static void demoGenericResponse() {
        System.out.println("--- 场景 1：泛型响应封装 ---");

        // 成功响应：带数据
        ApiResponse<UserDTO> successResponse = new ApiResponse<>(
                200, "操作成功",
                new UserDTO("张三", "zhangsan@example.com", 28)
        );
        System.out.println("成功响应（带数据）：");
        System.out.println(JSON.toJSONString(successResponse, JSONWriter.Feature.PrettyFormat));

        // 成功响应：带分页数据
        List<UserDTO> userList = Arrays.asList(
                new UserDTO("张三", "zhangsan@example.com", 28),
                new UserDTO("李四", "lisi@example.com", 32)
        );
        ApiResponse<PageResult<UserDTO>> pageResponse = new ApiResponse<>(
                200, "查询成功",
                new PageResult<>(userList, 100L, 1, 10)
        );
        System.out.println("\n成功响应（带分页）：");
        System.out.println(JSON.toJSONString(pageResponse, JSONWriter.Feature.PrettyFormat));

        // 错误响应
        ApiResponse<Void> errorResponse = new ApiResponse<>(500, "服务器内部错误", null);
        System.out.println("\n错误响应：");
        System.out.println(JSON.toJSONString(errorResponse));

        // 反序列化验证：使用 TypeReference 保持泛型类型
        String json = JSON.toJSONString(pageResponse);
        ApiResponse<PageResult<UserDTO>> parsed = JSON.parseObject(
                json,
                new TypeReference<ApiResponse<PageResult<UserDTO>>>() {}
        );
        System.out.println("\n反序列化后数据类型验证：");
        System.out.println("records count: " + parsed.getData().getRecords().size());
        System.out.println("first user: " + parsed.getData().getRecords().get(0).getUsername());

        System.out.println();
    }
}
