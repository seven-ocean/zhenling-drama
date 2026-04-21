package com.drama.service.adapter;

/**
 * AI 服务 API 调用异常
 * 用于透传第三方 AI 厂商（如 MiniMax）返回的具体错误信息
 *
 * 包含三层信息：
 * 1. vendorCode   - 厂商原始错误码（如 MiniMax 的 1008=余额不足）
 * 2. vendorMessage- 厂商原始错误消息
 * 3. userMessage  - 翻译后的用户友好中文提示（直接展示给前端用户）
 * 4. operation    - 触发失败的操作类型（文本生成/图片生成/视频生成/TTS）
 */
public class AiApiException extends RuntimeException {

    private final int vendorCode;
    private final String vendorMessage;
    private final String operation;

    public AiApiException(int vendorCode, String vendorMessage, String userMessage, String operation) {
        super(userMessage);
        this.vendorCode = vendorCode;
        this.vendorMessage = vendorMessage;
        this.operation = operation;
    }

    public int getVendorCode() {
        return vendorCode;
    }

    public String getVendorMessage() {
        return vendorMessage;
    }

    public String getOperation() {
        return operation;
    }
}
