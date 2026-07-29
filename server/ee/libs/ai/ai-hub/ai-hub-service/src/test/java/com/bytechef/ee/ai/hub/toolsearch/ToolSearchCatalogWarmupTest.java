/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ToolSearchCatalogWarmupTest {

    private final ToolSearchCatalogFeeder feeder = mock(ToolSearchCatalogFeeder.class);

    @Test
    void testWarmUpIsNoOpWhenFeederAbsent() {
        ToolSearchCatalogWarmup warmup = new ToolSearchCatalogWarmup(null, List.of());

        warmup.warmUp();

        verifyNoInteractions(feeder);
    }

    @Test
    void testWarmUpPopulatesCatalogAndGlobalToolsExactlyOnce() {
        AiHubGlobalToolCatalog askCatalog = new AiHubGlobalToolCatalog(
            "ask-session", List.of(mock(ToolCallback.class)));

        ToolSearchCatalogWarmup warmup = new ToolSearchCatalogWarmup(feeder, List.of(askCatalog));

        // Idempotent: repeated first-turn calls (and concurrent callers, serialized on the monitor) populate once.
        warmup.warmUp();
        warmup.warmUp();

        verify(feeder, times(1)).populate();
        verify(feeder, times(1)).populateGlobalTools("ask-session", askCatalog.toolCallbacks());
    }
}
