package com.example.bytebuddy;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * Byte Buddy 基础演示
 *
 * 本类演示 Byte Buddy 的三个核心基础能力：
 * 1. 动态创建新类（不依赖任何已有类）
 * 2. 子类化并覆写方法（返回固定值/委托给自定义拦截器）
 * 3. 方法拦截 —— 在目标方法前后注入逻辑（最典型的 AOP 场景）
 *
 * Byte Buddy 官方 GitHub：https://github.com/raphael/byte-buddy
 * 官方 GitHub（正确）：https://github.com/raphael-junit/byte-buddy
 * 真实地址：https://github.com/raphael/byte-buddy（实际：bytebuddy.net / GitHub: byte-buddy）
 */
public class ByteBuddyBasicDemo {

    // ========== 1. 要操作的目标类 ==========

    /**
     * 普通 Greeter 接口，用于演示动态实现
     */
    public interface Greeter {
        String greet(String name);
    }

    /**
     * 普通业务类，用于演示子类代理与方法拦截
     */
    public static class UserService {
        public String findUser(String userId) {
            System.out.println("  [UserService] 真实方法执行：findUser(" + userId + ")");
            return "User-" + userId;
        }

        public void deleteUser(String userId) {
            System.out.println("  [UserService] 真实方法执行：deleteUser(" + userId + ")");
        }
    }

    // ========== 2. 拦截器 / 委托类 ==========

    /**
     * 固定返回值拦截器（用于 Demo 1）
     *
     * MethodDelegation 要求目标方法有 @RuntimeType，并且参数注解指明如何绑定：
     * - @AllArguments：原始方法的所有参数数组
     * - @Origin Method：原始方法对象
     * - @SuperCall Callable<?>：调用父类/原始方法的 Callable
     */
    public static class HelloInterceptor {
        @RuntimeType
        public static String greet(@AllArguments Object[] args) {
            // 直接构造返回值，不调用原始逻辑
            return "Hello, " + args[0] + "! (动态生成的实现)";
        }
    }

    /**
     * AOP 风格拦截器 —— 记录方法调用日志
     */
    public static class LoggingInterceptor {
        @RuntimeType
        public static Object intercept(
                @Origin Method method,
                @AllArguments Object[] args,
                @SuperCall Callable<?> superCall) throws Exception {
            long start = System.currentTimeMillis();
            System.out.println("  [AOP] 方法调用开始：" + method.getName());
            System.out.println("  [AOP] 参数：" + java.util.Arrays.toString(args));
            try {
                // 调用原始方法（super）
                Object result = superCall.call();
                long elapsed = System.currentTimeMillis() - start;
                System.out.println("  [AOP] 方法调用结束：" + method.getName()
                        + "，耗时 " + elapsed + "ms，返回值：" + result);
                return result;
            } catch (Exception e) {
                System.out.println("  [AOP] 方法抛出异常：" + e.getMessage());
                throw e;
            }
        }
    }

    // ========== 3. 演示方法 ==========

    /**
     * Demo 1：动态生成接口实现类
     *
     * 场景：不写任何实现类，由 Byte Buddy 在运行时生成一个 Greeter 接口实现
     */
    static void demo1_dynamicInterfaceImpl() throws Exception {
        System.out.println("\n--- Demo 1：动态生成接口实现类 ---");

        // subclass(Object.class).implement(Greeter.class) 或直接 subclass(Greeter.class)
        // 这里选择 subclass(Object.class) 并 implement(Greeter.class) 更通用
        DynamicType.Unloaded<Object> unloaded = new ByteBuddy()
                .subclass(Object.class)
                .implement(Greeter.class)
                // 拦截所有名为 greet 的方法，委托给 HelloInterceptor
                .method(ElementMatchers.named("greet"))
                .intercept(MethodDelegation.to(HelloInterceptor.class))
                .make();

        // 使用当前线程的 ClassLoader 加载生成的类
        Class<?> dynamicClass = unloaded
                .load(ByteBuddyBasicDemo.class.getClassLoader(),
                        ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();

        System.out.println("动态生成的类名：" + dynamicClass.getName());

        // 实例化并调用
        Greeter greeter = (Greeter) dynamicClass.newInstance();
        String result = greeter.greet("Alice");
        System.out.println("greet(\"Alice\") = " + result);
    }

    /**
     * Demo 2：子类化并用 FixedValue 覆写方法
     *
     * 场景：快速 Mock —— 让某个方法始终返回固定值，无需写 Mock 框架代码
     */
    static void demo2_fixedValueOverride() throws Exception {
        System.out.println("\n--- Demo 2：FixedValue 覆写方法（快速 Mock）---");

        // 继承 UserService，把 findUser 方法的返回值固定为 "MOCKED_USER"
        Class<? extends UserService> mockedClass = new ByteBuddy()
                .subclass(UserService.class)
                .method(ElementMatchers.named("findUser"))
                .intercept(FixedValue.value("MOCKED_USER"))
                .make()
                .load(ByteBuddyBasicDemo.class.getClassLoader(),
                        ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();

        UserService mockedService = mockedClass.newInstance();
        System.out.println("mockedService.findUser(\"42\") = " + mockedService.findUser("42"));
        // deleteUser 没有被 intercept，仍调用真实逻辑
        mockedService.deleteUser("42");
    }

    /**
     * Demo 3：方法拦截（AOP 日志）
     *
     * 场景：为已有类的方法添加前后日志，不修改原始类源码
     */
    static void demo3_methodInterceptionAop() throws Exception {
        System.out.println("\n--- Demo 3：方法拦截（AOP 日志增强）---");

        // 继承 UserService，对所有 public 方法注入 LoggingInterceptor
        Class<? extends UserService> enhancedClass = new ByteBuddy()
                .subclass(UserService.class)
                .method(ElementMatchers.isPublic())
                .intercept(MethodDelegation.to(LoggingInterceptor.class))
                .make()
                .load(ByteBuddyBasicDemo.class.getClassLoader(),
                        ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();

        UserService enhanced = enhancedClass.newInstance();

        System.out.println("\n调用 findUser：");
        String user = enhanced.findUser("U001");
        System.out.println("最终返回：" + user);

        System.out.println("\n调用 deleteUser：");
        enhanced.deleteUser("U001");
    }

    // ========== main ==========

    public static void main(String[] args) throws Exception {
        System.out.println("========== Byte Buddy 基础演示 ==========");
        demo1_dynamicInterfaceImpl();
        demo2_fixedValueOverride();
        demo3_methodInterceptionAop();
        System.out.println("\n========== 演示完成 ==========");
    }
}
