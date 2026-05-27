package project.server.common.exception;

import lombok.Getter;
import project.server.common.response.status.ResponseStatus;

/**
 * Bedrock / S3 호출 또는 프롬프트 로딩 단계에서 발생한 외부 의존성 오류를 사용자 응답으로 전달한다.
 * 챗봇 엔드포인트에서만 외부로 노출되며, 대시보드 AI 조언은 swallow 정책이므로 사용하지 않는다.
 */
@Getter
public class AiException extends RuntimeException {

    private final ResponseStatus exceptionStatus;

    public AiException(ResponseStatus exceptionStatus) {
        super(exceptionStatus.getMessage());
        this.exceptionStatus = exceptionStatus;
    }

    public AiException(ResponseStatus exceptionStatus, Throwable cause) {
        super(exceptionStatus.getMessage(), cause);
        this.exceptionStatus = exceptionStatus;
    }
}
