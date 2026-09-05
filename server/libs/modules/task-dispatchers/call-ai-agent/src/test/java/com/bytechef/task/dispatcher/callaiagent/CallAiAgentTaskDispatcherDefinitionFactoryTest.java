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

package com.bytechef.task.dispatcher.callaiagent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.definition.BaseProperty;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.task.dispatcher.definition.Option;
import com.bytechef.platform.workflow.task.dispatcher.definition.OutputDefinition;
import com.bytechef.platform.workflow.task.dispatcher.definition.Property;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDefinition.OptionsFunction;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDefinition.OutputFunction;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl;
import com.bytechef.platform.workflow.task.dispatcher.subflow.CallableAiAgentDataSource;
import com.bytechef.platform.workflow.task.dispatcher.subflow.CallableAiAgentDataSource.CallableAiAgentEntry;
import com.bytechef.platform.workflow.task.dispatcher.subflow.CallableAiAgentDataSource.ResolvedAiAgent;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowDataSource;
import com.bytechef.platform.workflow.task.dispatcher.subflow.domain.SubflowEntry;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import com.bytechef.test.jsonasssert.JsonFileAssert;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class CallAiAgentTaskDispatcherDefinitionFactoryTest {

    @Mock
    private CallableAiAgentDataSource callableAiAgentDataSource;

    @Mock
    private SubflowDataSource subflowDataSource;

    @Test
    void testGetTaskDispatcherDefinition() {
        SubflowDataSource stubSubflowDataSource = new SubflowDataSource() {

            @Override
            public BaseProperty.BaseValueProperty<?> getSubWorkflowInputSchema(String workflowUuid) {
                return null;
            }

            @Override
            public BaseProperty.BaseValueProperty<?> getSubWorkflowOutputSchema(String workflowUuid) {
                return null;
            }

            @Override
            public List<SubflowEntry> getSubWorkflows(PlatformType platformType, String triggerName, String search) {
                return List.of();
            }
        };

        JsonFileAssert.assertEquals(
            "definition/callAiAgent_v1.json",
            new CallAiAgentTaskDispatcherDefinitionFactory((CallableAiAgentDataSource) null, stubSubflowDataSource)
                .getDefinition());
    }

    @Test
    void testAgentOptionsListPublishedAgents() throws Exception {
        when(callableAiAgentDataSource.getCallableAgents("tri"))
            .thenReturn(List.of(new CallableAiAgentEntry("agent-uuid-1", "Triage Agent", null)));

        List<? extends Option<?>> options = agentOptionsFunction(
            new CallAiAgentTaskDispatcherDefinitionFactory(callableAiAgentDataSource, subflowDataSource))
                .apply("tri");

        assertThat(options).hasSize(1);
        assertThat(options.getFirst()
            .getLabel()).isEqualTo("Triage Agent");
        assertThat(options.getFirst()
            .getValue()).isEqualTo("agent-uuid-1");
    }

    @Test
    void testAgentOptionsAreEmptyWhenAgentsAreNotAvailable() throws Exception {
        List<? extends Option<?>> options = agentOptionsFunction(
            new CallAiAgentTaskDispatcherDefinitionFactory((CallableAiAgentDataSource) null, subflowDataSource))
                .apply(null);

        assertThat(options).isEmpty();
    }

    @Test
    void testOutputResolvesTheAgentWorkflowSchema() throws Exception {
        when(callableAiAgentDataSource.resolveAgent("agent-uuid-1", true))
            .thenReturn(new ResolvedAiAgent("agent-workflow-uuid", "Triage Agent", null));
        doReturn(TaskDispatcherDsl.object("result")).when(subflowDataSource)
            .getSubWorkflowOutputSchema("agent-workflow-uuid");

        OutputResponse outputResponse = outputFunction(
            new CallAiAgentTaskDispatcherDefinitionFactory(callableAiAgentDataSource, subflowDataSource))
                .apply(Map.of("agentUuid", "agent-uuid-1"));

        assertThat(outputResponse).isNotNull();
        assertThat(outputResponse.getOutputSchema()).isInstanceOf(Property.ObjectProperty.class);
    }

    @Test
    void testOutputIsNullWithoutAnAgentOrWhenAgentsAreNotAvailable() throws Exception {
        assertThat(
            outputFunction(new CallAiAgentTaskDispatcherDefinitionFactory(callableAiAgentDataSource, subflowDataSource))
                .apply(Map.of())).isNull();
        assertThat(
            outputFunction(
                new CallAiAgentTaskDispatcherDefinitionFactory((CallableAiAgentDataSource) null, subflowDataSource))
                    .apply(Map.of("agentUuid", "agent-uuid-1"))).isNull();
    }

    private static OptionsFunction agentOptionsFunction(CallAiAgentTaskDispatcherDefinitionFactory factory) {
        Property.StringProperty agentProperty = (Property.StringProperty) factory.getDefinition()
            .getProperties()
            .getFirst();

        return agentProperty.getOptionsFunction()
            .orElseThrow();
    }

    private static OutputFunction outputFunction(CallAiAgentTaskDispatcherDefinitionFactory factory) {
        return factory.getDefinition()
            .getOutputDefinition()
            .flatMap(OutputDefinition::getOutput)
            .map(function -> (OutputFunction) function)
            .orElseThrow();
    }
}
