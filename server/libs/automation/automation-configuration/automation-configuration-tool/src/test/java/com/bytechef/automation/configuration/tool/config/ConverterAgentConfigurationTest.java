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

package com.bytechef.automation.configuration.tool.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.ai.copilot.tool.ToolStateVisibilityMetrics;
import com.bytechef.automation.configuration.tool.ProjectWorkflowTools;
import com.bytechef.automation.configuration.tool.ScriptTools;
import com.bytechef.automation.data.table.tool.DataTableTools;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.component.tool.LookupComponentPropertyOptionsToolCallback;
import com.bytechef.platform.component.tool.PropertyOptionsResolver;
import com.bytechef.platform.component.tool.ScopedConnectionGuard;
import com.bytechef.platform.component.tool.TaskTools;
import com.bytechef.platform.workflow.tool.WorkflowInstructionTools;
import com.bytechef.platform.workflow.tool.WorkflowValidatorTools;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * @author Ivica Cardic
 */
class ConverterAgentConfigurationTest {

    private static final String DENIED_TOOLS_PREFIX = "You do not have these tools: ";

    private static final List<String> KNOWN_PICKER_AND_ASK_TOOL_NAMES = List.of(
        "askUserQuestion", "createConnection", "listConnectionsForComponent", "lookupActionPropertyOptions",
        "lookupClusterElementPropertyOptions", "lookupComponentPropertyOptions", "lookupTriggerPropertyOptions",
        "saveWorkflowTestConnection", "selectClusterElementPropertyOption", "selectConnection", "selectPropertyOption",
        "selectTriggerPropertyOption");

    private static final String LOOKUP_PROPERTY_OPTIONS_TOOL_NAME = new LookupComponentPropertyOptionsToolCallback(
        mock(ActionDefinitionService.class), mock(ActionDefinitionFacade.class), mock(TriggerDefinitionService.class),
        mock(TriggerDefinitionFacade.class), mock(PropertyOptionsResolver.class), ToolStateVisibilityMetrics.NOOP,
        mock(ScopedConnectionGuard.class))
            .getToolDefinition()
            .name();

    private static final Resource PROMPT_RESOURCE = new ByteArrayResource("prompt".getBytes(StandardCharsets.UTF_8));

    private final ConverterAgentConfiguration converterAgentConfiguration =
        new ConverterAgentConfiguration(PROMPT_RESOURCE, mock(WorkflowValidatorTools.class),
            mock(WorkflowInstructionTools.class), emptyDataTableToolsProvider());

    @Test
    void testConverterSubAgentPromptCreatesANewWorkflowFromAProjectId() {
        assertThat(converterSubAgentSection(converterAgentConfiguration).replaceAll("\\s+", " "))
            .contains("EITHER a `workflowId` (the workflow already exists) OR a `projectId`")
            .contains(
                "`getWorkflow(workflowId)` for its current `version`, then `updateWorkflow(workflowId, workflow, version)`")
            .contains("ONE `createProjectWorkflow(projectId, workflow)` call carrying the whole definition")
            .contains("If you are given both, the `workflowId` wins")
            .contains("Given neither a `workflowId` nor a `projectId`, say so plainly");
    }

    @Test
    void testConverterSubAgentPromptNamesOnlyToolsTheConverterHolds() {
        String subAgentSection = converterSubAgentSection(converterAgentConfiguration);

        Set<String> heldToolNames = specialistToolNames(false);

        heldToolNames.remove(LOOKUP_PROPERTY_OPTIONS_TOOL_NAME);

        Set<String> deniedToolNames = new HashSet<>();
        StringBuilder sectionWithoutDenials = new StringBuilder();

        for (String line : subAgentSection.split("\n")) {
            if (line.startsWith(DENIED_TOOLS_PREFIX)) {
                for (String toolName : line.substring(DENIED_TOOLS_PREFIX.length())
                    .split("[,.]")) {

                    if (!toolName.isBlank()) {
                        deniedToolNames.add(toolName.strip());
                    }
                }
            } else {
                sectionWithoutDenials.append(line)
                    .append('\n');
            }
        }

        assertThat(deniedToolNames).doesNotContainAnyElementsOf(heldToolNames);

        Set<String> knownToolNames = new HashSet<>(KNOWN_PICKER_AND_ASK_TOOL_NAMES);

        knownToolNames.addAll(heldToolNames);
        knownToolNames.add(LOOKUP_PROPERTY_OPTIONS_TOOL_NAME);
        knownToolNames.add("createProject");

        List<String> mentionedToolNames = knownToolNames.stream()
            .filter(toolName -> Pattern.compile("\\b" + toolName + "\\b")
                .matcher(sectionWithoutDenials)
                .find())
            .toList();

        List<String> calledToolNames = Pattern.compile("`(\\w+)\\(")
            .matcher(sectionWithoutDenials)
            .results()
            .map(matchResult -> matchResult.group(1))
            .toList();

        assertThat(mentionedToolNames).isNotEmpty();
        assertThat(calledToolNames).isNotEmpty();
        assertThat(heldToolNames).containsAll(mentionedToolNames)
            .containsAll(calledToolNames);
    }

    private static String converterSubAgentSection(ConverterAgentConfiguration configuration) {
        String systemPrompt = configuration.converterSubAgentSystemPrompt();

        assertThat(systemPrompt).startsWith("prompt");

        return systemPrompt.substring("prompt".length());
    }

    private static Set<String> specialistToolNames(boolean askToolAvailable) {
        Set<String> toolNames = new HashSet<>();

        for (Class<?> toolsClass : List.of(
            ProjectWorkflowTools.class, TaskTools.class, ScriptTools.class, WorkflowValidatorTools.class,
            WorkflowInstructionTools.class)) {

            for (Method method : toolsClass.getMethods()) {
                Tool tool = method.getAnnotation(Tool.class);

                if (tool != null) {
                    toolNames.add(tool.name()
                        .isEmpty() ? method.getName() : tool.name());
                }
            }
        }

        toolNames.add(LOOKUP_PROPERTY_OPTIONS_TOOL_NAME);

        if (askToolAvailable) {
            toolNames.add("askUserQuestion");
        }

        return toolNames;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<DataTableTools> emptyDataTableToolsProvider() {
        return mock(ObjectProvider.class);
    }
}
