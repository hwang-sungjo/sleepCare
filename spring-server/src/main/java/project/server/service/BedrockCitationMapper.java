package project.server.service;

import project.server.dto.ai.CitationItem;

import software.amazon.awssdk.services.bedrockagentruntime.model.Citation;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrievedReference;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code RetrieveAndGenerateResponse#citations()} 를 API DTO 로 변환한다.
 */
public final class BedrockCitationMapper {

    private BedrockCitationMapper() {
    }

    /**
     * Bedrock 인용 목록을 {@link CitationItem} 리스트로 변환한다. 비어 있으면 {@code null}.
     */
    public static List<CitationItem> fromBedrock(List<Citation> citations) {
        if (citations == null || citations.isEmpty()) {
            return null;
        }
        List<CitationItem> out = new ArrayList<>();
        for (Citation citation : citations) {
            List<RetrievedReference> refs = citation.retrievedReferences();
            if (refs == null) {
                continue;
            }
            for (RetrievedReference ref : refs) {
                String location = extractLocation(ref);
                String snippet = ref.content() == null ? null : ref.content().text();
                if (location == null && snippet == null) {
                    continue;
                }
                out.add(CitationItem.builder()
                        .location(location)
                        .snippet(snippet)
                        .build());
            }
        }
        return out.isEmpty() ? null : out;
    }

    private static String extractLocation(RetrievedReference ref) {
        if (ref.location() == null) {
            return null;
        }
        if (ref.location().s3Location() != null && ref.location().s3Location().uri() != null) {
            return ref.location().s3Location().uri();
        }
        // S3 외 데이터 소스 대비 — 위치 객체의 enum 문자열을 fallback 으로 노출.
        return ref.location().typeAsString();
    }
}
