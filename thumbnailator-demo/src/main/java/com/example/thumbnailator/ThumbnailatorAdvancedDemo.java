package com.example.thumbnailator;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.filters.Canvas;
import net.coobird.thumbnailator.geometry.AbsoluteSize;
import net.coobird.thumbnailator.geometry.Coordinate;
import net.coobird.thumbnailator.geometry.Positions;
import net.coobird.thumbnailator.geometry.Region;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;
import java.util.List;

/**
 * Thumbnailator 高级演示
 *
 * 涵盖功能：
 * 1. 添加水印（图片水印 / 文字水印）
 * 2. 图片格式转换（PNG → JPEG → WEBP模拟）
 * 3. 图片拼接/组合
 * 4. 从 InputStream/OutputStream 操作（适合Web场景）
 * 5. 高质量缩放（抗锯齿配置）
 *
 * Thumbnailator 版本：0.4.20
 */
public class ThumbnailatorAdvancedDemo {

    private static final String OUTPUT_DIR = "thumbnailator-output";
    private static final String BASE_IMAGE = OUTPUT_DIR + "/sample.png";

    public static void main(String[] args) throws IOException {
        // 确保示例图片存在（依赖 BasicDemo 生成）
        ensureSampleImage();

        System.out.println("===== Thumbnailator 高级演示 =====\n");

        // 1. 图片水印
        demo1_watermark();

        // 2. 格式转换
        demo2_formatConversion();

        // 3. 从 BufferedImage 直接操作
        demo3_fromBufferedImage();

        // 4. 流式操作（InputStream / OutputStream）
        demo4_streamOperations();

        // 5. 图片画布填充（Canvas Filter）
        demo5_canvasFill();

        System.out.println("\n所有输出文件保存在: " + new File(OUTPUT_DIR).getAbsolutePath());
    }

    /**
     * 演示1：添加水印
     * watermark(position, image, opacity) 在图片上叠加另一张图片
     */
    private static void demo1_watermark() throws IOException {
        System.out.println("--- 演示1：添加水印 ---");

        // 创建水印图片（红色半透明矩形 + 文字）
        BufferedImage watermarkImg = createWatermarkImage(200, 60, "© Sample Corp");

        // 在右下角添加图片水印，透明度 0.7
        File output = new File(OUTPUT_DIR + "/watermark_image.png");
        Thumbnails.of(BASE_IMAGE)
                .scale(1.0)                            // 保持原尺寸
                .watermark(
                        Positions.BOTTOM_RIGHT,        // 水印位置：右下角
                        watermarkImg,                  // 水印图片
                        0.7f                           // 透明度（0.0完全透明 ~ 1.0完全不透明）
                )
                .toFile(output);
        System.out.println("右下角图片水印: " + output.getName());

        // 在中心添加大水印，透明度 0.4
        File outputCenter = new File(OUTPUT_DIR + "/watermark_center.png");
        BufferedImage bigWatermark = createWatermarkImage(400, 100, "CONFIDENTIAL");
        Thumbnails.of(BASE_IMAGE)
                .scale(1.0)
                .watermark(Positions.CENTER, bigWatermark, 0.4f)
                .toFile(outputCenter);
        System.out.println("中心大水印: " + outputCenter.getName() + "\n");
    }

    /**
     * 演示2：图片格式转换
     * outputFormat(format) 转换输出格式
     */
    private static void demo2_formatConversion() throws IOException {
        System.out.println("--- 演示2：格式转换 ---");

        // PNG → JPEG（减小文件体积）
        File jpegOutput = new File(OUTPUT_DIR + "/converted.jpg");
        Thumbnails.of(BASE_IMAGE)
                .scale(1.0)
                .outputFormat("jpg")          // 指定输出格式
                .outputQuality(0.85f)         // JPEG质量（PNG转JPEG时生效）
                .toFile(jpegOutput);

        long pngSize = new File(BASE_IMAGE).length();
        long jpegSize = jpegOutput.length();
        System.out.println("PNG → JPEG 转换:");
        System.out.println("  原始PNG大小: " + pngSize + " bytes");
        System.out.println("  JPEG大小: " + jpegSize + " bytes");
        System.out.printf("  压缩率: %.1f%%\n", (1.0 - (double) jpegSize / pngSize) * 100);

        // 同时缩放并转换格式
        File thumbJpeg = new File(OUTPUT_DIR + "/thumb_300.jpg");
        Thumbnails.of(BASE_IMAGE)
                .size(300, 300)
                .outputFormat("jpg")
                .outputQuality(0.9f)
                .toFile(thumbJpeg);
        System.out.println("缩放并转JPEG: " + thumbJpeg.getName() + "\n");
    }

    /**
     * 演示3：从 BufferedImage 直接操作
     * 适合已在内存中持有图片对象的场景
     */
    private static void demo3_fromBufferedImage() throws IOException {
        System.out.println("--- 演示3：BufferedImage 直接操作 ---");

        // 读取图片到内存
        BufferedImage original = ImageIO.read(new File(BASE_IMAGE));
        System.out.println("原图尺寸: " + original.getWidth() + "x" + original.getHeight());

        // 从 BufferedImage 生成缩略图并返回 BufferedImage
        BufferedImage thumbnail = Thumbnails.of(original)
                .size(400, 300)
                .asBufferedImage();           // 返回 BufferedImage 而不是写文件
        System.out.println("缩略图尺寸: " + thumbnail.getWidth() + "x" + thumbnail.getHeight());

        // 继续对缩略图进行处理
        BufferedImage rotated = Thumbnails.of(thumbnail)
                .scale(1.0)
                .rotate(15)
                .asBufferedImage();
        System.out.println("旋转后尺寸: " + rotated.getWidth() + "x" + rotated.getHeight());

        // 保存结果
        File output = new File(OUTPUT_DIR + "/from_buffered_image.png");
        ImageIO.write(rotated, "png", output);
        System.out.println("结果保存: " + output.getName() + "\n");
    }

    /**
     * 演示4：流式操作 - 使用 InputStream/OutputStream
     * 适合 Web 应用中直接处理 HTTP 请求/响应流
     */
    private static void demo4_streamOperations() throws IOException {
        System.out.println("--- 演示4：流式操作 ---");

        // 从文件输入流读取
        File inputFile = new File(BASE_IMAGE);
        File outputFile = new File(OUTPUT_DIR + "/from_stream.jpg");

        try (InputStream inputStream = new FileInputStream(inputFile);
             OutputStream outputStream = new FileOutputStream(outputFile)) {

            Thumbnails.of(inputStream)       // 从输入流读取
                    .size(250, 250)
                    .outputFormat("jpg")
                    .outputQuality(0.8f)
                    .toOutputStream(outputStream); // 写入输出流
        }

        System.out.println("从InputStream读取 → OutputStream写入");
        System.out.println("  输出: " + outputFile.getName());

        // 模拟内存中的图片数据（字节数组）
        byte[] imageBytes = readFileBytes(inputFile);
        System.out.println("  原始字节数: " + imageBytes.length);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Thumbnails.of(new ByteArrayInputStream(imageBytes))
                .size(100, 100)
                .outputFormat("png")
                .toOutputStream(baos);

        System.out.println("  缩略图字节数: " + baos.size() + "\n");
    }

    /**
     * 演示5：Canvas 填充 - 将图片填充到指定画布尺寸，不足部分用背景色填充
     * 适合统一尺寸场景（如商品图、头像等）
     */
    private static void demo5_canvasFill() throws IOException {
        System.out.println("--- 演示5：Canvas 画布填充 ---");

        // 创建一张非标准比例的图片
        BufferedImage tallImage = createColorBlock(200, 400, new Color(255, 200, 100));
        File tallFile = new File(OUTPUT_DIR + "/tall_image.png");
        ImageIO.write(tallImage, "png", tallFile);
        System.out.println("创建高图片: 200x400");

        // 使用 Canvas 过滤器将图片填充到 300x300 画布（白色背景）
        File canvasOutput = new File(OUTPUT_DIR + "/canvas_fill.png");
        Thumbnails.of(tallFile)
                .size(300, 300)
                .addFilter(new Canvas(300, 300, Positions.CENTER, Color.WHITE)) // 白色背景填充
                .toFile(canvasOutput);

        BufferedImage result = ImageIO.read(canvasOutput);
        System.out.println("Canvas填充后尺寸: " + result.getWidth() + "x" + result.getHeight());
        System.out.println("输出文件: " + canvasOutput.getName() + "\n");
    }

    // ========================= 工具方法 =========================

    /**
     * 确保示例图片存在
     */
    private static void ensureSampleImage() throws IOException {
        File sampleFile = new File(BASE_IMAGE);
        if (!sampleFile.exists()) {
            System.out.println("示例图片不存在，正在创建...");
            File outputDir = new File(OUTPUT_DIR);
            outputDir.mkdirs();
            BufferedImage image = createSampleImage(800, 600);
            ImageIO.write(image, "png", sampleFile);
        }
    }

    /**
     * 创建带文字的水印图片
     */
    private static BufferedImage createWatermarkImage(int width, int height, String text) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 半透明红色背景
        g2d.setColor(new Color(200, 0, 0, 160));
        g2d.fillRoundRect(0, 0, width, height, 10, 10);

        // 白色文字
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, height / 3));
        FontMetrics fm = g2d.getFontMetrics();
        int x = (width - fm.stringWidth(text)) / 2;
        int y = (height + fm.getAscent() - fm.getDescent()) / 2;
        g2d.drawString(text, x, y);

        g2d.dispose();
        return image;
    }

    /**
     * 创建纯色色块图片
     */
    private static BufferedImage createColorBlock(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(color);
        g2d.fillRect(0, 0, width, height);
        g2d.setColor(Color.DARK_GRAY);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString(width + "x" + height, 20, height / 2);
        g2d.dispose();
        return image;
    }

    /**
     * 创建渐变示例图片
     */
    private static BufferedImage createSampleImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        GradientPaint gradient = new GradientPaint(0, 0, new Color(70, 130, 180), width, height, new Color(255, 165, 0));
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, width, height);
        g2d.setColor(Color.WHITE);
        g2d.fillOval(width / 4, height / 4, width / 2, height / 2);
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        String text = "Thumbnailator Demo";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(text, (width - fm.stringWidth(text)) / 2, height / 2 + fm.getAscent() / 2);
        g2d.dispose();
        return image;
    }

    /**
     * 读取文件字节
     */
    private static byte[] readFileBytes(File file) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream is = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
        }
        return baos.toByteArray();
    }
}
