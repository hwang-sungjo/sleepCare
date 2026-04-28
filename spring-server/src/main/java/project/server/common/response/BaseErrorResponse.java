package project.server.common.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import project.server.common.response.status.BaseExceptionResponseStatus;
import project.server.common.response.status.ResponseStatus;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@JsonPropertyOrder({ "code", "status", "message", "timestamp" })
@Schema(description = "공통 에러 응답")
public class BaseErrorResponse implements ResponseStatus {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    @Schema(description = "서비스 내부 에러 코드", example = "5002")
    private final int code;

    @Schema(description = "HTTP 상태 코드", example = "400")
    private final int status;

    @Schema(description = "사용자에게 노출되는 에러 메시지", example = "이미 존재하는 닉네임입니다.")
    private final String message;

    @Schema(description = "에러 발생 시각(Asia/Seoul)", example = "2026-04-28T19:20:15.123")
    private final LocalDateTime timestamp;

    public BaseErrorResponse(ResponseStatus status) {
        ResponseStatus resolved =
                status != null ? status : BaseExceptionResponseStatus.SERVER_ERROR;
        this.code = resolved.getCode();
        this.status = resolved.getStatus();
        this.message = resolved.getMessage();
        this.timestamp = LocalDateTime.now(KOREA_ZONE);
    }

    public BaseErrorResponse(ResponseStatus status, String message) {
        ResponseStatus resolved =
                status != null ? status : BaseExceptionResponseStatus.SERVER_ERROR;
        this.code = resolved.getCode();
        this.status = resolved.getStatus();
        this.message = message;
        this.timestamp = LocalDateTime.now(KOREA_ZONE);
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return message;
    }

}
