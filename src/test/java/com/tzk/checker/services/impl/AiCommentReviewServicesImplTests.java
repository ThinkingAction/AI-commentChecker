package com.tzk.checker.services.impl;

import com.tzk.checker.client.QwenClient;
import com.tzk.checker.dto.rep.AiCommentReviewResponse;
import com.tzk.checker.prompt.AiCommentReviewPromptTemplate;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiCommentReviewServicesImplTests {

    @Test
    void shouldConvertModelJsonToBusinessResponse() {
        QwenClient qwenClient = (systemMessage, userMessage) -> """
                {"type":"正常","riskLevel":"低风险","reason":"正常购物评价","suggestion":"放行"}
                """;
        AiCommentReviewServicesImpl service = new AiCommentReviewServicesImpl(
                new AiCommentReviewPromptTemplate(), qwenClient, new ObjectMapper());

        AiCommentReviewResponse response = service.checkComment("这个质量不错");

        assertEquals("正常", response.getType());
        assertEquals("低风险", response.getRiskLevel());
        assertEquals("正常购物评价", response.getReason());
        assertEquals("放行", response.getSuggestion());
    }
}
