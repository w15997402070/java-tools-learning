package com.example.zxing;

import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * ZXing 高级演示
 * <p>
 * 演示内容：
 * 1. 自定义颜色二维码（彩色前景/背景）
 * 2. 带 Logo 的二维码（中心嵌入图标）
 * 3. 批量生成多个二维码
 * </p>
 *
 * 注意事项：
 * - 带 Logo 时建议使用高纠错级别（H），Logo 占比不超过 30%
 * - 颜色过浅或对比度不足会导致扫描失败，建议前景色深、背景色浅
 */
public class ZXingAdvancedDemo {

    private static final String OUTPUT_DIR = "output";

    public static void main(String[] args) throws Exception {
        new File(OUTPUT_DIR).mkdirs();

        System.out.println("=== ZXing 高级演示 ===\n");

        // 1. 自定义颜色二维码
        generateColorQRCode();

        // 2. 带 Logo 的二维码
        generateQRCodeWithLogo();

        // 3. 批量生成二维码
        batchGenerateQRCodes();
    }

    /**
     * 示例1：自定义颜色二维码
     * <p>
     * MatrixToImageConfig 允许自定义前景色（ON_COLOR）和背景色（OFF_COLOR）。
     * 颜色格式为 ARGB 整数（Alpha | Red | Green | Blue）。
     * </p>
     */
    private static void generateColorQRCode() throws Exception {
        System.out.println("--- 示例1：自定义颜色二维码 ---");

        String content = "https://www.example.com/product/001";
        int width = 300;
        int height = 300;

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 1);

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints);

        // 深蓝色前景 + 米白色背景
        // ARGB: 0xFF1A237E = 深蓝, 0xFFFFF8E1 = 米白
        MatrixToImageConfig config = new MatrixToImageConfig(0xFF1A237E, 0xFFFFF8E1);

        Path path = FileSystems.getDefault().getPath(OUTPUT_DIR + "/colored_qrcode.png");
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path, config);

        System.out.println("✅ 彩色二维码生成成功：" + OUTPUT_DIR + "/colored_qrcode.png");
        System.out.println("   前景色：深蓝 #1A237E  背景色：米白 #FFF8E1\n");
    }

    /**
     * 示例2：带 Logo 的二维码
     * <p>
     * 实现原理：
     * 1. 先生成标准二维码 BufferedImage
     * 2. 在中心绘制一个圆角矩形 Logo 区域
     * 3. 由于 Logo 遮挡了部分码点，必须使用高纠错级别 H（30%容错）
     * </p>
     */
    private static void generateQRCodeWithLogo() throws Exception {
        System.out.println("--- 示例2：带 Logo 的二维码 ---");

        String content = "https://github.com/zxing/zxing";
        int width = 400;
        int height = 400;

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        // ⚠️ 带 Logo 必须使用最高纠错级别 H，否则被遮挡部分无法恢复
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 1);

        // Step 1: 生成二维码 BufferedImage
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints);
        BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

        // Step 2: 在中心叠加 Logo（用纯色矩形模拟 Logo，实际项目可换为真实图片）
        int logoSize = width / 5;  // Logo 占二维码的 20%
        int logoX = (width - logoSize) / 2;
        int logoY = (height - logoSize) / 2;

        Graphics2D g2d = qrImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 绘制白色圆角矩形背景（避免 Logo 和码点混淆）
        g2d.setColor(Color.WHITE);
        g2d.fillRoundRect(logoX - 4, logoY - 4, logoSize + 8, logoSize + 8, 10, 10);

        // 绘制 Logo（示例：蓝色圆形）
        g2d.setColor(new Color(0x1565C0));  // 蓝色
        g2d.fillOval(logoX, logoY, logoSize, logoSize);

        // 绘制 "Z" 字符代表 ZXing
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, logoSize / 2));
        FontMetrics fm = g2d.getFontMetrics();
        String logoText = "Z";
        int textX = logoX + (logoSize - fm.stringWidth(logoText)) / 2;
        int textY = logoY + (logoSize - fm.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(logoText, textX, textY);
        g2d.dispose();

        // Step 3: 保存
        File outputFile = new File(OUTPUT_DIR + "/logo_qrcode.png");
        ImageIO.write(qrImage, "PNG", outputFile);

        System.out.println("✅ Logo 二维码生成成功：" + OUTPUT_DIR + "/logo_qrcode.png");
        System.out.println("   Logo 占比：约 20%（纠错级别 H，最大容忍 30%）\n");
    }

    /**
     * 示例3：批量生成二维码
     * <p>
     * 模拟批量生成商品码或优惠券码场景。
     * 实际项目中通常从数据库读取内容，此处用模拟数据演示。
     * </p>
     */
    private static void batchGenerateQRCodes() throws Exception {
        System.out.println("--- 示例3：批量生成二维码 ---");

        // 模拟 5 个商品信息
        String[] products = {
            "PROD-2026-001|手机壳|¥29.9",
            "PROD-2026-002|数据线|¥19.9",
            "PROD-2026-003|充电头|¥39.9",
            "PROD-2026-004|耳机|¥99.9",
            "PROD-2026-005|保护膜|¥9.9"
        };

        String batchDir = OUTPUT_DIR + "/batch";
        new File(batchDir).mkdirs();

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);

        QRCodeWriter writer = new QRCodeWriter();
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < products.length; i++) {
            BitMatrix bitMatrix = writer.encode(products[i], BarcodeFormat.QR_CODE, 200, 200, hints);
            String fileName = batchDir + "/product_" + String.format("%03d", i + 1) + ".png";
            Path path = FileSystems.getDefault().getPath(fileName);
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("✅ 批量生成完成！共 " + products.length + " 个二维码");
        System.out.println("   输出目录：" + batchDir);
        System.out.println("   耗时：" + elapsed + " ms\n");
    }
}
