package com.yupi.yuaiagent.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.concurrent.TimeoutException;

/**
 * 全局异常处理：将未捕获的异常转为统一 JSON 错误响应，避免直接返回 500 堆栈。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ErrorResult> handleTimeout(TimeoutException e) {
        log.warn("请求超时: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.GATEWAY_TIMEOUT)
                .body(ErrorResult.of("请求超时，请重试或简化任务。"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResult> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数错误: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResult.of(e.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResult> handleRuntime(RuntimeException e) {
        log.error("运行时异常", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResult.of(sanitizeMessage(e)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResult> handleException(Exception e) {
        log.error("未捕获异常", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResult.of(sanitizeMessage(e)));
    }

    private static String sanitizeMessage(Throwable e) {
        String msg = e.getMessage();
        if (msg != null && !msg.isBlank()) {
            return msg.length() > 500 ? msg.substring(0, 500) + "..." : msg;
        }
        return "服务器内部错误，请稍后重试。";
    }
}
