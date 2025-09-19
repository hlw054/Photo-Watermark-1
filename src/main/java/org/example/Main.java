package org.example;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.file.FileSystemDirectory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * 图片EXIF时间水印工具
 * 支持读取图片EXIF信息中的拍摄时间，并添加时间水印
 */
public class Main {
    
    private static final Scanner scanner = new Scanner(System.in);
    private static final String[] SUPPORTED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".tiff", ".tif"};
    
    // 水印位置枚举
    public enum WatermarkPosition {
        TOP_LEFT(1, "左上"),
        TOP_CENTER(2, "中上"), 
        TOP_RIGHT(3, "右上"),
        MIDDLE_LEFT(4, "左中"),
        CENTER(5, "居中"),
        MIDDLE_RIGHT(6, "右中"),
        BOTTOM_LEFT(7, "左下"),
        BOTTOM_CENTER(8, "中下"),
        BOTTOM_RIGHT(9, "右下");
        
        private final int code;
        private final String description;
        
        WatermarkPosition(int code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public static WatermarkPosition fromCode(int code) {
            for (WatermarkPosition pos : values()) {
                if (pos.code == code) {
                    return pos;
                }
            }
            return CENTER; // 默认居中
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== 图片EXIF时间水印工具 ===");
        System.out.println();
        
        try {
            // 1. 获取输入路径
            String inputPath = getInputPath();
            Path path = Paths.get(inputPath);
            
            if (!Files.exists(path)) {
                System.out.println("错误：路径不存在！");
                return;
            }
            
            // 2. 获取图片文件列表
            List<File> imageFiles = getImageFiles(path);
            if (imageFiles.isEmpty()) {
                System.out.println("未找到支持的图片文件！");
                return;
            }
            
            System.out.println("找到 " + imageFiles.size() + " 个图片文件");
            
            // 3. 读取第一张图片的EXIF时间作为示例
            String sampleDate = getImageDate(imageFiles.get(0));
            System.out.println("已经完成读取年月日：" + sampleDate);
            System.out.println();
            
            // 4. 获取水印配置
            WatermarkConfig config = getWatermarkConfig();
            
            // 5. 处理图片
            processImages(imageFiles, config, path);
            
        } catch (Exception e) {
            System.err.println("程序执行出错：" + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
    
    /**
     * 获取用户输入的路径
     */
    private static String getInputPath() {
        System.out.print("请输入图片文件路径: ");
        return scanner.nextLine().trim();
    }
    
    /**
     * 获取指定路径下的所有图片文件
     */
    private static List<File> getImageFiles(Path path) throws IOException {
        List<File> imageFiles = new ArrayList<>();
        
        if (Files.isDirectory(path)) {
            // 处理目录
            Files.walk(path)
                .filter(Files::isRegularFile)
                .forEach(filePath -> {
                    String fileName = filePath.getFileName().toString().toLowerCase();
                    for (String ext : SUPPORTED_EXTENSIONS) {
                        if (fileName.endsWith(ext)) {
                            imageFiles.add(filePath.toFile());
                            break;
                        }
                    }
                });
        } else {
            // 处理单个文件
            String fileName = path.getFileName().toString().toLowerCase();
            for (String ext : SUPPORTED_EXTENSIONS) {
                if (fileName.endsWith(ext)) {
                    imageFiles.add(path.toFile());
                    break;
                }
            }
        }
        
        return imageFiles;
    }
    
    /**
     * 从图片文件中读取拍摄时间
     */
    private static String getImageDate(File imageFile) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(imageFile);
            
            // 尝试从EXIF数据中获取拍摄时间
            ExifSubIFDDirectory exifDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (exifDirectory != null) {
                Date date = exifDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                if (date == null) {
                    date = exifDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME);
                }
                if (date != null) {
                    return formatDate(date);
                }
            }
            
            // 如果EXIF中没有时间，使用文件修改时间
            FileSystemDirectory fileDirectory = metadata.getFirstDirectoryOfType(FileSystemDirectory.class);
            if (fileDirectory != null) {
                Date date = fileDirectory.getDate(FileSystemDirectory.TAG_FILE_MODIFIED_DATE);
                if (date != null) {
                    return formatDate(date);
                }
            }
            
            // 最后使用文件系统时间
            return formatDate(new Date(imageFile.lastModified()));
            
        } catch (Exception e) {
            // 如果读取EXIF失败，使用文件修改时间
            return formatDate(new Date(imageFile.lastModified()));
        }
    }
    
    /**
     * 格式化日期为"yyyy年MM月dd日"
     */
    private static String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日");
        return sdf.format(date);
    }
    
    /**
     * 获取水印配置
     */
    private static WatermarkConfig getWatermarkConfig() {
        System.out.println("请设置水印参数:");
        
        // 字体大小
        System.out.print("1. 字体大小 (默认24): ");
        String fontSizeInput = scanner.nextLine().trim();
        int fontSize = 24;
        if (!fontSizeInput.isEmpty()) {
            try {
                fontSize = Integer.parseInt(fontSizeInput);
            } catch (NumberFormatException e) {
                System.out.println("输入无效，使用默认值24");
            }
        }
        
        // 字体颜色
        System.out.print("2. 字体颜色 (默认black): ");
        String colorInput = scanner.nextLine().trim();
        Color fontColor = parseColor(colorInput.isEmpty() ? "black" : colorInput);
        
        // 水印位置
        System.out.println("3. 水印位置 (1-9):");
        System.out.println("   1) 左上  2) 中上  3) 右上");
        System.out.println("   4) 左中  5) 居中  6) 右中");
        System.out.println("   7) 左下  8) 中下  9) 右下");
        System.out.print("选择: ");
        
        int positionCode = 5; // 默认居中
        try {
            String positionInput = scanner.nextLine().trim();
            if (!positionInput.isEmpty()) {
                positionCode = Integer.parseInt(positionInput);
            }
        } catch (NumberFormatException e) {
            System.out.println("输入无效，使用默认位置：居中");
        }
        
        WatermarkPosition position = WatermarkPosition.fromCode(positionCode);
        
        return new WatermarkConfig(fontSize, fontColor, position);
    }
    
    /**
     * 解析颜色字符串
     */
    private static Color parseColor(String colorStr) {
        colorStr = colorStr.toLowerCase().trim();
        
        // 预定义颜色
        switch (colorStr) {
            case "black": return Color.BLACK;
            case "white": return Color.WHITE;
            case "red": return Color.RED;
            case "green": return Color.GREEN;
            case "blue": return Color.BLUE;
            case "yellow": return Color.YELLOW;
            case "cyan": return Color.CYAN;
            case "magenta": return Color.MAGENTA;
            case "gray": return Color.GRAY;
            case "darkgray": return Color.DARK_GRAY;
            case "lightgray": return Color.LIGHT_GRAY;
        }
        
        // 尝试解析RGB值 (格式: rgb(r,g,b) 或 r,g,b)
        if (colorStr.startsWith("rgb(") && colorStr.endsWith(")")) {
            colorStr = colorStr.substring(4, colorStr.length() - 1);
        }
        
        if (colorStr.contains(",")) {
            try {
                String[] parts = colorStr.split(",");
                if (parts.length == 3) {
                    int r = Integer.parseInt(parts[0].trim());
                    int g = Integer.parseInt(parts[1].trim());
                    int b = Integer.parseInt(parts[2].trim());
                    return new Color(r, g, b);
                }
            } catch (NumberFormatException e) {
                // 解析失败，使用默认颜色
            }
        }
        
        return Color.BLACK; // 默认黑色
    }
    
    /**
     * 处理所有图片
     */
    private static void processImages(List<File> imageFiles, WatermarkConfig config, Path originalPath) throws IOException {
        // 创建输出目录
        String outputDirName = originalPath.getFileName().toString() + "_watermark";
        Path outputDir = originalPath.getParent().resolve(outputDirName);
        Files.createDirectories(outputDir);
        
        System.out.println();
        System.out.println("正在处理...");
        
        int successCount = 0;
        int failCount = 0;
        
        for (File imageFile : imageFiles) {
            try {
                String dateStr = getImageDate(imageFile);
                System.out.print(imageFile.getName() + " - " + dateStr + " - ");
                
                // 添加水印
                BufferedImage watermarkedImage = addWatermark(imageFile, dateStr, config);
                
                // 保存图片
                String outputFileName = getOutputFileName(imageFile.getName());
                File outputFile = outputDir.resolve(outputFileName).toFile();
                
                String format = getImageFormat(imageFile.getName());
                ImageIO.write(watermarkedImage, format, outputFile);
                
                System.out.println("完成");
                successCount++;
                
            } catch (Exception e) {
                System.out.println("失败: " + e.getMessage());
                failCount++;
            }
        }
        
        System.out.println();
        System.out.println("全部完成! 水印图片已保存到 " + outputDir.toString());
        System.out.println("成功: " + successCount + " 张，失败: " + failCount + " 张");
    }
    
    /**
     * 为图片添加水印
     */
    private static BufferedImage addWatermark(File imageFile, String watermarkText, WatermarkConfig config) throws IOException {
        BufferedImage originalImage = ImageIO.read(imageFile);
        BufferedImage watermarkedImage = new BufferedImage(
            originalImage.getWidth(), 
            originalImage.getHeight(), 
            originalImage.getType()
        );
        
        Graphics2D g2d = watermarkedImage.createGraphics();
        
        // 绘制原图
        g2d.drawImage(originalImage, 0, 0, null);
        
        // 设置字体和颜色
        Font font = new Font("SimSun", Font.BOLD, config.fontSize);
        g2d.setFont(font);
        g2d.setColor(config.fontColor);
        
        // 设置抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // 计算水印位置
        FontMetrics fontMetrics = g2d.getFontMetrics();
        int textWidth = fontMetrics.stringWidth(watermarkText);
        int textHeight = fontMetrics.getHeight();
        
        Point position = calculateWatermarkPosition(
            originalImage.getWidth(), 
            originalImage.getHeight(), 
            textWidth, 
            textHeight, 
            config.position
        );
        
        // 添加阴影效果提高可读性
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.drawString(watermarkText, position.x + 2, position.y + 2);
        
        // 绘制水印文字
        g2d.setColor(config.fontColor);
        g2d.drawString(watermarkText, position.x, position.y);
        
        g2d.dispose();
        
        return watermarkedImage;
    }
    
    /**
     * 计算水印位置
     */
    private static Point calculateWatermarkPosition(int imageWidth, int imageHeight, int textWidth, int textHeight, WatermarkPosition position) {
        int x, y;
        int margin = 20; // 边距
        
        switch (position) {
            case TOP_LEFT:
                x = margin;
                y = textHeight + margin;
                break;
            case TOP_CENTER:
                x = (imageWidth - textWidth) / 2;
                y = textHeight + margin;
                break;
            case TOP_RIGHT:
                x = imageWidth - textWidth - margin;
                y = textHeight + margin;
                break;
            case MIDDLE_LEFT:
                x = margin;
                y = (imageHeight + textHeight) / 2;
                break;
            case CENTER:
                x = (imageWidth - textWidth) / 2;
                y = (imageHeight + textHeight) / 2;
                break;
            case MIDDLE_RIGHT:
                x = imageWidth - textWidth - margin;
                y = (imageHeight + textHeight) / 2;
                break;
            case BOTTOM_LEFT:
                x = margin;
                y = imageHeight - margin;
                break;
            case BOTTOM_CENTER:
                x = (imageWidth - textWidth) / 2;
                y = imageHeight - margin;
                break;
            case BOTTOM_RIGHT:
                x = imageWidth - textWidth - margin;
                y = imageHeight - margin;
                break;
            default:
                x = (imageWidth - textWidth) / 2;
                y = (imageHeight + textHeight) / 2;
        }
        
        return new Point(x, y);
    }
    
    /**
     * 获取输出文件名
     */
    private static String getOutputFileName(String originalFileName) {
        int lastDotIndex = originalFileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            String nameWithoutExt = originalFileName.substring(0, lastDotIndex);
            String extension = originalFileName.substring(lastDotIndex);
            return nameWithoutExt + "_wm" + extension;
        }
        return originalFileName + "_wm";
    }
    
    /**
     * 获取图片格式
     */
    private static String getImageFormat(String fileName) {
        String lowerCase = fileName.toLowerCase();
        if (lowerCase.endsWith(".png")) {
            return "png";
        } else if (lowerCase.endsWith(".tiff") || lowerCase.endsWith(".tif")) {
            return "tiff";
        } else {
            return "jpg"; // 默认JPG格式
        }
    }
    
    /**
     * 水印配置类
     */
    private static class WatermarkConfig {
        final int fontSize;
        final Color fontColor;
        final WatermarkPosition position;
        
        WatermarkConfig(int fontSize, Color fontColor, WatermarkPosition position) {
            this.fontSize = fontSize;
            this.fontColor = fontColor;
            this.position = position;
        }
    }
}
