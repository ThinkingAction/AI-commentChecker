package com.tzk.checker.services;

import com.tzk.checker.dto.rep.AiCommentReviewResponse;

public interface AiCommentReviewServices {

    /**
     * 检查评论内容
     * @param commentStr - 评论内容字符串
     * @return 检查结果
     */
    AiCommentReviewResponse checkComment(String commentStr);
}
