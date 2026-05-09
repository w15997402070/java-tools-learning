package com.example.mapstruct;

import com.example.mapstruct.dto.DeliveryInfoDTO;
import com.example.mapstruct.dto.UserDTO;
import com.example.mapstruct.entity.Address;
import com.example.mapstruct.entity.User;
import com.example.mapstruct.mapper.DeliveryInfoMapper;
import com.example.mapstruct.mapper.UserMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * MapStruct 实战演示 - Spring Boot 集成模式
 *
 * <p>演示内容：
 * 1. 多源对象合并映射（User + Address -> DeliveryInfoDTO）
 * 2. 表达式拼接字段（fullAddress = 省+市+区+详细地址）
 * 3. 实战场景：批量用户列表映射（模拟分页查询后的数据处理）
 * 4. 性能对比说明（MapStruct vs BeanUtils vs 手写）
 * 5. Spring Boot 集成模式代码示例（以注释形式展示）
 *
 * <p>注意：本 Demo 以独立方式运行，展示 Spring Boot 集成模式时使用注释说明。
 *
 * <p>运行方法：
 * <pre>
 *   java -cp target/mapstruct-demo-1.0-SNAPSHOT.jar com.example.mapstruct.MapStructPracticalDemo
 * </pre>
 */
public class MapStructPracticalDemo {

    public static void main(String[] args) {
        System.out.println("========== MapStruct 实战演示 ==========\n");

        demo1MultiSourceMapping();
        demo2FullAddressConcatenation();
        demo3BatchUserListProcessing();
        demo4PerformanceComparison();
        demo5SpringBootIntegrationGuide();
    }

    /**
     * Demo 1: 多源映射（User + Address -> DeliveryInfoDTO）
     *
     * <p>场景：电商下单时，收货信息来自用户基础信息（联系人、电话）
     * 和地址信息（省市区详细地址）两个对象，需要合并到一个配送DTO。
     */
    private static void demo1MultiSourceMapping() {
        System.out.println("--- Demo 1: 多源对象合并映射（电商配送信息）---");

        User user = new User(101L, "customer001", "p", "陈晓明",
                "chenxm@example.com", "13912345678", 32,
                new BigDecimal("2000"), new Date(), new Date(), 1);

        Address address = new Address(
                "广东省",
                "深圳市",
                "南山区",
                "科技园南路18号腾讯大厦B座1001室",
                "518057"
        );

        // 一次调用，合并两个对象的字段
        DeliveryInfoDTO dto = DeliveryInfoMapper.INSTANCE.toDTO(user, address);

        System.out.println("来源 User.realName=" + user.getRealName());
        System.out.println("来源 User.phone=" + user.getPhone());
        System.out.println("来源 Address.province=" + address.getProvince());
        System.out.println("来源 Address.city=" + address.getCity());

        System.out.println("\n合并后 DeliveryInfoDTO：");
        System.out.println("  收件人: " + dto.getRecipientName());
        System.out.println("  电话: " + dto.getPhone());
        System.out.println("  省: " + dto.getProvince());
        System.out.println("  市: " + dto.getCity());
        System.out.println("  区: " + dto.getDistrict());
        System.out.println("  详细地址: " + dto.getDetail());
        System.out.println("  邮编: " + dto.getZipCode());
        System.out.println("  完整地址: " + dto.getFullAddress());
        System.out.println("✅ 两个对象字段成功合并，expression 拼接完整地址");
        System.out.println();
    }

    /**
     * Demo 2: 不同地址格式的拼接演示
     */
    private static void demo2FullAddressConcatenation() {
        System.out.println("--- Demo 2: 完整地址拼接 ---");

        User user = new User();
        user.setRealName("李丽");
        user.setPhone("13766667777");

        Address[] addresses = {
                new Address("北京市", "北京市", "朝阳区", "建国路1号", "100020"),
                new Address("上海市", "上海市", "浦东新区", "世纪大道1号", "200120"),
                new Address(null, "成都市", "锦江区", "红星路三段1号", null),
                new Address("四川省", null, null, "某某路1号", "610000")
        };

        System.out.println("地址拼接效果：");
        for (Address addr : addresses) {
            DeliveryInfoDTO dto = DeliveryInfoMapper.INSTANCE.toDTO(user, addr);
            System.out.println("  [" + addr.getCity() + "] -> 完整地址: " + dto.getFullAddress());
        }
        System.out.println("✅ null 字段自动跳过，不会出现 'null' 字符串");
        System.out.println();
    }

    /**
     * Demo 3: 实战场景 - 批量用户列表处理（模拟分页查询）
     *
     * <p>场景：后台管理系统用户列表接口，从数据库查询出 User 列表，
     * 映射为 UserDTO 列表返回前端（过滤 password 等敏感字段）。
     */
    private static void demo3BatchUserListProcessing() {
        System.out.println("--- Demo 3: 批量用户列表处理（分页查询场景）---");

        // 模拟从数据库查询到的用户列表
        List<User> dbUsers = new ArrayList<>();
        String[] names = {"张伟", "王芳", "李娜", "刘洋", "陈军"};
        for (int i = 0; i < names.length; i++) {
            User u = new User(
                    (long)(i + 1),
                    "user" + String.format("%03d", i + 1),
                    "hashed_password_" + i,   // 数据库中的加密密码，不应暴露
                    names[i],
                    "user" + i + "@company.com",
                    "138" + String.format("%08d", i),
                    25 + i,
                    new BigDecimal(1000 * (i + 1)),
                    new Date(),
                    new Date(),
                    i % 2 == 0 ? 1 : 0   // 奇偶交替状态
            );
            dbUsers.add(u);
        }

        // 一行代码完成批量映射
        long startTime = System.currentTimeMillis();
        List<UserDTO> dtoList = UserMapper.INSTANCE.toDTOList(dbUsers);
        long elapsed = System.currentTimeMillis() - startTime;

        System.out.println("共映射 " + dtoList.size() + " 条记录，耗时 " + elapsed + " ms");
        System.out.println("用户列表（已过滤敏感字段）：");
        for (UserDTO dto : dtoList) {
            System.out.printf("  [%d] %-10s %-8s  状态:%-4s  余额:%.2f%n",
                    dto.getId(), dto.getUsername(), dto.getName(),
                    dto.getStatusDesc(), dto.getBalance());
        }
        System.out.println("✅ 批量映射，password 字段自动过滤，无需手写 for 循环");
        System.out.println();
    }

    /**
     * Demo 4: 性能对比说明
     *
     * <p>MapStruct vs BeanUtils.copyProperties vs 手写 setter
     */
    private static void demo4PerformanceComparison() {
        System.out.println("--- Demo 4: 性能对比 ---");

        System.out.println("MapStruct vs 常见映射方案性能对比：");
        System.out.println();
        System.out.printf("%-25s %-15s %-15s %-20s%n",
                "方案", "性能", "类型安全", "备注");
        System.out.println("─".repeat(80));

        String[][] comparison = {
                {"手写 setter（推荐）", "★★★★★", "✅ 编译期", "100% 安全，但代码冗余"},
                {"MapStruct（推荐）", "★★★★★", "✅ 编译期", "编译期生成setter代码，零反射"},
                {"ModelMapper", "★★☆☆☆", "❌ 运行期", "基于反射，慢10-20倍，易出错"},
                {"BeanUtils.copyProperties", "★★★☆☆", "❌ 运行期", "基于反射，字段名必须完全一致"},
                {"Dozer", "★★☆☆☆", "❌ 运行期", "XML配置繁琐，反射，已不推荐"},
                {"Orika", "★★★☆☆", "❌ 运行期", "字节码生成，比反射快，复杂"}
        };

        for (String[] row : comparison) {
            System.out.printf("%-25s %-15s %-15s %-20s%n",
                    row[0], row[1], row[2], row[3]);
        }

        System.out.println();
        System.out.println("MapStruct 的核心优势：");
        System.out.println("  1. 编译期生成纯 Java 代码（等同于手写 setter），零反射开销");
        System.out.println("  2. 编译期类型检查，字段名错误编译报错，不是运行时 NPE");
        System.out.println("  3. 生成的代码可读、可调试（在 target/generated-sources/ 下）");
        System.out.println("  4. 支持复杂映射：嵌套、多源、集合、自定义转换");
        System.out.println("✅ 企业级项目首选映射框架");
        System.out.println();
    }

    /**
     * Demo 5: Spring Boot 集成模式指南
     *
     * <p>展示在 Spring Boot 中使用 MapStruct 的最佳实践。
     * 注意：以下代码仅作说明，不在此 Demo 中执行。
     */
    private static void demo5SpringBootIntegrationGuide() {
        System.out.println("--- Demo 5: Spring Boot 集成模式指南 ---");

        System.out.println("1. pom.xml 依赖（Spring Boot 场景）：");
        System.out.println("   <dependency>");
        System.out.println("       <groupId>org.mapstruct</groupId>");
        System.out.println("       <artifactId>mapstruct</artifactId>");
        System.out.println("       <version>1.5.5.Final</version>");
        System.out.println("   </dependency>");
        System.out.println("   <!-- spring-boot-starter 不含 mapstruct，需手动添加 -->");
        System.out.println();

        System.out.println("2. Mapper 接口声明（Spring 模式）：");
        System.out.println("   @Mapper(componentModel = \"spring\")  // 生成 @Component");
        System.out.println("   public interface UserMapper {");
        System.out.println("       // MapStruct 生成 UserMapperImpl extends UserMapper");
        System.out.println("       // Spring 自动注册为 Bean");
        System.out.println("   }");
        System.out.println();

        System.out.println("3. Service 中注入使用：");
        System.out.println("   @Service");
        System.out.println("   public class UserService {");
        System.out.println("       @Autowired");
        System.out.println("       private UserMapper userMapper;  // 直接注入，无需 INSTANCE");
        System.out.println();
        System.out.println("       public UserDTO getUser(Long id) {");
        System.out.println("           User user = userRepository.findById(id).orElseThrow();");
        System.out.println("           return userMapper.toDTO(user);  // 编译期生成的映射");
        System.out.println("       }");
        System.out.println("   }");
        System.out.println();

        System.out.println("4. 查看生成的映射代码（调试利器）：");
        System.out.println("   target/generated-sources/annotations/");
        System.out.println("   └── com/example/mapstruct/mapper/UserMapperImpl.java");
        System.out.println("   （编译后可直接查看 MapStruct 生成的 setter 代码）");
        System.out.println();

        System.out.println("5. 常见注意事项：");
        System.out.println("   ⚠️  Lombok + MapStruct 时，annotationProcessorPaths 中 Lombok 必须在前");
        System.out.println("   ⚠️  lombok.version 和 mapstruct.version 要匹配稳定版");
        System.out.println("   ⚠️  不要混用 @Mapper(componentModel=\"spring\") 和 Mappers.getMapper()");
        System.out.println("   ⚠️  多模块 Maven 项目中，每个模块的 processor 配置需单独声明");
        System.out.println("   ✅  IntelliJ IDEA 需安装 \"MapStruct Support\" 插件获得代码补全");

        System.out.println("\n========== 演示完成 ==========");
    }
}
