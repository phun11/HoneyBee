package com.example.agritrace.controller;

import com.example.agritrace.dto.ApiResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ApiResponse<?> handle(Exception e) {
        return ApiResponse.fail(rootMessage(e));
    }

    private String rootMessage(Throwable e) {
        Throwable cur = e;
        Throwable root = e;
        while (cur != null) { root = cur; cur = cur.getCause(); }
        String msg = root.getMessage();
        if (msg == null || msg.isBlank()) msg = e.getMessage();
        if (e instanceof DataAccessException) {
            return "Database error: " + msg + " | Gợi ý: kiểm tra đã chạy đúng file database/oracle/00_FULL_RESET_HONEYBEE_WEB.sql trên schema HONEYBEE_WEB chưa.";
        }
        return msg == null ? e.getClass().getSimpleName() : msg;
    }
}
