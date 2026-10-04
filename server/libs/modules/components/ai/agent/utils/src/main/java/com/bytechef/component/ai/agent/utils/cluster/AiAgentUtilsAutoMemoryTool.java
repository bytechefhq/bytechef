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

package com.bytechef.component.ai.agent.utils.cluster;

import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.ai.agent.memory.AutoMemoryDirectoryOps;
import com.bytechef.platform.ai.agent.memory.AutoMemoryTools;
import com.bytechef.platform.ai.agent.memory.MemoryResourceResolver;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryOwner;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.JobContextAware;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.NoSuchElementException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

/**
 * @author Ivica Cardic
 */
public class AiAgentUtilsAutoMemoryTool {

    private static final Logger log = LoggerFactory.getLogger(AiAgentUtilsAutoMemoryTool.class);

    private static final String NO_JOB_CONTEXT_REASON =
        "this agent was invoked from a context that does not identify the running workflow, so there is no memory "
            + "owner.";

    private static final String NOT_STORED_REASON =
        "this ByteChef installation runs as microservices, which do not store memory; memory requires the ByteChef "
            + "server application.";

    private static final String NO_RUNNING_JOB_REASON =
        "this run is not a deployed workflow or integration run (for example an editor test run), so there is no "
            + "memory owner. Memory works once the workflow is deployed.";

    private final AiAutoMemoryService aiAutoMemoryService;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectService projectService;

    public final ClusterElementDefinition<ToolCallbackProviderFunction> clusterElementDefinition;

    @SuppressFBWarnings("EI")
    public AiAgentUtilsAutoMemoryTool(
        AiAutoMemoryService aiAutoMemoryService, ProjectDeploymentService projectDeploymentService,
        ProjectService projectService) {

        this.aiAutoMemoryService = aiAutoMemoryService;
        this.projectDeploymentService = projectDeploymentService;
        this.projectService = projectService;

        this.clusterElementDefinition = ComponentDsl.<ToolCallbackProviderFunction>clusterElement("autoMemoryTool")
            .title("Auto Memory Tool")
            .description(
                "Persistent long-term memory for the agent, shared by all workflows of a project deployment (or " +
                    "kept per integration instance for embedded integrations) and per environment. Not available in " +
                    "editor test runs.")
            .type(TOOLS)
            .object(() -> this::apply);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private ToolCallbackProvider apply(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        OwnerResolution ownerResolution = resolveOwner(context);

        AiAutoMemoryOwner owner = ownerResolution.owner();
        String unavailableReason = ownerResolution.unavailableReason();

        AutoMemoryTools autoMemoryTools;

        if (owner == null) {
            log.info("Auto memory is unavailable for this run: {}", unavailableReason);

            UnavailableAutoMemory unavailableAutoMemory = new UnavailableAutoMemory(
                "Memory is unavailable: " + unavailableReason);

            autoMemoryTools = new AutoMemoryTools(unavailableAutoMemory, unavailableAutoMemory);
        } else {
            MemoryResourceResolver resolver = new AutoMemoryResourceResolver(aiAutoMemoryService, owner);
            AutoMemoryDirectoryOps directoryOps = new ServiceBackedAutoMemoryDirectoryOps(aiAutoMemoryService, owner);

            autoMemoryTools = new AutoMemoryTools(resolver, directoryOps);
        }

        ToolCallback[] callbacks = MethodToolCallbackProvider.builder()
            .toolObjects(autoMemoryTools)
            .build()
            .getToolCallbacks();

        return ToolCallbackProvider.from(List.of(callbacks));
    }

    private OwnerResolution resolveOwner(Context context) {
        if (context instanceof ActionContextAware actionContextAware) {
            return resolveOwner(actionContextAware);
        }

        if (context instanceof JobContextAware jobContextAware && jobContextAware.toActionContext(
            "aiAgentUtils", 1, "autoMemoryTool", null) instanceof ActionContextAware actionContextAware) {

            return resolveOwner(actionContextAware);
        }

        log.warn("Auto memory cannot identify the running workflow from a {} context", context.getClass());

        return OwnerResolution.unavailable(NO_JOB_CONTEXT_REASON);
    }

    private OwnerResolution resolveOwner(ActionContextAware actionContextAware) {
        PlatformType platformType = actionContextAware.getPlatformType();
        Long principalId = actionContextAware.getJobPrincipalId();
        Long environmentId = actionContextAware.getEnvironmentId();

        if (principalId == null) {
            return OwnerResolution.unavailable(NO_RUNNING_JOB_REASON);
        }

        Environment environment = toEnvironment(environmentId);

        if (environment == null) {
            log.warn("Unknown environment id {} for auto memory principal {}", environmentId, principalId);

            return OwnerResolution.unavailable("this run's environment is unknown.");
        }

        if (!aiAutoMemoryService.isAvailable()) {
            return OwnerResolution.unavailable(NOT_STORED_REASON);
        }

        if (platformType == PlatformType.AUTOMATION) {
            Long workspaceId = findWorkspaceId(principalId);

            if (workspaceId == null) {
                return OwnerResolution.unavailable("this run's deployment could not be found.");
            }

            return OwnerResolution.of(
                new AiAutoMemoryOwner(
                    workspaceId, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, principalId, environment));
        }

        if (platformType == PlatformType.EMBEDDED) {
            return OwnerResolution.of(
                new AiAutoMemoryOwner(
                    Workspace.DEFAULT_WORKSPACE_ID, AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE, principalId,
                    environment));
        }

        log.warn("Unsupported platform type {} for auto memory principal {}", platformType, principalId);

        return OwnerResolution.unavailable("this kind of workflow does not support memory.");
    }

    private static @Nullable Environment toEnvironment(@Nullable Long environmentId) {
        Environment[] environments = Environment.values();

        if (environmentId == null || environmentId < 0 || environmentId >= environments.length) {
            return null;
        }

        return environments[environmentId.intValue()];
    }

    private @Nullable Long findWorkspaceId(long projectDeploymentId) {
        try {
            ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(projectDeploymentId);

            Project project = projectService.getProject(projectDeployment.getProjectId());

            return project.getWorkspaceId();
        } catch (NoSuchElementException exception) {
            log.warn("Could not resolve the workspace of project deployment {}", projectDeploymentId, exception);

            return null;
        }
    }

    private record OwnerResolution(@Nullable AiAutoMemoryOwner owner, @Nullable String unavailableReason) {

        static OwnerResolution of(AiAutoMemoryOwner owner) {
            return new OwnerResolution(owner, null);
        }

        static OwnerResolution unavailable(String reason) {
            return new OwnerResolution(null, reason);
        }
    }
}
