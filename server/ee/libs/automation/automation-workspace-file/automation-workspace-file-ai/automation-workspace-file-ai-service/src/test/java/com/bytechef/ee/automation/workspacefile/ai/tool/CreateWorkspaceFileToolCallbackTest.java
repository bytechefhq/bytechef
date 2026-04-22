/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workspacefile.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.workspacefile.domain.WorkspaceFile;
import com.bytechef.automation.workspacefile.service.WorkspaceFileFacade;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
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
class CreateWorkspaceFileToolCallbackTest {

    @Mock
    private WorkspaceFileFacade facade;

    @Mock
    private WorkspaceContextProvider context;

    private CreateWorkspaceFileToolCallback callback;

    @BeforeEach
    void setUp() {
        callback = new CreateWorkspaceFileToolCallback(facade, context);
    }

    @Test
    void testCallHappyPath() {
        when(context.currentWorkspaceId()).thenReturn(7L);
        when(context.currentSourceOrdinal()).thenReturn((short) 0);
        when(context.lastUserPrompt()).thenReturn("write a runbook");

        WorkspaceFile saved = new WorkspaceFile();

        saved.setId(42L);
        saved.setName("runbook.md");
        saved.setSizeBytes(10L);

        when(
            facade.createFromAi(
                eq(7L), eq("runbook.md"), eq("text/markdown"), eq("# runbook"),
                eq((short) 0), eq("write a runbook")))
                    .thenReturn(saved);

        String input = "{\"filename\":\"runbook.md\",\"mimeType\":\"text/markdown\",\"content\":\"# runbook\"}";

        String result = callback.call(input);

        assertThat(result).contains("\"id\":42");
        assertThat(result).contains("\"name\":\"runbook.md\"");
    }

    @Test
    void testCallRejectsDisallowedMime() {
        String input = "{\"filename\":\"x.png\",\"mimeType\":\"image/png\",\"content\":\"...\"}";

        String result = callback.call(input);

        assertThat(result).contains("error");
        assertThat(result).containsIgnoringCase("mime");
    }

    @Test
    void testCallPrefersToolContextOverProvider() {
        WorkspaceFile saved = new WorkspaceFile();

        saved.setId(5L);
        saved.setName("doc.md");
        saved.setSizeBytes(3L);

        when(
            facade.createFromAi(
                eq(88L), eq("doc.md"), eq("text/markdown"), eq("body"),
                eq((short) 7), eq("tool-context prompt")))
                    .thenReturn(saved);

        ToolContext toolContext = new ToolContext(Map.of(
            AgUiToolContextWorkspaceContextProvider.TOOL_CONTEXT_WORKSPACE_ID_KEY, 88L,
            AgUiToolContextWorkspaceContextProvider.TOOL_CONTEXT_SOURCE_ORDINAL_KEY, (short) 7,
            AgUiToolContextWorkspaceContextProvider.TOOL_CONTEXT_LAST_USER_PROMPT_KEY, "tool-context prompt"));

        String input = "{\"filename\":\"doc.md\",\"mimeType\":\"text/markdown\",\"content\":\"body\"}";

        String result = callback.call(input, toolContext);

        assertThat(result).contains("\"id\":5");

        verify(context, never()).currentWorkspaceId();
    }

    @Test
    void testCallReturnsErrorWhenWorkspaceContextMissing() {
        when(context.currentWorkspaceId()).thenReturn(null);

        String input = "{\"filename\":\"doc.md\",\"mimeType\":\"text/markdown\",\"content\":\"body\"}";

        String result = callback.call(input);

        assertThat(result).contains("error");
        assertThat(result).containsIgnoringCase("workspace context unavailable");
    }
}
