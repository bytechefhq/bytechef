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

package com.bytechef.component.claude.code.workspace;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.ai.workspace.AgentWorkspaceLayout;
import com.bytechef.platform.ai.workspace.AgentWorkspaceQuota;
import com.bytechef.platform.component.definition.ActionContextAware;
import java.nio.file.Path;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public class ClaudeCodeWorkspaceResolver {

    private final @Nullable Path basedir;
    private final long workspaceMaxBytes;

    public ClaudeCodeWorkspaceResolver(@Nullable String basedir, long workspaceMaxBytes) {
        this.basedir = basedir == null || basedir.isBlank() ? null : Path.of(basedir)
            .toAbsolutePath()
            .normalize();
        this.workspaceMaxBytes = workspaceMaxBytes;
    }

    public AgentWorkspaceQuota getQuota(Path workspace) {
        return new AgentWorkspaceQuota(workspace, workspaceMaxBytes);
    }

    public Path resolve(ActionContext context, @Nullable String workspaceName) {
        if (basedir == null) {
            throw new IllegalStateException(
                "Claude Code workspaces are not configured: set bytechef.component.claude-code.workspace.basedir");
        }

        if (!(context instanceof ActionContextAware actionContextAware)) {
            throw new IllegalStateException(
                "The Claude Code workspace cannot be resolved because the running workflow cannot be identified " +
                    "from a " + context.getClass()
                        .getName());
        }

        return AgentWorkspaceLayout.resolve(basedir, actionContextAware, workspaceName);
    }
}
