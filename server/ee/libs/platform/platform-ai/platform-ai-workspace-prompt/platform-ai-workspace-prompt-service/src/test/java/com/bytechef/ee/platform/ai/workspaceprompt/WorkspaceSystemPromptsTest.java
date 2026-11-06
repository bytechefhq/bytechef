/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.workspaceprompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.workspaceprompt.service.WorkspaceSystemPromptService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class WorkspaceSystemPromptsTest {

    private final WorkspaceSystemPromptService service = mock(WorkspaceSystemPromptService.class);
    private final WorkspaceSystemPrompts workspaceSystemPrompts = new WorkspaceSystemPrompts(service);

    @Test
    void testFetchPromptReturnsStoredPrompt() {
        when(service.fetchWorkspaceSystemPrompt(7L)).thenReturn(Optional.of("Be concise."));

        assertThat(workspaceSystemPrompts.fetchPrompt(7L)).isEqualTo("Be concise.");
    }

    @Test
    void testFetchPromptReturnsNullForNullWorkspace() {
        assertThat(workspaceSystemPrompts.fetchPrompt(null)).isNull();
    }

    @Test
    void testFetchPromptMemoizesLookups() {
        when(service.fetchWorkspaceSystemPrompt(7L)).thenReturn(Optional.of("Be concise."));

        workspaceSystemPrompts.fetchPrompt(7L);
        workspaceSystemPrompts.fetchPrompt(7L);

        verify(service, times(1)).fetchWorkspaceSystemPrompt(7L);
    }

    @Test
    void testFetchPromptFailsOpenOnLookupError() {
        when(service.fetchWorkspaceSystemPrompt(7L)).thenThrow(new IllegalStateException("db down"));

        assertThat(workspaceSystemPrompts.fetchPrompt(7L)).isNull();
    }
}
