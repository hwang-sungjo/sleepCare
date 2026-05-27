package project.server.common.exception_handler;

import jakarta.annotation.Priority;
import lombok.extern.slf4j.Slf4j;
import project.server.common.exception.AiException;
import project.server.common.response.BaseErrorResponse;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@Priority(0)
@RestControllerAdvice
public class AiExceptionControllerAdvice {

    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(AiException.class)
    public BaseErrorResponse handle_AiException(AiException e) {
        log.error("[handle_AiException]", e);
        return new BaseErrorResponse(e.getExceptionStatus(), e.getMessage());
    }
}
