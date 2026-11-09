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

package com.bytechef.platform.ai.workspace;

import com.bytechef.platform.component.definition.ActionContextAware;
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
public final class AgentWorkspaceLayout {

    public static final String DEFAULT_WORKSPACE_NAME = "default";
    public static final String TEST_DIRECTORY = "test";

    private static final Pattern UNSAFE_SEGMENT_CHARACTERS = Pattern.compile("[^A-Za-z0-9_-]");
    private static final Pattern WORKSPACE_NAME_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private AgentWorkspaceLayout() {
    }

    public static Path resolve(Path basedir, ActionContextAware actionContextAware, @Nullable String workspaceName) {
        Path normalizedBasedir = basedir.toAbsolutePath()
            .normalize();

        String name = validateWorkspaceName(workspaceName);

        Path tenantDirectory = normalizedBasedir.resolve(toSegment(TenantContext.getCurrentTenantId()));

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

        return createDirectory(normalizedBasedir, workspaceDirectory);
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
