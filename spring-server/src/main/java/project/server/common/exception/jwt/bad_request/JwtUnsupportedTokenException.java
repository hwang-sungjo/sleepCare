package project.server.common.exception.jwt.bad_request;

import lombok.Getter;
import project.server.common.response.status.ResponseStatus;

@Getter
public class JwtUnsupportedTokenException extends JwtBadRequestException {

    private final ResponseStatus exceptionStatus;

    public JwtUnsupportedTokenException(ResponseStatus exceptionStatus) {
        super(exceptionStatus);
        this.exceptionStatus = exceptionStatus;
    }

}
