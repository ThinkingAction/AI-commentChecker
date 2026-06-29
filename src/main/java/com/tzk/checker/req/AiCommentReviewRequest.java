package com.tzk.checker.req;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(value = "评论Ai识别请求体")
public class AiCommentReviewRequest implements Serializable {

    @ApiModelProperty(value = "用户评论")
    private String commentStr;
}
