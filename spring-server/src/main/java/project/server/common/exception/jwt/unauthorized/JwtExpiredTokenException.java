package project.server.common.exception.jwt.unauthorized;

import lombok.Getter;
import project.server.common.response.status.ResponseStatus;

@Getter
public class JwtExpiredTokenException extends JwtUnauthorizedTokenException {

    private final ResponseStatus exceptionStatus;

    public JwtExpiredTokenException(ResponseStatus exceptionStatus) {
        super(exceptionStatus);
        this.exceptionStatus = exceptionStatus;
    }

}
