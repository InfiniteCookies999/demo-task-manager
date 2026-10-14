package infinitecookies.task.manager.exceptions;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(
        ResponseStatusException exception) {

        // TODO (maddie): we should log these exceptions.

        return ResponseEntity
            .status(exception.getStatusCode())
            .body(Map.of("message", exception.getReason() != null
                ? exception.getReason()
                : "Request failed"
            ));
    }
}
