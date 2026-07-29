/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.tool.toolsearch.ToolIndex;
import org.springframework.ai.tool.toolsearch.ToolReference;
import org.springframework.ai.vectorstore.VectorStore;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class MultiSessionToolIndexTest {

    @Mock
    private ToolIndex delegate;

    @Mock
    private VectorStore vectorStore;

    @Test
    void testIndexToolsDelegatesTheWholeBatchInsteadOfLoopingIndexTool() {
        MultiSessionToolIndex multiSessionToolIndex = new MultiSessionToolIndex(
            delegate, vectorStore, Set.of("ai_hub_tool_catalog:global:build"));

        List<ToolReference> toolReferences = List.of(
            ToolReference.builder()
                .toolName("listDataTables")
                .summary("List data tables")
                .build(),
            ToolReference.builder()
                .toolName("queryDataTable")
                .summary("Query a data table")
                .build());

        multiSessionToolIndex.indexTools("conversation-1", toolReferences);

        // The batch must reach the wrapped index intact so VectorToolIndex issues ONE vectorStore.add and the store's
        // BatchingStrategy collapses the embedding round-trips. Pre-fix the ToolIndex interface default looped
        // indexTool per reference, producing one embedding HTTP request per static tool on the first turn of every
        // new conversation.
        verify(delegate).indexTools("conversation-1", toolReferences);
        verify(delegate, never()).indexTool(anyString(), any());
    }
}
