package com.example.commonscodec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.net.URLCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Apache Commons Codec 基础演示。
 *
 * 涵盖：
 * 1. Base64 编码/解码（标准、URL 安全、是否换行）
 * 2. Hex 编码/解码（16 进制字符串与字节数组互转）
 * 3. URLCodec 编码/解码（application/x-www-form-urlencoded）
 * 4. DigestUtils 计算 MD5 / SHA-256 摘要
 *
 * <p>Commons Codec 的定位是"编码与摘要工具箱"，不依赖任何第三方库，
 * 在 Java 8 环境中可以安全替换 sun.misc.BASE64Encoder 等 JDK 私有 API。</p>
 */
public class CommonsCodecBasicDemo {

    private static final Logger log = LoggerFactory.getLogger(CommonsCodecBasicDemo.class);

    public static void main(String[] args) throws Exception {
        demoBase64();
        demoHex();
        demoUrlCodec();
        demoDigestUtils();
    }

    /**
     * 1. Base64 编解码演示。
     *
     * <p>Commons Codec 提供 {@link Base64} 类，支持：</p>
     * <ul>
     *   <li>标准 Base64（RFC 4648）</li>
     *   <li>URL 与文件名安全 Base64（-、_ 替换 +、/）</li>
     *   <li>控制是否输出换行（chunk 每 76 字符）</li>
     * </ul>
     */
    private static void demoBase64() {
        log.info("===== 1. Base64 编解码 =====");

        String original = "Apache Commons Codec = 编码工具箱";
        byte[] bytes = original.getBytes(StandardCharsets.UTF_8);

        // 标准 Base64（可能含 +、/、=）
        String standardBase64 = Base64.encodeBase64String(bytes);
        String decodedStandard = new String(Base64.decodeBase64(standardBase64), StandardCharsets.UTF_8);

        // URL 安全 Base64（用于 URL 参数、JWT、文件名）
        String urlSafeBase64 = Base64.encodeBase64URLSafeString(bytes);
        String decodedUrlSafe = new String(Base64.decodeBase64(urlSafeBase64), StandardCharsets.UTF_8);

        // 不带换行的 Base64（适合 JSON 内嵌）
        String noChunkBase64 = new String(Base64.encodeBase64(bytes, false), StandardCharsets.UTF_8);

        log.info("原文: {}", original);
        log.info("标准 Base64: {}", standardBase64);
        log.info("URL 安全 Base64: {}", urlSafeBase64);
        log.info("无换行 Base64: {}", noChunkBase64);
        log.info("标准解码还原: {}", decodedStandard);
        log.info("URL 安全解码还原: {}", decodedUrlSafe);
        log.info("解码一致性验证: {}", original.equals(decodedStandard) && original.equals(decodedUrlSafe));
    }

    /**
     * 2. Hex 编码/解码演示。
     *
     * <p>Hex 将字节数组转换为 16 进制字符串（0-9, a-f），
     * 常用于密钥展示、二进制数据可读化、与前端/移动端对齐。</p>
     */
    private static void demoHex() {
        log.info("===== 2. Hex 编解码 =====");

        byte[] rawBytes = new byte[] {0x00, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF, 0x12, 0x34};

        // 字节数组 -> 小写 16 进制字符串
        String hexString = Hex.encodeHexString(rawBytes);
        // 带 true 参数表示大写
        String hexUpper = Hex.encodeHexString(rawBytes, true);

        // 16 进制字符串 -> 字节数组
        byte[] decoded = new byte[0];
        try {
            decoded = Hex.decodeHex(hexString.toCharArray());
        } catch (org.apache.commons.codec.DecoderException e) {
            log.error("Hex 解码失败", e);
        }

        log.info("原始字节: {}", Arrays.toString(rawBytes));
        log.info("Hex（小写）: {}", hexString);
        log.info("Hex（大写）: {}", hexUpper);
        log.info("Hex 解码还原: {}", Arrays.toString(decoded));
        log.info("还原一致性: {}", Arrays.equals(rawBytes, decoded));
    }

    /**
     * 3. URLCodec 编码/解码演示。
     *
     * <p>{@link URLCodec} 实现 application/x-www-form-urlencoded 编码，
     * 与 {@link java.net.URLEncoder} 不同之处在于默认使用 UTF-8 且行为更可预测。</p>
     */
    private static void demoUrlCodec() throws Exception {
        log.info("===== 3. URLCodec 编解码 =====");

        URLCodec codec = new URLCodec(StandardCharsets.UTF_8.name());

        String raw = "name=张三&age=25&desc=hello world! 你好@#";
        String encoded = codec.encode(raw);
        String decoded = codec.decode(encoded);

        log.info("原始字符串: {}", raw);
        log.info("URL 编码后: {}", encoded);
        log.info("URL 解码后: {}", decoded);
        log.info("还原一致性: {}", raw.equals(decoded));

        // 仅编码 key/value 中的特殊字符，保留 = 与 & 作为分隔符
        String key = "搜索关键词";
        String value = "A/B 测试";
        log.info("key 编码: {}", codec.encode(key));
        log.info("value 编码: {}", codec.encode(value));
    }

    /**
     * 4. DigestUtils 摘要演示。
     *
     * <p>MD5 与 SHA-256 是文件校验、密码摘要、数据去重最常见的算法。
     * Commons Codec 把 {@link java.security.MessageDigest} 包装成静态工具方法，避免样板代码。</p>
     *
     * <p>注意：MD5 已被证明不安全，仅用于兼容性校验；密码存储请使用 PBKDF2/Argon2/bcrypt/scrypt。</p>
     */
    private static void demoDigestUtils() {
        log.info("===== 4. DigestUtils 摘要 =====");

        String text = "CommonsCodec2026";
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

        // 常见摘要算法：MD5、SHA-1、SHA-256、SHA-512
        String md5Hex = DigestUtils.md5Hex(bytes);
        String sha1Hex = DigestUtils.sha1Hex(bytes);
        String sha256Hex = DigestUtils.sha256Hex(bytes);
        String sha512Hex = DigestUtils.sha512Hex(bytes);

        log.info("原文: {}", text);
        log.info("MD5    (32 字符): {}", md5Hex);
        log.info("SHA-1  (40 字符): {}", sha1Hex);
        log.info("SHA-256(64 字符): {}", sha256Hex);
        log.info("SHA-512(128 字符): {}", sha512Hex);

        // 重载：直接传字符串
        log.info("MD5 重载字符串: {}", DigestUtils.md5Hex(text));
        log.info("SHA-256 重载字符串: {}", DigestUtils.sha256Hex(text));
    }
}
