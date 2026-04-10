package com.example.zxing;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * ZXing 基础演示
 * <p>
 * 演示内容：
 * 1. 生成简单二维码（PNG 格式）
 * 2. 解析二维码图片，读取内容
 * 3. 生成条形码（CODE_128 格式）
 * </p>
 *
 * ZXing（Zebra Crossing）是 Google 开源的多格式 1D/2D 条码图像处理库，
 * 支持 QR Code、Data Matrix、PDF417、EAN、UPC、Code 128 等几十种格式。
 */
public class ZXingBasicDemo {

    /** 二维码输出目录 */
    private static final String OUTPUT_DIR = "output";

    public static void main(String[] args) throws Exception {
        // 确保输出目录存在
        new File(OUTPUT_DIR).mkdirs();

        System.out.println("=== ZXing 基础演示 ===\n");

        // 1. 生成简单二维码
        generateSimpleQRCode();

        // 2. 解析二维码
        decodeQRCode();

        // 3. 生成条形码
        generateBarcode();
    }

    /**
     * 示例1：生成简单二维码
     * <p>
     * 使用 QRCodeWriter 生成一张包含 URL 的二维码图片，
     * 通过 MatrixToImageWriter 将 BitMatrix 写入 PNG 文件。
     * </p>
     */
    private static void generateSimpleQRCode() throws Exception {
        System.out.println("--- 示例1：生成简单二维码 ---");

        String content = "https://github.com/zxing/zxing";
        int width = 300;
        int height = 300;
        String filePath = OUTPUT_DIR + "/simple_qrcode.png";

        // 编码提示：设置字符集和纠错级别
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        // ERROR_CORRECTION：L=7% M=15% Q=25% H=30%
        // 纠错级别越高，二维码越密集，但受损时恢复能力越强
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        // 留白宽度（安静区），建议不小于4
        hints.put(EncodeHintType.MARGIN, 1);

        // 生成 BitMatrix（二维点阵）
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints);

        // 将 BitMatrix 写入文件
        Path path = FileSystems.getDefault().getPath(filePath);
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);

        System.out.println("✅ 二维码生成成功：" + filePath);
        System.out.println("   内容：" + content);
        System.out.println("   尺寸：" + width + "x" + height + "\n");
    }

    /**
     * 示例2：解析二维码图片
     * <p>
     * 读取刚才生成的二维码图片，使用 MultiFormatReader 解码，
     * 输出二维码中存储的文本内容。
     * </p>
     */
    private static void decodeQRCode() throws Exception {
        System.out.println("--- 示例2：解析二维码 ---");

        String filePath = OUTPUT_DIR + "/simple_qrcode.png";
        File file = new File(filePath);

        if (!file.exists()) {
            System.out.println("⚠️  文件不存在，请先运行示例1\n");
            return;
        }

        // 读取图片为 BufferedImage
        BufferedImage image = ImageIO.read(file);

        // 将图片转换为 ZXing 需要的 LuminanceSource（亮度源）
        LuminanceSource source = new BufferedImageLuminanceSource(image);

        // HybridBinarizer：先尝试全局二值化，再局部二值化，适合大多数场景
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        // 解码提示
        Map<DecodeHintType, Object> hints = new HashMap<>();
        hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
        // 尝试更难的解码（轻微变形/噪点图片）
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

        // MultiFormatReader 自动识别格式
        MultiFormatReader reader = new MultiFormatReader();
        Result result = reader.decode(bitmap, hints);

        System.out.println("✅ 解析成功！");
        System.out.println("   格式：" + result.getBarcodeFormat());
        System.out.println("   内容：" + result.getText());
        System.out.println("   结果点数：" + result.getResultPoints().length + "\n");
    }

    /**
     * 示例3：生成 CODE_128 条形码
     * <p>
     * CODE_128 是当前最常用的一维条形码格式，
     * 可编码全部 128 个 ASCII 字符，常用于物流、零售等场景。
     * </p>
     */
    private static void generateBarcode() throws Exception {
        System.out.println("--- 示例3：生成 CODE_128 条形码 ---");

        String content = "SN-20260410-00001";
        int width = 400;
        int height = 100;
        String filePath = OUTPUT_DIR + "/barcode_code128.png";

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 10);

        // 使用 MultiFormatWriter 统一入口，指定 CODE_128 格式
        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.CODE_128, width, height, hints);

        Path path = FileSystems.getDefault().getPath(filePath);
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);

        System.out.println("✅ 条形码生成成功：" + filePath);
        System.out.println("   内容：" + content);
        System.out.println("   格式：CODE_128\n");
    }
}
