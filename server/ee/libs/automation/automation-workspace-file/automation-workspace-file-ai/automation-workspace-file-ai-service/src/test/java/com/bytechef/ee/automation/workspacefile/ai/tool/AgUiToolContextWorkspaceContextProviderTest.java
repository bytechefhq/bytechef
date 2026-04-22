/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workspacefile.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AgUiToolContextWorkspaceContextProviderTest {

    @Test
    void testReturnsNullWhenNoContextBound() {
        AgUiToolContextWorkspaceContextProvider provider = new AgUiToolContextWorkspaceContextProvider();

        assertThat(provider.currentWorkspaceId()).isNull();
        assertThat(provider.currentSourceOrdinal()).isNull();
        assertThat(provider.lastUserPrompt()).isNull();
    }

    @Test
    void testRunWithContextExposesValuesAndRestoresAfterwards() {
        AgUiToolContextWorkspaceContextProvider provider = new AgUiToolContextWorkspaceContextProvider();

        WorkspaceInvocationContext context = new WorkspaceInvocationContext(99L, (short) 2, "make a file");

        AgUiToolContextWorkspaceContextProvider.runWithContext(context, () -> {
            assertThat(provider.currentWorkspaceId()).isEqualTo(99L);
            assertThat(provider.currentSourceOrdinal()).isEqualTo((short) 2);
            assertThat(provider.lastUserPrompt()).isEqualTo("make a file");
        });

        assertThat(provider.currentWorkspaceId()).isNull();
    }

    @Test
    void testNestedRunWithContextRestoresPrevious() {
        AgUiToolContextWorkspaceContextProvider provider = new AgUiToolContextWorkspaceContextProvider();

        WorkspaceInvocationContext outer = new WorkspaceInvocationContext(1L, (short) 0, "outer");
        WorkspaceInvocationContext inner = new WorkspaceInvocationContext(2L, (short) 1, "inner");

        AgUiToolContextWorkspaceContextProvider.runWithContext(outer, () -> {
            assertThat(provider.currentWorkspaceId()).isEqualTo(1L);

            AgUiToolContextWorkspaceContextProvider.runWithContext(inner, () -> {
                assertThat(provider.currentWorkspaceId()).isEqualTo(2L);
                assertThat(provider.lastUserPrompt()).isEqualTo("inner");
            });

            assertThat(provider.currentWorkspaceId()).isEqualTo(1L);
        });

        assertThat(provider.currentWorkspaceId()).isNull();
    }
}
