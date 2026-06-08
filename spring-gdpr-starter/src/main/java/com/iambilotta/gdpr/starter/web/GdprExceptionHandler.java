package com.iambilotta.gdpr.starter.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Web error contract for {@link GdprController}. Maps a rejected request (blank/unknown subject id,
 * any {@link IllegalArgumentException} raised by the erasure or access services) to a {@code 400
 * Bad Request} with a small JSON body, instead of a raw {@code 500}.
 *
 * <p>Scoped to {@code assignableTypes = GdprController} so it never intercepts the host application's
 * own controllers: the advice only governs the {@code /gdpr/**} surface this library mounts. Handler
 * failures during erasure are deliberately NOT caught here: they propagate as a server error
 * (fail-fast), which is the honest contract documented in ADR-0004 (there is no 207 partial-status).
 */
@RestControllerAdvice(assignableTypes = GdprController.class)
public class GdprExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "bad_request", "message", ex.getMessage()));
    }
}
