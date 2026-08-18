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

package com.bytechef.component.infobip.trigger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.AgentChannelDefinition;
import com.bytechef.component.definition.AgentReplyDefinition;
import com.bytechef.component.definition.AgentRequestDefinition;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.OutputDefinition;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.infobip.InfobipComponentHandler;
import com.bytechef.component.infobip.action.InfobipSendWhatsappTextMessageAction;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Exercises the {@code infobip} agent channel.
 * <p>
 * The trigger's output is schema-less — {@code dynamicWebhookRequest} returns Infobip's webhook body verbatim — so
 * {@code ComponentDsl} validates the request paths against nothing at all. They are pinned here instead, and the shape
 * they assume is Infobip's documented inbound-message payload
 * (<a href="https://www.infobip.com/docs/api/channels/whatsapp/whatsapp-inbound-messages">Infobip WhatsApp inbound
 * messages</a>: {@code {"results": [{"from": ..., "to": ..., "message": {"text": ...}}]}}).
 * <p>
 * These paths are carried over from {@code ChannelDefinitions} unchanged. Infobip documents {@code results} as an
 * ARRAY, and the platform's SpEL evaluator does not auto-flatten a list, so if that documentation is accurate then
 * {@code results.from} has never resolved. That is a pre-existing condition this refactor reproduces rather than
 * introduces or fixes — there is no payload fixture in this repo to settle it against.
 *
 * @author Ivica Cardic
 */
class InfobipNewWhatsAppMessageTriggerAgentChannelTest {

    private static final TriggerDefinition TRIGGER_DEFINITION = InfobipNewWhatsAppMessageTrigger.TRIGGER_DEFINITION;

    private static final ActionDefinition REPLY_ACTION_DEFINITION =
        InfobipSendWhatsappTextMessageAction.ACTION_DEFINITION;

    /**
     * The paths reproduce today's generated envelope expressions minus their {@code ${__NODE__.}} wrapper. Nothing in
     * the component validates them, so a drifted path would construct, deploy and read nothing at run time.
     */
    @Test
    void testRequestPathsReproduceTodaysEnvelopeExpressions() {
        AgentRequestDefinition agentRequestDefinition = getAgentRequestDefinition();

        assertEquals(
            "results.from", agentRequestDefinition.getConversationIdPath(),
            "the inbound sender is both the conversation the agent is talking in and the address a reply goes back "
                + "to; the trigger declares no output schema, so nothing but this test pins the path");
        assertEquals(
            "results.message.text", agentRequestDefinition.getMessagePath(),
            "Infobip nests the inbound text under message.text");
    }

    /**
     * The trigger declares no output at all, so there is no attachments field to bind — an absent
     * {@code attachmentsPath} is how a channel says it carries none, and the generator then wires the literal
     * {@code []} that is mapped today.
     */
    @Test
    void testAttachmentsAreDeliberatelyUnbound() {
        AgentRequestDefinition agentRequestDefinition = getAgentRequestDefinition();

        assertTrue(
            agentRequestDefinition.getAttachmentsPath()
                .isEmpty(),
            "infobip binds no attachmentsPath, so the generator wires [] as it does today");

        assertTrue(
            TRIGGER_DEFINITION.getOutputDefinition()
                .flatMap(OutputDefinition::getOutputResponse)
                .isEmpty(),
            "the trigger returns Infobip's raw webhook body and declares no output schema, so there is no attachments "
                + "field it could have bound");
    }

    /**
     * A channel-row key is only accepted when the paired trigger declares a property of that name. Unlike twilio, where
     * the property had to be added optional so that saved workflows stayed valid, {@code number} predates this channel
     * and is already required — so a channel row cannot leave the reply's sender unset.
     */
    @Test
    void testTriggerAlreadyRequiresTheChannelRowNumber() {
        Property numberProperty = getTriggerProperty("number");

        assertTrue(
            numberProperty.getRequired(),
            "number is required on the trigger already, so the reply's sender can never be unset on a channel row");
        assertEquals(Property.Type.STRING, numberProperty.getType());
    }

    @Test
    void testReplyAddressesTheInboundSenderAndCarriesTheAgentText() {
        AgentReplyDefinition agentReplyDefinition = getAgentReplyDefinition();

        assertEquals(
            Optional.of("to"), agentReplyDefinition.getConversationIdProperty(),
            "the reply is addressed to the recipient number, which the envelope fills from the inbound sender");
        assertEquals("text", agentReplyDefinition.getMessageProperty());
        assertTrue(
            agentReplyDefinition.getAttachmentsProperty()
                .isEmpty(),
            "sendWhatsappTextMessage declares no attachments property, matching the unbound request half");
    }

    /**
     * The sender is taken from the channel row's {@code number} rather than from the inbound {@code results.to} the
     * envelope maps today, so two rows on one Infobip account each answer as the number they were configured with.
     */
    @Test
    void testReplySenderComesFromTheChannelRowNumber() {
        AgentReplyDefinition agentReplyDefinition = getAgentReplyDefinition();

        assertEquals(
            Map.of("number", "from"), agentReplyDefinition.getChannelParameters(),
            "the channel row's number fills the action's from, replacing the inbound results.to mapped today");
        assertEquals(
            Map.of(), agentReplyDefinition.getFixedParameters(),
            "sendWhatsappTextMessage needs no property pinned to a constant: from, to and text are all reachable");
    }

    @Test
    void testChannelPairsTheTriggerAndTheReplyActionUnderTheStoredKey() {
        AgentChannelDefinition agentChannelDefinition = getAgentChannelDefinition();

        assertEquals("infobip", agentChannelDefinition.getName(), "the stored channel key must not drift");
        assertEquals(Optional.of("Infobip (WhatsApp)"), agentChannelDefinition.getTitle());
        assertEquals(
            Optional.of("whatsApp"), agentChannelDefinition.getApprovalChannelName(),
            "approvals reach the user through the component's own whatsApp approval channel element, whose name is "
                + "not the channel key");

        TriggerDefinition triggerDefinition = agentChannelDefinition.getTrigger();

        assertEquals(TRIGGER_DEFINITION.getName(), triggerDefinition.getName());

        ActionDefinition actionDefinition = agentChannelDefinition.getReplyAction()
            .orElseThrow();

        assertEquals(REPLY_ACTION_DEFINITION.getName(), actionDefinition.getName());

        assertEquals(
            Map.of(), agentChannelDefinition.getTriggerParameters(),
            "the trigger needs no pinned parameters: the number it listens on and the keyword it filters by are both "
                + "per-row configuration, not constants of the channel");
    }

    /**
     * {@code newSMS} declares the same {@code number} and {@code keyword} properties and would pair just as well with
     * {@code sendSMS}, but declaring it is separate work. Pinning the count keeps a second channel from arriving
     * unnoticed under this one's key.
     */
    @Test
    void testOnlyTheWhatsAppChannelIsDeclared() {
        AgentChannelDefinition agentChannelDefinition = getAgentChannelDefinition();

        assertEquals(
            "newWhatsappMessage", agentChannelDefinition.getTrigger()
                .getName(),
            "the sole declared channel is the WhatsApp one; newSMS is deliberately left undeclared");
    }

    private static AgentChannelDefinition getAgentChannelDefinition() {
        InfobipComponentHandler infobipComponentHandler = new InfobipComponentHandler();

        ComponentDefinition componentDefinition = infobipComponentHandler.getDefinition();

        List<AgentChannelDefinition> agentChannelDefinitions = componentDefinition.getAgentChannels();

        assertEquals(1, agentChannelDefinitions.size(), "the infobip component declares exactly one agent channel");

        return agentChannelDefinitions.getFirst();
    }

    private static AgentReplyDefinition getAgentReplyDefinition() {
        return REPLY_ACTION_DEFINITION.getAgentReplyDefinition()
            .orElseThrow(() -> new AssertionError("the reply action must declare an agentReply descriptor"));
    }

    private static AgentRequestDefinition getAgentRequestDefinition() {
        return TRIGGER_DEFINITION.getAgentRequestDefinition()
            .orElseThrow(() -> new AssertionError("the trigger must declare an agentRequest descriptor"));
    }

    private static Property getTriggerProperty(String name) {
        List<? extends Property> properties = TRIGGER_DEFINITION.getProperties();

        return properties.stream()
            .filter(property -> name.equals(property.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("property '%s' must be declared".formatted(name)));
    }
}
