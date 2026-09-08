package com.example.commonscodec;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.codec.language.Metaphone;
import org.apache.commons.codec.language.Soundex;
import org.apache.commons.codec.net.QuotedPrintableCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Apache Commons Codec 进阶演示。
 *
 * 涵盖：
 * 1. HmacUtils / HmacAlgorithms：HMAC-MD5 / HMAC-SHA256 消息认证码
 * 2. Base64 流式编解码（Base64InputStream / Base64OutputStream）
 * 3. QuotedPrintableCodec：邮件/HTTP 头常用可打印字符编码
 * 4. Soundex / Metaphone：英文语音相似度编码（姓名去重/搜索提示）
 *
 * <p>HMAC 与简单哈希的区别：HMAC 需要密钥，可以抵抗长度扩展攻击，
 * 常用于接口签名校验、JWT 签名、Webhook 防篡改。</p>
 */
public class CommonsCodecAdvancedDemo {

    private static final Logger log = LoggerFactory.getLogger(CommonsCodecAdvancedDemo.class);

    public static void main(String[] args) throws Exception {
        demoHmac();
        demoBase64Stream();
        demoQuotedPrintable();
        demoPhonetic();
    }

    /**
     * 1. HMAC 消息认证码演示。
     *
     * <p>HMAC（Hash-based Message Authentication Code）在哈希基础上加入密钥，
     * 用于"只有持有相同密钥的双方"才能验证数据完整性。</p>
     */
    private static void demoHmac() {
        log.info("===== 1. HMAC 消息认证码 =====");

        String message = "orderId=10086&amount=1999.00&timestamp=1695123456";
        String key = "my_secret_key_2026";

        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);

        // HmacUtils 静态方法（推荐简单场景）
        String hmacMd5Hex = new HmacUtils(HmacAlgorithms.HMAC_MD5, keyBytes).hmacHex(messageBytes);
        String hmacSha1Hex = new HmacUtils(HmacAlgorithms.HMAC_SHA_1, keyBytes).hmacHex(messageBytes);
        String hmacSha256Hex = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, keyBytes).hmacHex(messageBytes);
        String hmacSha512Hex = new HmacUtils(HmacAlgorithms.HMAC_SHA_512, keyBytes).hmacHex(messageBytes);

        log.info("原文: {}", message);
        log.info("HMAC-MD5   : {}", hmacMd5Hex);
        log.info("HMAC-SHA1  : {}", hmacSha1Hex);
        log.info("HMAC-SHA256: {}", hmacSha256Hex);
        log.info("HMAC-SHA512: {}", hmacSha512Hex);

        // 相同密钥与消息，再次计算应完全一致
        String hmacSha256Again = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, keyBytes).hmacHex(messageBytes);
        log.info("HMAC 可重复验证: {}", hmacSha256Hex.equals(hmacSha256Again));

        // 不同密钥产生完全不同结果（防止伪造）
        String hmacWithOtherKey = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, "other_key".getBytes(StandardCharsets.UTF_8)).hmacHex(messageBytes);
        log.info("换密钥后结果是否相同: {}", hmacSha256Hex.equals(hmacWithOtherKey));
    }

    /**
     * 2. Base64 流式编解码。
     *
     * <p>当数据量较大（如大文件、网络流）时，使用流式 API 可以避免把整个文件读到内存中。
     * {@link Base64InputStream} / {@link Base64OutputStream} 会自动做分块处理。</p>
     */
    private static void demoBase64Stream() throws IOException {
        log.info("===== 2. Base64 流式编解码 =====");

        String original = "这是一段将被流式 Base64 编码的中文文本，演示 Commons Codec 的流处理能力。";
        byte[] rawBytes = original.getBytes(StandardCharsets.UTF_8);

        // 编码：ByteArrayInputStream -> Base64OutputStream -> ByteArrayOutputStream
        ByteArrayOutputStream encodedBaos = new ByteArrayOutputStream();
        try (Base64OutputStream base64Out = new Base64OutputStream(encodedBaos, true)) {
            base64Out.write(rawBytes);
        }
        String encoded = encodedBaos.toString(StandardCharsets.UTF_8.name());

        // 解码：ByteArrayInputStream(base64) -> Base64InputStream -> ByteArrayOutputStream
        ByteArrayInputStream encodedBais = new ByteArrayInputStream(encoded.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream decodedBaos = new ByteArrayOutputStream();
        try (Base64InputStream base64In = new Base64InputStream(encodedBais)) {
            byte[] buffer = new byte[64];
            int len;
            while ((len = base64In.read(buffer)) != -1) {
                decodedBaos.write(buffer, 0, len);
            }
        }
        String decoded = decodedBaos.toString(StandardCharsets.UTF_8.name());

        log.info("原文长度: {} 字节", rawBytes.length);
        log.info("Base64 长度: {} 字符", encoded.length());
        log.info("解码还原: {}", decoded);
        log.info("还原一致性: {}", original.equals(decoded));
    }

    /**
     * 3. Quoted-Printable 编码演示。
     *
     * <p>Quoted-Printable（QP）将非 ASCII 字节编码为 =XX 形式，
     * 常用于邮件正文、HTTP 头、旧版协议中需要保持" mostly readable "的场景。</p>
     */
    private static void demoQuotedPrintable() throws Exception {
        log.info("===== 3. Quoted-Printable 编解码 =====");

        QuotedPrintableCodec qpCodec = new QuotedPrintableCodec(StandardCharsets.UTF_8);

        String raw = "你好，Commons Codec！Hello = ? 测试 QP 编码";
        String encoded = qpCodec.encode(raw);
        String decoded = qpCodec.decode(encoded);

        log.info("原文: {}", raw);
        log.info("QP 编码: {}", encoded);
        log.info("QP 解码: {}", decoded);
        log.info("还原一致性: {}", raw.equals(decoded));

        // QP 对纯 ASCII 文本几乎原样输出，仅对特殊字符转义
        String ascii = "Hello World!";
        log.info("ASCII QP 编码: {}", qpCodec.encode(ascii));
    }

    /**
     * 4. 语音相似度编码（Soundex / Metaphone）。
     *
     * <p>这些算法根据英文发音对姓名编码，发音相近的单词会得到相同或相似的编码，
     * 常用于客户姓名去重、拼写纠错、模糊搜索。</p>
     *
     * <p>注意：仅对拉丁字母有效，中文姓名需使用拼音或自定义算法。</p>
     */
    private static void demoPhonetic() {
        log.info("===== 4. 语音相似度编码 =====");

        Soundex soundex = new Soundex();
        Metaphone metaphone = new Metaphone();

        String[][] pairs = {
                {"Robert", "Rupert"},
                {"Smith", "Smyth"},
                {"John", "Jon"},
                {"Knight", "Night"}
        };

        log.info(String.format("%-10s %-10s %-10s %-10s %-10s %s",
                "Name1", "Name2", "Soundex1", "Soundex2", "Metaphone1", "Metaphone2"));

        for (String[] pair : pairs) {
            String s1 = soundex.encode(pair[0]);
            String s2 = soundex.encode(pair[1]);
            String m1 = metaphone.encode(pair[0]);
            String m2 = metaphone.encode(pair[1]);

            log.info(String.format("%-10s %-10s %-10s %-10s %-10s %-10s | Soundex 相同: %s | Metaphone 相同: %s",
                    pair[0], pair[1], s1, s2, m1, m2,
                    s1.equals(s2), m1.equals(m2)));
        }
    }
}
