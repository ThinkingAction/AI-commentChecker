package com.tzk.checker.rep;


import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class AiCommentReviewResponse {

    /**
     * {
     *   "type": "广告引流",
     *   "riskLevel": "中风险",
     *   "reason": "引导站外联系",
     *   "suggestion": "人工审核"
     * }
     */

    @ApiModelProperty(value = "评论类型")
    private String type;

    @ApiModelProperty(value = "风险等级")
    private String riskLevel;

    @ApiModelProperty(value = "评论类型简单描述")
    private String reason;

    @ApiModelProperty(value = "处理建议")
    private String suggestion;
}
