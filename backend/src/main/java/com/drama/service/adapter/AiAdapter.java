package com.drama.service.adapter;

/**
 * AI服务适配器接口
 */
public interface AiAdapter {

    /**
     * 获取厂商名称
     */
    String getProvider();

    /**
     * 文本生成
     */
    String generateText(String prompt, String model);

    /**
     * 图片生成
     */
    String generateImage(String prompt, String model);

    /**
     * 视频生成
     */
    String generateVideo(String prompt, String model);

    /**
     * TTS语音
     */
    String generateTTS(String text, String voiceId, String model);

    /**
     * 检查服务状态
     */
    boolean checkHealth();
}