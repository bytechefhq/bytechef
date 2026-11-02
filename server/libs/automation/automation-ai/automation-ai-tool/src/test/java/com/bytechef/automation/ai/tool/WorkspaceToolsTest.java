/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.tool.model.WorkspaceInfo;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class WorkspaceToolsTest {

    private final WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

    @Test
    void testListWorkspacesMapsTheAccessibleWorkspaces() {
        when(workspaceFacade.getMyWorkspaces()).thenReturn(
            List.of(new Workspace(1L, "Default"), new Workspace(2L, "Marketing")));

        List<WorkspaceInfo> workspaceInfos = new WorkspaceTools(workspaceFacade).listWorkspaces();

        assertThat(workspaceInfos).containsExactly(new WorkspaceInfo(1L, "Default"),
            new WorkspaceInfo(2L, "Marketing"));
    }

    @Test
    void testListWorkspacesReturnsAnEmptyListWhenNoWorkspaceIsAccessible() {
        when(workspaceFacade.getMyWorkspaces()).thenReturn(List.of());

        assertThat(new WorkspaceTools(workspaceFacade).listWorkspaces()).isEmpty();
    }
}
