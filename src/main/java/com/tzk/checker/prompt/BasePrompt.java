package com.tzk.checker.prompt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BasePrompt implements Serializable {

    /**
     * 角色描述
     */
    private String roleInfo;

    /**
     * 任务描述
     */
    private String taskInfo;

    /**
     * 输入内容
     */
    private String input;

    /**
     * 输出格式
     */
    private String outputFormat;
}
