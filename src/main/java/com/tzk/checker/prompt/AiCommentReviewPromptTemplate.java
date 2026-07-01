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
                
                审核类型说明：
                - 正常：评论内容未发现明显广告、引流、低俗、暴力、敏感或其它违规内容。
                - 广告引流：包含站外联系方式、加微信、私聊、加群、引导跳转、主页引导、推广营销、导流交易等内容。
                - 色情低俗：包含色情暗示、低俗表达、擦边引导或不适合公开展示的内容。
                - 暴力血腥：包含暴力威胁、攻击性表达、伤害描述、血腥描述或鼓动攻击等内容。
                - 敏感内容：包含平台高风险敏感表达、煽动性内容或不适合公开讨论的高风险内容。
                - 其它违规：不属于以上具体类型，但存在明显不适合公开展示的违规内容。

                风险等级只能从以下值中选择：
                低风险
                中风险
                高风险

                处理建议只能从以下值中选择：
                放行
                人工审核
                先隐藏后复核
                拦截
                
                理由：
                当前评论审核结果的理由

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
                
                Few-shot:
                评论：这个质量不错，下次还买
                输出：
                {
                "type": "正常",
                "riskLevel": "低风险",
                "reason": "正常购物评价",
                "suggestion": "放行"
                }
                
                评论：加我微信，价格更便宜
                输出：
                {
                "type": "广告引流",
                "riskLevel": "高风险",
                "reason": "引导添加微信交易",
                "suggestion": "拦截"
                }
                
                评论：价格太贵了，想打死你
                输出：
                {
                "type": "暴力血腥",
                "riskLevel": "中风险",
                "reason": "包含暴力威胁表达",
                "suggestion": "先隐藏后复核"
                }
                
                评论：这个衣服真性感，都能看到奶头
                输出：
                {
                "type": "色情低俗",
                "riskLevel": "中风险",
                "reason": "包含低俗露骨描述",
                "suggestion": "先隐藏后复核"
                }
                
                评论：真是个傻逼商家
                输出：
                {
                "type": "其它违规",
                "riskLevel": "中风险",
                "reason": "包含辱骂攻击表达",
                "suggestion": "人工审核"
                }
                
                
                
                """.formatted(commentStr);
    }
}
