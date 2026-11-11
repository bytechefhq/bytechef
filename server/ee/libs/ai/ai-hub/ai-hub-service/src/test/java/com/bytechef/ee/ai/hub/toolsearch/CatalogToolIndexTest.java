/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.toolsearch.ToolReference;
import org.springframework.ai.tool.toolsearch.ToolSearchRequest;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class CatalogToolIndexTest {

    private static final String CATALOG_SESSION_ID = ToolSearchCatalogFeeder.GLOBAL_BUILD_SESSION_ID;

    @Mock
    private VectorStore vectorStore;

    @Test
    void testIndexToolsWritesNothingForAConversation() {
        CatalogToolIndex catalogToolIndex = new CatalogToolIndex(vectorStore, Set.of(CATALOG_SESSION_ID));

        catalogToolIndex.indexTools(
            "conversation-1",
            List.of(
                ToolReference.builder()
                    .toolName("listProjects")
                    .summary("List projects")
                    .build()));
        catalogToolIndex.indexTool(
            "conversation-1",
            ToolReference.builder()
                .toolName("getWorkflow")
                .summary("Get a workflow")
                .build());

        verifyNoInteractions(vectorStore);
    }

    @Test
    void testClearIndexDeletesTheConversationRows() {
        CatalogToolIndex catalogToolIndex = new CatalogToolIndex(vectorStore, Set.of(CATALOG_SESSION_ID));

        catalogToolIndex.clearIndex("conversation-1");

        ArgumentCaptor<Filter.Expression> expressionCaptor = ArgumentCaptor.forClass(Filter.Expression.class);

        verify(vectorStore).delete(expressionCaptor.capture());

        assertThat(expressionCaptor.getValue()
            .toString()).contains("conversation-1");
    }

    @Test
    void testClearIndexLeavesTheCatalogSessionAlone() {
        CatalogToolIndex catalogToolIndex = new CatalogToolIndex(vectorStore, Set.of(CATALOG_SESSION_ID));

        catalogToolIndex.clearIndex(CATALOG_SESSION_ID);

        verify(vectorStore, never()).delete(any(Filter.Expression.class));
    }

    @Test
    void testSearchCoversOnlyTheCatalogSessions() {
        CatalogToolIndex catalogToolIndex = new CatalogToolIndex(vectorStore, Set.of(CATALOG_SESSION_ID));

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.<Document>of());

        catalogToolIndex.search(new ToolSearchRequest("conversation-1", "deploy a project", 5, null));

        ArgumentCaptor<SearchRequest> searchRequestCaptor = ArgumentCaptor.forClass(SearchRequest.class);

        verify(vectorStore).similaritySearch(searchRequestCaptor.capture());

        String filterExpression = String.valueOf(searchRequestCaptor.getValue()
            .getFilterExpression());

        assertThat(filterExpression).contains(CATALOG_SESSION_ID)
            .doesNotContain("conversation-1");
    }
}
