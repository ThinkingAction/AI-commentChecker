package com.tzk.checker.controller;

import com.tzk.checker.rep.AiCommentReviewResponse;
import com.tzk.checker.req.AiCommentReviewRequest;
import com.tzk.checker.services.AiCommentReviewServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

@Api(tags = "AI评论审核")
@RestController
@RequestMapping("/ai-comment-review")
public class AiCommentReviewController {

    @Autowired
    private AiCommentReviewServices aiCommentReviewServices;

    @ApiOperation(value = "评论审核", notes = "根据评论内容判断是否违规，并返回审核结果")
    @PostMapping("/check")
    public AiCommentReviewResponse aiCommentCheck(@RequestBody AiCommentReviewRequest request) {
        return aiCommentReviewServices.checkComment(request.getCommentStr());
    }
}
