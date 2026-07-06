package com.tzk.checker.services.impl;

import com.tzk.checker.client.QwenClient;
import com.tzk.checker.client.QwenClientException;
import com.tzk.checker.dto.rep.AiCommentReviewResponse;
import com.tzk.checker.prompt.AiCommentReviewPromptTemplate;
import com.tzk.checker.services.AiCommentReviewServices;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class AiCommentReviewServicesImpl implements AiCommentReviewServices {

    private final AiCommentReviewPromptTemplate commentReviewPromptTemplate;
    private final QwenClient qwenClient;
    private final ObjectMapper objectMapper;

    public AiCommentReviewServicesImpl(AiCommentReviewPromptTemplate commentReviewPromptTemplate,
                                       QwenClient qwenClient,
                                       ObjectMapper objectMapper) {
        this.commentReviewPromptTemplate = commentReviewPromptTemplate;
        this.qwenClient = qwenClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiCommentReviewResponse checkComment(String commentStr) {
        // Service 只负责组织审核任务；鉴权、HTTP 请求和 Qwen 响应结构解析由 QwenClient 封装。
        String modelContent = qwenClient.chat(
                commentReviewPromptTemplate.buildSystemPrompt(), commentStr);

        try {
            // 提示词约束模型仅返回 JSON，此处将模型文本转换为项目现有的业务响应对象。
            return objectMapper.readValue(modelContent, AiCommentReviewResponse.class);
        } catch (Exception exception) {
            // 不向上层返回空对象或不完整结果，统一按模型调用结果不可用处理。
            throw new QwenClientException("Qwen 返回的审核结果 JSON 解析失败", exception);
        }
    }
}
