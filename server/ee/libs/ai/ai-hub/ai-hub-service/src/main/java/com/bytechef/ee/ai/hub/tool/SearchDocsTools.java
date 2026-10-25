/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.platform.configuration.ai.EmbeddingProviderStatusProvider;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

/**
 * Searches the ByteChef documentation the Copilot indexes, so the AI Hub reads it only on the turns that need it
 * instead of adding it to every prompt.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class SearchDocsTools {

    private static final String DOCS_FILTER = "category == 'docs'";
    private static final String NAME = "name";
    private static final int TOP_K = 4;

    private final EmbeddingProviderStatusProvider embeddingProviderStatusProvider;
    private final VectorStore vectorStore;

    @SuppressFBWarnings("EI")
    public SearchDocsTools(EmbeddingProviderStatusProvider embeddingProviderStatusProvider, VectorStore vectorStore) {
        this.embeddingProviderStatusProvider = embeddingProviderStatusProvider;
        this.vectorStore = vectorStore;
    }

    @Tool(
        description = "Search the ByteChef documentation and return the most relevant excerpts with the page each " +
            "comes from. Use it when the user asks how a ByteChef feature, setting or concept works and the other " +
            "tools do not answer it; do not use it for component properties, which getAction, getTrigger and " +
            "getClusterElement return. An empty excerpts list means the documentation has nothing on the query.")
    public DocsSearch searchDocs(
        @ToolParam(description = "What to look up, as a short question or phrase") String query) {

        Environment environment = EnvironmentContext.getCurrentEnvironment();

        if (!embeddingProviderStatusProvider.isEmbeddingActive(environment.ordinal())) {
            throw new IllegalStateException(
                "Documentation search is unavailable: this environment has no default embedding model. An admin " +
                    "can set one in Settings > AI.");
        }

        List<Document> documents = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .topK(TOP_K)
                .filterExpression(DOCS_FILTER)
                .build());

        return new DocsSearch(
            documents.stream()
                .filter(document -> document.getText() != null)
                .map(SearchDocsTools::toExcerpt)
                .toList());
    }

    private static DocsExcerpt toExcerpt(Document document) {
        Map<String, Object> metadata = document.getMetadata();

        return new DocsExcerpt(String.valueOf(metadata.getOrDefault(NAME, "")), document.getText());
    }

    public record DocsExcerpt(String page, String text) {
    }

    public record DocsSearch(List<DocsExcerpt> excerpts) {

        public DocsSearch {
            excerpts = List.copyOf(excerpts);
        }
    }
}
