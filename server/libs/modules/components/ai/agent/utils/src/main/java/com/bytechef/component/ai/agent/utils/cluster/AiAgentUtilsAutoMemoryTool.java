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

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.ai.agent.memory.AutoMemoryDirectoryOps;
import com.bytechef.platform.ai.agent.memory.AutoMemoryTools;
import com.bytechef.platform.ai.agent.memory.MemoryResourceResolver;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

/**
 * Provides persistent long-term memory scoped to the running workflow's principal. The memory is backed by
 * {@link AiAutoMemoryService} and the scope is resolved per agent run from the action context, by platform type:
 * <ul>
 * <li>{@link PlatformType#AUTOMATION} — owned by the project deployment
 * ({@link AiAutoMemoryPrincipalType#PROJECT_DEPLOYMENT}); the principal id is the job principal (deployment) id and the
 * workspace id is read from the trigger-injected {@code __jobParameters.contextStore.workspaceId} job metadata entry.
 * Connected-user embedded workflows run under AUTOMATION too, so they are covered here.</li>
 * <li>{@link PlatformType#EMBEDDED} — owned by the integration instance
 * ({@link AiAutoMemoryPrincipalType#INTEGRATION_INSTANCE}); the principal id is the job principal (integration
 * instance) id and the workspace is the {@link Workspace#DEFAULT_WORKSPACE_ID} bucket. Embedded iPaaS has no
 * deployment&rarr;workspace chain, so the globally-unique integration-instance id is the isolation axis and the
 * discriminator keeps it from colliding with deployment-owned rows.</li>
 * </ul>
 * When the scope cannot be resolved (e.g. an editor test run, an unknown platform, or a missing principal/workspace),
 * an inert empty tool provider is returned.
 *
 * @author Ivica Cardic
 */
public class AiAgentUtilsAutoMemoryTool {

    /**
     * Reserved job metadata key under which trigger-time {@code JobParameter} overrides are stored.
     */
    private static final String JOB_PARAMETERS_METADATA_KEY = "__jobParameters";

    /**
     * JobParameter key set by the context-store workflow-context contributor carrying the running workflow's workspace
     * id.
     */
    private static final String WORKSPACE_ID_JOB_PARAM_KEY = "contextStore.workspaceId";

    private final AiAutoMemoryService aiAutoMemoryService;

    public final ClusterElementDefinition<ToolCallbackProviderFunction> clusterElementDefinition;

    @SuppressFBWarnings("EI")
    public AiAgentUtilsAutoMemoryTool(AiAutoMemoryService aiAutoMemoryService) {
        this.aiAutoMemoryService = aiAutoMemoryService;

        this.clusterElementDefinition = ComponentDsl.<ToolCallbackProviderFunction>clusterElement("autoMemoryTool")
            .title("Auto Memory Tool")
            .description("Persistent long-term memory scoped to the running workflow deployment.")
            .type(TOOLS)
            .object(() -> this::apply);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private ToolCallbackProvider apply(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        ActionContextAware aware = (ActionContextAware) context;

        PlatformType platformType = aware.getPlatformType();
        Long principalId = aware.getJobPrincipalId();
        Long environmentId = aware.getEnvironmentId();

        int environment = environmentId == null ? 0 : environmentId.intValue();

        AiAutoMemoryPrincipalType principalType = null;
        Long workspaceId = null;

        if (platformType == PlatformType.AUTOMATION) {
            principalType = AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT;
            workspaceId = extractWorkspaceId(aware.getJobMetadata());
        } else if (platformType == PlatformType.EMBEDDED) {
            principalType = AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE;
            workspaceId = Workspace.DEFAULT_WORKSPACE_ID;
        }

        if (principalType == null || principalId == null || workspaceId == null) {
            return ToolCallbackProvider.from(List.of());
        }

        MemoryResourceResolver resolver = new AutoMemoryResourceResolver(
            aiAutoMemoryService, workspaceId, principalType, principalId, environment);
        AutoMemoryDirectoryOps directoryOps = new DbBackedAutoMemoryDirectoryOps(
            aiAutoMemoryService, workspaceId, principalType, principalId, environment);

        AutoMemoryTools autoMemoryTools = new AutoMemoryTools(resolver, directoryOps);

        ToolCallback[] callbacks = MethodToolCallbackProvider.builder()
            .toolObjects(autoMemoryTools)
            .build()
            .getToolCallbacks();

        return ToolCallbackProvider.from(List.of(callbacks));
    }

    @SuppressWarnings("unchecked")
    private static Long extractWorkspaceId(Map<String, Object> jobMetadata) {
        Object jobParameters = jobMetadata.get(JOB_PARAMETERS_METADATA_KEY);

        if (!(jobParameters instanceof Map<?, ?> map)) {
            return null;
        }

        Object value = ((Map<String, ?>) map).get(WORKSPACE_ID_JOB_PARAM_KEY);

        if (value instanceof Number number) {
            return number.longValue();
        }

        return null;
    }
}
