package com.example.commonscollections4;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.collections4.bag.HashBag;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.collections4.list.SetUniqueList;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Commons Collections4 实战 Demo — 模拟后端开发常见业务场景
 *
 * 覆盖内容：
 * 1. 用户角色权限一对多建模（MultiValuedMap）
 * 2. 商品分类销量统计（Bag）
 * 3. 去重登录历史记录（SetUniqueList）
 * 4. HTTP Header 大小写不敏感读取（CaseInsensitiveMap）
 * 5. 临时缓存 / 限流计数器（PassiveExpiringMap）
 * 6. 双向映射做短码↔ID（BidiMap）
 * 7. Map 合并与默认值处理（MapUtils）
 * 8. 集合运算做权限判定（SetUtils / CollectionUtils）
 * 9. 顺序 Map 做流程步骤索引（LinkedMap）
 */
public class CommonsCollections4PracticalDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("===== Commons Collections4 实战Demo =====\n");

        demoUserRolePermissions();
        demoCategorySalesCount();
        demoDeduplicateLoginHistory();
        demoHttpHeadersInsensitive();
        demoExpiringCache();
        demoShortCodeMapping();
        demoMapMergeWithDefaults();
        demoPermissionCheck();
        demoWorkflowSteps();
    }

    /**
     * 1. 用户角色权限一对多建模
     *
     * 场景：一个用户拥有多个角色，每个角色拥有多个权限，需要快速查询某用户的所有权限
     */
    static void demoUserRolePermissions() {
        System.out.println("--- 1. 用户角色权限一对多建模 ---");

        // 角色 -> 权限列表（MultiValuedMap 一个 key 对应多个 value）
        MultiValuedMap<String, String> rolePermissions = new ArrayListValuedHashMap<>();
        rolePermissions.put("ADMIN", "user:read");
        rolePermissions.put("ADMIN", "user:write");
        rolePermissions.put("ADMIN", "user:delete");
        rolePermissions.put("EDITOR", "article:read");
        rolePermissions.put("EDITOR", "article:write");
        rolePermissions.put("VIEWER", "article:read");

        // 用户 -> 角色
        Map<String, Set<String>> userRoles = new HashMap<>();
        userRoles.put("alice", new HashSet<>(Arrays.asList("ADMIN", "EDITOR")));
        userRoles.put("bob", new HashSet<>(Arrays.asList("VIEWER")));

        // 聚合用户所有权限
        for (String user : userRoles.keySet()) {
            Set<String> roles = userRoles.get(user);
            Set<String> permissions = new HashSet<>();
            for (String role : roles) {
                permissions.addAll(rolePermissions.get(role));
            }
            System.out.println("用户 " + user + " 拥有权限: " + permissions);
        }
        System.out.println();
    }

    /**
     * 2. 商品分类销量统计（Bag）
     *
     * 场景：统计一段时间内各分类的销售件数，Bag 天然支持计数
     */
    static void demoCategorySalesCount() {
        System.out.println("--- 2. 商品分类销量统计 ---");

        Bag<String> salesBag = new HashBag<>();
        // 模拟订单中的商品分类
        String[] categories = {"手机", "电脑", "手机", "配件", "电脑", "手机", "配件", "手机"};
        salesBag.addAll(Arrays.asList(categories));

        System.out.println("总订单行数: " + salesBag.size());
        System.out.println("分类种类: " + salesBag.uniqueSet());
        for (String category : salesBag.uniqueSet()) {
            System.out.println("  " + category + " 销量: " + salesBag.getCount(category));
        }

        // 判断哪些分类是热卖的（销量>=2）
        Set<String> hotCategories = new HashSet<>();
        for (String category : salesBag.uniqueSet()) {
            if (salesBag.getCount(category) >= 2) {
                hotCategories.add(category);
            }
        }
        System.out.println("热卖分类: " + hotCategories);
        System.out.println();
    }

    /**
     * 3. 去重登录历史记录（SetUniqueList）
     *
     * 场景：展示用户最近访问过的 10 个页面，同一页面只保留最近的一次位置
     */
    static void demoDeduplicateLoginHistory() {
        System.out.println("--- 3. 去重登录/浏览历史 ---");

        List<String> rawHistory = Arrays.asList(
                "/home", "/product/1001", "/home", "/cart", "/product/1001", "/order", "/home"
        );
        List<String> history = SetUniqueList.setUniqueList(new ArrayList<>(rawHistory));
        System.out.println("去重并保持最近出现顺序: " + history);

        // 新访问一个已存在的页面，不会再次加入
        history.add("/home");
        System.out.println("再次访问 /home: " + history);
        System.out.println();
    }

    /**
     * 4. HTTP Header 大小写不敏感读取
     *
     * 场景：HTTP/1.1 Header 名大小写不敏感，框架层可用 CaseInsensitiveMap 统一处理
     */
    static void demoHttpHeadersInsensitive() {
        System.out.println("--- 4. HTTP Header 大小写不敏感 ---");

        CaseInsensitiveMap<String, String> headers = new CaseInsensitiveMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Request-Id", "uuid-1234");
        headers.put("Authorization", "Bearer xxx");

        System.out.println("content-type: " + headers.get("content-type"));
        System.out.println("X-REQUEST-ID: " + headers.get("X-REQUEST-ID"));
        System.out.println("Authorization contains: " + headers.containsKey("authorization"));
        System.out.println();
    }

    /**
     * 5. 临时缓存 / 限流计数器（PassiveExpiringMap）
     *
     * 场景：短信验证码 5 分钟内有效；API 限流计数器 1 秒后重置
     */
    static void demoExpiringCache() throws InterruptedException {
        System.out.println("--- 5. 临时缓存与限流计数器 ---");

        // 5 秒过期的验证码缓存
        Map<String, String> verifyCodeCache = new PassiveExpiringMap<>(5000);
        verifyCodeCache.put("13800001111", "582934");
        System.out.println("刚放入验证码: " + verifyCodeCache);

        // 1 秒过期的限流计数器
        PassiveExpiringMap<String, Integer> rateLimiter = new PassiveExpiringMap<>(1000);
        rateLimiter.put("ip:192.168.1.1", 1);
        System.out.println("限流计数器: " + rateLimiter);

        TimeUnit.MILLISECONDS.sleep(1200);
        System.out.println("1秒后验证码仍在: " + verifyCodeCache.get("13800001111"));
        System.out.println("1秒后限流计数器已过期: " + rateLimiter.get("ip:192.168.1.1"));
        System.out.println();
    }

    /**
     * 6. 双向映射做短码↔ID
     *
     * 场景：短链接服务中 shortCode 与 longUrlId 互相查找
     */
    static void demoShortCodeMapping() {
        System.out.println("--- 6. 短码与ID双向映射 ---");

        BidiMap<String, Long> shortCodeMap = new DualHashBidiMap<>();
        shortCodeMap.put("aBc123", 10001L);
        shortCodeMap.put("xYz789", 10002L);

        // 短码查 ID
        System.out.println("短码 aBc123 -> ID: " + shortCodeMap.get("aBc123"));
        // ID 查短码
        System.out.println("ID 10002 -> 短码: " + shortCodeMap.getKey(10002L));

        // 防止短码冲突：反向查 ID 是否已存在
        if (shortCodeMap.containsValue(10001L)) {
            System.out.println("ID 10001 已存在，不能重复生成短码");
        }
        System.out.println();
    }

    /**
     * 7. Map 合并与默认值处理
     *
     * 场景：合并默认配置与用户自定义配置；读取数值配置时提供兜底值
     */
    static void demoMapMergeWithDefaults() {
        System.out.println("--- 7. Map 合并与默认值 ---");

        Map<String, Object> defaults = new HashMap<>();
        defaults.put("timeout", 5000);
        defaults.put("retry", 3);
        defaults.put("enabled", true);

        Map<String, Object> userConfig = new HashMap<>();
        userConfig.put("timeout", 3000);

        // 用户配置优先，缺失项使用默认值
        Map<String, Object> finalConfig = new HashMap<>(defaults);
        finalConfig.putAll(userConfig);
        System.out.println("合并后配置: " + finalConfig);

        // 使用 MapUtils 安全读取整数，带默认值
        int timeout = MapUtils.getInteger(finalConfig, "timeout", 5000);
        int retry = MapUtils.getInteger(finalConfig, "retry", 0);
        boolean enabled = MapUtils.getBoolean(finalConfig, "enabled", false);
        System.out.println("timeout=" + timeout + ", retry=" + retry + ", enabled=" + enabled);
        System.out.println();
    }

    /**
     * 8. 集合运算做权限判定
     *
     * 场景：判断用户权限是否包含接口所需权限
     */
    static void demoPermissionCheck() {
        System.out.println("--- 8. 权限集合运算判定 ---");

        Set<String> userPerms = new HashSet<>(Arrays.asList("user:read", "user:write", "order:read"));
        Set<String> requiredPerms = new HashSet<>(Arrays.asList("user:read", "order:write"));

        // 交集：用户已拥有的所需权限
        Set<String> owned = SetUtils.intersection(userPerms, requiredPerms);
        System.out.println("用户已拥有的所需权限: " + owned);

        // 差集：还缺失的权限
        Set<String> missing = SetUtils.difference(requiredPerms, userPerms);
        System.out.println("缺失权限: " + missing);

        // 简单判定是否全部拥有
        boolean hasAll = missing.isEmpty();
        System.out.println("是否拥有全部所需权限: " + hasAll);

        // CollectionUtils 判定两个集合是否相等
        boolean equal = CollectionUtils.isEqualCollection(userPerms, requiredPerms);
        System.out.println("权限集合是否完全相等: " + equal);
        System.out.println();
    }

    /**
     * 9. 顺序 Map 做流程步骤索引
     *
     * 场景：审批流/工作流步骤按顺序执行，且需要按索引跳转
     */
    static void demoWorkflowSteps() {
        System.out.println("--- 9. 工作流步骤索引 ---");

        LinkedMap<String, String> workflow = new LinkedMap<>();
        workflow.put("submit", "提交申请");
        workflow.put("leader_approve", "直属领导审批");
        workflow.put("hr_approve", "HR 审批");
        workflow.put("finish", "完成归档");

        System.out.println("所有步骤: " + workflow);
        System.out.println("第 2 步 key: " + workflow.get(1)); // LinkedMap 支持按索引访问 key
        System.out.println("第 2 步名称: " + workflow.getValue(1));
        System.out.println("finish 的索引: " + workflow.indexOf("finish"));
        System.out.println();
    }
}
