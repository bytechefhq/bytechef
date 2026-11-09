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

package com.bytechef.component.ai.agent.utils.cluster.workspace;

import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.Context;
import com.bytechef.platform.ai.workspace.AgentWorkspaceLayout;
import com.bytechef.platform.ai.workspace.AgentWorkspaceQuota;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.JobContextAware;
import java.nio.file.Path;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public class AgentWorkspaceResolver {

    public static final String WORKSPACE_NAME = "workspaceName";

    private static final String COMPONENT_NAME = "aiAgentUtils";
    private static final int COMPONENT_VERSION = 1;

    private final @Nullable Path basedir;
    private final long workspaceMaxBytes;

    public AgentWorkspaceResolver(@Nullable Path basedir) {
        this(basedir, 0);
    }

    public AgentWorkspaceResolver(@Nullable Path basedir, long workspaceMaxBytes) {
        this.basedir = basedir == null ? null : basedir.toAbsolutePath()
            .normalize();
        this.workspaceMaxBytes = workspaceMaxBytes;
    }

    public AgentWorkspaceQuota getQuota(AgentWorkspace workspace) {
        return new AgentWorkspaceQuota(workspace.root(), workspaceMaxBytes);
    }

    public static ModifiableStringProperty workspaceNameProperty() {
        return string(WORKSPACE_NAME)
            .label("Workspace Name")
            .description(
                "Name of the workspace directory. Tools in this workflow with the same workspace name share files, " +
                    "including tools of different AI Agents; tools with different names do not. Letters, digits, " +
                    "'-' and '_' only.")
            .defaultValue(AgentWorkspaceLayout.DEFAULT_WORKSPACE_NAME)
            .advancedOption(true);
    }

    public AgentWorkspace resolve(Context context, String clusterElementName, @Nullable String workspaceName) {
        if (basedir == null) {
            throw new IllegalStateException(
                "Agent workspaces are not configured: set bytechef.ai.agent-utils.workspace.basedir");
        }

        ActionContextAware actionContextAware = toActionContextAware(context, clusterElementName);

        return AgentWorkspace.sandboxed(AgentWorkspaceLayout.resolve(basedir, actionContextAware, workspaceName));
    }

    private static ActionContextAware toActionContextAware(Context context, String clusterElementName) {
        if (context instanceof ActionContextAware actionContextAware) {
            return actionContextAware;
        }

        if (context instanceof JobContextAware jobContextAware && jobContextAware.toActionContext(
            COMPONENT_NAME, COMPONENT_VERSION, clusterElementName,
            null) instanceof ActionContextAware actionContextAware) {

            return actionContextAware;
        }

        throw new IllegalStateException(
            "The agent workspace cannot be resolved because the running workflow cannot be identified from a %s"
                .formatted(context.getClass()
                    .getName()));
    }
}
