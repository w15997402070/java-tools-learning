package com.example.zxing;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.GenericMultipleBarcodeReader;
import com.google.zxing.multi.MultipleBarcodeReader;
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
 * ZXing 实战演示
 * <p>
 * 演示内容：
 * 1. 名片二维码（vCard 格式）
 * 2. WiFi 连接二维码
 * 3. 一图多码识别（扫描图中多个二维码）
 * 4. 生成带说明文字的二维码海报
 * </p>
 *
 * 这些场景是实际项目中最常见的二维码应用需求。
 */
public class ZXingPracticalDemo {

    private static final String OUTPUT_DIR = "output";

    public static void main(String[] args) throws Exception {
        new File(OUTPUT_DIR).mkdirs();

        System.out.println("=== ZXing 实战演示 ===\n");

        // 1. 名片二维码（vCard）
        generateVCardQRCode();

        // 2. WiFi 二维码
        generateWifiQRCode();

        // 3. 生成带文字说明的二维码海报
        generateQRCodePoster();

        // 4. 演示多码识别能力（将两个二维码拼在一张图上再识别）
        demonstrateMultiDecode();
    }

    /**
     * 示例1：名片二维码（vCard 2.1 格式）
     * <p>
     * vCard 是国际通行的联系人信息标准，微信/手机通讯录均支持扫码识别。
     * 格式参考：https://www.ietf.org/rfc/rfc2426.txt
     * </p>
     */
    private static void generateVCardQRCode() throws Exception {
        System.out.println("--- 示例1：名片二维码（vCard） ---");

        // 构建 vCard 2.1 格式字符串
        StringBuilder vCard = new StringBuilder();
        vCard.append("BEGIN:VCARD\n");
        vCard.append("VERSION:2.1\n");
        vCard.append("N:王;正朋;;;\n");                           // 姓;名
        vCard.append("FN:王正朋\n");                               // 全名
        vCard.append("ORG:Java 学习小组\n");                       // 组织
        vCard.append("TITLE:高级工程师\n");                        // 职位
        vCard.append("TEL;CELL:+8613800138000\n");                // 手机
        vCard.append("EMAIL:example@company.com\n");              // 邮件
        vCard.append("URL:https://github.com/example\n");        // 网站
        vCard.append("END:VCARD");

        generateQRCode(vCard.toString(), OUTPUT_DIR + "/vcard_qrcode.png", 350, 350);

        System.out.println("✅ 名片二维码生成成功：" + OUTPUT_DIR + "/vcard_qrcode.png");
        System.out.println("   格式：vCard 2.1，可被微信/手机通讯录直接识别\n");
    }

    /**
     * 示例2：WiFi 连接二维码
     * <p>
     * WiFi 二维码格式（Android/iOS 均支持）：
     * WIFI:T:<认证类型>;S:<SSID>;P:<密码>;;
     * 认证类型：WPA / WEP / nopass
     * </p>
     */
    private static void generateWifiQRCode() throws Exception {
        System.out.println("--- 示例2：WiFi 连接二维码 ---");

        String ssid = "MyOfficeWifi";
        String password = "password123";
        String encType = "WPA";  // WPA / WEP / nopass

        // WiFi 二维码标准格式
        String wifiContent = String.format("WIFI:T:%s;S:%s;P:%s;;", encType, ssid, password);

        generateQRCode(wifiContent, OUTPUT_DIR + "/wifi_qrcode.png", 300, 300);

        System.out.println("✅ WiFi 二维码生成成功：" + OUTPUT_DIR + "/wifi_qrcode.png");
        System.out.println("   SSID：" + ssid);
        System.out.println("   加密：" + encType);
        System.out.println("   手机扫码可直接连接 WiFi（iOS 11+/Android 均支持）\n");
    }

    /**
     * 示例3：带文字说明的二维码海报
     * <p>
     * 在二维码下方绘制说明文字，适合用于打印张贴。
     * </p>
     */
    private static void generateQRCodePoster() throws Exception {
        System.out.println("--- 示例3：带文字说明的二维码海报 ---");

        String content = "https://github.com/zxing/zxing";
        int qrSize = 280;
        int posterWidth = 360;
        int posterHeight = 380;  // 多出底部文字区域

        // Step 1: 生成二维码 BitMatrix
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 1);

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, qrSize, qrSize, hints);
        BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

        // Step 2: 创建海报画布
        BufferedImage poster = new BufferedImage(posterWidth, posterHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = poster.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 白色背景
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, posterWidth, posterHeight);

        // 顶部标题
        g2d.setColor(new Color(0x1565C0));
        g2d.setFont(new Font("SansSerif", Font.BOLD, 18));
        String title = "扫码访问项目";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, (posterWidth - fm.stringWidth(title)) / 2, 35);

        // 绘制二维码（居中）
        int qrX = (posterWidth - qrSize) / 2;
        g2d.drawImage(qrImage, qrX, 50, null);

        // 底部说明文字
        g2d.setColor(Color.GRAY);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
        String subtitle = "ZXing - 二维码开源库";
        fm = g2d.getFontMetrics();
        g2d.drawString(subtitle, (posterWidth - fm.stringWidth(subtitle)) / 2, posterHeight - 30);

        g2d.setColor(new Color(0x90A4AE));
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));
        String url = content;
        fm = g2d.getFontMetrics();
        g2d.drawString(url, (posterWidth - fm.stringWidth(url)) / 2, posterHeight - 14);

        g2d.dispose();

        // Step 3: 保存
        File outputFile = new File(OUTPUT_DIR + "/qrcode_poster.png");
        ImageIO.write(poster, "PNG", outputFile);

        System.out.println("✅ 二维码海报生成成功：" + OUTPUT_DIR + "/qrcode_poster.png");
        System.out.println("   尺寸：" + posterWidth + "x" + posterHeight + "\n");
    }

    /**
     * 示例4：演示多码图识别
     * <p>
     * 先把两个不同内容的二维码拼在一张图里，
     * 然后用 GenericMultipleBarcodeReader 同时识别两个码。
     * 这在仓储扫码、票据识别等场景中很有用。
     * </p>
     */
    private static void demonstrateMultiDecode() throws Exception {
        System.out.println("--- 示例4：一图多码识别 ---");

        // 先生成两个二维码并拼合
        String[] contents = {"https://github.com", "https://stackoverflow.com"};
        int qrSize = 200;
        int totalWidth = qrSize * 2 + 20;  // 两码并排，间距20

        // 生成合并图
        BufferedImage combined = new BufferedImage(totalWidth, qrSize, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = combined.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, totalWidth, qrSize);

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);

        QRCodeWriter writer = new QRCodeWriter();
        for (int i = 0; i < contents.length; i++) {
            BitMatrix bm = writer.encode(contents[i], BarcodeFormat.QR_CODE, qrSize, qrSize, hints);
            BufferedImage qr = MatrixToImageWriter.toBufferedImage(bm);
            g2d.drawImage(qr, i * (qrSize + 10), 0, null);
        }
        g2d.dispose();

        // 保存合并图
        File combinedFile = new File(OUTPUT_DIR + "/multi_qrcode.png");
        ImageIO.write(combined, "PNG", combinedFile);

        // 使用 GenericMultipleBarcodeReader 识别多个码
        LuminanceSource source = new BufferedImageLuminanceSource(combined);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        Map<DecodeHintType, Object> decodeHints = new HashMap<>();
        decodeHints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
        decodeHints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

        MultiFormatReader baseReader = new MultiFormatReader();
        MultipleBarcodeReader multiReader = new GenericMultipleBarcodeReader(baseReader);

        try {
            Result[] results = multiReader.decodeMultiple(bitmap, decodeHints);
            System.out.println("✅ 多码识别成功！共识别到 " + results.length + " 个二维码：");
            for (int i = 0; i < results.length; i++) {
                System.out.println("   [" + (i + 1) + "] " + results[i].getText());
            }
        } catch (NotFoundException e) {
            // 多码识别对图片质量要求较高，演示环境可能只识别到1个
            System.out.println("⚠️  多码同时识别需要更好的图片间距，尝试单码识别...");
            LuminanceSource src2 = new BufferedImageLuminanceSource(combined);
            BinaryBitmap bm2 = new BinaryBitmap(new HybridBinarizer(src2));
            try {
                Result r = new MultiFormatReader().decode(bm2, decodeHints);
                System.out.println("   单码识别成功：" + r.getText());
            } catch (Exception ex) {
                System.out.println("   识别失败（图片合并后码密度过大）");
            }
        }
        System.out.println();
    }

    /**
     * 通用二维码生成辅助方法
     */
    private static void generateQRCode(String content, String filePath, int width, int height)
            throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 1);

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints);
        Path path = FileSystems.getDefault().getPath(filePath);
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
    }
}
