package project.server.common.response;

import static project.server.common.response.status.BaseExceptionResponseStatus.SUCCESS;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import project.server.common.response.status.ResponseStatus;

@Getter
@JsonPropertyOrder({ "code", "status", "message", "result" })
@Schema(description = "성공 응답 공통 형식입니다. 필드 result 에 엔드포인트별 본문 타입이 담깁니다.")
public class BaseResponse<T> implements ResponseStatus {

    @Schema(description = "서버 정의 성공 코드", example = "1000")
    private final int code;
    @Schema(description = "HTTP 상태 코드와 동일한 값", example = "200")
    private final int status;
    @Schema(description = "성공 메시지(고정 문자열)", example = "요청에 성공하였습니다.")
    private final String message;

    @Schema(description = "엔드포인트별 응답 본문. 값이 없는 경우 JSON 에서 필드 생략")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T result;

    public BaseResponse(T result) {
        this.code = SUCCESS.getCode();
        this.status = SUCCESS.getStatus();
        this.message = SUCCESS.getMessage();
        this.result = result;
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
