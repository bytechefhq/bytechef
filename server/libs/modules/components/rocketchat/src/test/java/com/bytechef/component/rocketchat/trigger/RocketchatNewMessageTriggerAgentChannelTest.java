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
 * Pins the payload shape the {@code rocketchat} agent channel's request descriptor assumes.
 * <p>
 * {@code newMessage} declares an EMPTY output and hands Rocket.Chat's raw webhook body through untouched, so
 * {@code ComponentDsl} accepts its request paths unchecked — the documented gap of the design spec's §4, not a defect
 * introduced here. Nothing inside this repository states what those fields are called, which leaves the external
 * documentation of Rocket.Chat's outgoing-webhook integration (https://docs.rocket.chat/docs/integrations) as the only
 * source: it posts {@code channel_id}, {@code channel_name}, {@code message_id}, {@code text}, {@code timestamp},
 * {@code user_id} and {@code user_name}. Pinning the declared paths against those names is the only guard available, so
 * it is the guard this test provides — a rename on either side then fails here instead of silently reading nothing at
 * run time.
 * <p>
 * The expected paths and property names are spelled as literals rather than taken from {@code RocketchatConstants}, so
 * that the test checks the production declaration instead of restating it.
 *
 * @author Ivica Cardic
 */
class RocketchatNewMessageTriggerAgentChannelTest {

    private static final TriggerDefinition TRIGGER_DEFINITION = RocketchatNewMessageTrigger.TRIGGER_DEFINITION;

    /**
     * The premise the rest of this class rests on. With neither a static output schema nor an output function, the
     * DSL's first-segment check does not run, so a mistyped path would be accepted at build time and would read nothing
     * at run time. That is why the literals below are pinned rather than resolved.
     */
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

    /**
     * {@code channel_id} is Rocket.Chat's own name for the room the message arrived in, and it carries a real room
     * identifier — which is precisely what the reply's {@code roomId} is sent to {@code /chat.postMessage} as.
     */
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

    /**
     * Rocket.Chat posts a flat payload, so a dotted path would descend into a field that does not exist. Nothing
     * validates that at build time here, hence the explicit assertion.
     */
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

    /**
     * Rocket.Chat carries no attachments into the agent, and an absent {@code attachmentsPath} is how a channel says so
     * — the generator then wires the literal {@code []}, which is exactly today's mapping. Binding a path would be
     * unfounded twice over here: the trigger declares no output at all, so there is no field to bind, and the
     * outgoing-webhook payload documents none either.
     */
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

    /**
     * Noted, not fixed. {@code roomId}'s own picker offers {@code #name} values and its description says so, while the
     * agent channel feeds it {@code channel_id}, a real room identifier. The two disagree, and the channel is the one
     * that is right: {@code roomId} is sent verbatim as the {@code roomId} body key of {@code /chat.postMessage}, which
     * wants an identifier rather than a name. Pinned so the deviation stays visible instead of being rediscovered, and
     * so whoever reconciles the picker knows a channel depends on the current behaviour.
     */
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
