package project.server.common.exception;

import lombok.Getter;
import project.server.common.response.status.ResponseStatus;

@Getter
public class BadRequestException extends RuntimeException {

    private final ResponseStatus exceptionStatus;

    public BadRequestException(ResponseStatus exceptionStatus) {
        super(exceptionStatus.getMessage());
        this.exceptionStatus = exceptionStatus;
    }

}
