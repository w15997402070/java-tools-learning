package com.example.hutool;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.CircleCaptcha;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;

import javax.crypto.SecretKey;
import java.io.File;

/**
 * Hutool 进阶功能演示
 * 
 * 覆盖：加密解密、验证码生成、HTTP客户端、定时任务、压缩解压
 * 
 * @author java-tools-learning
 */
public class HutoolAdvancedDemo {

    public static void main(String[] args) {
        Console.log("========== Hutool 进阶功能演示 ==========\n");

        // === 1. 加密解密 ===
        Console.log("--- 1. 加密解密 ---");
        demoCrypto();

        // === 2. 验证码生成 ===
        Console.log("\n--- 2. 验证码生成 ---");
        demoCaptcha();

        // === 3. HTTP客户端 ===
        Console.log("\n--- 3. HTTP客户端 ---");
        demoHttpClient();

        // === 4. 定时任务 ===
        Console.log("\n--- 4. 定时任务 ---");
        demoCron();

        // === 5. 压缩与解压 ===
        Console.log("\n--- 5. 压缩与解压 ---");
        demoZip();

        Console.log("\n========== 进阶演示完成 ==========");
    }

    /**
     * 加密解密演示
     */
    private static void demoCrypto() {
        String data = "Hello, 这是待加密的数据";

        // === MD5 / SHA256 摘要 ===
        String md5 = DigestUtil.md5Hex(data);
        String sha256 = DigestUtil.sha256Hex(data);
        Console.log("MD5('{}') : {}", data, md5);
        Console.log("SHA256('{}'): {}", sha256);

        // SecureUtil 统一入口
        String md5_2 = SecureUtil.md5(data);
        Console.log("SecureUtil.md5: {}", md5_2);
        Console.log("MD5 16位: {}", SecureUtil.md5(data).substring(8, 24));

        // === AES 对称加密 ===
        // 生成随机密钥
        byte[] key = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue()).getEncoded();
        AES aes = SecureUtil.aes(key);

        String encrypted = aes.encryptBase64(data);
        Console.log("AES加密后(Base64): {}", encrypted);

        String decrypted = aes.decryptStr(encrypted);
        Console.log("AES解密后: {}", decrypted);

        // === 使用固定密钥的AES ===
        AES aesFixed = SecureUtil.aes("my-secret-key123".getBytes());
        String encFixed = aesFixed.encryptBase64("敏感数据需要保护");
        String decFixed = aesFixed.decryptStr(encFixed);
        Console.log("固定密钥AES - 加密: {}", encFixed);
        Console.log("固定密钥AES - 解密: {}", decFixed);

        // === RSA 非对称加密（内部生成密钥对）===
        cn.hutool.crypto.asymmetric.RSA rsa = new cn.hutool.crypto.asymmetric.RSA();
        String rsaEncrypted = rsa.encryptBase64("RSA加密测试", cn.hutool.crypto.asymmetric.KeyType.PublicKey);
        String rsaDecrypted = rsa.decryptStr(rsaEncrypted, cn.hutool.crypto.asymmetric.KeyType.PrivateKey);
        Console.log("RSA加密(内部生成密钥对): {}", rsaEncrypted);
        Console.log("RSA解密: {}", rsaDecrypted);
    }

    /**
     * 验证码生成演示
     */
    private static void demoCaptcha() {
        // 生成线段干扰验证码
        CircleCaptcha captcha = CaptchaUtil.createCircleCaptcha(200, 100, 4, 20);
        
        // 获取验证码文字
        String code = captcha.getCode();
        Console.log("验证码文字: {}", code);

        // 获取Base64编码的图片（可直接用于<img>标签src）
        String base64Image = captcha.getImageBase64Data();
        Console.log("Base64图片(前50字符): {}...", base64Image.substring(0, 50));
        Console.log("图片长度: {}", base64Image.length());

        // 写入文件
        String userDir = System.getProperty("user.dir");
        File captchaFile = new File(userDir, "target/captcha-test.png");
        captcha.write(captchaFile.getAbsolutePath());
        Console.log("验证码已保存: {}", captchaFile.getAbsolutePath());

        // 验证
        boolean match = captcha.verify(code);
        Console.log("验证码校验: {}", match ? "通过" : "失败");
        boolean matchFalse = captcha.verify("xxxx");
        Console.log("错误验证码校验: {}", matchFalse ? "通过" : "失败");

        Console.log("  (在HTML中: <img src='data:image/png;base64,{}'/>)", base64Image.substring(0, 20) + "...");
    }

    /**
     * HTTP客户端演示
     */
    private static void demoHttpClient() {
        // === GET 请求（简化版） ===
        Console.log("发送GET请求到 postman-echo...");
        try {
            String result = HttpUtil.get("https://postman-echo.com/get?foo=bar");
            Console.log("GET响应(前200字符): {}...", result.length() > 200 ? result.substring(0, 200) : result);
        } catch (Exception e) {
            Console.log("GET请求失败(网络不可达时预期): {}", e.getMessage());
        }

        // === POST JSON 请求 ===
        Console.log("\n发送POST JSON请求...");
        try {
            HttpResponse response = HttpRequest.post("https://postman-echo.com/post")
                    .header("Content-Type", "application/json")
                    .header("X-Custom-Header", "HutoolDemo")
                    .body("{\"name\":\"Hutool\",\"version\":\"5.8.25\"}")
                    .timeout(5000)  // 5秒超时
                    .execute();

            Console.log("响应状态码: {}", response.getStatus());
            Console.log("响应体(前200字符): {}...", 
                    response.body().length() > 200 ? response.body().substring(0, 200) : response.body());
        } catch (Exception e) {
            Console.log("POST请求失败(网络不可达时预期): {}", e.getMessage());
        }

        // === 下载文件到本地 ===
        Console.log("\n尝试下载文件...");
        try {
            long size = HttpUtil.downloadFile(
                    "https://www.baidu.com/img/PCtm_d9c8750bed0b3c7d089fa7d55720d6cf.png",
                    System.getProperty("user.dir") + "/target/baidu-logo.png"
            );
            Console.log("文件下载完成，大小: {} bytes", size);
        } catch (Exception e) {
            Console.log("下载失败(网络不可达时预期): {}", e.getMessage());
        }
    }

    /**
     * 定时任务演示（CronUtil）
     */
    private static void demoCron() {
        Console.log("Hutool CronUtil 定时任务演示");

        // 设置支持秒级别匹配（默认是分钟级）
        CronUtil.setMatchSecond(true);

        // 添加一个简单的定时任务 - 每2秒执行一次
        CronUtil.schedule("*/2 * * * * *", (Task) () -> {
            // 空任务，仅演示
        });

        // 启动
        CronUtil.start();
        Console.log("CronUtil已启动，任务每2秒执行一次");
        Console.log("当前任务列表: {}", CronUtil.getScheduler().size());

        // 模拟运行5秒后停止
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        CronUtil.stop();
        Console.log("CronUtil已停止");
    }

    /**
     * 压缩与解压演示
     */
    private static void demoZip() {
        String userDir = System.getProperty("user.dir");
        String targetDir = userDir + "/target";
        String zipDir = targetDir + "/zip-demo";
        String zipFile = targetDir + "/demo.zip";

        // 创建测试文件
        File dir = new File(zipDir);
        FileUtil.mkdir(dir);
        FileUtil.writeUtf8String("文件内容1 - 测试中文", new File(zipDir + "/file1.txt"));
        FileUtil.writeUtf8String("文件内容2", new File(zipDir + "/file2.txt"));
        FileUtil.mkdir(new File(zipDir + "/subdir"));
        FileUtil.writeUtf8String("子目录文件", new File(zipDir + "/subdir/file3.txt"));

        // 打包为ZIP
        File zip = ZipUtil.zip(zipDir);
        // 移动到指定位置
        File targetZip = new File(zipFile);
        FileUtil.move(zip, targetZip, true);
        Console.log("压缩完成: {} ({} bytes)", targetZip.getAbsolutePath(), FileUtil.size(targetZip));

        // 列出ZIP内容
        try {
            java.util.zip.ZipFile zf = new java.util.zip.ZipFile(zipFile);
            java.util.List<String> entries = ZipUtil.listFileNames(zf, "UTF-8");
            Console.log("ZIP内容: {}", entries);
            zf.close();
        } catch (java.io.IOException e) {
            Console.log("读取ZIP失败: {}", e.getMessage());
        }

        // 解压
        String unzipDir = targetDir + "/unzipped";
        FileUtil.mkdir(new File(unzipDir));
        ZipUtil.unzip(zipFile, unzipDir);
        Console.log("解压到: {}", unzipDir);
        Console.log("解压后文件列表: {}", FileUtil.listFileNames(unzipDir));

        // GZip 压缩字符串
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            sb.append("这是一段需要压缩的长文本，重复多次。");
        }
        String original = sb.toString();
        byte[] gzipped = ZipUtil.gzip(original, "UTF-8");
        String ungzipped = ZipUtil.unGzip(gzipped, "UTF-8");
        Console.log("GZip压缩: {} -> {} bytes, 还原: {}", original.length(), gzipped.length, ungzipped.substring(0, 20) + "...");
    }
}
