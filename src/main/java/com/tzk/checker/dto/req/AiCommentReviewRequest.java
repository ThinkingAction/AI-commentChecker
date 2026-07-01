package com.tzk.checker.dto.req;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(value = "评论Ai识别请求体")
public class AiCommentReviewRequest implements Serializable {

    @ApiModelProperty(value = "用户评论", required = true)
    @NotBlank(message = "评论不可为空")
    private String commentStr;
}
