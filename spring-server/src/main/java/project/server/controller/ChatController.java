package project.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import project.server.common.argument_resolver.PreAuthorize;
import project.server.common.exception.UserException;
import project.server.common.response.BaseErrorResponse;
import project.server.common.response.BaseResponse;
import project.server.dto.ai.ChatRequest;
import project.server.dto.ai.ChatResponse;
import project.server.service.ChatbotService;

import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static project.server.common.response.status.BaseExceptionResponseStatus.INVALID_USER_VALUE;
import static project.server.util.BindingResultUtils.getErrorMessages;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
@Tag(name = "Chat", description = "AI 챗봇 API (KB 검색 + Converse + DB 스킬)")
public class ChatController {

    private final ChatbotService chatbotService;

    @Operation(
            summary = "챗봇 메시지 전송",
            description =
                    "S3 의 chatbot 페르소나 프롬프트와 결합하여 사용자 메시지를 Bedrock RAG 에 위임합니다. "
                            + "응답의 sessionId 를 다음 요청에 그대로 실어 보내면 멀티턴 대화가 됩니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "응답 성공",
                    content = @Content(schema = @Schema(implementation = ChatResponse.class))),
            @ApiResponse(
                    responseCode = "401",
                    description = "JWT 검증 실패",
                    content = @Content(
                            schema = @Schema(implementation = BaseErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "InvalidToken",
                                    value = """
                                            {
                                              "code": 4003,
                                              "status": 401,
                                              "message": "유효하지 않은 토큰입니다.",
                                              "timestamp": "2026-04-28T19:20:15.123"
                                            }
                                            """))),
            @ApiResponse(
                    responseCode = "503",
                    description = "AI 응답 생성 실패 (Bedrock/S3 일시 오류)",
                    content = @Content(
                            schema = @Schema(implementation = BaseErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "AiGenerationFailed",
                                    value = """
                                            {
                                              "code": 6002,
                                              "status": 503,
                                              "message": "AI 응답 생성에 실패했습니다.",
                                              "timestamp": "2026-04-28T19:20:15.123"
                                            }
                                            """)))
    })
    @PostMapping("/messages")
    public BaseResponse<ChatResponse> sendMessage(
            @Parameter(hidden = true) @PreAuthorize long userId,
            @Validated @RequestBody ChatRequest request, BindingResult bindingResult) {
        log.info("[ChatController.sendMessage] user={} sessionId={}", userId, request.getSessionId());
        if (bindingResult.hasErrors()) {
            throw new UserException(INVALID_USER_VALUE, getErrorMessages(bindingResult));
        }
        return new BaseResponse<>(chatbotService.reply(userId, request));
    }
}
