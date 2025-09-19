## 图片 EXIF 时间水印工具（Java 命令行）

一个简单易用的 Java 命令行工具：读取图片 EXIF 中的拍摄时间，将其以可配置的文字水印绘制到图片上，并保存到新目录。

### 功能特性
- 交互式命令行，支持输入单个文件或目录
- 自动读取 EXIF 拍摄时间（DateTimeOriginal/CreateDate），无 EXIF 时回退为文件修改时间
- 时间格式：yyyy年MM月dd日
- 可配置水印样式：字体大小、颜色、九宫格位置
- Java 2D 绘制文字水印，带阴影增强可读性
- 保持原始图片格式与质量（jpg/png/tiff）
- 输出到“原目录名_watermark”子目录，文件名追加“_wm”后缀

### 技术栈
- Java 8+
- Maven
- metadata-extractor（读取 EXIF 元数据）
- Java 2D（绘制水印文本）

### 环境要求
- JDK 8 或以上
- Maven 3.x
- Windows PowerShell、CMD 或其他终端

### 快速开始
1) 克隆或下载本项目
2) 安装依赖并编译

```bash
mvn compile
```

3) 运行程序（推荐在项目根目录执行）

PowerShell（注意：不要使用 `&&` 连接命令）：
```powershell
mvn compile
java -cp target/classes org.example.Main
```

CMD：
```cmd
mvn compile
java -cp target/classes org.example.Main
```

如需使用 Maven 插件运行，可在 `pom.xml` 配置 exec 插件后再使用，但本项目默认不强制引入，直接使用 `java -cp` 更简单。

### 交互流程示例
```
=== 图片EXIF时间水印工具 ===

请输入图片文件路径: C:\Users\me\Pictures
找到 12 个图片文件
已经完成读取年月日：2023年05月12日

请设置水印参数:
1. 字体大小 (默认24): 36
2. 字体颜色 (默认black): white
3. 水印位置 (1-9):
   1) 左上  2) 中上  3) 右上
   4) 左中  5) 居中  6) 右中
   7) 左下  8) 中下  9) 右下
选择: 9

正在处理...
photo1.jpg - 2023年05月12日 - 完成
photo2.jpg - 2023年05月12日 - 完成
...

全部完成! 水印图片已保存到 C:\Users\me\Pictures_watermark
成功: 12 张，失败: 0 张
```

### 使用说明
- 支持的图片扩展名：`.jpg`, `.jpeg`, `.png`, `.tiff`, `.tif`
- 颜色输入：
  - 英文颜色名（black、white、red、green、blue、yellow、cyan、magenta、gray、darkgray、lightgray）
  - 或 RGB 值：`r,g,b` 或 `rgb(r,g,b)`（0-255）
- 九宫格位置编号：
  - 1 左上，2 中上，3 右上
  - 4 左中，5 居中，6 右中
  - 7 左下，8 中下，9 右下

### 输出规则
- 在输入路径所在目录创建 `原目录名_watermark` 子目录（若输入为文件，则在该文件所在目录创建 `所在目录名_watermark`）
- 输出文件与原文件同名，追加 `_wm` 后缀，例如：`IMG_0001.jpg` -> `IMG_0001_wm.jpg`
- 图片格式保持与原文件一致

### 常见问题（FAQ）
- Q: 运行时提示 “No line found” 或程序直接退出？
  - A: 这是在非交互环境下运行或输入被中断导致。请在交互式终端中运行，并在提示后输入有效路径与参数。

- Q: PowerShell 里命令用 `&&` 连接失败？
  - A: PowerShell 不支持 `&&` 作为命令分隔符，请分两行执行。

- Q: 没有 EXIF 时间怎么办？
  - A: 程序会自动回退到文件系统的“最后修改时间”。

- Q: 中文字体显示异常或字体不理想？
  - A: 程序默认使用 `SimSun`（宋体）。如需调整，可修改 `Main` 中的字体名称，或使用系统存在的其他中文字体。

- Q: 目录里包含大量非图片文件？
  - A: 程序只处理指定扩展名的图片文件，其他文件会被忽略。

### 项目结构
```
Photo-Watermark-1/
├─ pom.xml
└─ src/
   └─ main/
      └─ java/
         └─ org/
            └─ example/
               └─ Main.java
```

### 开发说明（实现要点）
- 使用 `java.nio.file` 处理路径与遍历
- 使用 `metadata-extractor` 解析 EXIF，优先取 `DateTimeOriginal`，其次 `DateTime`
- 格式化时间为 `yyyy年MM月dd日`
- 使用 Java 2D 绘制文字，带阴影（半透明黑色偏移）增强可读性
- 依据九宫格位置计算文本绘制坐标，并设置边距

### 许可证
本项目仅用于学习与演示用途，按需修改和使用。


