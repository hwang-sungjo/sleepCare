package project.server.service;

import project.server.dto.ai.CitationItem;

import java.util.List;

/**
 * KB {@code Retrieve} API 결과 — Converse system 보강 및 {@link CitationItem} 노출용.
 */
public record KnowledgeBaseRetrieveResult(List<CitationItem> citations, String contextForPrompt) {

    public static KnowledgeBaseRetrieveResult empty() {
        return new KnowledgeBaseRetrieveResult(null, "");
    }

    public boolean hasContext() {
        return contextForPrompt != null && !contextForPrompt.isBlank();
    }
}
