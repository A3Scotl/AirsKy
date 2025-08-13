/*
 * @ (#) ApiResponseUtil.java 1.0 8/12/2025
 *
 * Copyright (c) 2025 IUH.All rights reserved
 */

package iuh.fit.airsky.util;

/*
 * @description
 * @author : Nguyen Truong An
 * @date : 8/12/2025
 * @version 1.0
 */
import iuh.fit.airsky.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.ZonedDateTime;

/**
 * Utility class for building API responses.
 */
public class ApiResponseUtil {
    public static <T> ResponseEntity<ApiResponse<T>> buildResponse(boolean success, String message, T data, String path, ZonedDateTime timestamp) {
        return ResponseEntity.ok(new ApiResponse<>(success, message, data, null, timestamp, path));
    }

    public static <T> ResponseEntity<ApiResponse<T>> buildResponse(boolean success, String message, T data, String path) {
        return buildResponse(success, message, data, path, ZonedDateTime.now());
    }

    public static <T> ResponseEntity<ApiResponse<T>> buildErrorResponse(HttpStatus status, String message, String error, String path, ZonedDateTime timestamp) {
        return ResponseEntity.status(status)
                .body(new ApiResponse<>(false, message, null, error, timestamp, path));
    }

    public static <T> ResponseEntity<ApiResponse<T>> buildErrorResponse(HttpStatus status, String message, String error, String path) {
        return buildErrorResponse(status, message, error, path, ZonedDateTime.now());
    }
}