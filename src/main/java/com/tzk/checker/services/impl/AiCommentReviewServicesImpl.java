package com.tzk.checker.services.impl;

import com.tzk.checker.rep.AiCommentReviewResponse;
import com.tzk.checker.services.AiCommentReviewServices;
import org.springframework.stereotype.Service;


@Service
public class AiCommentReviewServicesImpl implements AiCommentReviewServices {
    @Override
    public AiCommentReviewResponse checkComment(String commentStr) {
        AiCommentReviewResponse reviewResponse = new AiCommentReviewResponse();
        if (commentStr.contains("error")){
            reviewResponse.setType("暴力");
            reviewResponse.setRiskLevel("高度风险");
            reviewResponse.setReason("存在血腥描述");
            reviewResponse.setSuggestion("屏蔽");
        }else if (commentStr.contains("warning")){
            reviewResponse.setType("广告引流");
            reviewResponse.setRiskLevel("中度风险");
            reviewResponse.setReason("存在引导站外交流");
            reviewResponse.setSuggestion("人工复核");
        }else {
            reviewResponse.setType("正常");
            reviewResponse.setRiskLevel("无风险");
            reviewResponse.setReason("");
            reviewResponse.setSuggestion("通过");
        }

        return reviewResponse;
    }
}
