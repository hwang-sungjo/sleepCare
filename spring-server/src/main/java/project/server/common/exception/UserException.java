package project.server.common.exception;

import lombok.Getter;
import project.server.common.response.status.ResponseStatus;

@Getter
public class UserException extends RuntimeException {

    private final ResponseStatus exceptionStatus;

    public UserException(ResponseStatus exceptionStatus) {
        super(exceptionStatus.getMessage());
        this.exceptionStatus = exceptionStatus;
    }

    public UserException(ResponseStatus exceptionStatus, String message) {
        super(message);
        this.exceptionStatus = exceptionStatus;
    }

}
