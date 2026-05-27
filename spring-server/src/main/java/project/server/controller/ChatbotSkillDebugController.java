package project.server.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import project.server.ai.chatbot.skill.ChatbotSkillExecutor;
import project.server.ai.chatbot.skill.ChatbotSkillId;
import project.server.ai.chatbot.skill.ChatbotSkillResult;
import project.server.common.argument_resolver.PreAuthorize;
import project.server.common.response.BaseResponse;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 로컬/개발용 DB 스킬 직접 실행 (Bedrock 없이 SQL 검증).
 */
@Hidden
@RestController
@Profile("devPort")
@RequiredArgsConstructor
@RequestMapping("/internal/chatbot/skills")
@Tag(name = "Chatbot Skills (internal)", description = "개발용 DB 스킬 디버그 API")
public class ChatbotSkillDebugController {

    private final ChatbotSkillExecutor skillExecutor;

    @Operation(summary = "스킬 1건 실행", description = "JWT userId 로만 DB 조회. body 는 스킬별 파라미터(JSON).")
    @PostMapping("/{skillId}")
    public BaseResponse<ChatbotSkillResult> executeSkill(
            @PreAuthorize long userId,
            @PathVariable String skillId,
            @RequestBody(required = false) Map<String, Object> body) {
        ChatbotSkillId skill = ChatbotSkillId.fromOperationId(skillId);
        Map<String, Object> params = body == null ? Map.of() : body;
        ChatbotSkillResult result = skillExecutor.execute(skill, userId, params);
        return new BaseResponse<>(result);
    }
}
