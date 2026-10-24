package com.axiomtx.gateway;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

// error bodies follow fastapi's {"detail": ...} shape
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final PropertyNamingStrategies.SnakeCaseStrategy SNAKE_CASE =
            new PropertyNamingStrategies.SnakeCaseStrategy();

    public record FieldProblem(List<String> loc, String msg, String type) {
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, List<FieldProblem>>> invalid(MethodArgumentNotValidException e) {
        List<FieldProblem> problems = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldProblem(
                        List.of("body", SNAKE_CASE.translate(error.getField())),
                        error.getDefaultMessage(),
                        "value_error"))
                .toList();
        return ResponseEntity.unprocessableEntity().body(Map.of("detail", problems));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, List<FieldProblem>>> unreadable(HttpMessageNotReadableException e) {
        var problem = new FieldProblem(List.of("body"), "malformed or missing json body", "json_invalid");
        return ResponseEntity.unprocessableEntity().body(Map.of("detail", List.of(problem)));
    }

    @ExceptionHandler(TransactionPublisher.RoutingException.class)
    ResponseEntity<Map<String, String>> routingFailed(TransactionPublisher.RoutingException e) {
        log.error("failed to route transaction", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("detail", "Internal routing failure"));
    }
}
