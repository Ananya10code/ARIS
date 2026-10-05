package com.aris.probe;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

/** Logs every unhandled controller exception with its stack trace; the AI engine reads this log to find the failing code. */
@RestControllerAdvice
public class ErrorLogger {
    private static final Logger log = LoggerFactory.getLogger(ErrorLogger.class);

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handle(RuntimeException ex, HttpServletRequest req) {
        if (ex instanceof ErrorResponse || ex instanceof HttpMessageConversionException) throw ex; // leave Spring's own 4xx handling alone
        log.error("ARIS_ERROR endpoint={} exception={}", req.getRequestURI(), ex.toString(), ex);
        return ResponseEntity.status(500).body(Map.of("error", String.valueOf(ex.getMessage())));
    }
}
