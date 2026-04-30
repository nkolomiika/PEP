package ru.pep.platform.api;

import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.pep.platform.service.CorePlatformService;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(CorePlatformService.NotFoundException.class)
    ResponseEntity<Map<String, String>> notFound(RuntimeException exception) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler({CorePlatformService.AccessDeniedException.class, AccessDeniedException.class})
    ResponseEntity<Map<String, String>> accessDenied(RuntimeException exception) {
        return error(HttpStatus.FORBIDDEN, "ACCESS_DENIED", exception.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    ResponseEntity<Map<String, String>> validationFailed(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Проверьте заполненные поля");
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(Map.of("code", code, "message", message));
    }
}
