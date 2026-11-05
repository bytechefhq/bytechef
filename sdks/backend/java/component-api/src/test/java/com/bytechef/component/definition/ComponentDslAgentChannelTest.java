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

package com.bytechef.component.definition;

import static com.bytechef.component.definition.AgentChannelDefinition.ATTACHMENTS;
import static com.bytechef.component.definition.AgentChannelDefinition.CONVERSATION_ID;
import static com.bytechef.component.definition.AgentChannelDefinition.MESSAGE;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.agentChannel;
import static com.bytechef.component.definition.ComponentDsl.agentChannelRequest;
import static com.bytechef.component.definition.ComponentDsl.agentReply;
import static com.bytechef.component.definition.ComponentDsl.agentRequest;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableAgentChannelDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 */
class ComponentDslAgentChannelTest {

    @Test
    void testAcceptsConformingIdentityPair() {
        ModifiableTriggerDefinition triggerDefinition = conformingTrigger();

        ModifiableActionDefinition actionDefinition = action("responseToRequest")
            .properties(
                string(MESSAGE),
                array(ATTACHMENTS).items(fileEntry()))
            .agentReply(agentReply().attachments(ATTACHMENTS));

        AgentChannelDefinition agentChannelDefinition =
            agentChannel("chat", triggerDefinition, actionDefinition)
                .title("Chat")
                .description("The hosted chat interface.")
                .approvalChannel("chat")
                .triggerParameters(Map.of("mode", 1));

        assertEquals("chat", agentChannelDefinition.getName());
        assertEquals(Optional.of("Chat"), agentChannelDefinition.getTitle());
        assertEquals(Optional.of("The hosted chat interface."), agentChannelDefinition.getDescription());
        assertSame(triggerDefinition, agentChannelDefinition.getTrigger());
        assertEquals(Optional.of("chat"), agentChannelDefinition.getApprovalChannelName());
        assertEquals(Map.of("mode", 1), agentChannelDefinition.getTriggerParameters());

        Optional<ActionDefinition> replyAction = agentChannelDefinition.getReplyAction();

        assertTrue(replyAction.isPresent());
        assertSame(actionDefinition, replyAction.get());

        AgentRequestDefinition agentRequestDefinition = triggerDefinition.getAgentRequestDefinition()
            .orElseThrow();

        assertEquals(CONVERSATION_ID, agentRequestDefinition.getConversationIdPath());
        assertEquals(MESSAGE, agentRequestDefinition.getMessagePath());
        assertEquals(Optional.empty(), agentRequestDefinition.getAttachmentsPath());

        AgentReplyDefinition agentReplyDefinition = actionDefinition.getAgentReplyDefinition()
            .orElseThrow();

        assertEquals(MESSAGE, agentReplyDefinition.getMessageProperty());
        assertEquals(Optional.empty(), agentReplyDefinition.getConversationIdProperty());
        assertEquals(Optional.of(ATTACHMENTS), agentReplyDefinition.getAttachmentsProperty());
        assertEquals(Map.of(), agentReplyDefinition.getChannelParameters());
        assertEquals(Map.of(), agentReplyDefinition.getFixedParameters());
    }

    @Test
    void testAcceptsLegacyPairViaPaths() {
        ModifiableTriggerDefinition triggerDefinition = trigger("newMessage")
            .type(TriggerType.STATIC_WEBHOOK)
            .output(
                outputSchema(
                    object()
                        .properties(
                            integer("update_id"),
                            object("message")
                                .properties(
                                    string("text"),
                                    object("chat")
                                        .properties(integer("id"))))))
            .agentRequest(
                agentRequest()
                    .conversationId("message.chat.id")
                    .message("message.text"));

        ModifiableActionDefinition actionDefinition = action("sendMessage")
            .properties(string("chat_id"), string("text"))
            .agentReply(
                agentReply()
                    .conversationId("chat_id")
                    .message("text"));

        AgentChannelDefinition agentChannelDefinition = agentChannel("telegram", triggerDefinition, actionDefinition);

        AgentRequestDefinition agentRequestDefinition = triggerDefinition.getAgentRequestDefinition()
            .orElseThrow();

        assertEquals("message.chat.id", agentRequestDefinition.getConversationIdPath());
        assertEquals("message.text", agentRequestDefinition.getMessagePath());

        AgentReplyDefinition agentReplyDefinition = actionDefinition.getAgentReplyDefinition()
            .orElseThrow();

        assertEquals("text", agentReplyDefinition.getMessageProperty());
        assertEquals(Optional.of("chat_id"), agentReplyDefinition.getConversationIdProperty());
        assertEquals("telegram", agentChannelDefinition.getName());
    }

    @Test
    void testAcceptsSchemaLessTriggerOutput() {
        ModifiableTriggerDefinition triggerDefinition = trigger("newMessage")
            .type(TriggerType.STATIC_WEBHOOK)
            .output()
            .agentRequest(
                agentRequest()
                    .conversationId("channel_id")
                    .message("text"));

        AgentChannelDefinition agentChannelDefinition = agentChannel("rocketchat", triggerDefinition);

        assertEquals("rocketchat", agentChannelDefinition.getName());
        assertEquals(Optional.empty(), agentChannelDefinition.getReplyAction());
    }

    @Test
    void testAcceptsFunctionValuedTriggerOutput() {
        ModifiableTriggerDefinition triggerDefinition = trigger("newWorkflowCall")
            .type(TriggerType.STATIC_WEBHOOK)
            .output((inputParameters, connectionParameters, context) -> null)
            .agentRequest(agentRequest());

        AgentChannelDefinition agentChannelDefinition = agentChannel("workflowCall", triggerDefinition);

        assertEquals("workflowCall", agentChannelDefinition.getName());
    }

    @Test
    void testAcceptsChannelWithNoReplyAction() {
        AgentChannelDefinition agentChannelDefinition = agentChannel("chat", conformingTrigger());

        assertEquals(Optional.empty(), agentChannelDefinition.getReplyAction());
        assertEquals(Optional.empty(), agentChannelDefinition.getApprovalChannelName());
        assertEquals(Map.of(), agentChannelDefinition.getTriggerParameters());
    }

    @Test
    void testAcceptsDottedReplyPropertyWhenFirstSegmentIsDeclared() {
        ModifiableActionDefinition actionDefinition = action("responseToWorkflowCall")
            .properties(dynamicProperties("response"))
            .agentReply(agentReply().message("response.message"));

        AgentReplyDefinition agentReplyDefinition = actionDefinition.getAgentReplyDefinition()
            .orElseThrow();

        assertEquals("response.message", agentReplyDefinition.getMessageProperty());
    }

    @Test
    void testAcceptsChannelParameterAndFixedParameter() {
        ModifiableTriggerDefinition triggerDefinition = trigger("newWhatsappMessage")
            .type(TriggerType.STATIC_WEBHOOK)
            .properties(string("number"))
            .output()
            .agentRequest(
                agentRequest()
                    .conversationId("From")
                    .message("Body"));

        ModifiableActionDefinition actionDefinition = action("sendWhatsAppMessage")
            .properties(string("To"), string("Body"), string("From"), bool("useTemplate"))
            .agentReply(
                agentReply()
                    .conversationId("To")
                    .message("Body")
                    .channelParameter("number", "From")
                    .fixedParameter("useTemplate", false));

        agentChannel("twilio", triggerDefinition, actionDefinition);

        AgentReplyDefinition agentReplyDefinition = actionDefinition.getAgentReplyDefinition()
            .orElseThrow();

        assertEquals(Map.of("number", "From"), agentReplyDefinition.getChannelParameters());
        assertEquals(Map.of("useTemplate", false), agentReplyDefinition.getFixedParameters());
    }

    @Test
    void testRejectsInvalidName() {
        ModifiableTriggerDefinition triggerDefinition = conformingTrigger();

        IllegalArgumentException exception =
            assertThrows(IllegalArgumentException.class, () -> agentChannel("bad-name", triggerDefinition));

        assertTrue(exception.getMessage()
            .contains("bad-name"), exception.getMessage());
        assertTrue(exception.getMessage()
            .contains("[a-zA-Z0-9]+"), exception.getMessage());
    }

    @Test
    void testRejectsReplyPropertyTheActionDoesNotDeclare() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> action("sendMessage")
                .properties(string("chat_id"), string("text"))
                .agentReply(agentReply().message("body")));

        assertTrue(exception.getMessage()
            .contains("body"), exception.getMessage());
    }

    @Test
    void testRejectsFixedParameterKeyTheActionDoesNotDeclare() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> action("sendMessage")
                .properties(string("text"))
                .agentReply(
                    agentReply()
                        .message("text")
                        .fixedParameter("useTemplate", false)));

        assertTrue(exception.getMessage()
            .contains("useTemplate"), exception.getMessage());
    }

    @Test
    void testRejectsChannelParameterKeyThatIsNotATriggerProperty() {
        ModifiableTriggerDefinition triggerDefinition = trigger("newWhatsappMessage")
            .type(TriggerType.STATIC_WEBHOOK)
            .output()
            .agentRequest(agentRequest());

        ModifiableActionDefinition actionDefinition = action("sendWhatsAppMessage")
            .properties(string(MESSAGE), string("From"))
            .agentReply(agentReply().channelParameter("number", "From"));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> agentChannel("twilio", triggerDefinition, actionDefinition));

        assertTrue(exception.getMessage()
            .contains("number"), exception.getMessage());
    }

    @Test
    void testRejectsDottedChannelParameterKeyEvenWhenFirstSegmentIsATriggerProperty() {
        ModifiableTriggerDefinition triggerDefinition = trigger("newWhatsappMessage")
            .type(TriggerType.STATIC_WEBHOOK)
            .properties(string("number"))
            .output()
            .agentRequest(agentRequest());

        ModifiableActionDefinition actionDefinition = action("sendWhatsAppMessage")
            .properties(dynamicProperties("response"), string("From"), bool("useTemplate"))
            .agentReply(
                agentReply()
                    .message("response.message")
                    .fixedParameter("response.useTemplate", false)
                    .channelParameter("number.x", "From"));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> agentChannel("twilio", triggerDefinition, actionDefinition));

        assertTrue(exception.getMessage()
            .contains("number.x"), exception.getMessage());

        AgentReplyDefinition agentReplyDefinition = actionDefinition.getAgentReplyDefinition()
            .orElseThrow();

        assertEquals("response.message", agentReplyDefinition.getMessageProperty());
        assertEquals(Map.of("response.useTemplate", false), agentReplyDefinition.getFixedParameters());
    }

    @Test
    void testRejectsTriggerWithoutAgentRequestDefinition() {
        ModifiableTriggerDefinition triggerDefinition = trigger("newChatRequest")
            .type(TriggerType.STATIC_WEBHOOK)
            .output(outputSchema(agentChannelRequest()));

        IllegalArgumentException exception =
            assertThrows(IllegalArgumentException.class, () -> agentChannel("chat", triggerDefinition));

        assertTrue(exception.getMessage()
            .contains("newChatRequest"), exception.getMessage());
    }

    @Test
    void testRejectsReplyActionWithoutAgentReplyDefinition() {
        ModifiableTriggerDefinition triggerDefinition = conformingTrigger();

        ModifiableActionDefinition actionDefinition = action("responseToRequest")
            .properties(string(MESSAGE));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> agentChannel("chat", triggerDefinition, actionDefinition));

        assertTrue(exception.getMessage()
            .contains("responseToRequest"), exception.getMessage());
    }

    @Test
    void testRejectsPathAbsentFromDeclaredTriggerOutputSchema() {
        ModifiableTriggerDefinition triggerDefinition = trigger("newChatRequest")
            .type(TriggerType.STATIC_WEBHOOK)
            .output(outputSchema(agentChannelRequest()))
            .agentRequest(agentRequest().conversationId("chat.id"));

        IllegalArgumentException exception =
            assertThrows(IllegalArgumentException.class, () -> agentChannel("chat", triggerDefinition));

        assertTrue(exception.getMessage()
            .contains("chat.id"), exception.getMessage());
    }

    @Test
    void testAcceptsFullyDeclaredNestedRequestPath() {
        AgentChannelDefinition agentChannelDefinition =
            agentChannel("telegram", nestedOutputTrigger("message.chat.id"));

        assertEquals("telegram", agentChannelDefinition.getName());
    }

    @Test
    void testRejectsNestedRequestPathSegmentTheOutputSchemaDoesNotDeclare() {
        ModifiableTriggerDefinition triggerDefinition = nestedOutputTrigger("message.chat.identifier");

        IllegalArgumentException exception =
            assertThrows(IllegalArgumentException.class, () -> agentChannel("telegram", triggerDefinition));

        assertTrue(exception.getMessage()
            .contains("message.chat.identifier"), exception.getMessage());
    }

    @Test
    void testRejectsRequestPathDescendingIntoANonObjectProperty() {
        ModifiableTriggerDefinition triggerDefinition = nestedOutputTrigger("message.text.body");

        IllegalArgumentException exception =
            assertThrows(IllegalArgumentException.class, () -> agentChannel("telegram", triggerDefinition));

        assertTrue(exception.getMessage()
            .contains("message.text.body"), exception.getMessage());
    }

    @Test
    void testAcceptsRequestPathBelowAnObjectDeclaringNoProperties() {
        ModifiableTriggerDefinition triggerDefinition = trigger("newMessage")
            .type(TriggerType.STATIC_WEBHOOK)
            .output(
                outputSchema(
                    object()
                        .properties(
                            object("payload"))))
            .agentRequest(
                agentRequest()
                    .conversationId("payload.anything.at.all")
                    .message("payload.whatever"));

        assertEquals("opaque", agentChannel("opaque", triggerDefinition)
            .getName());
    }

    @Test
    void testUnverifiedPathsFallsBackToFirstSegmentValidation() {
        ModifiableTriggerDefinition triggerDefinition = nestedOutputTrigger("message.chat.identifier")
            .agentRequest(
                agentRequest()
                    .conversationId("message.chat.identifier")
                    .message("message.text")
                    .unverifiedPaths("the declared schema is known to disagree with the real payload"));

        AgentChannelDefinition agentChannelDefinition = agentChannel("telegram", triggerDefinition);

        AgentRequestDefinition agentRequestDefinition = triggerDefinition.getAgentRequestDefinition()
            .orElseThrow();

        assertEquals("telegram", agentChannelDefinition.getName());
        assertEquals(
            Optional.of("the declared schema is known to disagree with the real payload"),
            agentRequestDefinition.getUnverifiedPathsReason());
    }

    @Test
    void testUnverifiedPathsStillRejectsAnUndeclaredFirstSegment() {
        ModifiableTriggerDefinition triggerDefinition = nestedOutputTrigger("message.text")
            .agentRequest(
                agentRequest()
                    .conversationId("envelope.chat.id")
                    .message("message.text")
                    .unverifiedPaths("the declared schema is known to disagree with the real payload"));

        IllegalArgumentException exception =
            assertThrows(IllegalArgumentException.class, () -> agentChannel("telegram", triggerDefinition));

        assertTrue(exception.getMessage()
            .contains("envelope.chat.id"), exception.getMessage());
    }

    @Test
    void testDeclaresNoUnverifiedPathsReasonByDefault() {
        ModifiableTriggerDefinition triggerDefinition = conformingTrigger();

        AgentRequestDefinition agentRequestDefinition = triggerDefinition.getAgentRequestDefinition()
            .orElseThrow();

        assertEquals(Optional.empty(), agentRequestDefinition.getUnverifiedPathsReason());
    }

    @Test
    void testUndeclaredUnverifiedPathsReasonIsAbsentFromSerializedDefinition() {
        ModifiableTriggerDefinition triggerDefinition = conformingTrigger();

        ComponentDefinition componentDefinition = component("chat").triggers(triggerDefinition);

        Map<String, ?> triggerMap = firstOf(toMap(componentDefinition), "triggers");
        Map<?, ?> agentRequestMap = (Map<?, ?>) triggerMap.get("agentRequestDefinition");

        assertFalse(agentRequestMap.containsKey("unverifiedPathsReason"), agentRequestMap.toString());
    }

    @Test
    void testPlainComponentDeclaresNoAgentChannels() {
        assertEquals(List.of(), component("plain").getAgentChannels());
    }

    @Test
    void testUndeclaredDescriptorsAreAbsentFromSerializedDefinition() {
        ComponentDefinition componentDefinition = component("plain")
            .triggers(trigger("plainTrigger").type(TriggerType.STATIC_WEBHOOK))
            .actions(action("plainAction").properties(string("text")));

        Map<String, ?> componentMap = toMap(componentDefinition);

        assertFalse(componentMap.containsKey("agentChannels"), componentMap.toString());

        Map<String, ?> actionMap = firstOf(componentMap, "actions");

        assertFalse(actionMap.containsKey("agentReplyDefinition"), actionMap.toString());

        Map<String, ?> triggerMap = firstOf(componentMap, "triggers");

        assertFalse(triggerMap.containsKey("agentRequestDefinition"), triggerMap.toString());
    }

    @Test
    void testDeclaredDescriptorsAreSerialized() {
        ModifiableTriggerDefinition triggerDefinition = conformingTrigger();

        ModifiableActionDefinition actionDefinition = action("responseToRequest")
            .properties(string(MESSAGE))
            .agentReply(agentReply().message(MESSAGE));

        ComponentDefinition componentDefinition = component("chat")
            .triggers(triggerDefinition)
            .actions(actionDefinition)
            .agentChannels(agentChannel("chat", triggerDefinition, actionDefinition).title("Chat"));

        Map<String, ?> componentMap = toMap(componentDefinition);

        assertEquals(1, ((List<?>) componentMap.get("agentChannels")).size(), componentMap.toString());

        Map<String, ?> actionMap = firstOf(componentMap, "actions");
        Map<?, ?> agentReplyMap = (Map<?, ?>) actionMap.get("agentReplyDefinition");

        assertEquals(MESSAGE, agentReplyMap.get("messageProperty"), actionMap.toString());

        Map<String, ?> triggerMap = firstOf(componentMap, "triggers");
        Map<?, ?> agentRequestMap = (Map<?, ?>) triggerMap.get("agentRequestDefinition");

        assertEquals(CONVERSATION_ID, agentRequestMap.get("conversationIdPath"), triggerMap.toString());
    }

    @Test
    void testSerializedAgentChannelCarriesNamesInsteadOfNestedDefinitions() {
        ModifiableTriggerDefinition triggerDefinition = conformingTrigger();

        ModifiableActionDefinition actionDefinition = action("responseToRequest")
            .properties(string(MESSAGE))
            .agentReply(agentReply().message(MESSAGE));

        ComponentDefinition componentDefinition = component("chat")
            .triggers(triggerDefinition)
            .actions(actionDefinition)
            .agentChannels(agentChannel("chat", triggerDefinition, actionDefinition));

        Map<String, ?> agentChannelMap = firstOf(toMap(componentDefinition), "agentChannels");

        assertFalse(agentChannelMap.containsKey("trigger"), agentChannelMap.toString());
        assertFalse(agentChannelMap.containsKey("replyAction"), agentChannelMap.toString());

        assertEquals("newChatRequest", agentChannelMap.get("triggerName"), agentChannelMap.toString());
        assertEquals("responseToRequest", agentChannelMap.get("replyActionName"), agentChannelMap.toString());
    }

    @Test
    void testSerializedAgentChannelWithNoReplyActionOmitsTheReplyActionName() {
        ModifiableTriggerDefinition triggerDefinition = conformingTrigger();

        ComponentDefinition componentDefinition = component("chat")
            .triggers(triggerDefinition)
            .agentChannels(agentChannel("chat", triggerDefinition));

        Map<String, ?> agentChannelMap = firstOf(toMap(componentDefinition), "agentChannels");

        assertFalse(agentChannelMap.containsKey("replyActionName"), agentChannelMap.toString());

        assertEquals("newChatRequest", agentChannelMap.get("triggerName"), agentChannelMap.toString());
    }

    @Test
    void testComponentReturnsDeclaredAgentChannels() {
        AgentChannelDefinition agentChannelDefinition = agentChannel("chat", conformingTrigger());

        ComponentDefinition componentDefinition = component("chat").agentChannels(agentChannelDefinition);

        assertEquals(List.of(agentChannelDefinition), componentDefinition.getAgentChannels());
    }

    @Test
    void testTriggerParametersKeepDeclarationOrder() {
        List<String> declaredNames = List.of(
            "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten");

        Map<String, Object> declaredTriggerParameters = new LinkedHashMap<>();

        for (String declaredName : declaredNames) {
            declaredTriggerParameters.put(declaredName, declaredName);
        }

        AgentChannelDefinition agentChannelDefinition =
            agentChannel("chat", conformingTrigger()).triggerParameters(declaredTriggerParameters);

        Map<String, Object> triggerParameters = agentChannelDefinition.getTriggerParameters();

        assertEquals(declaredNames, List.copyOf(triggerParameters.keySet()));
    }

    @Test
    void testTriggerParameterAccumulatesInDeclarationOrder() {
        List<String> declaredNames = List.of(
            "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten");

        ModifiableAgentChannelDefinition agentChannelDefinition = agentChannel("chat", conformingTrigger());

        for (String declaredName : declaredNames) {
            agentChannelDefinition.triggerParameter(declaredName, declaredName);
        }

        assertEquals(declaredNames, List.copyOf(agentChannelDefinition.getTriggerParameters()
            .keySet()));
    }

    @Test
    void testTriggerParameterOverwritesInPlace() {
        ModifiableAgentChannelDefinition agentChannelDefinition = agentChannel("chat", conformingTrigger())
            .triggerParameter("first", 1)
            .triggerParameter("second", 2)
            .triggerParameter("first", 3);

        Map<String, Object> triggerParameters = agentChannelDefinition.getTriggerParameters();

        assertEquals(List.of("first", "second"), List.copyOf(triggerParameters.keySet()));
        assertEquals(3, triggerParameters.get("first"));
    }

    @Test
    void testTriggerParameterAccumulatesOnTopOfTheMapOverload() {
        Map<String, Object> declaredTriggerParameters = new LinkedHashMap<>();

        declaredTriggerParameters.put("first", 1);

        ModifiableAgentChannelDefinition agentChannelDefinition = agentChannel("chat", conformingTrigger())
            .triggerParameters(declaredTriggerParameters)
            .triggerParameter("second", 2);

        assertEquals(List.of("first", "second"), List.copyOf(agentChannelDefinition.getTriggerParameters()
            .keySet()));
    }

    @Test
    void testRejectsNullTriggerParameterKeyOrValue() {
        ModifiableAgentChannelDefinition agentChannelDefinition = agentChannel("chat", conformingTrigger());

        assertThrows(NullPointerException.class, () -> agentChannelDefinition.triggerParameter(null, 1));
        assertThrows(NullPointerException.class, () -> agentChannelDefinition.triggerParameter("first", null));
    }

    private static ModifiableTriggerDefinition nestedOutputTrigger(String conversationIdPath) {
        return trigger("newMessage")
            .type(TriggerType.STATIC_WEBHOOK)
            .output(
                outputSchema(
                    object()
                        .properties(
                            integer("update_id"),
                            object("message")
                                .properties(
                                    string("text"),
                                    object("chat")
                                        .properties(integer("id"))))))
            .agentRequest(
                agentRequest()
                    .conversationId(conversationIdPath)
                    .message("message.text"));
    }

    private static ModifiableTriggerDefinition conformingTrigger() {
        return trigger("newChatRequest")
            .type(TriggerType.STATIC_WEBHOOK)
            .output(outputSchema(agentChannelRequest()))
            .agentRequest(agentRequest());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> firstOf(Map<String, ?> componentMap, String key) {
        List<?> list = (List<?>) componentMap.get(key);

        return (Map<String, ?>) list.getFirst();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> toMap(Object definition) {
        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.readValue(objectMapper.writeValueAsString(definition), Map.class);
    }
}
