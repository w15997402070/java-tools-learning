package com.example.mapstruct;

import com.example.mapstruct.dto.UserCreateRequest;
import com.example.mapstruct.dto.UserDTO;
import com.example.mapstruct.entity.User;
import com.example.mapstruct.mapper.UserMapper;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * MapStruct 基础演示
 *
 * <p>演示内容：
 * 1. 简单字段映射（字段名相同自动映射）
 * 2. 字段名不同时的显式映射（@Mapping source/target）
 * 3. 字段忽略（password 不映射到 DTO）
 * 4. 自定义表达式转换（Date -> String 格式化，状态码 -> 描述）
 * 5. 反向映射（DTO -> Entity）
 * 6. 从请求对象创建实体
 * 7. 增量更新已有对象（@MappingTarget）
 * 8. 批量映射（List<User> -> List<UserDTO>）
 *
 * <p>运行方法：
 * <pre>
 *   mvn clean package -DskipTests
 *   java -cp target/mapstruct-demo-1.0-SNAPSHOT.jar com.example.mapstruct.MapStructBasicDemo
 * </pre>
 */
public class MapStructBasicDemo {

    public static void main(String[] args) {
        System.out.println("========== MapStruct 基础演示 ==========\n");

        demo1SimpleMapping();
        demo2ReverseMapping();
        demo3FromCreateRequest();
        demo4UpdateMapping();
        demo5BatchMapping();
    }

    /**
     * Demo 1: 简单映射 User -> UserDTO
     */
    private static void demo1SimpleMapping() {
        System.out.println("--- Demo 1: User -> UserDTO ---");

        // 构造测试数据
        User user = new User(
                1001L,
                "zhangsan",
                "encrypted_password_123",  // 此字段不应出现在 DTO 中
                "张三",
                "zhangsan@example.com",
                "13812345678",
                28,
                new BigDecimal("9999.88"),
                new Date(),
                new Date(),
                1
        );

        // 使用 MapStruct 映射（调用编译期生成的实现类）
        UserDTO dto = UserMapper.INSTANCE.toDTO(user);

        System.out.println("原始 User 对象：");
        System.out.println("  username=" + user.getUsername());
        System.out.println("  realName=" + user.getRealName());
        System.out.println("  password=" + user.getPassword());
        System.out.println("  status=" + user.getStatus());
        System.out.println("  createTime=" + user.getCreateTime());

        System.out.println("\n映射后 UserDTO 对象：");
        System.out.println("  username=" + dto.getUsername());
        System.out.println("  name=" + dto.getName());           // realName -> name
        System.out.println("  statusDesc=" + dto.getStatusDesc()); // 1 -> "启用"
        System.out.println("  createTimeStr=" + dto.getCreateTimeStr()); // Date -> "yyyy-MM-dd HH:mm:ss"

        // 验证 password 字段不在 DTO 中（安全检查）
        // UserDTO 类中根本没有 password 字段，编译期保证安全

        System.out.println("\n✅ password 字段已成功隔离，不在 DTO 中");
        System.out.println();
    }

    /**
     * Demo 2: 反向映射 UserDTO -> User
     */
    private static void demo2ReverseMapping() {
        System.out.println("--- Demo 2: UserDTO -> User（反向映射）---");

        UserDTO dto = new UserDTO();
        dto.setId(2001L);
        dto.setUsername("lisi");
        dto.setName("李四");
        dto.setEmail("lisi@example.com");
        dto.setPhone("13987654321");
        dto.setAge(30);

        User user = UserMapper.INSTANCE.toEntity(dto);

        System.out.println("UserDTO.name=" + dto.getName());
        System.out.println("User.realName=" + user.getRealName());   // name -> realName
        System.out.println("User.id=" + user.getId());               // ignored -> null
        System.out.println("User.password=" + user.getPassword());   // ignored -> null
        System.out.println("User.createTime=" + user.getCreateTime()); // ignored -> null
        System.out.println("✅ 关键字段（id/password/createTime）已成功忽略");
        System.out.println();
    }

    /**
     * Demo 3: 从创建请求映射到 User Entity
     */
    private static void demo3FromCreateRequest() {
        System.out.println("--- Demo 3: UserCreateRequest -> User ---");

        UserCreateRequest request = new UserCreateRequest(
                "wangwu",
                "raw_password",
                "王五",
                "wangwu@example.com",
                "13700001111",
                25
        );

        User user = UserMapper.INSTANCE.fromCreateRequest(request);

        System.out.println("请求 username=" + request.getUsername());
        System.out.println("请求 realName=" + request.getRealName());
        System.out.println("映射后 User.username=" + user.getUsername());
        System.out.println("映射后 User.realName=" + user.getRealName());
        System.out.println("映射后 User.id=" + user.getId());         // ignored -> null（由数据库生成）
        System.out.println("映射后 User.status=" + user.getStatus()); // ignored -> null（由后端设置默认值）
        System.out.println("✅ id/status/balance 已被忽略，待后端填充");
        System.out.println();
    }

    /**
     * Demo 4: 增量更新（@MappingTarget）
     */
    private static void demo4UpdateMapping() {
        System.out.println("--- Demo 4: 增量更新（@MappingTarget）---");

        // 模拟数据库中已有的 User 对象
        User existingUser = new User(
                3001L,
                "zhaoliu",
                "hashed_password_xyz",
                "赵六",
                "zhaoliu@example.com",
                "13611112222",
                35,
                new BigDecimal("5000.00"),
                new Date(),
                new Date(),
                1
        );

        // 前端传来的更新请求（只更新部分字段）
        UserDTO updateDTO = new UserDTO();
        updateDTO.setName("赵六（已更新）");
        updateDTO.setEmail("zhaoliu_new@example.com");
        updateDTO.setPhone("13633334444");
        updateDTO.setAge(36);

        System.out.println("更新前 User.realName=" + existingUser.getRealName());
        System.out.println("更新前 User.id=" + existingUser.getId());
        System.out.println("更新前 User.password=" + existingUser.getPassword());

        // updateFromDTO：将 DTO 的字段更新到已有的 User 对象上
        UserMapper.INSTANCE.updateFromDTO(updateDTO, existingUser);

        System.out.println("更新后 User.realName=" + existingUser.getRealName());   // 已更新
        System.out.println("更新后 User.id=" + existingUser.getId());               // 不变：3001L
        System.out.println("更新后 User.password=" + existingUser.getPassword());   // 不变
        System.out.println("更新后 User.balance=" + existingUser.getBalance());     // 不变
        System.out.println("✅ @MappingTarget 精准更新，不影响 id/password/balance");
        System.out.println();
    }

    /**
     * Demo 5: 批量映射 List<User> -> List<UserDTO>
     */
    private static void demo5BatchMapping() {
        System.out.println("--- Demo 5: 批量映射 List<User> -> List<UserDTO> ---");

        List<User> users = Arrays.asList(
                new User(1L, "user001", "pwd1", "用户001", "u1@test.com", "13800000001", 20,
                        BigDecimal.TEN, new Date(), new Date(), 1),
                new User(2L, "user002", "pwd2", "用户002", "u2@test.com", "13800000002", 25,
                        BigDecimal.ZERO, new Date(), new Date(), 0),
                new User(3L, "user003", "pwd3", "用户003", "u3@test.com", "13800000003", 30,
                        new BigDecimal("100"), new Date(), new Date(), 1)
        );

        List<UserDTO> dtos = UserMapper.INSTANCE.toDTOList(users);

        System.out.println("批量映射 " + users.size() + " 个用户：");
        for (UserDTO dto : dtos) {
            System.out.println("  [" + dto.getId() + "] " + dto.getUsername()
                    + " -> " + dto.getName()
                    + " 状态:" + dto.getStatusDesc()
                    + " 创建时间:" + dto.getCreateTimeStr());
        }
        System.out.println("✅ 批量映射完成，只需一行代码，无需手写循环");
        System.out.println();

        System.out.println("========== 演示完成 ==========");
    }
}
