package com.example.commonscodec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.UUID;

/**
 * Apache Commons Codec 实战演示。
 *
 * 涵盖：
 * 1. 文件校验和（MD5/SHA-256）与下载完整性校验
 * 2. 带随机盐的密码摘要存储（HMAC-SHA256 + salt）
 * 3. 简易"API 签名 token"生成与校验（HMAC + Base64 + timestamp）
 * 4. Spring Boot 集成配置示例
 *
 * <p>注意：本演示中的密码摘要仅用于说明 Commons Codec 的用法，
 * 生产环境推荐使用 Spring Security 的 BCryptPasswordEncoder 或 Argon2。</p>
 */
public class CommonsCodecPracticalDemo {

    private static final Logger log = LoggerFactory.getLogger(CommonsCodecPracticalDemo.class);

    public static void main(String[] args) throws Exception {
        demoFileChecksum();
        demoPasswordHash();
        demoApiSignatureToken();
    }

    /**
     * 1. 文件校验和：验证下载文件是否完整、未被篡改。
     *
     * <p>Commons Codec 的 {@link DigestUtils} 提供流式方法，可以直接从 InputStream 计算摘要，
     * 避免把大文件全部读到内存。</p>
     */
    private static void demoFileChecksum() throws IOException {
        log.info("===== 1. 文件校验和 =====");

        // 构造一个临时文本文件
        File tempFile = new File("target/commons-codec-sample.txt");
        if (!tempFile.getParentFile().exists()) {
            tempFile.getParentFile().mkdirs();
        }
        Files.write(tempFile.toPath(),
                "Apache Commons Codec 文件校验和测试内容。\n第二行数据。".getBytes(StandardCharsets.UTF_8));

        // 流式计算 MD5 / SHA-256
        String md5;
        String sha256;
        try (FileInputStream fis = new FileInputStream(tempFile)) {
            md5 = DigestUtils.md5Hex(fis);
        }
        try (FileInputStream fis = new FileInputStream(tempFile)) {
            sha256 = DigestUtils.sha256Hex(fis);
        }

        log.info("文件: {}", tempFile.getAbsolutePath());
        log.info("MD5    : {}", md5);
        log.info("SHA-256: {}", sha256);

        // 模拟"服务端下发的期望摘要"，客户端做比对
        String expectedSha256 = sha256;
        log.info("下载完整性校验通过: {}", expectedSha256.equals(sha256));

        // 使用 MessageDigestAlgorithms 获取算法名（避免硬编码字符串）
        log.info("支持算法示例: {}、{}",
                org.apache.commons.codec.digest.MessageDigestAlgorithms.MD5,
                org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_256);
    }

    /**
     * 2. 带盐密码摘要存储与校验。
     *
     * <p>思路：为每个用户生成随机 salt，摘要 = HMAC-SHA256(password + salt, pepper)。
     * 验证时重新计算并比对。pepper 是全局密钥，保存在服务器配置中。</p>
     */
    private static void demoPasswordHash() {
        log.info("===== 2. 带盐密码摘要 =====");

        String globalPepper = "app_wide_secret_pepper";
        String userPassword = "MyP@ssw0rd!";

        // 注册：生成随机 salt 并计算摘要
        String salt = generateSalt();
        String storedHash = hashPassword(userPassword, salt, globalPepper);
        log.info("salt: {}", salt);
        log.info("storedHash: {}", storedHash);

        // 登录：用用户输入的密码 + 数据库中的 salt 重新计算
        boolean loginOk = verifyPassword(userPassword, storedHash, salt, globalPepper);
        log.info("正确密码验证: {}", loginOk);

        boolean loginFail = verifyPassword("WrongPassword", storedHash, salt, globalPepper);
        log.info("错误密码验证: {}", loginFail);
    }

    private static String generateSalt() {
        byte[] saltBytes = new byte[16];
        new SecureRandom().nextBytes(saltBytes);
        return Hex.encodeHexString(saltBytes);
    }

    private static String hashPassword(String password, String salt, String pepper) {
        byte[] key = pepper.getBytes(StandardCharsets.UTF_8);
        byte[] message = (password + salt).getBytes(StandardCharsets.UTF_8);
        return new HmacUtils(HmacAlgorithms.HMAC_SHA_256, key).hmacHex(message);
    }

    private static boolean verifyPassword(String password, String expectedHash, String salt, String pepper) {
        return expectedHash.equals(hashPassword(password, salt, pepper));
    }

    /**
     * 3. 简易 API 签名 Token：防重放与防篡改。
     *
     * <p>Token 格式：base64(payload) + "." + base64(hmac(payload, secret))<br>
     * payload 包含：随机 nonce、时间戳、用户标识。</p>
     */
    private static void demoApiSignatureToken() {
        log.info("===== 3. API 签名 Token =====");

        String apiSecret = "api_server_secret_2026";
        String userId = "10086";

        // 生成 token
        String token = generateToken(userId, apiSecret, System.currentTimeMillis());
        log.info("生成 Token: {}", token);

        // 校验 token
        boolean valid = validateToken(token, apiSecret, 5 * 60 * 1000L); // 有效期 5 分钟
        log.info("Token 校验: {}", valid);

        // 篡改 payload 后应校验失败
        String tampered = token.substring(0, token.length() - 4) + "XXXX";
        log.info("篡改后校验: {}", validateToken(tampered, apiSecret, 5 * 60 * 1000L));
    }

    private static String generateToken(String userId, String secret, long timestamp) {
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String payload = userId + "|" + timestamp + "|" + nonce;
        String payloadBase64 = Base64.encodeBase64URLSafeString(payload.getBytes(StandardCharsets.UTF_8));

        byte[] key = secret.getBytes(StandardCharsets.UTF_8);
        byte[] message = payloadBase64.getBytes(StandardCharsets.UTF_8);
        String signature = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, key).hmacHex(message);
        String signatureBase64 = Base64.encodeBase64URLSafeString(signature.getBytes(StandardCharsets.UTF_8));

        return payloadBase64 + "." + signatureBase64;
    }

    private static boolean validateToken(String token, String secret, long maxAgeMillis) {
        if (token == null || !token.contains(".")) {
            return false;
        }
        String[] parts = token.split("\\.", 2);
        if (parts.length != 2) {
            return false;
        }
        String payloadBase64 = parts[0];
        String signatureBase64 = parts[1];

        // 重新计算签名
        byte[] key = secret.getBytes(StandardCharsets.UTF_8);
        byte[] message = payloadBase64.getBytes(StandardCharsets.UTF_8);
        String expectedSignature = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, key).hmacHex(message);
        String expectedSignatureBase64 = Base64.encodeBase64URLSafeString(expectedSignature.getBytes(StandardCharsets.UTF_8));

        if (!expectedSignatureBase64.equals(signatureBase64)) {
            return false;
        }

        // 解析 payload 并校验时间戳
        String payload = new String(Base64.decodeBase64(payloadBase64), StandardCharsets.UTF_8);
        String[] payloadParts = payload.split("\\|", 3);
        if (payloadParts.length != 3) {
            return false;
        }
        try {
            long timestamp = Long.parseLong(payloadParts[1]);
            return System.currentTimeMillis() - timestamp <= maxAgeMillis;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
