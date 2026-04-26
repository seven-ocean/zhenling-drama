package com.drama.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * ASS (Advanced SubStation Alpha) 字幕文件渲染器
 *
 * <p>生成符合 FFmpeg {@code subtitles} 滤镜标准的 .ass 字幕文件。
 * 相比 drawtext 滤镜的优势：
 * <ul>
 *   <li>✅ 完美支持 CJK 中文（可指定字体文件）</li>
 *   <li>✅ 自动换行 + 多行排版</li>
 *   <li>✅ 丰富样式：描边、阴影、半透明背景条</li>
 *   <li>✅ 精确时间轴控制（精确到厘秒）</li>
 * </ul>
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * Path assFile = subtitleRenderer.generateAssFile(
 *     "你到底想怎么样？",  // 台词文本
 *     0f,                    // 开始时间(秒)
 *     8.3f,                  // 结束时间(秒)
 *     Paths.get("/tmp/sub.ass")
 * );
 * // 然后在FFmpeg命令中使用: -vf "subtitles='/tmp/sub.ass'"
 * }</pre>
 */
@Slf4j
@Component
public class SubtitleRenderer {

    // ========== 配置项（从 application.yml 读取）==========

    /** 字体文件绝对路径（优先级最高，指定后忽略 fontName） */
    @Value("${subtitle.font-file:}")
    private String fontFile;

    /** 回退字体名称（当 fontFile 为空时使用系统字体名） */
    @Value("${subtitle.font-name:Microsoft YaHei}")
    private String fontName;

    /** 字号（基于 PlayResY=1080 的基准分辨率） */
    @Value("${subtitle.font-size:28}")
    private int fontSize;

    /** 字幕位置：bottom(底部) / top(顶部) / center(居中) */
    @Value("${subtitle.position:bottom}")
    private String position;

    /** 主颜色（白色，ASS 格式 BGR + Alpha: &HAABBGGRR） */
    @Value("${subtitle.primary-color:&H00FFFFFF}")
    private String primaryColor;

    /** 描边颜色（深灰色，用于白色文字的轮廓线） */
    @Value("${subtitle.outline-color:&H000000FF}")
    private String outlineColor;

    /** 背景色（半透明黑色，用于背景条） */
    @Value("${subtitle.back-color:&H80000000}")
    private String backColor;

    /** 描边宽度 */
    @Value("${subtitle.outline-width:2}")
    private int outlineWidth;

    /** 阴影深度 */
    @Value("${subtitle.shadow-depth:2}")
    private int shadowDepth;

    /** 底部边距（像素） */
    @Value("${subtitle.margin-v:40}")
    private int marginV;

    /** 基准分辨率宽度 */
    private static final int RES_X = 1920;
    /** 基准分辨率高度 */
    private static final int RES_Y = 1080;

    // ====================================================================
    // 初始化校验
    // ====================================================================

    @PostConstruct
    public void validateFontConfig() {
        if (fontFile != null && !fontFile.isBlank()) {
            if (!Files.exists(Path.of(fontFile))) {
                log.warn("[Subtitle] ⚠ Configured font file NOT found: {}, falling back to system font '{}'",
                        fontFile, fontName);
                fontFile = "";
            } else {
                log.info("[Subtitle] ✓ Using custom font file: {}", fontFile);
            }
        } else {
            log.info("[Subtitle] Using system font: '{}' (position={}, size={})",
                    fontName, position, fontSize);
        }
    }

    // ====================================================================
    // 核心API
    // ====================================================================

    /**
     * 生成单个镜头的 ASS 字幕文件
     *
     * @param dialogue    台词文本（可为多行）
     * @param startTime   字幕出现时刻（秒），相对于本镜头起始
     * @param endTime     字幕消失时刻（秒），相对于本镜头起始
     * @param outputFile  输出 .ass 文件路径
     * @return 生成的 .ass 文件路径；如果 dialogue 为空则返回 null
     */
    public Path generateAssFile(String dialogue, float startTime, float endTime, Path outputFile) {
        // 无台词 → 不生成字幕
        if (dialogue == null || dialogue.isBlank()) {
            log.debug("[Subtitle] Empty dialogue, skipping ASS generation");
            return null;
        }

        try {
            String assContent = buildAssContent(dialogue.trim(), startTime, endTime);

            // 确保父目录存在
            Path parent = outputFile.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            Files.writeString(outputFile, assContent, StandardCharsets.UTF_8);

            log.debug("[Subtitle] Generated ASS file: {} ({} bytes, {}→{}s)",
                    outputFile, Files.size(outputFile), startTime, endTime);
            return outputFile;
        } catch (IOException e) {
            log.error("[Subtitle] Failed to write ASS file: {}: {}", outputFile, e.getMessage());
            return null;
        }
    }

    /**
     * 快捷方法：从 ComposeService 调用，自动计算时间范围
     *
     * @param dialogue      台词文本
     * @param finalDuration 最终输出时长（秒，来自 AudioSyncEngine 的结果）
     * @param outputPath    输出视频路径（ass 文件将输出到同目录）
     * @return ASS 文件路径，或 null
     */
    public Path generateForShot(String dialogue, float finalDuration, String outputPath) {
        if (dialogue == null || dialogue.isBlank()) return null;

        // 字幕跟随音频时间轴：从 0 到 finalDuration
        // 如果没有音频（finalDuration 来自视频本身），同样全程显示
        Path assPath = Path.of(outputPath + ".ass");
        return generateAssFile(dialogue, 0f, finalDuration > 0 ? finalDuration : 5f, assPath);
    }

    // ====================================================================
    // ASS 内容构建
    // ====================================================================

    /**
     * 构建完整的 ASS 文件内容
     */
    private String buildAssContent(String text, float start, float end) {
        // 确定实际使用的字体标识符
        String safeFont = resolveFontIdentifier();

        // ASS 时间格式: H:MM:SS.cc (厘秒精度)
        String startStr = formatAssTime(start);
        String endStr = formatAssTime(end);

        // 对齐方式: 1=左下, 2=中下(默认), 3=右下, 4=中上, 5=居中, etc.
        int alignment = resolveAlignment();

        return """
                [Script Info]
                Title: Zhenling Drama Subtitle
                ScriptType: v4.00+
                PlayResX: %d
                PlayResY: %d
                WrapStyle: 0
                ScaledBorderAndShadow: yes
                YCbCr Matrix: None

                [V4+ Styles]
                Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
                Style: Default,%s,%d,%s,&H000000FF,%s,%s,-1,0,0,0,100,100,0,0,1,%d,%d,%d,%d,%d,1

                [Events]
                Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
                Dialogue: 0,%s,%s,Default,,0,0,0,,%s
                """.formatted(
                RES_X, RES_Y,
                safeFont, fontSize,
                primaryColor,
                outlineColor,
                backColor,
                outlineWidth,
                shadowDepth,
                alignment,
                20,       // MarginL
                20,       // MarginR
                marginV,  // MarginV
                startStr,
                endStr,
                escapeAssText(text)
        );
    }

    /**
     * 解析字体标识符：
     * - 如果配置了字体文件路径，使用完整路径（ASS 支持绝对路径引用字体）
     * - 否则使用系统字体名称回退
     */
    private String resolveFontIdentifier() {
        if (fontFile != null && !fontFile.isBlank()) {
            // Windows 下 FFmpeg 的 subtitles 滤镜需要正斜杠路径
            return fontFile.replace("\\", "/");
        }
        return fontName;
    }

    /**
     * 根据位置配置解析 ASS Alignment 值
     * <pre>
     * 1=左下  2=中下  3=右下
     * 4=中上  5=居中  6=右上
     * 7=左中  8=中中  9=右中
     * </pre>
     */
    private int resolveAlignment() {
        if (position == null) return 2;  // 默认中下
        return switch (position.toLowerCase()) {
            case "top" -> 4;       // 中上
            case "center", "middle" -> 5;  // 居中
            default -> 2;         // 中下(默认)
        };
    }

    /**
     * 时间格式化: 浮点秒数 → ASS 格式 H:MM:SS.cc
     *
     * <p>示例:
     * <ul>
     *   <li>0.0    → 0:00:00.00</li>
     *   <li>2.5    → 0:00:02.50</li>
     *   <li>65.123 → 0:01:05.12</li>
     *   <li>3720   → 1:02:00.00</li>
     * </ul>
     */
    private String formatAssTime(float seconds) {
        int totalSec = Math.max(0, (int) seconds);
        int h = totalSec / 3600;
        int m = (totalSec % 3600) / 60;
        float s = Math.max(0, seconds % 60);
        return "%d:%02d:%05.2f".formatted(h, m, s);
    }

    /**
     * ASS 特殊字符转义
     *
     * <p>ASS 中需要转义的字符:
     * <ul>
     *   <li>\ → \\ (反斜杠)</li>
     *   <li>{ → \{ (样式块开始)</li>
     *   <li>} → \} (样式块结束)</li>
     *   <li>\n → \\N (ASS 强制换行符)</li>
     * </ul>
     */
    private String escapeAssText(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")    // 反斜杠必须最先替换
                   .replace("{", "\\{")      // 样式块标记
                   .replace("}", "\\}")      // 样式块标记
                   .replace("\n", "\\N");    // Java换行符 → ASS换行符
    }

    // ====================================================================
    // 公开工具方法（供外部查询配置状态）
    // ====================================================================

    public boolean isFontAvailable() {
        if (fontFile != null && !fontFile.isBlank()) {
            return Files.exists(Path.of(fontFile));
        }
        return true;  // 系统字体名称无法验证可用性，假设可用
    }

    @Data
    public static class SubtitleStyleConfig {
        private String fontIdentifier;
        private int fontSize;
        private String position;
        private String primaryColor;
        private String outlineColor;
        private String backColor;
        private int marginV;
    }

    /**
     * 返回当前字幕渲染配置（用于前端展示或调试）
     */
    public SubtitleStyleConfig getStyleConfig() {
        SubtitleStyleConfig cfg = new SubtitleStyleConfig();
        cfg.setFontIdentifier(resolveFontIdentifier());
        cfg.setFontSize(fontSize);
        cfg.setPosition(position);
        cfg.setPrimaryColor(primaryColor);
        cfg.setOutlineColor(outlineColor);
        cfg.setBackColor(backColor);
        cfg.setMarginV(marginV);
        return cfg;
    }
}
