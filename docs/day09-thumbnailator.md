# Day 09: Thumbnailator - Java 图片处理库

## 简介

**Thumbnailator** 是一个简洁优雅的 Java 缩略图/图片处理库，由 coobird 开发。与直接使用 Java AWT/2D API 相比，Thumbnailator 提供了流式的链式 API，让图片处理代码更加简洁，同时内部使用高质量的缩放算法（渐进式双线性缩放），保证了良好的图片质量。

| 属性 | 详情 |
|------|------|
| **GitHub** | https://github.com/coobird/thumbnailator |
| **Maven Central** | https://central.sonatype.com/artifact/net.coobird/thumbnailator |
| **最新版本** | 0.4.20 |
| **Star 数** | 5.0k+ |
| **许可证** | MIT |
| **Java 兼容性** | Java 8+ |

## 核心特性

- ✅ 链式流式 API，代码极简
- ✅ 高质量渐进式双线性缩放算法
- ✅ 支持缩略图生成、旋转、裁剪、水印
- ✅ 支持批量处理（目录/文件列表）
- ✅ 支持 InputStream/OutputStream（适合 Web 场景）
- ✅ 支持 BufferedImage 直接操作
- ✅ 支持格式转换（PNG/JPEG/GIF/BMP 等）
- ✅ Canvas Filter（画布填充，统一尺寸背景色）

## Maven 依赖配置

```xml
<dependency>
    <groupId>net.coobird</groupId>
    <artifactId>thumbnailator</artifactId>
    <version>0.4.20</version>
</dependency>
```

> **注意**：Thumbnailator 无传递依赖，只依赖 JDK 内置的 AWT/ImageIO，pom 极干净。

## 基础用法

### 生成缩略图

```java
// 生成固定尺寸缩略图（保持宽高比，不超过指定尺寸）
Thumbnails.of("input.jpg")
    .size(200, 200)
    .toFile("thumb.jpg");

// 按比例缩放
Thumbnails.of("input.png")
    .scale(0.5)
    .toFile("half.png");

// 强制拉伸到指定尺寸（忽略宽高比）
Thumbnails.of("input.jpg")
    .forceSize(200, 200)
    .toFile("force.jpg");
```

### 旋转图片

```java
// 顺时针旋转 90 度
Thumbnails.of("input.jpg")
    .scale(1.0)
    .rotate(90)
    .toFile("rotated.jpg");
```

### 裁剪图片

```java
import net.coobird.thumbnailator.geometry.Positions;

// 从中心裁剪为 300x300 正方形
Thumbnails.of("input.jpg")
    .size(300, 300)
    .crop(Positions.CENTER)
    .toFile("cropped.jpg");
```

### 添加水印

```java
// 在右下角添加图片水印，透明度 70%
BufferedImage watermark = ImageIO.read(new File("logo.png"));
Thumbnails.of("input.jpg")
    .scale(1.0)
    .watermark(Positions.BOTTOM_RIGHT, watermark, 0.7f)
    .toFile("watermarked.jpg");
```

### 格式转换

```java
// PNG → JPEG，质量 85%
Thumbnails.of("input.png")
    .scale(1.0)
    .outputFormat("jpg")
    .outputQuality(0.85f)
    .toFile("output.jpg");  // 注意：如果 toFile 有扩展名，outputFormat 优先
```

### 批量处理

```java
// 批量处理目录中所有图片
File[] inputFiles = new File("images/").listFiles();
Thumbnails.of(inputFiles)
    .size(200, 200)
    .toFiles(new File("thumbs/"), Rename.PREFIX_DOT_THUMBNAIL);
    // 输出：thumbs/thumbnail.photo1.jpg, thumbs/thumbnail.photo2.jpg ...
```

### InputStream / OutputStream 操作

```java
// 适合 Spring MVC / Spring Boot 中处理上传文件
InputStream inputStream = request.getInputStream();
OutputStream outputStream = response.getOutputStream();

Thumbnails.of(inputStream)
    .size(800, 600)
    .outputFormat("jpg")
    .outputQuality(0.9f)
    .toOutputStream(outputStream);
```

### Canvas 画布填充

```java
import net.coobird.thumbnailator.filters.Canvas;

// 将任意比例图片填充为 300x300，不足区域白色补充
Thumbnails.of("input.png")
    .size(300, 300)
    .addFilter(new Canvas(300, 300, Positions.CENTER, Color.WHITE))
    .toFile("canvas_filled.jpg");
```

## Spring Boot 集成方式

### 1. 添加依赖（无需额外 starter）

```xml
<dependency>
    <groupId>net.coobird</groupId>
    <artifactId>thumbnailator</artifactId>
    <version>0.4.20</version>
</dependency>
```

### 2. 图片上传处理服务

```java
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;

@Service
public class ImageProcessingService {

    private static final int MAX_WIDTH = 1920;
    private static final int MAX_HEIGHT = 1080;
    private static final float JPEG_QUALITY = 0.85f;

    /**
     * 处理用户上传的图片：限制最大尺寸并压缩
     */
    public byte[] processUploadedImage(MultipartFile file) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Thumbnails.of(file.getInputStream())
                .size(MAX_WIDTH, MAX_HEIGHT)       // 限制最大尺寸
                .outputFormat("jpg")
                .outputQuality(JPEG_QUALITY)
                .toOutputStream(baos);
        return baos.toByteArray();
    }

    /**
     * 生成头像缩略图（多尺寸）
     */
    public void generateAvatarThumbnails(InputStream inputStream, String userId,
                                          Path outputDir) throws IOException {
        // 先将输入流转为字节（因为 InputStream 只能读一次）
        byte[] imageData = inputStream.readAllBytes();

        int[][] sizes = {{200, 200}, {100, 100}, {50, 50}};
        String[] names = {"large", "medium", "small"};

        for (int i = 0; i < sizes.length; i++) {
            Path output = outputDir.resolve(userId + "_" + names[i] + ".jpg");
            Thumbnails.of(new ByteArrayInputStream(imageData))
                    .size(sizes[i][0], sizes[i][1])
                    .crop(Positions.CENTER)
                    .outputFormat("jpg")
                    .outputQuality(0.9f)
                    .toFile(output.toFile());
        }
    }

    /**
     * 添加水印
     */
    public byte[] addWatermark(InputStream inputStream,
                                java.awt.image.BufferedImage watermark) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Thumbnails.of(inputStream)
                .scale(1.0)
                .watermark(Positions.BOTTOM_RIGHT, watermark, 0.6f)
                .outputFormat("jpg")
                .outputQuality(0.9f)
                .toOutputStream(baos);
        return baos.toByteArray();
    }
}
```

### 3. REST Controller 示例

```java
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final ImageProcessingService imageService;

    public ImageController(ImageProcessingService imageService) {
        this.imageService = imageService;
    }

    /**
     * 上传并压缩图片，直接返回处理后的字节流
     */
    @PostMapping(value = "/compress", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> compressImage(@RequestParam("file") MultipartFile file) 
            throws Exception {
        byte[] result = imageService.processUploadedImage(file);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(result);
    }
}
```

### 4. 配置类（可选，管理水印图片）

```java
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

@Configuration
public class ImageConfig {

    @Bean
    public BufferedImage watermarkImage() throws Exception {
        // 从 classpath 加载水印图片（放在 resources/images/watermark.png）
        InputStream is = getClass().getResourceAsStream("/images/watermark.png");
        return ImageIO.read(is);
    }
}
```

## 注意事项

### Bug 风险

1. **透明PNG转JPEG变黑问题**
   - **现象**：PNG 图片有透明通道（ARGB），转 JPEG 时透明区域变黑
   - **原因**：JPEG 不支持 Alpha 通道，透明像素被填充为黑色
   - **解决方案**：转换前先用白色背景填充透明区域：
   ```java
   // 方案1：使用 Canvas filter
   Thumbnails.of("transparent.png")
       .size(300, 300)
       .addFilter(new Canvas(300, 300, Positions.CENTER, Color.WHITE))
       .outputFormat("jpg")
       .toFile("output.jpg");
   
   // 方案2：手动替换透明背景
   BufferedImage source = ImageIO.read(new File("transparent.png"));
   BufferedImage background = new BufferedImage(source.getWidth(), source.getHeight(),
       BufferedImage.TYPE_INT_RGB);
   Graphics2D g = background.createGraphics();
   g.setColor(Color.WHITE);
   g.fillRect(0, 0, source.getWidth(), source.getHeight());
   g.drawImage(source, 0, 0, null);
   g.dispose();
   Thumbnails.of(background).outputFormat("jpg").toFile("output.jpg");
   ```

2. **大文件内存溢出（OOM）**
   - **现象**：处理超大图片（如 30MB+ JPEG）时可能 OOM
   - **原因**：Thumbnailator 将整张图片加载到内存
   - **解决方案**：对超大图片，先用流式读取+分块处理，或限制上传文件大小
   ```yaml
   # Spring Boot 配置限制上传大小
   spring:
     servlet:
       multipart:
         max-file-size: 10MB
         max-request-size: 10MB
   ```

3. **InputStream 只能读一次**
   - **现象**：同一个 InputStream 不能生成多种尺寸的缩略图
   - **解决方案**：先转为字节数组，再多次包装为 `ByteArrayInputStream`
   ```java
   byte[] data = inputStream.readAllBytes();
   // 然后多次使用 new ByteArrayInputStream(data)
   ```

### 性能问题

1. **高质量算法比较慢**
   - Thumbnailator 默认使用 `PROGRESSIVE_BILINEAR`（渐进式双线性），质量高但较慢
   - 对性能敏感的场景，可以在保存前 size 时减少中间步骤
   - 批量处理时建议使用线程池异步处理

2. **JPEG 输出质量与文件大小的平衡**
   - `outputQuality(1.0f)` → 最高质量，文件最大
   - `outputQuality(0.85f)` → 推荐值（视觉无损，体积减少30-50%）
   - `outputQuality(0.7f)` → 明显压缩，适合缩略图展示
   - `outputQuality(0.5f)` → 质量明显下降

3. **批量处理建议**
   - 使用 `Thumbnails.of(File[])` 而不是循环调用单个，可减少初始化开销
   - Web 场景建议异步处理（`@Async` + 线程池）

### 使用限制

1. **不支持 WebP 格式**（Java 标准 ImageIO 不支持），需额外引入 `imageio-webp` 插件：
   ```xml
   <dependency>
       <groupId>com.twelvemonkeys.imageio</groupId>
       <artifactId>imageio-webp</artifactId>
       <version>3.10.1</version>
   </dependency>
   ```

2. **没有原生的文字水印功能**，需要自行用 AWT 创建文字水印图片再传入

3. **不支持 SVG、PDF、视频帧** 等非 bitmap 格式

4. **GIF 动图**：只处理第一帧，不支持保留动画

5. **CMYK 颜色模式的 JPEG**：部分 CMYK JPEG 文件读取后颜色可能异常，需要特殊处理

## 运行方法

### 环境要求

- Java 8+
- Maven 3.6+

### 编译运行

```bash
cd thumbnailator-demo

# 编译
mvn clean package -DskipTests

# 运行基础演示（生成示例图片并处理）
mvn exec:java -Dexec.mainClass="com.example.thumbnailator.ThumbnailatorBasicDemo"

# 运行高级演示（水印、格式转换、流式操作）
mvn exec:java -Dexec.mainClass="com.example.thumbnailator.ThumbnailatorAdvancedDemo"

# 运行实战演示（电商批量、头像、响应式图片）
mvn exec:java -Dexec.mainClass="com.example.thumbnailator.ThumbnailatorPracticalDemo"
```

### 输出目录

运行后会在当前目录下生成 `thumbnailator-output/` 文件夹，包含：

```
thumbnailator-output/
├── sample.png                    # 原始示例图（800x600）
├── thumb_200x150.png             # 200x150 缩略图
├── thumb_200x150_force.png       # 强制拉伸的缩略图
├── scale_50percent.png           # 50% 缩放
├── rotate_90.png                 # 旋转90度
├── crop_center.png               # 中心裁剪
├── watermark_image.png           # 带水印图片
├── converted.jpg                 # PNG转JPEG
├── from_stream.jpg               # 流式操作输出
├── canvas_fill.png               # Canvas填充
├── ecommerce/                    # 电商场景
│   ├── product_raw_*.png         # 原始商品图
│   └── product_final_*.jpg       # 处理后商品图（带水印）
├── avatars/                      # 头像场景
│   ├── avatar_large_square.png
│   ├── avatar_large_circle.png
│   └── ...
├── responsive/                   # 响应式图片
│   ├── banner_2x.png             # 1600x900 高清图
│   ├── banner_1x.jpg             # 800x450
│   └── banner_0.5x.jpg           # 400x225
└── batch_output/                 # 批量处理输出
    └── thumbnail.photo_*.jpg
```

## 与同类库对比

| 特性 | Thumbnailator | imgscalr | AWT/Graphics2D | ImageMagick |
|------|:---:|:---:|:---:|:---:|
| API 简洁度 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐ |
| 图片质量 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 功能丰富度 | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 无外部依赖 | ✅ | ✅ | ✅ | ❌（需安装） |
| WebP支持 | ❌ | ❌ | ❌ | ✅ |
| 批量处理 | ✅ | ❌ | ❌ | ✅ |
| 推荐场景 | Web图片处理 | 高质量缩放 | 简单需求 | 专业图像处理 |
