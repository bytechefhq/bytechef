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

import com.bytechef.automation.ai.tool.model.WorkspaceInfo;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class WorkspaceTools {

    private final WorkspaceFacade workspaceFacade;

    @SuppressFBWarnings("EI")
    public WorkspaceTools(WorkspaceFacade workspaceFacade) {
        this.workspaceFacade = workspaceFacade;
    }

    @Tool(
        description = "List the workspaces the current user can access (all workspaces for a tenant admin). Returns each workspace's id and name; pass the id as workspaceId to the tools that work within a workspace.")
    public List<WorkspaceInfo> listWorkspaces() {
        return workspaceFacade.getMyWorkspaces()
            .stream()
            .map(workspace -> new WorkspaceInfo(workspace.getId(), workspace.getName()))
            .toList();
    }
}
