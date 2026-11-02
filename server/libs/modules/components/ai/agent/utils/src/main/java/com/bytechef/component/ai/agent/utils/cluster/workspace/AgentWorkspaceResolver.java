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
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agent.common.workspace.Workspace;
import org.springframework.util.FileSystemUtils;

/**
 * Resolves the directory an agent's file and shell tools are confined to. Every workspace lives under the configured
 * base directory, separated by tenant, and then by the run's owner: the project deployment or integration instance, its
 * environment and its workflow. Tools of one agent that use the same workspace name share files; nothing outside the
 * workspace is reachable through them.
 *
 * <p>
 * Runs without an owner (editor test runs) get a workspace per job that is removed once it is older than
 * {@link #TEST_WORKSPACE_RETENTION}.
 *
 * @author Ivica Cardic
 */
public class AgentWorkspaceResolver {

    public static final String DEFAULT_WORKSPACE_NAME = "default";
    public static final String WORKSPACE_NAME = "workspaceName";

    static final Duration TEST_WORKSPACE_RETENTION = Duration.ofHours(24);
    static final String TEST_DIRECTORY = "test";

    private static final Logger log = LoggerFactory.getLogger(AgentWorkspaceResolver.class);

    private static final String COMPONENT_NAME = "aiAgentUtils";
    private static final int COMPONENT_VERSION = 1;
    private static final Pattern UNSAFE_SEGMENT_CHARACTERS = Pattern.compile("[^A-Za-z0-9_-]");
    private static final Pattern WORKSPACE_NAME_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private final @Nullable Path basedir;

    /**
     * @param basedir the directory all workspaces are created under, or {@code null} when none is configured, in which
     *                case resolving a workspace fails
     */
    public AgentWorkspaceResolver(@Nullable Path basedir) {
        this.basedir = basedir == null ? null : basedir.toAbsolutePath()
            .normalize();
    }

    public static ModifiableStringProperty workspaceNameProperty() {
        return string(WORKSPACE_NAME)
            .label("Workspace Name")
            .description(
                "Name of the agent's workspace directory. Tools of the same agent with the same workspace name share " +
                    "files. Letters, digits, '-' and '_' only.")
            .defaultValue(DEFAULT_WORKSPACE_NAME)
            .advancedOption(true);
    }

    /**
     * Returns the workspace of the agent run the given context belongs to, creating its directory if needed.
     *
     * @param context            the context the tool's cluster element was applied with
     * @param clusterElementName the name of the tool's cluster element
     * @param workspaceName      the workspace name the tool was configured with, or {@code null} for the default
     * @return the workspace rooted at the agent's own directory
     * @throws IllegalArgumentException if the workspace name is not a single safe path segment
     * @throws IllegalStateException    if no base directory is configured or the run cannot be identified from the
     *                                  context
     */
    public Workspace resolve(Context context, String clusterElementName, @Nullable String workspaceName) {
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
            Path testDirectory = tenantDirectory.resolve(TEST_DIRECTORY);

            removeExpiredTestWorkspaces(testDirectory);

            workspaceDirectory = testDirectory.resolve(toTestRunSegment(actionContextAware))
                .resolve(name);
        } else {
            workspaceDirectory = tenantDirectory.resolve(toPlatformSegment(actionContextAware.getPlatformType()))
                .resolve(String.valueOf(principalId))
                .resolve(toEnvironmentSegment(actionContextAware.getEnvironmentId()))
                .resolve(toWorkflowSegment(actionContextAware))
                .resolve(name);
        }

        return Workspace.local(createDirectory(basedir, workspaceDirectory));
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

    private static void removeExpiredTestWorkspaces(Path testDirectory) {
        if (!Files.isDirectory(testDirectory)) {
            return;
        }

        Instant expiry = Instant.now()
            .minus(TEST_WORKSPACE_RETENTION);

        try (Stream<Path> testWorkspaces = Files.list(testDirectory)) {
            testWorkspaces.filter(testWorkspace -> isExpired(testWorkspace, expiry))
                .forEach(AgentWorkspaceResolver::deleteTestWorkspace);
        } catch (IOException ioException) {
            log.warn("Failed to list the agent test workspaces in {}", testDirectory, ioException);
        }
    }

    private static boolean isExpired(Path testWorkspace, Instant expiry) {
        try {
            FileTime lastModifiedTime = Files.getLastModifiedTime(testWorkspace);

            return lastModifiedTime.toInstant()
                .isBefore(expiry);
        } catch (IOException ioException) {
            return false;
        }
    }

    private static void deleteTestWorkspace(Path testWorkspace) {
        try {
            FileSystemUtils.deleteRecursively(testWorkspace);
        } catch (IOException ioException) {
            log.warn("Failed to delete the expired agent test workspace {}", testWorkspace, ioException);
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

    private static String toPlatformSegment(@Nullable PlatformType platformType) {
        if (platformType == null) {
            return "unknown";
        }

        return platformType.name()
            .toLowerCase(Locale.ROOT);
    }

    private static String toSegment(@Nullable String value) {
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
