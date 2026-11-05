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

package com.bytechef.component.rocketchat.trigger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.AgentChannelDefinition;
import com.bytechef.component.definition.AgentReplyDefinition;
import com.bytechef.component.definition.AgentRequestDefinition;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.OutputDefinition;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.rocketchat.RocketchatComponentHandler;
import com.bytechef.component.rocketchat.action.RocketchatSendChannelMessageAction;
import com.bytechef.definition.BaseProperty;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class RocketchatNewMessageTriggerAgentChannelTest {

    private static final TriggerDefinition TRIGGER_DEFINITION = RocketchatNewMessageTrigger.TRIGGER_DEFINITION;

    @Test
    void testTriggerDeclaresNoOutputSoThePathsCannotBeValidatedAtBuildTime() {
        OutputDefinition outputDefinition = TRIGGER_DEFINITION.getOutputDefinition()
            .orElseThrow(() -> new AssertionError("the trigger declares .output(), it is just empty"));

        assertTrue(
            outputDefinition.getOutputResponse()
                .isEmpty(),
            "newMessage declares no static output schema, so its request paths are unvalidatable at build time");
        assertTrue(
            outputDefinition.getOutput()
                .isEmpty(),
            "newMessage declares no output function either");
    }

    @Test
    void testConversationIdPathIsRocketChatsOutgoingWebhookChannelIdField() {
        AgentRequestDefinition agentRequestDefinition = getAgentRequestDefinition();

        assertEquals(
            "channel_id", agentRequestDefinition.getConversationIdPath(),
            "the conversation identifier must keep addressing the room the message arrived in, which is also the "
                + "room the reply is sent back to");
    }

    @Test
    void testMessagePathIsRocketChatsOutgoingWebhookTextField() {
        AgentRequestDefinition agentRequestDefinition = getAgentRequestDefinition();

        assertEquals("text", agentRequestDefinition.getMessagePath());
    }

    @Test
    void testBothRequestPathsAreFlatBecauseTheWebhookPayloadIsFlat() {
        AgentRequestDefinition agentRequestDefinition = getAgentRequestDefinition();

        assertFalse(
            agentRequestDefinition.getConversationIdPath()
                .contains("."),
            "the outgoing-webhook payload is flat, so the conversation identifier path must not be dotted");
        assertFalse(
            agentRequestDefinition.getMessagePath()
                .contains("."),
            "the outgoing-webhook payload is flat, so the message path must not be dotted");
    }

    @Test
    void testAttachmentsAreDeliberatelyUnbound() {
        AgentRequestDefinition agentRequestDefinition = getAgentRequestDefinition();

        assertTrue(
            agentRequestDefinition.getAttachmentsPath()
                .isEmpty(),
            "rocketchat binds no attachmentsPath, so the generator wires [] as it does today");
    }

    @Test
    void testReplyPropertiesAreDeclaredBySendChannelMessage() {
        AgentReplyDefinition agentReplyDefinition = getAgentReplyDefinition();

        assertEquals(
            "roomId", agentReplyDefinition.getConversationIdProperty()
                .orElseThrow(
                    () -> new AssertionError(
                        "the reply must be addressed: a Rocket.Chat message goes to a room, it is not a response "
                            + "down an open request")));
        assertEquals("text", agentReplyDefinition.getMessageProperty());
        assertTrue(
            agentReplyDefinition.getAttachmentsProperty()
                .isEmpty(),
            "sendChannelMessage declares no attachments property, matching the unbound request half");
        assertEquals(
            Map.of(), agentReplyDefinition.getChannelParameters(),
            "the channel row configures nothing the reply needs: the room comes from the incoming message");
        assertEquals(Map.of(), agentReplyDefinition.getFixedParameters());

        Set<String> propertyNames = getReplyActionPropertyNames();

        assertTrue(propertyNames.contains("roomId"), "the reply's conversationId property must really be declared");
        assertTrue(propertyNames.contains("text"), "the reply's message property must really be declared");
    }

    @Test
    void testReplyRoomPropertyDocumentsANamePickerWhileTheChannelFeedsARoomIdentifier() {
        AgentReplyDefinition agentReplyDefinition = getAgentReplyDefinition();

        String conversationIdProperty = agentReplyDefinition.getConversationIdProperty()
            .orElseThrow();

        Property property = RocketchatSendChannelMessageAction.ACTION_DEFINITION.getProperties()
            .stream()
            .filter(actionProperty -> conversationIdProperty.equals(actionProperty.getName()))
            .findFirst()
            .orElseThrow();

        String description = property.getDescription()
            .orElseThrow();

        assertTrue(
            description.contains("#"),
            "the property still documents a '#name', while the channel addresses the reply by room identifier");
    }

    @Test
    void testChannelPairsTheTriggerAndTheReplyActionUnderTheStoredKey() {
        AgentChannelDefinition agentChannelDefinition = getAgentChannelDefinition();

        assertEquals("rocketchat", agentChannelDefinition.getName(), "the stored channel key must not drift");
        assertEquals(Optional.of("Rocket.Chat"), agentChannelDefinition.getTitle());
        assertEquals(
            Optional.of("rocketchat"), agentChannelDefinition.getApprovalChannelName(),
            "approvals reach the user through the component's own rocketchat approval channel element");

        TriggerDefinition triggerDefinition = agentChannelDefinition.getTrigger();

        assertEquals(TRIGGER_DEFINITION.getName(), triggerDefinition.getName());

        ActionDefinition actionDefinition = agentChannelDefinition.getReplyAction()
            .orElseThrow();

        assertEquals(RocketchatSendChannelMessageAction.ACTION_DEFINITION.getName(), actionDefinition.getName());

        assertEquals(
            Map.of(), agentChannelDefinition.getTriggerParameters(),
            "the trigger needs no pinned parameters: it subscribes to every room the integration covers and emits "
                + "Rocket.Chat's own payload");
    }

    private static AgentChannelDefinition getAgentChannelDefinition() {
        RocketchatComponentHandler rocketchatComponentHandler = new RocketchatComponentHandler();

        ComponentDefinition componentDefinition = rocketchatComponentHandler.getDefinition();

        List<AgentChannelDefinition> agentChannelDefinitions = componentDefinition.getAgentChannels();

        assertEquals(1, agentChannelDefinitions.size(), "the rocketchat component declares exactly one agent channel");

        return agentChannelDefinitions.getFirst();
    }

    private static AgentReplyDefinition getAgentReplyDefinition() {
        return RocketchatSendChannelMessageAction.ACTION_DEFINITION.getAgentReplyDefinition()
            .orElseThrow(() -> new AssertionError("the reply action must declare an agentReply descriptor"));
    }

    private static AgentRequestDefinition getAgentRequestDefinition() {
        return TRIGGER_DEFINITION.getAgentRequestDefinition()
            .orElseThrow(() -> new AssertionError("the trigger must declare an agentRequest descriptor"));
    }

    private static Set<String> getReplyActionPropertyNames() {
        return RocketchatSendChannelMessageAction.ACTION_DEFINITION.getProperties()
            .stream()
            .map(BaseProperty::getName)
            .collect(Collectors.toSet());
    }
}
