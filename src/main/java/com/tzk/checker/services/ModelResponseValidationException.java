package com.tzk.checker.services;

import com.tzk.checker.client.QwenClientException;

/**
 * 模型返回内容不符合评论审核业务约束时抛出的异常。
 */
public class ModelResponseValidationException extends QwenClientException {

    private final String code;

    public ModelResponseValidationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public ModelResponseValidationException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
