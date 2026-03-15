package com.yupi.yuaiagent.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 全局异常处理返回的统一错误结构。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResult {

    private boolean success = false;
    private String message;

    public static ErrorResult of(String message) {
        return new ErrorResult(false, message);
    }
}
