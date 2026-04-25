package com.drama.service.adapter;

import com.drama.entity.AiConfig;

/**
 * AI服务适配器接口
 */
public interface AiAdapter {

    /**
     * 获取厂商名称
     */
    String getProvider();

    /**
     * 初始化配置（从数据库加载）
     */
    void initConfig(AiConfig config);

    /**
     * 文本生成（Chat Completion）
     * @param prompt 用户提示词
     * @param model 模型名称
     * @return 生成的文本
     */
    String generateText(String prompt, String model);

    /**
     * 图片生成
     * @param prompt 图片提示词
     * @param model 模型名称
     * @return 生成的图片URL或Base64
     */
    String generateImage(String prompt, String model);

    /**
     * 视频生成（图生视频）
     * @param imageUrl 参考图片URL
     * @param model 模型名称
     * @return 生成的视频URL或任务ID
     */
    String generateVideo(String imageUrl, String model);

    /**
     * 视频生成（带扩展参数）
     * @param imageUrl 参考图片URL
     * @param prompt 文本提示词
     * @param model 模型名称
     * @param duration 视频时长（秒）
     * @param resolution 分辨率（如 512P / 768P / 1080P）
     * @return 生成的视频URL或任务ID
     */
    default String generateVideo(String imageUrl, String prompt, String model, Integer duration, String resolution) {
        // 默认实现调用旧方法，保持向后兼容
        return generateVideo(imageUrl, model);
    }

    /**
     * TTS语音合成
     * @param text 待合成文本
     * @param voiceId 音色ID
     * @param model 模型名称
     * @return 音频URL或Base64数据
     */
    String generateTTS(String text, String voiceId, String model);

    /**
     * 检查服务状态
     */
    boolean checkHealth();

    /**
     * 轮询异步视频任务状态（返回 "completed" / "processing" / "failed" + URL或error）
     * @param taskId 厂商返回的任务ID
     * @return 格式: "status:{status}" 或 "completed:{url}" 或 "failed:{errorMsg}"
     */
    default String pollVideoStatus(String taskId) {
        return "status:unknown";
    }

    /**
     * 是否已配置
     */
    boolean isConfigured();
}
