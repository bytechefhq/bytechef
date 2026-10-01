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

package com.bytechef.component.workflow.trigger;

import static com.bytechef.component.definition.ComponentDsl.agentChannelRequest;
import static com.bytechef.component.workflow.constant.WorkflowConstants.AI_AGENT_CALL_INPUT_SCHEMA;
import static com.bytechef.component.workflow.constant.WorkflowConstants.WORKFLOW_CALL;
import static com.bytechef.platform.component.constant.WorkflowConstants.INPUT_SCHEMA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.definition.AgentChannelDefinition;
import com.bytechef.component.definition.AgentRequestDefinition;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.OutputSchema;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.OutputFunction;
import com.bytechef.component.workflow.WorkflowComponentHandler;
import com.bytechef.component.workflow.subflow.sync.SubflowSyncExecutor;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.definition.BaseProperty;
import com.bytechef.definition.BaseProperty.BaseObjectProperty;
import com.bytechef.definition.BaseProperty.BaseValueProperty;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.PropertyFactory;
import com.bytechef.platform.util.SchemaUtils;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowDataSource;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
public class WorkflowNewWorkflowCallTriggerAgentChannelTest {

    private static final OutputSchema OUTPUT_SCHEMA = new OutputSchema() {

        @Override
        public ValueProperty<?> getOutputSchema(String jsonSchema) {
            return (ValueProperty<?>) SchemaUtils.getJsonSchemaProperty(
                jsonSchema, PropertyFactory.JSON_SCHEMA_PROPERTY_FACTORY);
        }

        @Override
        public ValueProperty<?> getOutputSchema(String propertyName, String jsonSchema) {
            return (ValueProperty<?>) SchemaUtils.getJsonSchemaProperty(
                propertyName, jsonSchema, PropertyFactory.JSON_SCHEMA_PROPERTY_FACTORY);
        }

        @Override
        public ValueProperty<?> getOutputSchema(Object value) {
            return (ValueProperty<?>) SchemaUtils.getOutputSchema(value, PropertyFactory.PROPERTY_FACTORY);
        }

        @Override
        public Object getSampleOutput(BaseProperty definitionProperty) {
            return SchemaUtils.getSampleOutput(definitionProperty);
        }
    };

    @Test
    void testPinnedInputSchemaDerivesContractFieldNames() throws Exception {
        Set<String> propertyNames = derivePropertyNames(AI_AGENT_CALL_INPUT_SCHEMA);

        assertEquals(
            Set.of(
                AgentChannelDefinition.CONVERSATION_ID, AgentChannelDefinition.MESSAGE,
                AgentChannelDefinition.ATTACHMENTS),
            propertyNames,
            "the derived output must carry exactly the agent channel contract's field names");
    }

    @Test
    void testPinnedInputSchemaRequiresBothConversationIdAndMessage() {
        List<String> required = JsonUtils.readList(AI_AGENT_CALL_INPUT_SCHEMA, "$.required", String.class);

        assertEquals(
            Set.of(AgentChannelDefinition.CONVERSATION_ID, AgentChannelDefinition.MESSAGE),
            Set.copyOf(required),
            "the pinned inputSchema must require both conversationId and message");
    }

    @Test
    void testRequiredListDoesNotAffectTheDerivedContractFields() throws Exception {
        String noneRequiredSchema = AI_AGENT_CALL_INPUT_SCHEMA.replace(
            "\"required\":[\"conversationId\",\"message\"]", "\"required\":[]");

        assertNotEquals(
            AI_AGENT_CALL_INPUT_SCHEMA, noneRequiredSchema,
            "the required list must actually have been swapped, otherwise this test compares a schema with itself");

        Set<String> pinnedPropertyNames = derivePropertyNames(AI_AGENT_CALL_INPUT_SCHEMA);
        Set<String> noneRequiredPropertyNames = derivePropertyNames(noneRequiredSchema);

        assertEquals(
            pinnedPropertyNames, noneRequiredPropertyNames,
            "the derived contract fields must not depend on the schema's required list");
    }

    @Test
    void testUnpinnedInputSchemaDoesNotDeriveContractFieldNames() throws Exception {
        Set<String> propertyNames = derivePropertyNames(
            "{\"type\":\"object\",\"properties\":{\"somethingElse\":{\"type\":\"string\"}}}");

        assertEquals(Set.of("somethingElse"), propertyNames);

        assertNull(invokeTriggerOutput(null), "with no inputSchema at all the trigger derives no output");
    }

    @Test
    void testDerivedOutputSatisfiesTheChannelsRequestPaths() throws Exception {
        AgentRequestDefinition agentRequestDefinition = WorkflowNewWorkflowCallTrigger.TRIGGER_DEFINITION
            .getAgentRequestDefinition()
            .orElseThrow();

        Set<String> propertyNames = derivePropertyNames(AI_AGENT_CALL_INPUT_SCHEMA);

        assertTrue(
            propertyNames.contains(agentRequestDefinition.getConversationIdPath()),
            "conversationIdPath '%s' must be derivable from the pinned inputSchema"
                .formatted(agentRequestDefinition.getConversationIdPath()));
        assertTrue(
            propertyNames.contains(agentRequestDefinition.getMessagePath()),
            "messagePath '%s' must be derivable from the pinned inputSchema"
                .formatted(agentRequestDefinition.getMessagePath()));

        String attachmentsPath = agentRequestDefinition.getAttachmentsPath()
            .orElseThrow(
                () -> new AssertionError(
                    "attachmentsPath must be bound: the pinned inputSchema advertises 'attachments', but an empty "
                        + "attachmentsPath means 'this channel carries no attachments', which would make the "
                        + "generator wire [] instead of the caller's value"));

        assertTrue(
            propertyNames.contains(attachmentsPath),
            "attachmentsPath '%s' must be derivable from the pinned inputSchema".formatted(attachmentsPath));
    }

    @Test
    void testEveryAdvertisedContractFieldIsBoundByTheRequestDescriptor() throws Exception {
        AgentRequestDefinition agentRequestDefinition = WorkflowNewWorkflowCallTrigger.TRIGGER_DEFINITION
            .getAgentRequestDefinition()
            .orElseThrow();

        Set<String> boundPaths = Stream.concat(
            Stream.of(agentRequestDefinition.getConversationIdPath(), agentRequestDefinition.getMessagePath()),
            agentRequestDefinition.getAttachmentsPath()
                .stream())
            .collect(Collectors.toSet());

        assertEquals(
            derivePropertyNames(AI_AGENT_CALL_INPUT_SCHEMA), boundPaths,
            "the request descriptor must bind exactly the fields the pinned inputSchema advertises");
    }

    @Test
    void testChannelPinsTheInputSchemaUnderTheTriggersOwnPropertyName() {
        AgentChannelDefinition agentChannelDefinition = getAgentChannelDefinition();

        assertEquals(WORKFLOW_CALL, agentChannelDefinition.getName(), "the stored channel key must not drift");

        assertEquals(
            Map.of(INPUT_SCHEMA, AI_AGENT_CALL_INPUT_SCHEMA), agentChannelDefinition.getTriggerParameters(),
            "the channel must pin the contract schema under the trigger's own inputSchema property");

        List<String> triggerPropertyNames = WorkflowNewWorkflowCallTrigger.TRIGGER_DEFINITION.getProperties()
            .stream()
            .map(BaseProperty::getName)
            .toList();

        assertTrue(
            triggerPropertyNames.contains(INPUT_SCHEMA),
            "the pinned trigger parameter must name a property the trigger actually declares");
    }

    @Test
    void testPinnedInputSchemaDeclaresTheSamePropertyNamesAsTheContractHelper() throws Exception {
        assertEquals(
            contractHelperPropertyNames(), derivePropertyNames(AI_AGENT_CALL_INPUT_SCHEMA),
            "the pinned schema must spell the same contract fields ComponentDsl.agentChannelRequest() declares");
    }

    @Test
    void testPinnedInputSchemaRequiresTheContractHelpersFieldsPlusMessage() {
        Set<String> pinnedRequired = Set.copyOf(
            JsonUtils.readList(AI_AGENT_CALL_INPUT_SCHEMA, "$.required", String.class));

        Set<String> helperRequired = agentChannelRequest().getProperties()
            .stream()
            .filter(BaseProperty::getRequired)
            .map(BaseProperty::getName)
            .collect(Collectors.toSet());

        assertEquals(Set.of(AgentChannelDefinition.CONVERSATION_ID), helperRequired);

        assertTrue(
            pinnedRequired.containsAll(helperRequired),
            "the pinned schema may only be stricter than the contract, never looser");

        Set<String> additionallyRequired = pinnedRequired.stream()
            .filter(name -> !helperRequired.contains(name))
            .collect(Collectors.toSet());

        assertEquals(
            Set.of(AgentChannelDefinition.MESSAGE), additionallyRequired,
            "message is the one field this channel requires beyond the general contract; a workflow calling an agent "
                + "with no text at all is almost certainly a mistake");
    }

    private static Set<String> contractHelperPropertyNames() {
        return agentChannelRequest().getProperties()
            .stream()
            .map(BaseProperty::getName)
            .collect(Collectors.toSet());
    }

    private static AgentChannelDefinition getAgentChannelDefinition() {
        WorkflowComponentHandler workflowComponentHandler = new WorkflowComponentHandler(
            Mockito.mock(SubflowDataSource.class), Mockito.mock(SubflowResolver.class),
            Mockito.mock(SubflowSyncExecutor.class));

        ComponentDefinition componentDefinition = workflowComponentHandler.getDefinition();

        List<AgentChannelDefinition> agentChannelDefinitions = componentDefinition.getAgentChannels();

        assertEquals(1, agentChannelDefinitions.size(), "the workflow component declares exactly one agent channel");

        return agentChannelDefinitions.getFirst();
    }

    private static Set<String> derivePropertyNames(String inputSchema) throws Exception {
        OutputResponse outputResponse = invokeTriggerOutput(inputSchema);

        assertNotNull(outputResponse, "the inputSchema must derive an output");

        BaseValueProperty<?> outputSchema = outputResponse.getOutputSchema();

        BaseObjectProperty<?> objectProperty = assertInstanceOf(
            BaseObjectProperty.class, outputSchema, "the derived output must be an object schema");

        return objectProperty.getProperties()
            .stream()
            .map(BaseProperty::getName)
            .collect(Collectors.toSet());
    }

    private static OutputResponse invokeTriggerOutput(String inputSchema) throws Exception {
        TriggerContext triggerContext = Mockito.mock(TriggerContext.class);

        Mockito.when(triggerContext.outputSchema(Mockito.any()))
            .thenAnswer(invocation -> {
                ContextFunction<OutputSchema, ?> contextFunction = invocation.getArgument(0);

                return contextFunction.apply(OUTPUT_SCHEMA);
            });

        Parameters inputParameters = ParametersFactory.create(
            inputSchema == null ? Map.of() : Map.of(INPUT_SCHEMA, inputSchema));

        ModifiableTriggerDefinition triggerDefinition = WorkflowNewWorkflowCallTrigger.TRIGGER_DEFINITION;

        OutputFunction outputFunction = (OutputFunction) triggerDefinition.getOutputDefinition()
            .orElseThrow()
            .getOutput()
            .orElseThrow();

        return outputFunction.apply(inputParameters, ParametersFactory.create(Map.of()), triggerContext);
    }
}
