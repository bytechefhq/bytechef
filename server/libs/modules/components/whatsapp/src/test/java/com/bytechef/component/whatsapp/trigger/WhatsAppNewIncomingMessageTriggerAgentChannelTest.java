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

package com.bytechef.component.whatsapp.trigger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.AgentChannelDefinition;
import com.bytechef.component.definition.AgentReplyDefinition;
import com.bytechef.component.definition.AgentRequestDefinition;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.OutputDefinition;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.whatsapp.WhatsAppComponentHandler;
import com.bytechef.component.whatsapp.action.WhatsAppSendMessageAction;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.definition.BaseProperty;
import com.bytechef.definition.BaseProperty.BaseObjectProperty;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class WhatsAppNewIncomingMessageTriggerAgentChannelTest {

    private static final TriggerDefinition TRIGGER_DEFINITION = WhatsAppNewIncomingMessageTrigger.TRIGGER_DEFINITION;

    @Test
    void testConversationIdPathIsTodaysExpressionUnchanged() {
        AgentRequestDefinition agentRequestDefinition = getAgentRequestDefinition();

        assertEquals(
            "entry.changes.value.messages.from", agentRequestDefinition.getConversationIdPath(),
            "the conversation identifier must keep addressing the number the message came from, which is also the "
                + "number the reply is addressed to; this is today's ${__NODE__.} expression minus the wrapper");
    }

    @Test
    void testMessagePathIsTodaysExpressionUnchanged() {
        AgentRequestDefinition agentRequestDefinition = getAgentRequestDefinition();

        assertEquals(
            "entry.changes.value.messages.text.body", agentRequestDefinition.getMessagePath(),
            "this is today's ${__NODE__.} expression minus the wrapper");
    }

    @Test
    void testTheDescriptorPathsDisagreeWithTheDeclaredSchema() {
        AgentRequestDefinition agentRequestDefinition = getAgentRequestDefinition();

        assertTrue(
            resolvePath(agentRequestDefinition.getConversationIdPath())
                .isEmpty(),
            "the declared schema does not nest messages under value, so today's conversationId path does not resolve "
                + "against it");
        assertTrue(
            resolvePath(agentRequestDefinition.getMessagePath())
                .isEmpty(),
            "likewise for the message path");

        assertTrue(
            resolvePath("entry.changes.value")
                .isPresent(),
            "value itself is declared; it is the descent from value into messages that the schema does not support");
        assertTrue(
            resolvePath("entry.changes.messages.from")
                .isPresent(),
            "the schema places messages beside value rather than inside it — this is the path the schema implies, and "
                + "it is not the path the channel reads");
        assertTrue(
            resolvePath("entry.changes.messages.text.body")
                .isPresent(),
            "likewise for the message text");
    }

    @Test
    void testAnUndeclaredPathDoesNotResolve() {
        assertFalse(
            resolvePath("entry.changes.messages.somethingElse")
                .isPresent(),
            "a path naming a field the trigger does not declare must not resolve");
        assertFalse(
            resolvePath("entry.changes.messages.from.body")
                .isPresent(),
            "a path descending into a leaf must not resolve");
    }

    @Test
    void testTheDeclaredNestingIsObjectsNotArrays() {
        for (String path : List.of("entry", "entry.changes", "entry.changes.messages")) {
            BaseProperty property = resolvePath(path)
                .orElseThrow();

            assertInstanceOf(
                BaseObjectProperty.class, property,
                "'%s' is declared an object here while Meta documents an array".formatted(path));
        }
    }

    @Test
    void testAttachmentsAreDeliberatelyUnbound() {
        AgentRequestDefinition agentRequestDefinition = getAgentRequestDefinition();

        assertTrue(
            agentRequestDefinition.getAttachmentsPath()
                .isEmpty(),
            "whatsApp binds no attachmentsPath, so the generator wires [] as it does today");

        assertFalse(
            getMessagesPropertyNames().contains(AgentChannelDefinition.ATTACHMENTS),
            "the trigger's messages output must declare no attachments field, otherwise the unbound "
                + "attachmentsPath would be dropping something the channel actually carries");
    }

    @Test
    void testSenderNumberStaysOrdinaryConfiguration() {
        Set<String> triggerPropertyNames = TRIGGER_DEFINITION.getProperties()
            .stream()
            .map(BaseProperty::getName)
            .collect(Collectors.toSet());

        assertTrue(triggerPropertyNames.contains("senderNumber"));

        AgentReplyDefinition agentReplyDefinition = getAgentReplyDefinition();

        assertTrue(
            agentReplyDefinition.getChannelParameters()
                .isEmpty(),
            "the reply takes no parameter from the channel row; it is addressed from the request");
    }

    @Test
    void testReplyPropertiesAreDeclaredBySendMessage() {
        AgentReplyDefinition agentReplyDefinition = getAgentReplyDefinition();

        assertEquals(
            "to", agentReplyDefinition.getConversationIdProperty()
                .orElseThrow(
                    () -> new AssertionError(
                        "the reply must be addressed: a WhatsApp message goes to a phone number, it is not a response "
                            + "down an open request")));
        assertEquals("body", agentReplyDefinition.getMessageProperty());
        assertTrue(
            agentReplyDefinition.getAttachmentsProperty()
                .isEmpty(),
            "sendMessage declares no attachments property, matching the unbound request half");

        Set<String> propertyNames = WhatsAppSendMessageAction.ACTION_DEFINITION.getProperties()
            .stream()
            .map(BaseProperty::getName)
            .collect(Collectors.toSet());

        assertTrue(propertyNames.contains("to"), "the reply's conversationId property must really be declared");
        assertTrue(propertyNames.contains("body"), "the reply's message property must really be declared");
    }

    @Test
    void testChannelPairsTheTriggerAndTheReplyActionUnderTheStoredKey() {
        AgentChannelDefinition agentChannelDefinition = getAgentChannelDefinition();

        assertEquals(
            "whatsapp", agentChannelDefinition.getName(),
            "the stored channel key is lowercase, unlike the component's own name 'whatsApp'; it must not drift");
        assertEquals(Optional.of("WhatsApp"), agentChannelDefinition.getTitle());
        assertEquals(
            Optional.of("whatsApp"), agentChannelDefinition.getApprovalChannelName(),
            "approvals reach the user through the component's own whatsApp approval channel element, whose name keeps "
                + "the capital A");

        TriggerDefinition triggerDefinition = agentChannelDefinition.getTrigger();

        assertEquals(
            "messageReceived", triggerDefinition.getName(),
            "the registered trigger is 'messageReceived' despite the class being named after an incoming message");

        ActionDefinition actionDefinition = agentChannelDefinition.getReplyAction()
            .orElseThrow();

        assertEquals("sendMessage", actionDefinition.getName());

        assertTrue(
            agentChannelDefinition.getTriggerParameters()
                .isEmpty(),
            "no trigger parameter is fixed for this channel");
    }

    private static AgentChannelDefinition getAgentChannelDefinition() {
        WhatsAppComponentHandler whatsAppComponentHandler = new WhatsAppComponentHandler();

        ComponentDefinition componentDefinition = whatsAppComponentHandler.getDefinition();

        List<AgentChannelDefinition> agentChannelDefinitions = componentDefinition.getAgentChannels();

        assertEquals(1, agentChannelDefinitions.size(), "the whatsApp component declares exactly one agent channel");

        return agentChannelDefinitions.getFirst();
    }

    private static AgentReplyDefinition getAgentReplyDefinition() {
        return WhatsAppSendMessageAction.ACTION_DEFINITION.getAgentReplyDefinition()
            .orElseThrow(() -> new AssertionError("the reply action must declare an agentReply descriptor"));
    }

    private static AgentRequestDefinition getAgentRequestDefinition() {
        return TRIGGER_DEFINITION.getAgentRequestDefinition()
            .orElseThrow(() -> new AssertionError("the trigger must declare an agentRequest descriptor"));
    }

    private static Set<String> getMessagesPropertyNames() {
        BaseProperty messagesProperty = resolvePath("entry.changes.messages").orElseThrow();

        BaseObjectProperty<?> objectProperty = assertInstanceOf(BaseObjectProperty.class, messagesProperty);

        return objectProperty.getProperties()
            .stream()
            .map(BaseProperty::getName)
            .collect(Collectors.toSet());
    }

    private static Optional<BaseProperty> resolvePath(String path) {
        BaseProperty currentProperty = TRIGGER_DEFINITION.getOutputDefinition()
            .flatMap(OutputDefinition::getOutputResponse)
            .map(OutputResponse::getOutputSchema)
            .orElseThrow(() -> new AssertionError("the trigger must declare a static output schema"));

        for (String segment : path.split("\\.")) {
            if (!(currentProperty instanceof BaseObjectProperty<?> objectProperty)) {
                return Optional.empty();
            }

            Optional<BaseProperty> nextProperty = objectProperty.getProperties()
                .stream()
                .filter(property -> segment.equals(property.getName()))
                .map(BaseProperty.class::cast)
                .findFirst();

            if (nextProperty.isEmpty()) {
                return Optional.empty();
            }

            currentProperty = nextProperty.get();
        }

        return Optional.of(currentProperty);
    }
}
