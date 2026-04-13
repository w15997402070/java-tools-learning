package com.example.thumbnailator;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import net.coobird.thumbnailator.name.Rename;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Thumbnailator 基础演示
 * 
 * 涵盖功能：
 * 1. 生成缩略图（指定尺寸 / 按比例）
 * 2. 图片旋转与翻转
 * 3. 图片裁剪（从中心 / 自定义位置）
 * 4. 批量处理目录中的所有图片
 *
 * Thumbnailator 版本：0.4.20
 * GitHub：https://github.com/coobird/thumbnailator
 */
public class ThumbnailatorBasicDemo {

    // 输出目录
    private static final String OUTPUT_DIR = "thumbnailator-output";

    public static void main(String[] args) throws IOException {
        // 创建输出目录
        File outputDir = new File(OUTPUT_DIR);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        System.out.println("===== Thumbnailator 基础演示 =====\n");

        // 1. 首先创建一张示例图片供后续操作使用
        BufferedImage sampleImage = createSampleImage(800, 600);
        File sampleFile = new File(OUTPUT_DIR + "/sample.png");
        ImageIO.write(sampleImage, "png", sampleFile);
        System.out.println("已创建示例图片: " + sampleFile.getAbsolutePath());
        System.out.println("  尺寸: 800x600\n");

        // 2. 生成固定尺寸缩略图
        demo1_fixedSizeThumbnail(sampleFile);

        // 3. 按比例缩放
        demo2_scaleThumbnail(sampleFile);

        // 4. 图片旋转
        demo3_rotateThumbnail(sampleFile);

        // 5. 图片裁剪
        demo4_cropThumbnail(sampleFile);

        // 6. 图片翻转
        demo5_flipThumbnail(sampleFile);

        System.out.println("\n所有输出文件保存在: " + new File(OUTPUT_DIR).getAbsolutePath());
    }

    /**
     * 演示1：生成固定尺寸缩略图
     * size(width, height) 会保持宽高比，在指定范围内缩放
     */
    private static void demo1_fixedSizeThumbnail(File source) throws IOException {
        System.out.println("--- 演示1：固定尺寸缩略图 ---");

        // 生成 200x150 缩略图（保持宽高比，不超过指定尺寸）
        File output = new File(OUTPUT_DIR + "/thumb_200x150.png");
        Thumbnails.of(source)
                .size(200, 150)        // 指定最大宽高，自动保持比例
                .toFile(output);

        BufferedImage result = ImageIO.read(output);
        System.out.println("生成 200x150 缩略图:");
        System.out.println("  实际尺寸: " + result.getWidth() + "x" + result.getHeight());
        System.out.println("  输出文件: " + output.getName());

        // 强制拉伸到指定尺寸（不保持宽高比）
        File outputForce = new File(OUTPUT_DIR + "/thumb_200x150_force.png");
        Thumbnails.of(source)
                .forceSize(200, 150)  // 强制拉伸，不保持比例
                .toFile(outputForce);

        BufferedImage resultForce = ImageIO.read(outputForce);
        System.out.println("强制拉伸到 200x150:");
        System.out.println("  实际尺寸: " + resultForce.getWidth() + "x" + resultForce.getHeight());
        System.out.println("  输出文件: " + outputForce.getName() + "\n");
    }

    /**
     * 演示2：按比例缩放
     * scale(ratio) 按比例缩小/放大图片
     */
    private static void demo2_scaleThumbnail(File source) throws IOException {
        System.out.println("--- 演示2：按比例缩放 ---");

        // 缩小到原来的 50%
        File output50 = new File(OUTPUT_DIR + "/scale_50percent.png");
        Thumbnails.of(source)
                .scale(0.5)           // 缩放到50%
                .toFile(output50);

        BufferedImage result50 = ImageIO.read(output50);
        System.out.println("缩放 50%:");
        System.out.println("  实际尺寸: " + result50.getWidth() + "x" + result50.getHeight());
        System.out.println("  输出文件: " + output50.getName());

        // 缩小到原来的 25%
        File output25 = new File(OUTPUT_DIR + "/scale_25percent.png");
        Thumbnails.of(source)
                .scale(0.25)          // 缩放到25%
                .outputQuality(0.85f) // 设置输出质量（0.0~1.0），适用于JPEG
                .toFile(output25);

        BufferedImage result25 = ImageIO.read(output25);
        System.out.println("缩放 25%:");
        System.out.println("  实际尺寸: " + result25.getWidth() + "x" + result25.getHeight());
        System.out.println("  输出文件: " + output25.getName() + "\n");
    }

    /**
     * 演示3：图片旋转
     * rotate(angle) 顺时针旋转指定角度
     */
    private static void demo3_rotateThumbnail(File source) throws IOException {
        System.out.println("--- 演示3：图片旋转 ---");

        // 旋转90度
        File output90 = new File(OUTPUT_DIR + "/rotate_90.png");
        Thumbnails.of(source)
                .scale(0.5)
                .rotate(90)           // 顺时针旋转90度
                .toFile(output90);
        System.out.println("旋转 90 度: " + output90.getName());

        // 旋转180度
        File output180 = new File(OUTPUT_DIR + "/rotate_180.png");
        Thumbnails.of(source)
                .scale(0.5)
                .rotate(180)          // 顺时针旋转180度
                .toFile(output180);
        System.out.println("旋转 180 度: " + output180.getName());

        // 旋转45度（斜角，会自动扩展画布）
        File output45 = new File(OUTPUT_DIR + "/rotate_45.png");
        Thumbnails.of(source)
                .scale(0.5)
                .rotate(45)           // 旋转45度
                .toFile(output45);
        System.out.println("旋转 45 度: " + output45.getName() + "\n");
    }

    /**
     * 演示4：图片裁剪
     * crop(position) 按照指定位置裁剪图片
     */
    private static void demo4_cropThumbnail(File source) throws IOException {
        System.out.println("--- 演示4：图片裁剪 ---");

        // 从中心裁剪为 300x200
        File outputCenter = new File(OUTPUT_DIR + "/crop_center.png");
        Thumbnails.of(source)
                .size(300, 200)
                .crop(Positions.CENTER) // 从中心裁剪
                .toFile(outputCenter);

        BufferedImage resultCenter = ImageIO.read(outputCenter);
        System.out.println("从中心裁剪 300x200:");
        System.out.println("  实际尺寸: " + resultCenter.getWidth() + "x" + resultCenter.getHeight());
        System.out.println("  输出文件: " + outputCenter.getName());

        // 从左上角裁剪
        File outputTopLeft = new File(OUTPUT_DIR + "/crop_top_left.png");
        Thumbnails.of(source)
                .size(300, 200)
                .crop(Positions.TOP_LEFT) // 从左上角裁剪
                .toFile(outputTopLeft);

        System.out.println("从左上角裁剪 300x200: " + outputTopLeft.getName());

        // 从右下角裁剪
        File outputBottomRight = new File(OUTPUT_DIR + "/crop_bottom_right.png");
        Thumbnails.of(source)
                .size(300, 200)
                .crop(Positions.BOTTOM_RIGHT) // 从右下角裁剪
                .toFile(outputBottomRight);

        System.out.println("从右下角裁剪 300x200: " + outputBottomRight.getName() + "\n");
    }

    /**
     * 演示5：图片翻转
     * 通过旋转实现水平/垂直翻转效果
     * 注意：Thumbnailator 本身没有直接的 flip 方法，需结合 AWT 处理
     */
    private static void demo5_flipThumbnail(File source) throws IOException {
        System.out.println("--- 演示5：图片翻转（借助AWT实现）---");

        // 读取原图
        BufferedImage original = ImageIO.read(source);

        // 水平翻转
        BufferedImage flippedH = flipHorizontal(original);
        File outputH = new File(OUTPUT_DIR + "/flip_horizontal.png");
        // 使用 Thumbnailator 对翻转后的图片进行缩放
        Thumbnails.of(flippedH)
                .scale(0.5)
                .toFile(outputH);
        System.out.println("水平翻转并缩放50%: " + outputH.getName());

        // 垂直翻转
        BufferedImage flippedV = flipVertical(original);
        File outputV = new File(OUTPUT_DIR + "/flip_vertical.png");
        Thumbnails.of(flippedV)
                .scale(0.5)
                .toFile(outputV);
        System.out.println("垂直翻转并缩放50%: " + outputV.getName());
    }

    // ========================= 工具方法 =========================

    /**
     * 创建一张彩色渐变示例图片（用于演示，无需外部图片文件）
     */
    private static BufferedImage createSampleImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // 绘制彩色渐变背景
        GradientPaint gradient = new GradientPaint(
                0, 0, new Color(70, 130, 180),      // 钢蓝色
                width, height, new Color(255, 165, 0) // 橙色
        );
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, width, height);

        // 绘制几何图形
        g2d.setColor(Color.WHITE);
        g2d.fillOval(width / 4, height / 4, width / 2, height / 2);

        g2d.setColor(new Color(255, 69, 0));
        g2d.setStroke(new BasicStroke(8));
        g2d.drawRect(50, 50, width - 100, height - 100);

        // 绘制文字
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        String text = "Thumbnailator Demo";
        FontMetrics fm = g2d.getFontMetrics();
        int textX = (width - fm.stringWidth(text)) / 2;
        int textY = height / 2 + fm.getAscent() / 2;
        g2d.drawString(text, textX, textY);

        g2d.dispose();
        return image;
    }

    /**
     * 水平翻转图片
     */
    private static BufferedImage flipHorizontal(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage flipped = new BufferedImage(w, h, image.getType());
        Graphics2D g2d = flipped.createGraphics();
        g2d.drawImage(image, 0, 0, w, h, w, 0, 0, h, null);
        g2d.dispose();
        return flipped;
    }

    /**
     * 垂直翻转图片
     */
    private static BufferedImage flipVertical(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage flipped = new BufferedImage(w, h, image.getType());
        Graphics2D g2d = flipped.createGraphics();
        g2d.drawImage(image, 0, 0, w, h, 0, h, w, 0, null);
        g2d.dispose();
        return flipped;
    }
}
