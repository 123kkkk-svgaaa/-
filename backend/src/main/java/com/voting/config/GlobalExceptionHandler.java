package com.voting.config;

import com.voting.dto.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler — converts exceptions to unified API error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntime(RuntimeException e) {
        log.error("Request error: {}", e.getMessage(), e);
        return Result.error(500, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleGeneric(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return Result.error(500, "服务器内部错误");
    }
}
