package com.example.thumbnailator;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import net.coobird.thumbnailator.name.Rename;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Thumbnailator 实战演示
 *
 * 模拟真实业务场景：
 * 1. 电商商品图批量处理（统一尺寸 + 白底填充 + 水印）
 * 2. 用户头像处理（圆形裁剪 + 缩放多尺寸）
 * 3. 批量目录缩略图生成
 * 4. 响应式图片生成（一张图生成多种尺寸）
 *
 * Thumbnailator 版本：0.4.20
 */
public class ThumbnailatorPracticalDemo {

    private static final String OUTPUT_DIR = "thumbnailator-output";
    private static final String ECOMMERCE_DIR = OUTPUT_DIR + "/ecommerce";
    private static final String AVATAR_DIR = OUTPUT_DIR + "/avatars";
    private static final String RESPONSIVE_DIR = OUTPUT_DIR + "/responsive";

    public static void main(String[] args) throws IOException {
        // 创建输出目录
        createDirectories(ECOMMERCE_DIR, AVATAR_DIR, RESPONSIVE_DIR);

        System.out.println("===== Thumbnailator 实战演示 =====\n");

        // 场景1：电商商品图批量处理
        scenario1_ecommerceBatch();

        // 场景2：用户头像处理（多尺寸）
        scenario2_avatarProcessing();

        // 场景3：响应式图片（一图多尺寸）
        scenario3_responsiveImages();

        // 场景4：图片批量重命名输出
        scenario4_batchRename();

        System.out.println("\n所有输出保存在: " + new File(OUTPUT_DIR).getAbsolutePath());
    }

    /**
     * 场景1：电商商品图批量处理
     * 需求：将各种尺寸的商品图，统一处理为 800x800，白色背景，右下角水印
     */
    private static void scenario1_ecommerceBatch() throws IOException {
        System.out.println("--- 场景1：电商商品图批量处理 ---");

        // 模拟3张不同尺寸的商品原图
        int[][] imageSizes = {{1200, 900}, {400, 600}, {1000, 1000}};
        List<File> productImages = new ArrayList<>();

        for (int i = 0; i < imageSizes.length; i++) {
            BufferedImage img = createProductImage(imageSizes[i][0], imageSizes[i][1],
                    "Product " + (i + 1), new Color(100 + i * 50, 150, 200 - i * 30));
            File imgFile = new File(ECOMMERCE_DIR + "/product_raw_" + (i + 1) + ".png");
            ImageIO.write(img, "png", imgFile);
            productImages.add(imgFile);
            System.out.println("原图" + (i + 1) + ": " + imageSizes[i][0] + "x" + imageSizes[i][1]);
        }

        // 创建水印
        BufferedImage watermark = createTextWatermark("DEMO STORE", 180, 50);

        // 批量处理：统一为 800x800，白色背景，添加水印
        System.out.println("\n开始批量处理...");
        for (int i = 0; i < productImages.size(); i++) {
            File input = productImages.get(i);
            File output = new File(ECOMMERCE_DIR + "/product_final_" + (i + 1) + ".jpg");

            Thumbnails.of(input)
                    .size(800, 800)                              // 限定最大尺寸
                    .watermark(Positions.BOTTOM_RIGHT, watermark, 0.6f) // 右下角水印
                    .outputFormat("jpg")
                    .outputQuality(0.90f)
                    .toFile(output);

            BufferedImage result = ImageIO.read(output);
            System.out.println("  已处理 product_" + (i + 1) + ": "
                    + result.getWidth() + "x" + result.getHeight()
                    + " (" + output.length() / 1024 + " KB)");
        }
        System.out.println("电商商品图处理完成！\n");
    }

    /**
     * 场景2：用户头像处理
     * 需求：生成大（200x200）、中（100x100）、小（50x50）三种尺寸头像
     * 同时支持圆形头像效果（通过遮罩实现）
     */
    private static void scenario2_avatarProcessing() throws IOException {
        System.out.println("--- 场景2：用户头像多尺寸处理 ---");

        // 创建原始头像图片（模拟用户上传的照片）
        BufferedImage originalAvatar = createAvatarImage(500, 500);
        File avatarFile = new File(AVATAR_DIR + "/avatar_original.png");
        ImageIO.write(originalAvatar, "png", avatarFile);
        System.out.println("原始头像: 500x500");

        // 生成三种尺寸的头像
        int[][] sizes = {{200, 200}, {100, 100}, {50, 50}};
        String[] names = {"large", "medium", "small"};

        for (int i = 0; i < sizes.length; i++) {
            int size = sizes[i][0];
            // 标准方形头像
            File squareOutput = new File(AVATAR_DIR + "/avatar_" + names[i] + "_square.png");
            Thumbnails.of(avatarFile)
                    .size(size, size)
                    .crop(Positions.CENTER)  // 从中心裁剪为正方形
                    .toFile(squareOutput);

            // 圆形头像（通过遮罩实现）
            BufferedImage squareAvatar = ImageIO.read(squareOutput);
            BufferedImage circleAvatar = makeCircularAvatar(squareAvatar);
            File circleOutput = new File(AVATAR_DIR + "/avatar_" + names[i] + "_circle.png");
            ImageIO.write(circleAvatar, "png", circleOutput);

            System.out.println(names[i] + " (" + size + "x" + size + "): "
                    + squareOutput.getName() + " / " + circleOutput.getName());
        }
        System.out.println("头像处理完成！\n");
    }

    /**
     * 场景3：响应式图片（一张图生成多种分辨率）
     * 需求：为Web页面生成 2x、1x、0.5x 三种分辨率版本
     */
    private static void scenario3_responsiveImages() throws IOException {
        System.out.println("--- 场景3：响应式图片（多分辨率）---");

        // 创建高清原图（1600x900，适合2x显示）
        BufferedImage hdImage = createBannerImage(1600, 900, "Banner Title");
        File hdFile = new File(RESPONSIVE_DIR + "/banner_2x.png");
        ImageIO.write(hdImage, "png", hdFile);
        System.out.println("原始高清图: 1600x900 (2x)");

        // 生成响应式版本
        double[] scales = {0.5, 0.25};
        String[] suffixes = {"1x", "0.5x"};

        for (int i = 0; i < scales.length; i++) {
            File output = new File(RESPONSIVE_DIR + "/banner_" + suffixes[i] + ".jpg");
            Thumbnails.of(hdFile)
                    .scale(scales[i])
                    .outputFormat("jpg")
                    .outputQuality(0.85f)
                    .toFile(output);

            BufferedImage result = ImageIO.read(output);
            System.out.println("生成 " + suffixes[i] + ": "
                    + result.getWidth() + "x" + result.getHeight()
                    + " (" + output.length() / 1024 + " KB)");
        }

        // WebP 模拟（实际JPEG压缩展示效果）
        File webpSimOutput = new File(RESPONSIVE_DIR + "/banner_compressed.jpg");
        Thumbnails.of(hdFile)
                .scale(0.5)
                .outputFormat("jpg")
                .outputQuality(0.5f)  // 低质量模拟小体积
                .toFile(webpSimOutput);
        System.out.println("高压缩版: " + webpSimOutput.length() / 1024 + " KB (质量50%)");
        System.out.println("响应式图片生成完成！\n");
    }

    /**
     * 场景4：批量重命名输出
     * Thumbnailator 支持批量处理目录中的图片，并用 Rename 策略重命名
     */
    private static void scenario4_batchRename() throws IOException {
        System.out.println("--- 场景4：批量重命名输出 ---");

        // 创建一批测试图片
        String batchInputDir = OUTPUT_DIR + "/batch_input";
        String batchOutputDir = OUTPUT_DIR + "/batch_output";
        new File(batchInputDir).mkdirs();
        new File(batchOutputDir).mkdirs();

        // 创建5张测试图片
        for (int i = 1; i <= 5; i++) {
            BufferedImage img = createColorBlockWithNumber(300, 200, i);
            ImageIO.write(img, "png", new File(batchInputDir + "/photo_" + i + ".png"));
        }
        System.out.println("创建了5张测试图片");

        // 批量处理：从目录读取所有图片，缩小后加 "thumb_" 前缀输出到另一目录
        Thumbnails.of(new File(batchInputDir).listFiles())
                .size(150, 100)
                .outputFormat("jpg")
                .outputQuality(0.85f)
                .toFiles(new File(batchOutputDir), Rename.PREFIX_DOT_THUMBNAIL); // 添加 "thumbnail." 前缀

        File[] outputFiles = new File(batchOutputDir).listFiles();
        System.out.println("批量输出了 " + (outputFiles != null ? outputFiles.length : 0) + " 张缩略图");
        if (outputFiles != null) {
            for (File f : outputFiles) {
                System.out.println("  " + f.getName() + " (" + f.length() / 1024 + " KB)");
            }
        }
        System.out.println("批量重命名完成！");
    }

    // ========================= 工具方法 =========================

    private static void createDirectories(String... dirs) {
        for (String dir : dirs) {
            new File(dir).mkdirs();
        }
    }

    /**
     * 创建商品图（带商品名称和颜色背景）
     */
    private static BufferedImage createProductImage(int width, int height, String name, Color bgColor) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(bgColor);
        g2d.fillRect(0, 0, width, height);
        // 绘制产品轮廓
        g2d.setColor(Color.WHITE);
        int margin = Math.min(width, height) / 8;
        g2d.fillRoundRect(margin, margin, width - 2 * margin, height - 2 * margin, 30, 30);
        // 产品名称
        g2d.setColor(bgColor.darker());
        g2d.setFont(new Font("Arial", Font.BOLD, Math.min(width, height) / 10));
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(name, (width - fm.stringWidth(name)) / 2, height / 2 + fm.getAscent() / 2);
        g2d.dispose();
        return image;
    }

    /**
     * 创建文字水印图片
     */
    private static BufferedImage createTextWatermark(String text, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(new Color(0, 0, 0, 120));
        g2d.fillRoundRect(0, 0, width, height, 8, 8);
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.setFont(new Font("Arial", Font.BOLD, height / 3));
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(text, (width - fm.stringWidth(text)) / 2,
                (height + fm.getAscent() - fm.getDescent()) / 2);
        g2d.dispose();
        return image;
    }

    /**
     * 创建头像示例图片
     */
    private static BufferedImage createAvatarImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // 渐变背景
        GradientPaint gp = new GradientPaint(0, 0, new Color(100, 180, 255),
                width, height, new Color(255, 100, 100));
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, width, height);
        // 人物轮廓（简单几何）
        g2d.setColor(new Color(255, 220, 180));
        g2d.fillOval(width / 3, height / 8, width / 3, height / 3); // 头部
        g2d.fillRoundRect(width / 5, height / 2, 3 * width / 5, height / 2, 30, 30); // 身体
        g2d.dispose();
        return image;
    }

    /**
     * 将方形图片转为圆形（带透明通道）
     */
    private static BufferedImage makeCircularAvatar(BufferedImage square) {
        int size = Math.min(square.getWidth(), square.getHeight());
        BufferedImage circle = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = circle.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // 使用圆形裁剪区域
        g2d.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, size, size));
        g2d.drawImage(square, 0, 0, size, size, null);
        // 绘制圆形边框
        g2d.setClip(null);
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawOval(1, 1, size - 3, size - 3);
        g2d.dispose();
        return circle;
    }

    /**
     * 创建横幅图片
     */
    private static BufferedImage createBannerImage(int width, int height, String title) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        GradientPaint gp = new GradientPaint(0, 0, new Color(20, 20, 60),
                width, height, new Color(60, 100, 180));
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, width, height);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 80));
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, (width - fm.stringWidth(title)) / 2, height / 2 + fm.getAscent() / 2);
        g2d.dispose();
        return image;
    }

    /**
     * 创建带数字的色块图片（用于批量测试）
     */
    private static BufferedImage createColorBlockWithNumber(int width, int height, int number) {
        Color[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.MAGENTA};
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(colors[(number - 1) % colors.length]);
        g2d.fillRect(0, 0, width, height);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 60));
        String text = String.valueOf(number);
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(text, (width - fm.stringWidth(text)) / 2, height / 2 + fm.getAscent() / 2);
        g2d.dispose();
        return image;
    }
}
