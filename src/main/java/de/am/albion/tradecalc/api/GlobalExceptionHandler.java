/*
 * Copyright 2026 Martin Absmeier.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.am.albion.tradecalc.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralised JSON error responses for the REST API. Keeps the controllers
 * free of try/catch noise and gives clients a stable shape.
 *
 * <p>Status code policy:
 * <ul>
 *     <li>{@code 400 Bad Request} — Bean Validation failures and malformed
 *         request bodies / query parameters.</li>
 *     <li>{@code 500 Internal Server Error} — anything else. The full
 *         stacktrace is logged; the response only carries a timestamp.</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                fieldErrors.put(fe.getField(),
                        fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(
                "validation_failed", "Request body failed validation", Map.of("fields", fieldErrors)));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(
                "type_mismatch",
                "Parameter '" + ex.getName() + "' has wrong type",
                Map.of("parameter", ex.getName())));
    }

    // Browsers probe /favicon.ico unconditionally; Spring 6 raises this for
    // every unknown static resource. It is a 404, not a server error — let
    // the default handling run by mapping it to NOT_FOUND instead of letting
    // the catch-all log it as ERROR.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(
                "not_found",
                "No resource at " + ex.getResourcePath(),
                Map.of("path", ex.getResourcePath())));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAny(Exception ex) {
        log.error("Unhandled error in REST layer", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error("internal_error", "Unexpected server error", Map.of()));
    }

    private static Map<String, Object> error(String code, String message, Map<String, ?> details) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("code", code);
        body.put("message", message);
        if (!details.isEmpty()) {
            body.put("details", details);
        }
        return body;
    }
}