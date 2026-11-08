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
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.JobContextAware;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.tenant.TenantContext;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public class AgentWorkspaceResolver {

    public static final String DEFAULT_WORKSPACE_NAME = "default";
    public static final String WORKSPACE_NAME = "workspaceName";

    static final String TEST_DIRECTORY = "test";

    private static final String COMPONENT_NAME = "aiAgentUtils";
    private static final int COMPONENT_VERSION = 1;
    private static final Pattern UNSAFE_SEGMENT_CHARACTERS = Pattern.compile("[^A-Za-z0-9_-]");
    private static final Pattern WORKSPACE_NAME_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,64}");

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
            .defaultValue(DEFAULT_WORKSPACE_NAME)
            .advancedOption(true);
    }

    public AgentWorkspace resolve(Context context, String clusterElementName, @Nullable String workspaceName) {
        if (basedir == null) {
            throw new IllegalStateException(
                "Agent workspaces are not configured: set bytechef.ai.agent-utils.workspace.basedir");
        }

        String name = validateWorkspaceName(workspaceName);

        ActionContextAware actionContextAware = toActionContextAware(context, clusterElementName);

        Path tenantDirectory = basedir.resolve(toSegment(TenantContext.getCurrentTenantId()));

        Path workspaceDirectory;

        Long principalId = actionContextAware.getJobPrincipalId();

        if (!isId(principalId)) {
            workspaceDirectory = tenantDirectory.resolve(TEST_DIRECTORY)
                .resolve(toTestRunSegment(actionContextAware))
                .resolve(name);
        } else {
            workspaceDirectory = tenantDirectory.resolve(toPlatformSegment(actionContextAware.getPlatformType()))
                .resolve(String.valueOf(principalId))
                .resolve(toEnvironmentSegment(actionContextAware.getEnvironmentId()))
                .resolve(toWorkflowSegment(actionContextAware))
                .resolve(name);
        }

        return AgentWorkspace.sandboxed(createDirectory(basedir, workspaceDirectory));
    }

    private static Path createDirectory(Path basedir, Path workspaceDirectory) {
        Path normalizedWorkspaceDirectory = workspaceDirectory.toAbsolutePath()
            .normalize();

        if (!normalizedWorkspaceDirectory.startsWith(basedir)) {
            throw new IllegalStateException(
                "Agent workspace %s is outside the workspace base directory".formatted(normalizedWorkspaceDirectory));
        }

        try {
            return Files.createDirectories(normalizedWorkspaceDirectory);
        } catch (IOException ioException) {
            throw new UncheckedIOException(
                "Failed to create the agent workspace " + normalizedWorkspaceDirectory, ioException);
        }
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

    private static boolean isId(@Nullable Long id) {
        return id != null && id > 0;
    }

    private static String toEnvironmentSegment(@Nullable Long environmentId) {
        Environment[] environments = Environment.values();

        if (environmentId == null || environmentId < 0 || environmentId >= environments.length) {
            return "environment-" + environmentId;
        }

        Environment environment = environments[environmentId.intValue()];

        return environment.name()
            .toLowerCase(Locale.ROOT);
    }

    static String toPlatformSegment(@Nullable PlatformType platformType) {
        if (platformType == null) {
            return "unknown";
        }

        return platformType.name()
            .toLowerCase(Locale.ROOT);
    }

    static String toSegment(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return "_";
        }

        return UNSAFE_SEGMENT_CHARACTERS.matcher(value)
            .replaceAll("_");
    }

    private static String toTestRunSegment(ActionContextAware actionContextAware) {
        Long jobId = actionContextAware.getJobId();

        if (isId(jobId)) {
            return "job-" + jobId;
        }

        return "workflow-" + toSegment(actionContextAware.getWorkflowId());
    }

    private static String toWorkflowSegment(ActionContextAware actionContextAware) {
        Long principalWorkflowId = actionContextAware.getJobPrincipalWorkflowId();

        if (isId(principalWorkflowId)) {
            return String.valueOf(principalWorkflowId);
        }

        return "workflow-" + toSegment(actionContextAware.getWorkflowId());
    }

    private static String validateWorkspaceName(@Nullable String workspaceName) {
        if (workspaceName == null || workspaceName.isBlank()) {
            return DEFAULT_WORKSPACE_NAME;
        }

        if (!WORKSPACE_NAME_PATTERN.matcher(workspaceName)
            .matches()) {

            throw new IllegalArgumentException(
                "Workspace name '%s' is invalid: use 1 to 64 letters, digits, '-' or '_'".formatted(workspaceName));
        }

        return workspaceName;
    }
}
