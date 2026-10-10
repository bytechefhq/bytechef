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

package com.bytechef.component.ai.agent.utils;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;
import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;
import static com.bytechef.component.definition.ai.agent.SubagentFunction.SUBAGENT;
import static com.bytechef.platform.component.definition.ai.agent.ModelFunction.MODEL;

import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.utils.action.AiAgentUtilsAppendFilesToAiSkillAction;
import com.bytechef.component.ai.agent.utils.action.AiAgentUtilsCreateAiSkillAction;
import com.bytechef.component.ai.agent.utils.action.AiAgentUtilsDeleteAiSkillAction;
import com.bytechef.component.ai.agent.utils.action.AiAgentUtilsRemoveFileFromAiSkillAction;
import com.bytechef.component.ai.agent.utils.action.AiAgentUtilsUpdateAiSkillAction;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsAskUserQuestionTool;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsAutoMemoryTool;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsBraveWebSearchTool;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsFileSystemTools;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsGlobTool;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsGrepTool;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsListDirectoryTool;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsShellTools;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsSmartWebFetchTool;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsSubagentTool;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsTaskTool;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsTodoWriteTool;
import com.bytechef.component.ai.agent.utils.cluster.workspace.AgentWorkspaceResolver;
import com.bytechef.component.ai.llm.facade.AiAgentToolFacade;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import com.bytechef.platform.ai.skill.facade.AiSkillFacade;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.ClusterRootComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component("aiAgentUtils_v1_ComponentHandler")
public class AiAgentUtilsComponentHandler implements ComponentHandler {

    private static final String SMART_WEB_FETCH_TOOL = "smartWebFetchTool";
    private static final String SUBAGENT_TOOL = "subagentTool";
    private static final String TASK_TOOL = "taskTool";

    private final ComponentDefinition componentDefinition;

    public AiAgentUtilsComponentHandler(
        AiAgentToolFacade aiAgentToolFacade, AiSkillFacade aiSkillFacade, ApplicationProperties applicationProperties,
        List<AiAgentUtilsClusterElementContributor> clusterElementContributors,
        ClusterElementDefinitionService clusterElementDefinitionService, AiAutoMemoryService aiAutoMemoryService,
        ProjectDeploymentService projectDeploymentService, ProjectService projectService) {

        ApplicationProperties.Ai.AgentUtils agentUtils = applicationProperties.getAi()
            .getAgentUtils();

        Set<String> allowedPrivateHosts = getAllowedPrivateHosts(agentUtils);
        AgentWorkspaceResolver agentWorkspaceResolver = createAgentWorkspaceResolver(agentUtils);

        AiAgentUtilsAutoMemoryTool agentUtilsAutoMemoryTool = new AiAgentUtilsAutoMemoryTool(
            aiAutoMemoryService, projectDeploymentService, projectService);
        AiAgentUtilsSmartWebFetchTool agentUtilsSmartWebFetchTool = new AiAgentUtilsSmartWebFetchTool(
            clusterElementDefinitionService, allowedPrivateHosts);
        AiAgentUtilsTaskTool agentUtilsTaskTool = new AiAgentUtilsTaskTool(
            aiAgentToolFacade, clusterElementDefinitionService);

        List<ClusterElementDefinition<?>> clusterElements = new ArrayList<>(List.of(
            AiAgentUtilsAskUserQuestionTool.CLUSTER_ELEMENT_DEFINITION,
            new AiAgentUtilsFileSystemTools(agentWorkspaceResolver).clusterElementDefinition,
            new AiAgentUtilsGrepTool(agentWorkspaceResolver).clusterElementDefinition,
            new AiAgentUtilsGlobTool(agentWorkspaceResolver).clusterElementDefinition,
            new AiAgentUtilsListDirectoryTool(agentWorkspaceResolver).clusterElementDefinition,
            agentUtilsSmartWebFetchTool.clusterElementDefinition,
            AiAgentUtilsBraveWebSearchTool.CLUSTER_ELEMENT_DEFINITION,
            agentUtilsAutoMemoryTool.clusterElementDefinition,
            AiAgentUtilsTodoWriteTool.CLUSTER_ELEMENT_DEFINITION,
            agentUtilsTaskTool.clusterElementDefinition,
            AiAgentUtilsSubagentTool.CLUSTER_ELEMENT_DEFINITION,
            tool(AiAgentUtilsAppendFilesToAiSkillAction.of(aiSkillFacade)),
            tool(AiAgentUtilsCreateAiSkillAction.of(aiSkillFacade)),
            tool(AiAgentUtilsDeleteAiSkillAction.of(aiSkillFacade)),
            tool(AiAgentUtilsRemoveFileFromAiSkillAction.of(aiSkillFacade)),
            tool(AiAgentUtilsUpdateAiSkillAction.of(aiSkillFacade))));

        ApplicationProperties.Ai.AgentUtils.Shell shell = agentUtils.getShell();

        if (shell.isEnabled()) {
            clusterElements.add(new AiAgentUtilsShellTools(agentWorkspaceResolver).clusterElementDefinition);
        }

        for (AiAgentUtilsClusterElementContributor clusterElementContributor : clusterElementContributors) {
            clusterElements.add(clusterElementContributor.getClusterElementDefinition());
        }

        List<ActionDefinition> actions = List.of(
            AiAgentUtilsAppendFilesToAiSkillAction.of(aiSkillFacade),
            AiAgentUtilsCreateAiSkillAction.of(aiSkillFacade),
            AiAgentUtilsDeleteAiSkillAction.of(aiSkillFacade),
            AiAgentUtilsRemoveFileFromAiSkillAction.of(aiSkillFacade),
            AiAgentUtilsUpdateAiSkillAction.of(aiSkillFacade));

        this.componentDefinition = new AiAgentUtilsComponentDefinitionImpl(
            component("aiAgentUtils")
                .title("AI Agent Utils")
                .description("AI Agent Utils brings Claude Code-inspired tools and agent skills.")
                .icon("path:assets/agent-utils.svg")
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .actions(actions)
                .clusterElements(clusterElements),
            buildActionClusterElementTypes(actions), buildClusterElementClusterElementTypes(clusterElements));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static AgentWorkspaceResolver createAgentWorkspaceResolver(ApplicationProperties.Ai.AgentUtils agentUtils) {
        ApplicationProperties.Ai.AgentUtils.Workspace workspace = agentUtils.getWorkspace();

        String basedir = workspace.getBasedir();

        if (basedir == null || basedir.isBlank()) {
            return new AgentWorkspaceResolver(null);
        }

        return new AgentWorkspaceResolver(Path.of(basedir));
    }

    private static Set<String> getAllowedPrivateHosts(ApplicationProperties.Ai.AgentUtils agentUtils) {
        List<String> allowedPrivateHosts = agentUtils.getAllowedPrivateHosts();

        if (allowedPrivateHosts == null) {
            return Set.of();
        }

        return Set.copyOf(allowedPrivateHosts);
    }

    private static Map<String, List<String>> buildActionClusterElementTypes(List<ActionDefinition> actions) {
        Map<String, List<String>> actionClusterElementTypes = new HashMap<>();

        for (ActionDefinition action : actions) {
            actionClusterElementTypes.put(action.getName(), List.of());
        }

        return actionClusterElementTypes;
    }

    private static Map<String, List<String>> buildClusterElementClusterElementTypes(
        List<ClusterElementDefinition<?>> clusterElements) {

        Map<String, List<String>> clusterElementClusterElementTypes = new HashMap<>();

        for (ClusterElementDefinition<?> clusterElement : clusterElements) {
            clusterElementClusterElementTypes.put(clusterElement.getName(), List.of());
        }

        clusterElementClusterElementTypes.put(TASK_TOOL, List.of(MODEL.name(), SUBAGENT.name()));
        clusterElementClusterElementTypes.put(SUBAGENT_TOOL, List.of(MODEL.name(), TOOLS.name()));
        clusterElementClusterElementTypes.put(SMART_WEB_FETCH_TOOL, List.of(MODEL.name()));

        return clusterElementClusterElementTypes;
    }

    private static class AiAgentUtilsComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements ClusterRootComponentDefinition {

        private final Map<String, List<String>> actionClusterElementTypes;
        private final Map<String, List<String>> clusterElementClusterElementTypes;

        AiAgentUtilsComponentDefinitionImpl(
            ComponentDefinition componentDefinition, Map<String, List<String>> actionClusterElementTypes,
            Map<String, List<String>> clusterElementClusterElementTypes) {

            super(componentDefinition);

            this.actionClusterElementTypes = Map.copyOf(actionClusterElementTypes);
            this.clusterElementClusterElementTypes = Map.copyOf(clusterElementClusterElementTypes);
        }

        @Override
        public Map<String, List<String>> getActionClusterElementTypes() {
            return actionClusterElementTypes;
        }

        @Override
        public List<ClusterElementType> getClusterElementTypes() {
            return List.of(MODEL, SUBAGENT, TOOLS);
        }

        @Override
        public Map<String, List<String>> getClusterElementClusterElementTypes() {
            return clusterElementClusterElementTypes;
        }
    }
}
