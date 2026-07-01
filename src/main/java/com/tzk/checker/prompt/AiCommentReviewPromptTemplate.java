package com.tzk.checker.prompt;

import org.springframework.stereotype.Component;

@Component
public class AiCommentReviewPromptTemplate {

    public String buildPrompt(String commentStr) {
        return """
                你是一个评论审核助手。

                任务：
                判断用户评论是否存在违规内容，并返回审核结果。

                审核类型只能从以下类型中选择：
                1. 正常
                2. 广告引流
                3. 色情低俗
                4. 暴力血腥
                5. 敏感内容
                6. 其它违规

                风险等级只能从以下值中选择：
                低风险
                中风险
                高风险

                处理建议只能从以下值中选择：
                放行
                人工审核
                先隐藏后复核
                拦截

                用户评论：
                <comment>
                %s
                </comment>

                输出要求：
                1. 只输出 JSON。
                2. 不要输出 Markdown。
                3. 不要输出解释。
                4. 不要输出额外字段。

                输出格式：
                {
                  "type": "",
                  "riskLevel": "",
                  "reason": "",
                  "suggestion": ""
                }
                """.formatted(commentStr);
    }
}
