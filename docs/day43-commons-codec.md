# Day 43: Apache Commons Codec — Java 编码与摘要工具箱

## 一、工具简介

**Apache Commons Codec** 是 Apache 官方出品的编码、解码与摘要工具库，提供 Base64、Hex、URL 编码、MD5/SHA 摘要、HMAC、Quoted-Printable、Soundex 等常用算法实现。

- **GitHub**: https://github.com/apache/commons-codec
- **官网文档**: https://commons.apache.org/proper/commons-codec/
- **Maven Central**: https://central.sonatype.com/artifact/commons-codec/commons-codec
- **星标**: 700+（Apache 官方项目，Java 生态中无处不在的底层依赖）
- **当前最新稳定版**: **1.17.1**（Java 8 兼容；2.x 起可能要求 Java 11+）
- **License**: Apache License 2.0
- **维护团队**: Apache Commons Team
- **首次发布**: 2003 年，历史悠久

### 为什么选择 Commons Codec？

| 维度 | JDK 内置 | 手写工具 | Commons Codec |
| --- | --- | --- | --- |
| Base64 | Java 8+ 有，但旧项目需兼容 | 容易出错 | ✅ 全版本兼容、API 稳定 |
| Hex | 无 | 容易出错 | ✅ 完整支持 |
| URL 编码 | URLEncoder（默认 CP-1252/ISO-8859-1） | 繁琐 | ✅ UTF-8 默认 |
| MD5/SHA | MessageDigest 样板代码多 | 繁琐 | ✅ 一行静态方法 |
| HMAC | Mac 样板代码多 | 繁琐 | ✅ 一行静态方法 |
| 0 依赖 | — | ✅ | ✅ |

**结论**：Commons Codec 是 Java 后端处理编码、摘要、签名的最稳妥基础库之一，被 Spring Security、Apache HttpClient、AWS SDK 等大量项目间接依赖。

### 核心能力图谱

```
编码/解码
├── Base64（标准 / URL 安全 / 流式）
├── Hex（16 进制）
├── URLCodec（application/x-www-form-urlencoded）
└── QuotedPrintableCodec

摘要与认证
├── DigestUtils（MD5 / SHA-1 / SHA-256 / SHA-512）
├── HmacUtils（HMAC-MD5 / HMAC-SHA256 等）
└── 流式摘要（直接读 InputStream）

其他
├── Soundex / Metaphone（英文语音相似度）
└── Crypt / Md5Crypt / Sha2Crypt（Unix 密码加密）
```

## 二、Maven 依赖配置

```xml
<properties>
    <commons.codec.version>1.17.1</commons.codec.version>
</properties>

<dependencies>
    <!-- Apache Commons Codec 核心库（0 传递依赖） -->
    <dependency>
        <groupId>commons-codec</groupId>
        <artifactId>commons-codec</artifactId>
        <version>${commons.codec.version}</version>
    </dependency>
</dependencies>
```

**注意**：
- commons-codec 1.17.x 是 **0 依赖** 的，不依赖 commons-lang3 / commons-io
- 1.17.x 要求 Java 8+，1.16 之前要求 Java 7+
- Java 8 项目请锁定到 **1.17.1** 或 **1.16.1**

## 三、核心 API 速览

### 3.1 Base64

```java
import org.apache.commons.codec.binary.Base64;

byte[] bytes = "hello".getBytes(StandardCharsets.UTF_8);

// 标准 Base64（可能含 + / =）
String standard = Base64.encodeBase64String(bytes);
byte[] decoded = Base64.decodeBase64(standard);

// URL 安全 Base64（JWT、URL 参数）
String urlSafe = Base64.encodeBase64URLSafeString(bytes);
byte[] decodedUrlSafe = Base64.decodeBase64(urlSafe);

// 流式编解码（大文件）
try (InputStream in = new Base64InputStream(new FileInputStream("data.b64"));
     OutputStream out = new FileOutputStream("data.bin")) {
    IOUtils.copy(in, out);
}
```

### 3.2 Hex

```java
import org.apache.commons.codec.binary.Hex;

byte[] raw = new byte[] {0x0A, (byte) 0xAB};
String hex = Hex.encodeHexString(raw);          // 0aab
byte[] decoded = Hex.decodeHex(hex.toCharArray());
String hexUpper = Hex.encodeHexString(raw, true); // 0AAB
```

### 3.3 URLCodec

```java
import org.apache.commons.codec.net.URLCodec;

URLCodec codec = new URLCodec(StandardCharsets.UTF_8.name());
String encoded = codec.encode("张三");  // %E5%BC%A0%E4%B8%89
String decoded = codec.decode(encoded); // 张三
```

### 3.4 DigestUtils（摘要）

```java
import org.apache.commons.codec.digest.DigestUtils;

// 字符串摘要
String md5 = DigestUtils.md5Hex("hello");
String sha256 = DigestUtils.sha256Hex("hello");

// 文件流式摘要（推荐大文件）
try (InputStream in = new FileInputStream("big.zip")) {
    String fileSha256 = DigestUtils.sha256Hex(in);
}

// 字节数组摘要
byte[] hash = DigestUtils.sha256("hello".getBytes(StandardCharsets.UTF_8));
```

### 3.5 HmacUtils（消息认证码）

```java
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;

byte[] key = "secret".getBytes(StandardCharsets.UTF_8);
byte[] message = "data".getBytes(StandardCharsets.UTF_8);

String hmacSha256 = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, key).hmacHex(message);
```

### 3.6 QuotedPrintableCodec

```java
import org.apache.commons.codec.net.QuotedPrintableCodec;

QuotedPrintableCodec codec = new QuotedPrintableCodec(StandardCharsets.UTF_8);
String encoded = codec.encode("你好 World");
String decoded = codec.decode(encoded);
```

### 3.7 语音相似度（Soundex / Metaphone）

```java
import org.apache.commons.codec.language.Soundex;
import org.apache.commons.codec.language.Metaphone;

Soundex soundex = new Soundex();
Metaphone metaphone = new Metaphone();

log.info("Robert -> Soundex: {}", soundex.encode("Robert"));     // R163
log.info("Rupert -> Soundex: {}", soundex.encode("Rupert"));   // R163（与 Robert 相同）
log.info("Knight -> Metaphone: {}", metaphone.encode("Knight")); // NXT
log.info("Night  -> Metaphone: {}", metaphone.encode("Night"));  // NXT（与 Knight 相同）
```

## 四、Spring Boot 集成

### 4.1 添加依赖

```xml
<dependency>
    <groupId>commons-codec</groupId>
    <artifactId>commons-codec</artifactId>
    <version>1.17.1</version>
</dependency>
```

Commons Codec 没有 Spring Boot starter，也不需要任何自动配置 —— 直接在 Service/Util 中调用工具类即可。

### 4.2 封装通用工具类（推荐）

```java
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

@Component
public class CryptoUtils {

    /** 生成随机 Hex 字符串作为 salt */
    public String generateSalt(int byteLength) {
        byte[] salt = new byte[byteLength];
        new SecureRandom().nextBytes(salt);
        return Hex.encodeHexString(salt);
    }

    /** 计算 HMAC-SHA256 */
    public String hmacSha256(String secret, String message) {
        return new HmacUtils(HmacAlgorithms.HMAC_SHA_256,
                secret.getBytes(StandardCharsets.UTF_8))
                .hmacHex(message.getBytes(StandardCharsets.UTF_8));
    }

    /** 计算 SHA-256（文件/字符串） */
    public String sha256(String text) {
        return DigestUtils.sha256Hex(text);
    }

    /** URL 安全 Base64 */
    public String base64UrlSafe(String text) {
        return Base64.encodeBase64URLSafeString(text.getBytes(StandardCharsets.UTF_8));
    }
}
```

### 4.3 接口签名校验示例

```java
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

public class ApiSignatureChecker {

    public boolean verify(String appSecret,
                          String timestamp,
                          String nonce,
                          String body,
                          String sign) {

        // 1. 校验时间戳防重放（示例：5 分钟有效）
        long ts = Long.parseLong(timestamp);
        if (Math.abs(System.currentTimeMillis() - ts) > 5 * 60 * 1000) {
            return false;
        }

        // 2. 按约定拼接签名字符串
        String signText = timestamp + "|" + nonce + "|" + body;

        // 3. 重新计算 HMAC-SHA256
        String expected = new HmacUtils(HmacAlgorithms.HMAC_SHA_256,
                appSecret.getBytes(StandardCharsets.UTF_8))
                .hmacHex(signText.getBytes(StandardCharsets.UTF_8));

        // 4. 常量时间比对（防止时序攻击）
        return StringUtils.hasText(sign) && constantTimeEquals(sign, expected);
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        if (aBytes.length != bBytes.length) return false;
        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        return result == 0;
    }
}
```

## 五、注意事项（Bug 风险 / 性能问题 / 使用限制）

### 5.1 安全相关

- **🔴 MD5 / SHA-1 已不安全**
  - MD5 和 SHA-1 已被破解，不再适合密码存储、证书签名、安全协议
  - 仅用于文件完整性校验、数据去重、非安全场景
  - 密码存储请使用 BCrypt / Argon2 / PBKDF2（Spring Security 提供）

- **🔴 HMAC 密钥必须保密**
  - HMAC 的安全性完全依赖于密钥长度与保密性
  - 密钥不要硬编码在代码中，应放入配置中心 / KMS / 环境变量
  - 密钥长度建议 ≥ 256 bit（32 字节以上）

- **🔴 Base64 不是加密**
  - Base64 只是编码，任何人都可以解码
  - 不要把敏感信息直接 Base64 后当成"加密"

### 5.2 编码陷阱

- **🟡 URLCodec 与 URLEncoder 行为不同**
  - JDK `URLEncoder.encode` 默认按 `application/x-www-form-urlencoded` 处理，且历史版本使用平台默认编码
  - Commons Codec `URLCodec` 明确指定字符集，默认 UTF-8，行为更可预测
  - 不要混用两者，否则空格、中文字符可能不一致

- **🟡 Base64 URL 安全 vs 标准 Base64**
  - 标准 Base64 含 `+`、`/`、`=`，不能直接放在 URL 参数或文件名中
  - URL 安全版本会把 `+`→`-`、`/`→`_`、去掉 `=`，适合 JWT、URL token
  - 存储到数据库时如需标准 Base64，建议再转义

- **🟡 Hex 大小写**
  - `Hex.encodeHexString(bytes, true)` 返回大写，默认小写
  - 与外部系统对接时务必约定大小写，否则比对会失败

### 5.3 性能

- **🟢 流式摘要适合大文件**
  - `DigestUtils.sha256Hex(InputStream)` 不会一次性加载整个文件
  - 适合 GB 级文件校验

- **🟢 零依赖、体积小**
  - commons-codec 1.17.1 约 350KB，0 依赖，不会引入冲突

- **🟡 避免在热点路径反复创建 HmacUtils 实例**
  - `HmacUtils` 实例持有 `Mac` 实例，创建有开销
  - 高频调用建议复用实例（注意线程安全：Mac 不是线程安全的）

### 5.4 API 演进

| 旧 API（1.10 之前） | 新 API（1.15+，推荐） |
| --- | --- |
| `DigestUtils.md5Hex(String)` | `DigestUtils.md5Hex(String)`（保持兼容） |
| `DigestUtils.md5(byte[])` | `DigestUtils.md5(byte[])`（返回 byte[]） |
| `new HmacUtils("HmacSHA256", key)` | `new HmacUtils(HmacAlgorithms.HMAC_SHA_256, key)` |

- 推荐用 `HmacAlgorithms` 枚举，避免算法名字符串拼写错误
- `DigestUtils` 是静态工具，线程安全

### 5.5 其他

- **🟡 仅适用于通用场景**
  - Commons Codec 不是完整的密码学库
  - 需要非对称加密、TLS、证书管理请用 BouncyCastle 或 JDK 自带 JCE

- **🟡 Soundex / Metaphone 仅对拉丁字母有效**
  - 中文姓名需先转拼音或自定义算法

## 六、运行方法

### 6.1 构建项目

```bash
cd commons-codec-demo
mvn clean package -DskipTests
```

成功后会在 `target/` 下生成：
- `commons-codec-demo.jar`（不含依赖）
- `dependency/` 目录（含全部依赖 JAR）

### 6.2 运行 Demo

```bash
# 复制依赖到 target/dependency
mvn dependency:copy-dependencies

# 运行基础 Demo（Base64 / Hex / URLCodec / MD5 / SHA-256）
java -Dfile.encoding=UTF-8 -cp "target/commons-codec-demo.jar;target/dependency/*" \
     com.example.commonscodec.CommonsCodecBasicDemo

# 运行进阶 Demo（HMAC / Base64 流 / QuotedPrintable / Soundex）
java -Dfile.encoding=UTF-8 -cp "target/commons-codec-demo.jar;target/dependency/*" \
     com.example.commonscodec.CommonsCodecAdvancedDemo

# 运行实战 Demo（文件校验和 / 密码摘要 / API 签名 Token）
java -Dfile.encoding=UTF-8 -cp "target/commons-codec-demo.jar;target/dependency/*" \
     com.example.commonscodec.CommonsCodecPracticalDemo
```

> Windows PowerShell 下 classpath 分号需改成分号（无需转义）；Git Bash 下若 `*` 不展开，请改用 `target/dependency/commons-codec-1.17.1.jar;target/dependency/...` 显式列出。

## 七、今日总结

| 能力 | 对应类 | 典型场景 |
| --- | --- | --- |
| Base64 编解码 | `Base64` | JWT、URL token、图片 base64 |
| Hex 编解码 | `Hex` | 密钥展示、随机盐 |
| URL 编码 | `URLCodec` | 表单参数、URL query |
| MD5/SHA 摘要 | `DigestUtils` | 文件校验、数据去重 |
| HMAC 签名 | `HmacUtils` | 接口签名、Webhook 校验 |
| Quoted-Printable | `QuotedPrintableCodec` | 邮件正文、旧协议兼容 |
| 语音相似度 | `Soundex` / `Metaphone` | 英文姓名去重/纠错 |

**一句话**：Apache Commons Codec 是 Java 后端的"瑞士军刀"级编码摘要库，掌握它能避免重复造轮子，也能绕过 JDK 历史 API 的种种陷阱。
