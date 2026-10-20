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

package com.bytechef.ai.copilot.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.agui.core.exception.AGUIException;
import com.agui.server.LocalAgent;
import com.bytechef.ai.copilot.agent.McpServerSpringAIAgent;
import com.bytechef.ai.copilot.agent.OverrideChatClientResolver;
import com.bytechef.ai.copilot.tool.CopilotAgentType;
import com.bytechef.ai.copilot.tool.LookupComponentPropertyOptionsToolCallback;
import com.bytechef.ai.copilot.tool.PropertyOptionsResolver;
import com.bytechef.ai.copilot.tool.ScopedConnectionGuard;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.copilot.tool.ToolStateVisibilityMetrics;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import com.bytechef.automation.ai.tool.ProjectWorkflowTools;
import com.bytechef.automation.ai.tool.ScriptTools;
import com.bytechef.platform.ai.tool.TaskTools;
import com.bytechef.platform.ai.tool.WorkflowInstructionTools;
import com.bytechef.platform.ai.tool.WorkflowValidatorTools;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * @author Ivica Cardic
 */
class CopilotConfigurationTest {

    private static final String DENIED_TOOLS_PREFIX = "You do not have these tools: ";

    private static final List<String> KNOWN_PICKER_AND_ASK_TOOL_NAMES = List.of(
        "askUserQuestion", "createConnection", "listConnectionsForComponent", "lookupActionPropertyOptions",
        "lookupComponentPropertyOptions", "lookupTriggerPropertyOptions", "saveWorkflowTestConnection",
        "selectConnection", "selectPropertyOption", "selectTriggerPropertyOption");

    private static final String LOOKUP_PROPERTY_OPTIONS_TOOL_NAME = new LookupComponentPropertyOptionsToolCallback(
        mock(ActionDefinitionService.class), mock(ActionDefinitionFacade.class), mock(TriggerDefinitionService.class),
        mock(TriggerDefinitionFacade.class), mock(PropertyOptionsResolver.class), ToolStateVisibilityMetrics.NOOP,
        mock(ScopedConnectionGuard.class))
            .getToolDefinition()
            .name();

    private static final Resource PROMPT_RESOURCE = new ByteArrayResource("prompt".getBytes(StandardCharsets.UTF_8));

    private final CopilotConfiguration copilotConfiguration = new CopilotConfiguration(
        PROMPT_RESOURCE, PROMPT_RESOURCE, PROMPT_RESOURCE, PROMPT_RESOURCE, PROMPT_RESOURCE, PROMPT_RESOURCE,
        PROMPT_RESOURCE, PROMPT_RESOURCE, PROMPT_RESOURCE, PROMPT_RESOURCE, PROMPT_RESOURCE, PROMPT_RESOURCE,
        PROMPT_RESOURCE, PROMPT_RESOURCE, PROMPT_RESOURCE, PROMPT_RESOURCE, PROMPT_RESOURCE,
        mock(WorkflowValidatorTools.class), mock(WorkflowInstructionTools.class),
        mock(InteractivePickerToolCallbacks.class), mock(ActionDefinitionService.class),
        mock(ActionDefinitionFacade.class), mock(TriggerDefinitionService.class), mock(TriggerDefinitionFacade.class),
        mock(PropertyOptionsResolver.class), mock(ScopedConnectionGuard.class));

    @Test
    void testMcpServerBuildAgentIsRegisteredUnderTheMcpServerSourceId() throws AGUIException {
        McpServerSpringAIAgent mcpServerSpringAIAgent = copilotConfiguration.mcpServerBuildSpringAIAgent(
            mock(ChatMemory.class), mock(ChatModel.class),
            CopilotConfigurationTest.<IntelligentToolChatClientFactory>emptyProvider(),
            PROMPT_RESOURCE, mock(SecurityContextRehydrator.class),
            CopilotConfigurationTest.<OverrideChatClientResolver>emptyProvider(),
            CopilotConfigurationTest.<SubAgentChatModelResolver>emptyProvider());

        assertThat(mcpServerSpringAIAgent.getAgentId()).isEqualTo("mcp_server_build");
    }

    @Test
    void testEveryRegisteredChatAgentIdIsAUniqueCopilotAgentTypeKey() throws ReflectiveOperationException {
        Set<String> copilotAgentTypeKeys = Arrays.stream(CopilotAgentType.values())
            .map(CopilotAgentType::key)
            .collect(Collectors.toSet());

        List<String> agentIds = new ArrayList<>();

        for (Method method : CopilotConfiguration.class.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Bean.class) && LocalAgent.class.isAssignableFrom(method.getReturnType())) {
                LocalAgent localAgent = (LocalAgent) invokeWithStubs(method);

                agentIds.add(localAgent.getAgentId());
            }
        }

        assertThat(agentIds)
            .hasSize(16)
            .doesNotHaveDuplicates()
            .allSatisfy(agentId -> assertThat(copilotAgentTypeKeys).contains(agentId))
            .contains("workflow_code_editor_ask", "workflow_code_editor_build", "converter_build", "mcp_server_build");
    }

    @Test
    void testConverterSubAgentPromptCreatesANewWorkflowFromAProjectId() {
        assertThat(converterSubAgentSection(copilotConfiguration).replaceAll("\\s+", " "))
            .contains("EITHER a `workflowId` (the workflow already exists) OR a `projectId`")
            .contains(
                "`getWorkflow(workflowId)` for its current `version`, then `updateWorkflow(workflowId, workflow, version)`")
            .contains("ONE `createProjectWorkflow(projectId, workflow)` call carrying the whole definition")
            .contains("If you are given both, the `workflowId` wins")
            .contains("Given neither a `workflowId` nor a `projectId`, say so plainly");
    }

    @Test
    void testConverterSubAgentPromptNamesOnlyToolsTheConverterHolds() {
        String subAgentSection = converterSubAgentSection(copilotConfiguration);

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
        CopilotConfiguration configuration = newPromptConfiguration();

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

    private static String buildWorkflowSubAgentSection(CopilotConfiguration configuration, boolean askToolAvailable) {
        String buildPrompt = readResource("prompt_workflow_editor_build.txt");
        String systemPrompt = configuration.buildWorkflowSubAgentSystemPrompt(askToolAvailable);

        assertThat(systemPrompt).startsWith(buildPrompt);

        return systemPrompt.substring(buildPrompt.length());
    }

    private static String converterSubAgentSection(CopilotConfiguration configuration) {
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

    private static CopilotConfiguration newPromptConfiguration() {
        return new CopilotConfiguration(
            PROMPT_RESOURCE, new ClassPathResource("prompt_workflow_editor_build.txt"),
            new ClassPathResource("prompt_workflow_editor_build_sub_agent.txt"), PROMPT_RESOURCE, PROMPT_RESOURCE,
            PROMPT_RESOURCE, PROMPT_RESOURCE, PROMPT_RESOURCE, PROMPT_RESOURCE, PROMPT_RESOURCE, PROMPT_RESOURCE,
            PROMPT_RESOURCE, PROMPT_RESOURCE, PROMPT_RESOURCE, PROMPT_RESOURCE, PROMPT_RESOURCE, PROMPT_RESOURCE,
            mock(WorkflowValidatorTools.class), mock(WorkflowInstructionTools.class),
            mock(InteractivePickerToolCallbacks.class), mock(ActionDefinitionService.class),
            mock(ActionDefinitionFacade.class), mock(TriggerDefinitionService.class),
            mock(TriggerDefinitionFacade.class), mock(PropertyOptionsResolver.class),
            mock(ScopedConnectionGuard.class));
    }

    private static String readResource(String name) {
        try (InputStream inputStream = new ClassPathResource(name).getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private Object invokeWithStubs(Method method) throws ReflectiveOperationException {
        Parameter[] parameters = method.getParameters();
        Object[] arguments = new Object[parameters.length];

        for (int index = 0; index < parameters.length; index++) {
            Class<?> parameterType = parameters[index].getType();

            if (parameterType == Resource.class) {
                arguments[index] = PROMPT_RESOURCE;
            } else if (parameterType == Optional.class) {
                arguments[index] = Optional.empty();
            } else if (parameterType == ObjectProvider.class) {
                arguments[index] = emptyProvider();
            } else {
                arguments[index] = mock(parameterType);
            }
        }

        method.setAccessible(true);

        try {
            return method.invoke(copilotConfiguration, arguments);
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException("Failed to build agent via " + method.getName(), exception.getCause());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> emptyProvider() {
        return mock(ObjectProvider.class);
    }
}
