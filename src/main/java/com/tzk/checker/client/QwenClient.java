package com.tzk.checker.client;

public interface QwenClient {

    /**
     * 调用 Qwen 并返回模型生成的文本内容。
     */
    String chat(String systemMessage, String userMessage);
}
