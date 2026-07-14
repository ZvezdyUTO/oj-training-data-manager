package com.ojtraining.manager.api.web;

import com.ojtraining.manager.trainingdata.common.app.account.OjHandleAccountException;
import com.ojtraining.manager.trainingdata.common.app.purge.OjStudentDataPurgeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiResponse<Void>> handleApiException(ApiException exception) {
        log.warn("Request rejected, errorCode={}, httpStatus={}",
                exception.errorCode(), exception.status().value());
        return error(exception.status(), exception.getMessage());
    }

    @ExceptionHandler(OjHandleAccountException.class)
    ResponseEntity<ApiResponse<Void>> handleHandleAccountException(OjHandleAccountException exception) {
        HttpStatus status = switch (exception.errorCode()) {
            case OJ_HANDLE_ACCOUNT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case OJ_HANDLE_ACCOUNT_IDENTITY_EXISTS, OJ_HANDLE_ACCOUNT_HANDLE_EXISTS -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
        log.warn("Training member request rejected, errorCode={}, httpStatus={}",
                exception.errorCode(), status.value());
        return error(status, exception.getMessage());
    }

    @ExceptionHandler(OjStudentDataPurgeException.class)
    ResponseEntity<ApiResponse<Void>> handlePurgeException(OjStudentDataPurgeException exception) {
        log.warn("Training data purge request rejected, errorCode={}, httpStatus={}",
                exception.errorCode(), HttpStatus.BAD_REQUEST.value());
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentNotValidException.class
    })
    ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception exception) {
        log.warn("Request rejected, errorCode=INVALID_REQUEST, httpStatus={}",
                HttpStatus.BAD_REQUEST.value());
        String message = exception instanceof IllegalArgumentException
                ? exception.getMessage()
                : "请求格式不正确";
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<ApiResponse<Void>> handleNotFound(NoSuchElementException exception) {
        log.warn("Resource not found, errorCode=RESOURCE_NOT_FOUND, httpStatus={}",
                HttpStatus.NOT_FOUND.value());
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiResponse<Void>> handleConflict(DataIntegrityViolationException exception) {
        log.warn("Persistence constraint rejected request, errorCode=DATA_CONFLICT, httpStatus={}",
                HttpStatus.CONFLICT.value());
        return error(HttpStatus.CONFLICT, "用户名或 OJ handle 已存在");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        log.error("Unhandled request failure, errorCode=INTERNAL_ERROR, httpStatus={}",
                HttpStatus.INTERNAL_SERVER_ERROR.value(), exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "服务内部错误");
    }

    private static ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(status.value(), message));
    }
}
