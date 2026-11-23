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

import com.agui.server.LocalAgent;
import com.bytechef.ai.agent.tool.AgentTypeRegistry;
import com.bytechef.ai.copilot.tool.InteractivePickerToolCallbacks;
import com.bytechef.automation.ai.mcp.tool.config.McpServerAgentConfiguration;
import com.bytechef.automation.configuration.tool.config.ClusterElementAgentConfiguration;
import com.bytechef.automation.configuration.tool.config.CodeEditorAgentConfiguration;
import com.bytechef.automation.configuration.tool.config.ConverterAgentConfiguration;
import com.bytechef.automation.configuration.tool.config.SkillsAgentConfiguration;
import com.bytechef.automation.data.table.tool.DataTableTools;
import com.bytechef.automation.workflow.execution.tool.config.WorkflowEditorAgentConfiguration;
import com.bytechef.automation.workflow.execution.tool.config.WorkflowExecutionAgentConfiguration;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.component.tool.PropertyOptionsResolver;
import com.bytechef.platform.component.tool.ScopedConnectionGuard;
import com.bytechef.platform.workflow.tool.WorkflowInstructionTools;
import com.bytechef.platform.workflow.tool.WorkflowValidatorTools;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * @author Ivica Cardic
 */
class CopilotConfigurationTest {

    private static final Resource PROMPT_RESOURCE = new ByteArrayResource("prompt".getBytes(StandardCharsets.UTF_8));

    @Test
    void testEveryRegisteredChatAgentIdIsAUniqueCopilotAgentTypeKey() throws ReflectiveOperationException {
        Set<String> copilotAgentTypeKeys = AgentTypeRegistry.keys();

        List<String> agentIds = new ArrayList<>();

        for (Method method : agentConfigurationMethods()) {
            if (method.isAnnotationPresent(Bean.class) && LocalAgent.class.isAssignableFrom(method.getReturnType())) {
                LocalAgent localAgent = (LocalAgent) invokeWithStubs(method);

                agentIds.add(localAgent.getAgentId());
            }
        }

        assertThat(agentIds)
            .hasSize(10)
            .doesNotHaveDuplicates()
            .allSatisfy(agentId -> assertThat(copilotAgentTypeKeys).contains(agentId))
            .contains("code_editor_ask", "code_editor_build", "converter_build", "mcp_server_build");
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
            return method.invoke(agentConfiguration(method.getDeclaringClass()), arguments);
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException("Failed to build agent via " + method.getName(), exception.getCause());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> emptyProvider() {
        return mock(ObjectProvider.class);
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<DataTableTools> emptyDataTableToolsProvider() {
        return mock(ObjectProvider.class);
    }

    private List<Object> agentConfigurations() {
        return List.of(
            new ClusterElementAgentConfiguration(PROMPT_RESOURCE, PROMPT_RESOURCE, mock(WorkflowValidatorTools.class),
                mock(WorkflowInstructionTools.class)),
            new CodeEditorAgentConfiguration(PROMPT_RESOURCE, PROMPT_RESOURCE, mock(WorkflowValidatorTools.class),
                mock(WorkflowInstructionTools.class)),
            new ConverterAgentConfiguration(PROMPT_RESOURCE, mock(WorkflowValidatorTools.class),
                mock(WorkflowInstructionTools.class), emptyDataTableToolsProvider()),
            new McpServerAgentConfiguration(),
            new SkillsAgentConfiguration(PROMPT_RESOURCE, PROMPT_RESOURCE, mock(WorkflowValidatorTools.class),
                mock(WorkflowInstructionTools.class)),
            new WorkflowEditorAgentConfiguration(PROMPT_RESOURCE, PROMPT_RESOURCE, PROMPT_RESOURCE,
                mock(WorkflowValidatorTools.class), mock(WorkflowInstructionTools.class),
                mock(InteractivePickerToolCallbacks.class), mock(ActionDefinitionService.class),
                mock(ActionDefinitionFacade.class), mock(TriggerDefinitionService.class),
                mock(TriggerDefinitionFacade.class), mock(PropertyOptionsResolver.class),
                mock(ScopedConnectionGuard.class), emptyDataTableToolsProvider()),
            new WorkflowExecutionAgentConfiguration(PROMPT_RESOURCE, mock(WorkflowValidatorTools.class),
                mock(WorkflowInstructionTools.class)));
    }

    private List<Method> agentConfigurationMethods() {
        List<Object> configurations = agentConfigurations();

        return configurations.stream()
            .flatMap(configuration -> Arrays.stream(configuration.getClass()
                .getDeclaredMethods()))
            .toList();
    }

    private Object agentConfiguration(Class<?> type) {
        List<Object> configurations = agentConfigurations();

        return configurations.stream()
            .filter(type::isInstance)
            .findFirst()
            .orElseThrow();
    }
}
