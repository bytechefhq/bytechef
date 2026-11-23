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

package com.bytechef.automation.workflow.execution.tool.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.ai.copilot.tool.InteractivePickerToolCallbacks;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * @author Ivica Cardic
 */
class WorkflowEditorAgentConfigurationTest {

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

    @Test
    void testBuildWorkflowSubAgentPromptForbidsInventedLookupOptions() {
        String subAgentSection = buildWorkflowSubAgentSection(newPromptConfiguration(), true);

        assertThat(subAgentSection)
            .contains("Your only option lookup tool is " + LOOKUP_PROPERTY_OPTIONS_TOOL_NAME)
            .contains("NEVER invent values for a property whose options come from a lookup")
            .contains("never offer a subset of real values as the only choices")
            .contains("returns more than 4 options, leave the parameter empty")
            .contains("Fixed options listed in the property definition itself may be offered as they are");
    }

    @Test
    void testBuildWorkflowSubAgentPromptOverridesThePickerGuidanceOfTheBuildPrompt() {
        String subAgentSection = buildWorkflowSubAgentSection(newPromptConfiguration(), false);

        assertThat(subAgentSection).contains("This section overrides the picker and question guidance above");
    }

    @Test
    void testBuildWorkflowSubAgentPromptOffersAskingOnlyWithTheAskTool() {
        WorkflowEditorAgentConfiguration configuration = newPromptConfiguration();

        assertThat(buildWorkflowSubAgentSection(configuration, true))
            .contains("ask ONE question with askUserQuestion and stop");
        assertThat(buildWorkflowSubAgentSection(configuration, false))
            .contains("You do not have these tools: askUserQuestion")
            .contains("Never ask the user a question");
    }

    @ParameterizedTest
    @ValueSource(booleans = {
        true, false
    })
    void testBuildWorkflowSubAgentPromptNamesOnlyToolsTheSpecialistHolds(boolean askToolAvailable) {
        String subAgentSection = buildWorkflowSubAgentSection(newPromptConfiguration(), askToolAvailable);

        Set<String> heldToolNames = specialistToolNames(askToolAvailable);
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

        List<String> mentionedToolNames = knownToolNames.stream()
            .filter(toolName -> Pattern.compile("\\b" + toolName + "\\b")
                .matcher(sectionWithoutDenials)
                .find())
            .toList();

        assertThat(mentionedToolNames).isNotEmpty();
        assertThat(heldToolNames).containsAll(mentionedToolNames);
    }

    private static String
        buildWorkflowSubAgentSection(WorkflowEditorAgentConfiguration configuration, boolean askToolAvailable) {
        String buildPrompt = readResource("prompt_workflow_editor_build.txt");
        String systemPrompt = configuration.buildWorkflowSubAgentSystemPrompt(askToolAvailable);

        assertThat(systemPrompt).startsWith(buildPrompt);

        return systemPrompt.substring(buildPrompt.length());
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

    private static WorkflowEditorAgentConfiguration newPromptConfiguration() {
        return new WorkflowEditorAgentConfiguration(PROMPT_RESOURCE,
            new ClassPathResource("prompt_workflow_editor_build.txt"),
            new ClassPathResource("prompt_workflow_editor_build_sub_agent.txt"), mock(WorkflowValidatorTools.class),
            mock(WorkflowInstructionTools.class), mock(InteractivePickerToolCallbacks.class),
            mock(ActionDefinitionService.class), mock(ActionDefinitionFacade.class),
            mock(TriggerDefinitionService.class), mock(TriggerDefinitionFacade.class),
            mock(PropertyOptionsResolver.class), mock(ScopedConnectionGuard.class), emptyDataTableToolsProvider());
    }

    private static String readResource(String name) {
        try (InputStream inputStream = new ClassPathResource(name).getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<DataTableTools> emptyDataTableToolsProvider() {
        return mock(ObjectProvider.class);
    }
}
