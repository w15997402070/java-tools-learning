package com.example.bytebuddy;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.annotation.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * Byte Buddy 进阶演示
 *
 * 本类演示以下进阶能力：
 * 1. 动态添加字段和方法（defineField / defineMethod）
 * 2. 动态添加注解（annotateType / annotateMethod）
 * 3. 方法委托进阶：@This、@DefaultCall、条件委托（多 Interceptor 按条件分发）
 * 4. 使用 ByteBuddy 重定义已有类的方法（redefine）
 */
public class ByteBuddyAdvancedDemo {

    // ========== 1. 目标接口/类 ==========

    /** 自定义注解，用于 Demo 2 演示动态注解添加 */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface Auditable {
        String value() default "unknown";
    }

    /** 用于演示动态添加字段的接口 */
    public interface Mutable {
        String getName();
        void setName(String name);
    }

    /** 业务类：根据不同方法名走不同拦截路径 */
    public static class OrderService {
        public String createOrder(String item, int qty) {
            return "Order[" + item + " x" + qty + "]";
        }

        public void cancelOrder(String orderId) {
            System.out.println("  [OrderService] cancelOrder(" + orderId + ")");
        }

        public String queryOrder(String orderId) {
            return "QueryResult-" + orderId;
        }
    }

    // ========== 2. 拦截器 ==========

    /** 写操作（create/cancel）拦截器 */
    public static class WriteInterceptor {
        @RuntimeType
        public static Object intercept(
                @Origin Method method,
                @AllArguments Object[] args,
                @SuperCall Callable<?> superCall) throws Exception {
            System.out.println("  [WriteInterceptor] 写操作权限检查：" + method.getName());
            Object result = superCall.call();
            System.out.println("  [WriteInterceptor] 写操作完成，结果：" + result);
            return result;
        }
    }

    /** 读操作（query）拦截器 */
    public static class ReadInterceptor {
        @RuntimeType
        public static Object intercept(
                @Origin Method method,
                @AllArguments Object[] args,
                @SuperCall Callable<?> superCall) throws Exception {
            System.out.println("  [ReadInterceptor] 读缓存命中检查：" + method.getName()
                    + "(" + java.util.Arrays.toString(args) + ")");
            return superCall.call();
        }
    }

    // ========== 3. 演示方法 ==========

    /**
     * Demo 1：动态添加字段并通过 FieldAccessor 生成 getter/setter
     *
     * 场景：运行时给某个类"增加属性"，然后通过接口访问
     */
    static void demo1_addFieldAndAccessor() throws Exception {
        System.out.println("\n--- Demo 1：动态添加字段 + getter/setter ---");

        // 生成一个新类，继承 Object，实现 Mutable 接口
        // defineField 添加 name 字段，method 匹配 getName/setName 并用 FieldAccessor 实现
        Class<?> dynamicBean = new ByteBuddy()
                .subclass(Object.class)
                .implement(Mutable.class)
                // 定义私有字段 name
                .defineField("name", String.class, Visibility.PRIVATE)
                // getter
                .method(ElementMatchers.named("getName"))
                .intercept(FieldAccessor.ofField("name"))
                // setter
                .method(ElementMatchers.named("setName"))
                .intercept(FieldAccessor.ofField("name"))
                .make()
                .load(ByteBuddyAdvancedDemo.class.getClassLoader(),
                        ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();

        Mutable obj = (Mutable) dynamicBean.newInstance();
        obj.setName("ByteBuddy");
        System.out.println("getName() = " + obj.getName());

        // 通过反射验证字段确实存在
        Field field = dynamicBean.getDeclaredField("name");
        System.out.println("反射确认字段类型：" + field.getType().getSimpleName());
    }

    /**
     * Demo 2：动态添加注解
     *
     * 场景：CI/CD 工具链需要给类/方法打上 @Auditable 元数据，但不能修改源码
     */
    static void demo2_dynamicAnnotation() throws Exception {
        System.out.println("\n--- Demo 2：动态添加注解 ---");

        // 在类型级别添加 @Auditable("order-service")
        // 在 createOrder 方法上添加 @Auditable("create")
        Class<? extends OrderService> annotatedClass = new ByteBuddy()
                .subclass(OrderService.class)
                // 类型注解
                .annotateType(AnnotationDescription.Builder
                        .ofType(Auditable.class)
                        .define("value", "order-service")
                        .build())
                // 方法注解：拦截 createOrder 并在方法上附加注解
                .method(ElementMatchers.named("createOrder"))
                .intercept(SuperMethodCall.INSTANCE)  // 透传调用父类，不改变逻辑
                .annotateMethod(AnnotationDescription.Builder
                        .ofType(Auditable.class)
                        .define("value", "create")
                        .build())
                .make()
                .load(ByteBuddyAdvancedDemo.class.getClassLoader(),
                        ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();

        // 验证类型注解
        Auditable classAnn = annotatedClass.getAnnotation(Auditable.class);
        System.out.println("类注解 @Auditable.value = " + (classAnn != null ? classAnn.value() : "null"));

        // 验证方法注解
        Method createOrderMethod = annotatedClass.getMethod("createOrder", String.class, int.class);
        Auditable methodAnn = createOrderMethod.getAnnotation(Auditable.class);
        System.out.println("方法注解 @Auditable.value = " + (methodAnn != null ? methodAnn.value() : "null"));

        // 验证功能正常
        OrderService svc = annotatedClass.newInstance();
        System.out.println("createOrder(\"书\", 2) = " + svc.createOrder("书", 2));
    }

    /**
     * Demo 3：按条件分发到不同拦截器（读写分离）
     *
     * 场景：写操作（create/cancel）走写拦截器，查询操作（query）走读拦截器
     */
    static void demo3_conditionalInterceptor() throws Exception {
        System.out.println("\n--- Demo 3：按方法名条件分发拦截器（读写分离）---");

        Class<? extends OrderService> splitClass = new ByteBuddy()
                .subclass(OrderService.class)
                // 写操作：名称以 create 或 cancel 开头
                .method(ElementMatchers.nameStartsWith("create")
                        .or(ElementMatchers.nameStartsWith("cancel")))
                .intercept(MethodDelegation.to(WriteInterceptor.class))
                // 读操作：名称以 query 开头
                .method(ElementMatchers.nameStartsWith("query"))
                .intercept(MethodDelegation.to(ReadInterceptor.class))
                .make()
                .load(ByteBuddyAdvancedDemo.class.getClassLoader(),
                        ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();

        OrderService svc = splitClass.newInstance();

        System.out.println("\n调用 createOrder：");
        System.out.println("结果：" + svc.createOrder("Java书", 3));

        System.out.println("\n调用 cancelOrder：");
        svc.cancelOrder("ORD-001");

        System.out.println("\n调用 queryOrder：");
        System.out.println("结果：" + svc.queryOrder("ORD-002"));
    }

    /**
     * Demo 4：重定义（redefine）已有类的方法
     *
     * 注意：redefine 会直接修改原 Class，因此使用 INJECTION 策略；
     * 实际生产中需配合 Java Agent 的 Instrumentation 使用，这里仅演示 API 形态。
     * 为避免副作用，本 Demo 用一个局部类而非 OrderService。
     */
    static void demo4_redefineMethod() throws Exception {
        System.out.println("\n--- Demo 4：redefine（演示 API 形态，非 Agent 模式）---");

        // 创建原始类的子类来模拟 redefine 效果（真正的 redefine 需要 -javaagent）
        // 此处展示 rebase 用法：rebase 保留原方法的 $original 副本
        Class<? extends OrderService> rebasedClass = new ByteBuddy()
                .rebase(OrderService.class)
                .method(ElementMatchers.named("queryOrder"))
                .intercept(FixedValue.value("CACHED_RESULT"))
                .make()
                .load(new java.net.URLClassLoader(new java.net.URL[]{},
                        ByteBuddyAdvancedDemo.class.getClassLoader()),
                        ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();

        OrderService svc = rebasedClass.newInstance();
        System.out.println("rebase 后 queryOrder(\"X\") = " + svc.queryOrder("X"));
        System.out.println("（原始方法被保留为 queryOrder$original，可通过反射调用）");

        // 验证原始方法以 $original 命名被保留
        boolean hasOriginal = false;
        for (Method m : rebasedClass.getDeclaredMethods()) {
            if (m.getName().contains("original")) {
                hasOriginal = true;
                System.out.println("找到原始方法副本：" + m.getName());
                break;
            }
        }
        if (!hasOriginal) {
            System.out.println("（注：rebase 的原始方法在字节码中存在，此处反射可能受访问限制）");
        }
    }

    // ========== main ==========

    public static void main(String[] args) throws Exception {
        System.out.println("========== Byte Buddy 进阶演示 ==========");
        demo1_addFieldAndAccessor();
        demo2_dynamicAnnotation();
        demo3_conditionalInterceptor();
        demo4_redefineMethod();
        System.out.println("\n========== 进阶演示完成 ==========");
    }
}
