/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.hub.tool.SearchDocsTools.DocsExcerpt;
import com.bytechef.ee.ai.hub.tool.SearchDocsTools.DocsSearch;
import com.bytechef.platform.configuration.ai.EmbeddingProviderStatusProvider;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class SearchDocsToolsTest {

    private final EmbeddingProviderStatusProvider embeddingProviderStatusProvider =
        mock(EmbeddingProviderStatusProvider.class);
    private final VectorStore vectorStore = mock(VectorStore.class);
    private final SearchDocsTools searchDocsTools = new SearchDocsTools(embeddingProviderStatusProvider, vectorStore);

    @BeforeEach
    void setUp() {
        EnvironmentContext.set(Environment.DEVELOPMENT);
    }

    @AfterEach
    void tearDown() {
        EnvironmentContext.clear();
    }

    @Test
    void testSearchDocsReturnsTheExcerptsWithTheirPages() {
        when(embeddingProviderStatusProvider.isEmbeddingActive(Environment.DEVELOPMENT.ordinal())).thenReturn(true);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(
            List.of(
                new Document("Approvals pause a run until someone decides.", Map.of("name", "approvals")),
                new Document("Workspaces group projects.", Map.of())));

        DocsSearch docsSearch = searchDocsTools.searchDocs("how do approvals work");

        assertThat(docsSearch.excerpts()).containsExactly(
            new DocsExcerpt("approvals", "Approvals pause a run until someone decides."),
            new DocsExcerpt("", "Workspaces group projects."));
    }

    @Test
    void testSearchDocsQueriesOnlyTheIndexedDocs() {
        when(embeddingProviderStatusProvider.isEmbeddingActive(Environment.DEVELOPMENT.ordinal())).thenReturn(true);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        DocsSearch docsSearch = searchDocsTools.searchDocs("how do approvals work");

        ArgumentCaptor<SearchRequest> searchRequestCaptor = ArgumentCaptor.forClass(SearchRequest.class);

        verify(vectorStore).similaritySearch(searchRequestCaptor.capture());

        SearchRequest searchRequest = searchRequestCaptor.getValue();

        assertThat(searchRequest.getQuery()).isEqualTo("how do approvals work");
        assertThat(searchRequest.getTopK()).isEqualTo(4);
        assertThat(searchRequest.getFilterExpression()).isNotNull();
        assertThat(String.valueOf(searchRequest.getFilterExpression())).contains("category", "docs");
        assertThat(docsSearch.excerpts()).isEmpty();
    }

    @Test
    void testSearchDocsFailsWithoutAnEmbeddingModelInTheEnvironment() {
        when(embeddingProviderStatusProvider.isEmbeddingActive(Environment.DEVELOPMENT.ordinal())).thenReturn(false);

        assertThatThrownBy(() -> searchDocsTools.searchDocs("how do approvals work"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("no default embedding model");

        verifyNoInteractions(vectorStore);
    }
}
