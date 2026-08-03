package com.tzk.checker.services.impl;

import com.tzk.checker.client.QwenClient;
import com.tzk.checker.client.QwenClientException;
import com.tzk.checker.dto.rep.AiCommentReviewResponse;
import com.tzk.checker.prompt.AiCommentReviewPromptTemplate;
import com.tzk.checker.services.AiCommentReviewServices;
import com.tzk.checker.services.ModelResponseValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;

@Slf4j
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
            AiCommentReviewResponse response = checkModelRep(modelContent);
            checkModelBusinessRules(response);
            return response;
        } catch (ModelResponseValidationException e) {
            //对业务校验未通过的结果进入纠错prompt拼接流程
            log.warn("用户评论:{},进入纠错流程，原因为：{}",commentStr,e.getMessage());
            return fixPrompt(e, modelContent,commentStr);
        } catch (Exception exception) {
            // 不向上层返回空对象或不完整结果，统一按模型调用结果不可用处理。
            throw new QwenClientException("Qwen 返回的审核结果 JSON 解析失败", exception);
        }

    }

    /**
     * 纠错流程
     * @param e 业务异常定义
     * @param modelContent 模型返回结果
     * @param commentStr 用户评论
     * @return 纠错后模型返回结果
     */
    private AiCommentReviewResponse fixPrompt(ModelResponseValidationException e, String modelContent, String commentStr) {
        ValidationError validationError = ValidationError.fromCode(e.getCode());

        String fixCommentStr = "用户原始输入为:"+commentStr+";上次模型输出为:"+modelContent+";对上次模型输出的校验结果为:"+validationError.fixPrompt;

        String fixModelContent = null;
        try {
            fixModelContent = qwenClient.chat(
                    commentReviewPromptTemplate.buildSystemPrompt(), fixCommentStr);
            AiCommentReviewResponse response = checkModelRep(fixModelContent);
            checkModelBusinessRules(response);
            return response;
        } catch (Exception exception){
            log.error("纠错流程再次失败，当前评论丢弃,用户输入:{},模型输出：{},失败原因：{}",commentStr,fixModelContent,exception.getMessage());
            return null;
        }


    }

    /**
     * 校验模型输出的 JSON 结构、必要字段及单个字段取值。
     *
     * @param modelContent
     * @return
     */
    private AiCommentReviewResponse checkModelRep(String modelContent) {
        JsonNode root;
        try {
            root = objectMapper.readTree(modelContent);
        } catch (Exception exception) {
            throw new ModelResponseValidationException(
                    ValidationError.INVALID_JSON.code, "模型返回结果不是合法 JSON", exception);
        }

        if (root == null || !root.isObject()) {
            throw new ModelResponseValidationException(
                    ValidationError.INVALID_JSON.code, "模型返回结果必须是 JSON 对象");
        }

        for (RequiredField requiredField : RequiredField.values()) {
            if (!root.has(requiredField.fieldName) || root.get(requiredField.fieldName).isNull()) {
                throw new ModelResponseValidationException(
                        ValidationError.MISSING_REQUIRED_FIELD.code,
                        "模型返回结果缺少必要业务字段：" + requiredField.fieldName);
            }
        }

        if (root.size() != RequiredField.values().length){
            throw new ModelResponseValidationException(
                    ValidationError.INVALID_RESPONSE_FIELDS.code, "模型返回结果业务字段冗余");
        }

        validateAllowedValue(root, RequiredField.TYPE, CommentType.values());
        validateAllowedValue(root, RequiredField.RISK_LEVEL, RiskLevel.values());
        validateAllowedValue(root, RequiredField.SUGGESTION, Suggestion.values());

        JsonNode reasonNode = root.get(RequiredField.REASON.fieldName);
        if (!reasonNode.isTextual()
                || reasonNode.asText().isBlank()
                || reasonNode.asText().codePointCount(0, reasonNode.asText().length()) > 30) {
            throw invalidFieldValue(RequiredField.REASON);
        }

        return objectMapper.treeToValue(root, AiCommentReviewResponse.class);
    }

    /**
     * 校验模型输出字段之间的业务组合关系。
     */
    private void checkModelBusinessRules(AiCommentReviewResponse response) {
        boolean isNormal = CommentType.NORMAL.value.equals(response.getType());
        boolean isLowRisk = RiskLevel.LOW.value.equals(response.getRiskLevel());
        boolean isPass = Suggestion.PASS.value.equals(response.getSuggestion());
        boolean isBlock = Suggestion.BLOCK.value.equals(response.getSuggestion());

        boolean normalRuleValid = !isNormal || (isLowRisk && isPass);
        boolean passRuleValid = !isPass || (isNormal && isLowRisk);
        boolean blockRuleValid = !isBlock || !isLowRisk;

        if (!normalRuleValid
                || !passRuleValid
                || !blockRuleValid) {
            throw new ModelResponseValidationException(
                    ValidationError.INVALID_BUSINESS_RULE.code,
                    "模型返回结果字段组合不符合业务规则");
        }
    }

    private void validateAllowedValue(JsonNode root,
                                      RequiredField field,
                                      AllowedValue[] allowedValues) {
        JsonNode fieldNode = root.get(field.fieldName);
        if (!fieldNode.isTextual()
                || Arrays.stream(allowedValues)
                .noneMatch(allowedValue -> allowedValue.value().equals(fieldNode.asText()))) {
            throw invalidFieldValue(field);
        }
    }

    private ModelResponseValidationException invalidFieldValue(RequiredField field) {
        return new ModelResponseValidationException(
                ValidationError.INVALID_FIELD_VALUE.code,
                "模型返回结果业务字段值不符合规则：" + field.fieldName);
    }

    private interface AllowedValue {
        String value();
    }

    private enum RequiredField {
        TYPE("type"),
        RISK_LEVEL("riskLevel"),
        REASON("reason"),
        SUGGESTION("suggestion");

        private final String fieldName;

        RequiredField(String fieldName) {
            this.fieldName = fieldName;
        }
    }

    private enum CommentType implements AllowedValue {
        NORMAL("正常"),
        ADVERTISEMENT("广告引流"),
        PORNOGRAPHY("色情低俗"),
        VIOLENCE("暴力血腥"),
        SENSITIVE("敏感内容"),
        OTHER_VIOLATION("其它违规");

        private final String value;

        CommentType(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }

    private enum RiskLevel implements AllowedValue {
        LOW("低风险"),
        MEDIUM("中风险"),
        HIGH("高风险");

        private final String value;

        RiskLevel(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }

    private enum Suggestion implements AllowedValue {
        PASS("放行"),
        MANUAL_REVIEW("人工审核"),
        HIDE_AND_REVIEW("先隐藏后复核"),
        BLOCK("拦截");

        private final String value;

        Suggestion(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }

    private enum ValidationError {
        INVALID_JSON("MODEL_RESPONSE_INVALID_JSON","JSON不合法，按照输出要求重新输出"),
        MISSING_REQUIRED_FIELD("MODEL_RESPONSE_MISSING_REQUIRED_FIELD","缺少必要字段,理解字段规则后按照输出格式重新输出"),
        INVALID_RESPONSE_FIELDS("MODEL_RESPONSE_INVALID_RESPONSE_FIELDS","存在多余字段,理解字段规则后按照输出格式重新输出"),
        INVALID_FIELD_VALUE("MODEL_RESPONSE_INVALID_FIELD_VALUE","业务字段值不符合规则,理解字段规则后按照输出格式重新输出"),
        INVALID_BUSINESS_RULE("MODEL_RESPONSE_INVALID_BUSINESS_RULE","业务字段值组合不符合业务规则,理解字段规则后校验字段值之间的关系，按照输出格式重新输出");

        private final String code;

        private final String fixPrompt;

        ValidationError(String code, String fixPrompt) {
            this.code = code;
            this.fixPrompt = fixPrompt;
        }

        public static ValidationError fromCode(String code) {
            for (ValidationError e : values()) {
                if (e.code.equals(code)) {
                    return e;
                }
            }
            throw new IllegalArgumentException("Unknown code: " + code);
        }
    }
}
