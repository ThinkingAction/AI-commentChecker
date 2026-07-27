package com.tzk.checker.services.impl;

import com.tzk.checker.client.QwenClient;
import com.tzk.checker.dto.rep.AiCommentReviewResponse;
import com.tzk.checker.prompt.AiCommentReviewPromptTemplate;
import com.tzk.checker.services.ModelResponseValidationException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void shouldRejectInvalidJson() {
        ModelResponseValidationException exception = assertValidationFailure("not-json");

        assertEquals("MODEL_RESPONSE_INVALID_JSON", exception.getCode());
    }

    @Test
    void shouldRejectMissingRequiredField() {
        ModelResponseValidationException exception = assertValidationFailure("""
                {"type":"正常","riskLevel":"低风险","reason":"正常购物评价"}
                """);

        assertEquals("MODEL_RESPONSE_MISSING_REQUIRED_FIELD", exception.getCode());
    }

    @Test
    void shouldRejectValueOutsideAllowedRange() {
        ModelResponseValidationException exception = assertValidationFailure("""
                {"type":"未知","riskLevel":"低风险","reason":"正常购物评价","suggestion":"放行"}
                """);

        assertEquals("MODEL_RESPONSE_INVALID_FIELD_VALUE", exception.getCode());
    }

    @Test
    void shouldRejectReasonLongerThanThirtyCharacters() {
        ModelResponseValidationException exception = assertValidationFailure("""
                {"type":"正常","riskLevel":"低风险","reason":"一二三四五六七八九十一二三四五六七八九十一二三四五六七八九十一","suggestion":"放行"}
                """);

        assertEquals("MODEL_RESPONSE_INVALID_FIELD_VALUE", exception.getCode());
    }

    private ModelResponseValidationException assertValidationFailure(String modelContent) {
        QwenClient qwenClient = (systemMessage, userMessage) -> modelContent;
        AiCommentReviewServicesImpl service = new AiCommentReviewServicesImpl(
                new AiCommentReviewPromptTemplate(), qwenClient, new ObjectMapper());

        return assertThrows(ModelResponseValidationException.class,
                () -> service.checkComment("测试评论"));
    }
}
