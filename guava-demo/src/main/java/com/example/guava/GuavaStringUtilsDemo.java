package com.example.guava;

import com.google.common.base.*;
import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import com.google.common.net.UrlEscapers;

import java.util.Arrays;
import java.util.Optional;

/**
 * GuavaStringUtilsDemo - Guava 字符串工具演示
 *
 * 演示以下功能：
 * 1. Strings - 字符串基本操作
 * 2. CharMatcher - 字符匹配器
 * 3. Escapers - 转义器
 * 4. Preconditions - 前置条件检查
 * 5. Optional - 更安全的空值处理
 * 6. Suppliers - 延迟求值
 */
public class GuavaStringUtilsDemo {
    
    public static void main(String[] args) {
        demoStrings();
        demoCharMatcher();
        demoEscapers();
        demoPreconditions();
        demoOptional();
        demoSuppliers();
    }
    
    /**
     * 演示 Strings 工具类
     * 比 String 原生方法更易用
     */
    private static void demoStrings() {
        System.out.println("========== 1. Strings 工具类 ==========");
        
        // 判空/空白
        System.out.println("nullOrEmpty: " + Strings.isNullOrEmpty(null));
        System.out.println("nullOrEmpty: " + Strings.isNullOrEmpty(""));
        System.out.println("nullOrEmpty: " + Strings.isNullOrEmpty(" "));
        System.out.println("为空只包含空白: " + Strings.nullToEmpty("  ").trim().isEmpty());
        
        // 补全
        System.out.println("前补零: " + Strings.padStart("123", 5, '0'));
        System.out.println("后补空格: " + Strings.padEnd("Java", 10, ' ').length());
        
        // 获取共同前缀/后缀
        System.out.println("共同前缀: " + Strings.commonPrefix("abccde", "abcfg"));
        System.out.println("共同后缀: " + Strings.commonSuffix("hello.txt", "world.txt"));
        
        // 重复字符串
        System.out.println("重复3次: " + Strings.repeat("abc", 3));
        
        // 长度限制
        String longStr = "这是一段很长的字符串，需要被截断";
        System.out.println("截断为12字符: " + Strings.lenientFormat("%.12s...", longStr));
        
        System.out.println();
    }
    
    /**
     * 演示 CharMatcher - 强大的字符匹配器
     * 比正则更高效的字符匹配和操作
     */
    private static void demoCharMatcher() {
        System.out.println("========== 2. CharMatcher（字符匹配器）==========");
        
        String text = "Hello, Java 2024! @#$%";
        
        // 内置匹配器
        System.out.println("原始文本: " + text);
        System.out.println("只保留数字: " + CharMatcher.digit().retainFrom(text));
        System.out.println("只保留字母: " + CharMatcher.javaLetter().retainFrom(text));
        System.out.println("只保留字母和空格: " + 
                CharMatcher.javaLetterOrDigit().or(CharMatcher.whitespace()).retainFrom(text));
        System.out.println("去除控制字符: " + CharMatcher.invisible().removeFrom("a\t\nb"));
        
        // 匹配统计
        System.out.println("字母数量: " + CharMatcher.javaLetter().countIn(text));
        System.out.println("数字数量: " + CharMatcher.digit().countIn(text));
        
        // 移除/替换
        System.out.println("移除数字: " + CharMatcher.digit().removeFrom(text));
        System.out.println("替换非字母为*: " + CharMatcher.javaLetter().negate().replaceFrom(text, '*'));
        
        // 自定义匹配
        CharMatcher custom = CharMatcher.anyOf("aeiou")
                .or(CharMatcher.digit())
                .or(CharMatcher.anyOf("@#$"));
        System.out.println("自定义匹配（元音/数字/特殊字符）: " + custom.retainFrom(text));
        
        // 修剪
        String whiteSpaced = "   Hello World   \n";
        System.out.println("修剪两边: " + CharMatcher.breakingWhitespace().trimFrom(whiteSpaced));
        
        System.out.println();
    }
    
    /**
     * 演示 Escapers - HTML/URL 转义
     */
    private static void demoEscapers() {
        System.out.println("========== 3. Escapers（转义器）==========");
        
        // HTML 转义
        Escaper htmlEscaper = Escapers.builder()
                .addEscape('"', "&quot;")
                .addEscape('\'', "&#39;")
                .addEscape('&', "&amp;")
                .addEscape('<', "&lt;")
                .addEscape('>', "&gt;")
                .build();
        
        String html = "<script>alert('XSS')</script>";
        System.out.println("HTML 转义前: " + html);
        System.out.println("HTML 转义后: " + htmlEscaper.escape(html));
        
        // URL 转义
        String urlParam = "query=Java & Spring";
        System.out.println("URL 转义前: " + urlParam);
        System.out.println("URL 转义后: " + UrlEscapers.urlPathSegmentEscaper().escape(urlParam));
        System.out.println("URL 查询参数转义: " + UrlEscapers.urlFormParameterEscaper().escape(urlParam));
        
        System.out.println();
    }
    
    /**
     * 演示 Preconditions - 参数验证
     * 比 if-throw 更优雅
     */
    private static void demoPreconditions() {
        System.out.println("========== 4. Preconditions（前置条件检查）==========");
        
        String name = "Alice";
        int age = 25;
        Object obj = null;
        
        // 基本检查
        try {
            Preconditions.checkNotNull(name, "名字不能为空");
            System.out.println("名字检查通过: " + name);
            
            // 范围检查
            Preconditions.checkArgument(age >= 0 && age <= 150, 
                    "年龄 %s 应在 0-150 之间", age);
            System.out.println("年龄检查通过: " + age);
            
            // 状态检查
            Preconditions.checkState(name.length() > 0, "名字长度必须大于0");
            
            // 元素索引检查
            int index = 2;
            int[] arr = {1, 2, 3, 4, 5};
            Preconditions.checkElementIndex(index, arr.length, "索引超出数组范围");
            System.out.println("索引检查通过: " + index + " -> " + arr[index]);
            
            // 位置索引检查
            Preconditions.checkPositionIndex(5, arr.length, "位置索引超出数组长度");
            
        } catch (IllegalArgumentException | NullPointerException | IndexOutOfBoundsException e) {
            System.out.println("检查失败: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
        
        try {
            // 故意触发 null 检查
            Preconditions.checkNotNull(obj, "对象不能为空");
        } catch (NullPointerException e) {
            System.out.println("Null检查异常: " + e.getMessage());
        }
        
        // 格式化消息
        try {
            int invalidAge = -5;
            Preconditions.checkArgument(invalidAge > 0, "年龄必须为正数，当前: %s", invalidAge);
        } catch (IllegalArgumentException e) {
            System.out.println("格式化的错误消息: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * 演示 Optional 工具
     * 比 Java 8 Optional 更早出现，但功能类似
     */
    private static void demoOptional() {
        System.out.println("========== 5. Optional（空值安全处理）==========");
        
        // 创建 Optional
        String value = "Hello";
        String nullValue = null;
        
        Optional<String> present = Optional.of(value);
        Optional<String> absent = Optional.fromNullable(nullValue);
        Optional<String> absent2 = Optional.absent();
        
        System.out.println("present 存在: " + present.isPresent());
        System.out.println("absent 存在: " + absent.isPresent());
        
        // 安全获取值
        System.out.println("present 值: " + present.or("默认值"));
        System.out.println("absent 值: " + absent.or("默认值"));
        
        // 条件获取
        String result = present.or(new Supplier<String>() {
            @Override
            public String get() {
                return "来自Supplier的默认值";
            }
        });
        System.out.println("Supplier 默认值: " + result);
        
        // 转换（类似 map）
        Optional<Integer> lengthOpt = present.transform(Str -> Str.length());
        System.out.println("转换为长度: " + lengthOpt.or(0));
        
        // 过滤
        Optional<String> filtered = present.filter(s -> s.length() > 10);
        System.out.println("过滤后存在: " + filtered.isPresent());
        
        // 链式调用
        String finalResult = Optional.fromNullable("test")
                .transform(String::toUpperCase)
                .filter(s -> s.length() > 3)
                .or("fallback");
        System.out.println("链式调用结果: " + finalResult);
        
        // 与 Java 8 Optional 的比较
        System.out.println("Guava Optional 已逐渐被 Java 8 Optional 取代，但 API 类似");
        
        System.out.println();
    }
    
    /**
     * 演示 Suppliers - 延迟求值/缓存
     */
    private static void demoSuppliers() {
        System.out.println("========== 6. Suppliers（供应商/延迟求值）==========");
        
        // 基本 supplier
        Supplier<String> simpleSupplier = new Supplier<String>() {
            private int count = 0;
            
            @Override
            public String get() {
                return "值 " + (++count);
            }
        };
        
        System.out.println("simpleSupplier 1: " + simpleSupplier.get());
        System.out.println("simpleSupplier 2: " + simpleSupplier.get());
        System.out.println("simpleSupplier 3: " + simpleSupplier.get());
        
        // 使用 lambda（Java 8+）
        Supplier<Long> timestampSupplier = () -> System.currentTimeMillis();
        System.out.println("当前时间戳1: " + timestampSupplier.get());
        try { Thread.sleep(10); } catch (InterruptedException e) {}
        System.out.println("当前时间戳2: " + timestampSupplier.get());
        
        // 记忆化 supplier（缓存结果）
        Supplier<Double> expensiveSupplier = new Supplier<Double>() {
            @Override
            public Double get() {
                // 模拟昂贵计算
                System.out.println("[MemoizedSupplier] 执行昂贵计算...");
                try { Thread.sleep(50); } catch (InterruptedException e) {}
                return Math.random() * 100;
            }
        };
        
        Supplier<Double> memoized = Suppliers.memoize(expensiveSupplier);
        
        System.out.println("第一次获取（会计算）:");
        System.out.println("结果: " + memoized.get());
        
        System.out.println("第二次获取（使用缓存）:");
        System.out.println("结果: " + memoized.get());
        
        System.out.println("第三次获取（使用缓存）:");
        System.out.println("结果: " + memoized.get());
        
        // 带失效时间的缓存
        Supplier<String> expiringSupplier = new Supplier<String>() {
            @Override
            public String get() {
                System.out.println("[ExpiringSupplier] 重新计算...");
                return "数据-" + System.currentTimeMillis();
            }
        };
        
        // 模拟：每500ms过期
        Supplier<String> cached = Suppliers.memoizeWithExpiration(expiringSupplier, 500, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        System.out.println("带过期时间的缓存:");
        System.out.println("第一次: " + cached.get());
        System.out.println("立即再次（缓存）: " + cached.get());
        
        try { Thread.sleep(600); } catch (InterruptedException e) {}
        System.out.println("600ms后（过期重新计算）: " + cached.get());
        
        System.out.println();
    }
}