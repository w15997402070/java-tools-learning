package com.example.orika;

import com.example.orika.dto.UserDTO;
import com.example.orika.entity.User;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.impl.DefaultMapperFactory;

import java.util.Date;

/**
 * Orika 基础演示：默认映射、字段别名、类型转换
 *
 * 演示内容：
 * 1. 同名字段自动映射
 * 2. 不同名字段通过 field() 配置映射
 * 3. 自定义类型转换器（Boolean -> String）
 * 4. 双向映射（entity <-> dto）
 */
public class OrikaBasicDemo {

    public static void main(String[] args) {
        System.out.println("=== Orika 基础演示 ===\n");

        // 1. 创建 MapperFactory（线程安全，通常作为单例）
        MapperFactory mapperFactory = new DefaultMapperFactory.Builder().build();

        // 2. 注册映射规则
        mapperFactory.classMap(User.class, UserDTO.class)
                // 字段名不同时显式映射
                .field("id", "userId")
                .field("username", "userName")
                .field("age", "userAge")
                .field("createTime", "registerTime")
                // 自定义转换：Boolean active -> String status
                .fieldMap("active", "status").converter("booleanToString").add()
                // 排除不需要映射的字段（password 不传到DTO）
                .exclude("password")
                .byDefault() // 其余同名字段自动映射
                .register();

        // 注册自定义转换器
        mapperFactory.getConverterFactory().registerConverter("booleanToString",
                new ma.glasnost.orika.CustomConverter<Boolean, String>() {
                    @Override
                    public String convert(Boolean source, ma.glasnost.orika.metadata.Type<? extends String> destinationType,
                                          ma.glasnost.orika.MappingContext mappingContext) {
                        return Boolean.TRUE.equals(source) ? "已激活" : "未激活";
                    }
                });

        // 3. 获取 MapperFacade（线程安全，用于执行映射）
        MapperFacade mapper = mapperFactory.getMapperFacade();

        // 4. 准备源数据
        User user = new User(
                1001L,
                "zhangsan",
                "secret123",
                "zhangsan@example.com",
                28,
                new Date(),
                true
        );

        // 5. Entity -> DTO 映射
        System.out.println("--- Entity -> DTO ---");
        UserDTO userDTO = mapper.map(user, UserDTO.class);
        System.out.println("源 User: id=" + user.getId() + ", username=" + user.getUsername()
                + ", age=" + user.getAge() + ", active=" + user.getActive());
        System.out.println("目标 UserDTO: userId=" + userDTO.getUserId() + ", userName=" + userDTO.getUserName()
                + ", userAge=" + userDTO.getUserAge() + ", status=" + userDTO.getStatus());
        System.out.println("email=" + userDTO.getEmail() + ", registerTime=" + userDTO.getRegisterTime());
        System.out.println("password 已排除，DTO中不存在: " + (userDTO.getStatus() != null));

        // 6. DTO -> Entity 反向映射
        System.out.println("\n--- DTO -> Entity（反向映射）---");
        UserDTO newDTO = new UserDTO();
        newDTO.setUserId(2002L);
        newDTO.setUserName("lisi");
        newDTO.setEmail("lisi@example.com");
        newDTO.setUserAge(35);
        newDTO.setRegisterTime(new Date());
        newDTO.setStatus("已激活");

        User reverseUser = mapper.map(newDTO, User.class);
        System.out.println("反向映射结果: id=" + reverseUser.getId()
                + ", username=" + reverseUser.getUsername()
                + ", age=" + reverseUser.getAge()
                + ", active=" + reverseUser.getActive());
        // 注意：反向映射时 String -> Boolean 需要另一个转换器，此处为演示保留默认行为

        System.out.println("\n=== 基础演示完成 ===");
    }
}
