package project.server.common.exception.jwt.unauthorized;

import lombok.Getter;
import project.server.common.response.status.ResponseStatus;

@Getter
public class JwtUnauthorizedTokenException extends RuntimeException {

    private final ResponseStatus exceptionStatus;

    public JwtUnauthorizedTokenException(ResponseStatus exceptionStatus) {
        super(exceptionStatus.getMessage());
        this.exceptionStatus = exceptionStatus;
    }

}
