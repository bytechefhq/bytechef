/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workspacefile.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.workspacefile.domain.WorkspaceFile;
import com.bytechef.automation.workspacefile.service.WorkspaceFileFacade;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ListWorkspaceFilesToolCallbackTest {

    @Mock
    private WorkspaceFileFacade facade;

    @Mock
    private WorkspaceContextProvider context;

    @Test
    void testCallHappyPath() {
        when(context.currentWorkspaceId()).thenReturn(11L);

        WorkspaceFile file = new WorkspaceFile();

        file.setId(101L);
        file.setName("notes.md");
        file.setMimeType("text/markdown");
        file.setSizeBytes(42L);

        when(facade.findAllByWorkspaceId(eq(11L), isNull())).thenReturn(List.of(file));

        ListWorkspaceFilesToolCallback callback = new ListWorkspaceFilesToolCallback(facade, context);

        String result = callback.call("{}");

        assertThat(result).contains("\"id\":101");
        assertThat(result).contains("\"name\":\"notes.md\"");
        assertThat(result).contains("\"mimeType\":\"text/markdown\"");
    }

    @Test
    void testCallPrefersToolContextOverProvider() {
        WorkspaceFile file = new WorkspaceFile();

        file.setId(202L);
        file.setName("spec.md");
        file.setMimeType("text/markdown");
        file.setSizeBytes(50L);

        when(facade.findAllByWorkspaceId(eq(42L), isNull())).thenReturn(List.of(file));

        ListWorkspaceFilesToolCallback callback = new ListWorkspaceFilesToolCallback(facade, context);

        ToolContext toolContext = new ToolContext(
            Map.of(AgUiToolContextWorkspaceContextProvider.TOOL_CONTEXT_WORKSPACE_ID_KEY, 42L));

        String result = callback.call("{}", toolContext);

        assertThat(result).contains("\"id\":202");

        verify(context, never()).currentWorkspaceId();
    }

    @Test
    void testCallReturnsErrorWhenWorkspaceContextMissing() {
        when(context.currentWorkspaceId()).thenReturn(null);

        ListWorkspaceFilesToolCallback callback = new ListWorkspaceFilesToolCallback(facade, context);

        String result = callback.call("{}");

        assertThat(result).contains("error");
        assertThat(result).containsIgnoringCase("workspace context unavailable");
    }
}
