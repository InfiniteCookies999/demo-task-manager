package infinitecookies.task.manager.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus statusCode;
    private final ApiErrorCode code;

    public ApiException(HttpStatus statusCode, ApiErrorCode code, String message) {
        super(message);
        this.statusCode = statusCode;
        this.code = code;
    }
}
