# Day 06 - ZXing：二维码/条形码处理库

## 工具简介

**ZXing**（发音 "Zebra Crossing"，斑马线）是 Google 开源的多格式 1D/2D 条码图像处理库，是 Java 生态中使用最广泛的二维码库。

| 属性 | 详情 |
|------|------|
| **GitHub** | https://github.com/zxing/zxing |
| **Stars** | 32k+ |
| **最新版本** | 3.5.3（2023） |
| **许可证** | Apache 2.0 |
| **支持格式** | QR Code、Data Matrix、PDF417、Aztec、EAN-13、UPC-A、Code 128、Code 39 等 20+ 种 |

### 核心模块

| 模块 | 说明 |
|------|------|
| `core` | 纯 Java 编解码引擎，无任何外部依赖 |
| `javase` | Java SE 扩展，提供 `MatrixToImageWriter`、图片 IO 工具 |
| `android` | Android 平台扩展（移动端扫码） |
| `android-integration` | 与 ZXing Android 应用集成的意图工具 |

---

## Maven 依赖配置

```xml
<!-- ZXing 核心编解码引擎 -->
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>core</artifactId>
    <version>3.5.3</version>
</dependency>

<!-- Java SE 扩展：MatrixToImageWriter 等图片工具 -->
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>javase</artifactId>
    <version>3.5.3</version>
</dependency>
```

> **说明**：服务端只需引入以上两个依赖即可。`javase` 内部依赖 `core`，实际上引入 `javase` 即可，但建议显式声明 `core` 以便版本管理。

---

## 核心 API 速查

### 编码（生成二维码）

```java
// 1. 创建编码器
QRCodeWriter writer = new QRCodeWriter();
// 或通用入口（支持所有格式）：
MultiFormatWriter writer = new MultiFormatWriter();

// 2. 设置编码参数
Map<EncodeHintType, Object> hints = new HashMap<>();
hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M); // L/M/Q/H
hints.put(EncodeHintType.MARGIN, 1); // 安静区宽度（建议 ≥ 1）

// 3. 生成 BitMatrix（二维点阵）
BitMatrix bitMatrix = writer.encode("内容", BarcodeFormat.QR_CODE, 300, 300, hints);

// 4. 输出到文件
MatrixToImageWriter.writeToPath(bitMatrix, "PNG", Paths.get("qrcode.png"));

// 4b. 输出到流（Web 场景）
MatrixToImageWriter.writeToStream(bitMatrix, "PNG", response.getOutputStream());

// 4c. 输出到 BufferedImage（二次处理）
BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);
```

### 解码（识别二维码）

```java
// 读取图片
BufferedImage image = ImageIO.read(new File("qrcode.png"));
LuminanceSource source = new BufferedImageLuminanceSource(image);
BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

// 解码参数
Map<DecodeHintType, Object> hints = new HashMap<>();
hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE); // 尝试更难的解码

// 解码
Result result = new MultiFormatReader().decode(bitmap, hints);
System.out.println(result.getText());           // 内容
System.out.println(result.getBarcodeFormat()); // 格式（如 QR_CODE）
```

### 纠错级别对比

| 级别 | 可恢复比例 | 适用场景 |
|------|-----------|---------|
| L（Low） | 7% | 干净环境，追求码密度最小 |
| M（Medium） | 15% | **默认推荐**，平衡密度与容错 |
| Q（Quartile） | 25% | 可能有轻微污损 |
| H（High） | 30% | **带 Logo 必选**，印刷品/户外 |

---

## Spring Boot 集成

### 1. 添加依赖（pom.xml）

```xml
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>core</artifactId>
    <version>3.5.3</version>
</dependency>
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>javase</artifactId>
    <version>3.5.3</version>
</dependency>
```

### 2. 封装 Service 组件

```java
@Service
public class QRCodeService {

    /**
     * 生成二维码并直接写入 HttpServletResponse（适合 REST API 返回图片）
     */
    public void generateToResponse(String content, int size, HttpServletResponse response)
            throws Exception {
        response.setContentType("image/png");
        response.setHeader("Cache-Control", "no-store");

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix bitMatrix = new QRCodeWriter()
                .encode(content, BarcodeFormat.QR_CODE, size, size, hints);
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", response.getOutputStream());
    }

    /**
     * 生成二维码并返回 Base64 字符串（适合前端 <img src="data:image/png;base64,..."> 展示）
     */
    public String generateBase64(String content, int size) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix bitMatrix = new QRCodeWriter()
                .encode(content, BarcodeFormat.QR_CODE, size, size, hints);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    /**
     * 解析上传的二维码图片
     */
    public String decode(MultipartFile file) throws Exception {
        BufferedImage image = ImageIO.read(file.getInputStream());
        LuminanceSource source = new BufferedImageLuminanceSource(image);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        Map<DecodeHintType, Object> hints = new HashMap<>();
        hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

        return new MultiFormatReader().decode(bitmap, hints).getText();
    }
}
```

### 3. Controller 层示例

```java
@RestController
@RequestMapping("/api/qrcode")
public class QRCodeController {

    @Autowired
    private QRCodeService qrCodeService;

    /**
     * GET /api/qrcode/generate?content=xxx&size=300
     * 直接返回 PNG 图片流
     */
    @GetMapping("/generate")
    public void generate(
            @RequestParam String content,
            @RequestParam(defaultValue = "300") int size,
            HttpServletResponse response) throws Exception {
        qrCodeService.generateToResponse(content, size, response);
    }

    /**
     * GET /api/qrcode/base64?content=xxx
     * 返回 JSON 包含 Base64 字符串
     */
    @GetMapping("/base64")
    public ResponseEntity<Map<String, String>> generateBase64(
            @RequestParam String content) throws Exception {
        String base64 = qrCodeService.generateBase64(content, 300);
        return ResponseEntity.ok(Collections.singletonMap("data", "data:image/png;base64," + base64));
    }

    /**
     * POST /api/qrcode/decode
     * 上传图片文件，返回解析内容
     */
    @PostMapping("/decode")
    public ResponseEntity<Map<String, String>> decode(
            @RequestParam("file") MultipartFile file) throws Exception {
        String result = qrCodeService.decode(file);
        return ResponseEntity.ok(Collections.singletonMap("content", result));
    }
}
```

### 4. 常见内容格式

```java
// URL
String url = "https://www.example.com";

// vCard 名片
String vcard = "BEGIN:VCARD\nVERSION:2.1\nFN:张三\nTEL:13800138000\nEMAIL:zs@example.com\nEND:VCARD";

// WiFi 连接（iOS 11+ / Android 均支持）
String wifi = "WIFI:T:WPA;S:MyNetwork;P:MyPassword;;";

// 短信（打开短信界面）
String sms = "SMSTO:13800138000:你好";

// 地理位置
String geo = "geo:39.9042,116.4074";  // 北京天安门

// 邮件
String email = "mailto:user@example.com?subject=Hello&body=World";
```

---

## 注意事项

### ⚠️ Bug 风险

**1. 中文乱码问题**
```java
// ❌ 错误：未指定字符集
new QRCodeWriter().encode("中文内容", BarcodeFormat.QR_CODE, 300, 300);

// ✅ 正确：明确指定 UTF-8
Map<EncodeHintType, Object> hints = new HashMap<>();
hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
new QRCodeWriter().encode("中文内容", BarcodeFormat.QR_CODE, 300, 300, hints);
```

**2. Logo 遮挡导致无法识别**
```java
// ❌ 错误：Logo 占比 30%，但用了 M 级纠错（只能恢复 15%）
hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);

// ✅ 正确：Logo 时必须用 H 级（30% 容错），且 Logo 占比不超过 30%
hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
```

**3. 前景色/背景色对比度不足**
```java
// ❌ 避免：浅色前景、深色背景（多数扫描器仅支持深色前景）
new MatrixToImageConfig(0xFFCCCCCC, 0xFF333333); // 可能扫描失败

// ✅ 推荐：深色前景 + 浅色背景（最佳对比）
new MatrixToImageConfig(0xFF000000, 0xFFFFFFFF);
```

**4. 解析时 NotFoundException**
```java
// 常见原因：
// - 图片旋转了 → 开启 TRY_HARDER
// - 图片噪点多 → 先做图片预处理（灰度化、对比度增强）
// - 安静区（白边）被裁剪 → 生成时保留足够 MARGIN

hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
```

### ⚡ 性能问题

| 场景 | 建议 |
|------|------|
| 高并发生成 | `QRCodeWriter` 是线程安全的，可单例复用，无需每次 new |
| 大批量生成 | 尺寸控制在 200-400px，避免生成 2000px+ 的超大图 |
| 识别模糊图片 | 先用 `BufferedImage` 做对比度增强再识别，避免直接解码失败 |
| Base64 响应 | 建议缓存（Redis），相同内容无需重复生成 |

### 📌 使用限制

1. **ZXing 本身不支持彩色 Logo 的二维码**，需要手动用 `Graphics2D` 绘制
2. **不支持微信小程序码**（小程序码需调用微信官方 API）
3. **不支持 PDF417 二维码的汉字优化**（长中文推荐用 QR Code）
4. **服务端无需任何桌面环境**（即使在 Headless Linux 上也能正常运行）
5. **ZXing 已进入维护模式**（2019 年后基本无新功能），项目稳定但不再活跃开发

---

## 演示代码说明

| 类名 | 功能 |
|------|------|
| `ZXingBasicDemo` | 基础：生成 QR Code / 解析 / 生成条形码 |
| `ZXingAdvancedDemo` | 高级：彩色二维码 / 带 Logo / 批量生成 |
| `ZXingPracticalDemo` | 实战：vCard 名片码 / WiFi 码 / 海报 / 多码识别 |

---

## 运行方法

```bash
# 进入项目目录
cd zxing-demo

# 编译打包
mvn clean package -DskipTests

# 运行基础演示（在 zxing-demo/ 目录下执行，output/ 会生成图片）
mvn exec:java -Dexec.mainClass="com.example.zxing.ZXingBasicDemo"

# 运行高级演示
mvn exec:java -Dexec.mainClass="com.example.zxing.ZXingAdvancedDemo"

# 运行实战演示
mvn exec:java -Dexec.mainClass="com.example.zxing.ZXingPracticalDemo"
```

> **输出位置**：生成的图片保存在 `zxing-demo/output/` 目录下。

---

## 相关资源

- [ZXing GitHub](https://github.com/zxing/zxing)
- [ZXing 在线解码工具](https://zxing.org/w/decode.jspx)
- [vCard 标准 RFC 2426](https://www.ietf.org/rfc/rfc2426.txt)
- [WiFi 二维码格式规范](https://github.com/zxing/zxing/wiki/Barcode-Contents#wifi-network-config-android)
