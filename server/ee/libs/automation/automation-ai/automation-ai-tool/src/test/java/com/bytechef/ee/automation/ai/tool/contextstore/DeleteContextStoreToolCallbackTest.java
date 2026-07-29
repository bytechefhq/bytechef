/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool.contextstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.ee.automation.contextstore.facade.WorkspaceContextStoreFacade;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class DeleteContextStoreToolCallbackTest {

    private final WorkspaceContextStoreFacade facade = Mockito.mock(WorkspaceContextStoreFacade.class);
    private final DeleteContextStoreToolCallback toolCallback = new DeleteContextStoreToolCallback(facade);

    @Test
    void deletesStoreScopedToContextWorkspace() {
        ToolContext toolContext = new ToolContext(
            AgentToolInvocationContext.builder()
                .workspaceId(7L)
                .build()
                .toToolContext());

        String result = toolCallback.call("{\"id\": 42}", toolContext);

        verify(facade).deleteWorkspaceContextStore(7L, 42L);
        assertThat(result).contains("\"deleted\":true");
    }

    @Test
    void rejectsMissingWorkspaceContext() {
        String result = toolCallback.call("{\"id\": 42}", new ToolContext(Map.of()));

        assertThat(result).contains("Workspace context unavailable");
    }

    @Test
    void rejectsMissingId() {
        ToolContext toolContext = new ToolContext(
            AgentToolInvocationContext.builder()
                .workspaceId(7L)
                .build()
                .toToolContext());

        String result = toolCallback.call("{}", toolContext);

        assertThat(result).contains("id is required");
    }
}
