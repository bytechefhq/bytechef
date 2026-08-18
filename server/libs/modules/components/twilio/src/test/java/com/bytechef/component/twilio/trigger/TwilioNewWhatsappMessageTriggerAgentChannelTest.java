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

package com.bytechef.component.twilio.trigger;

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
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.twilio.TwilioComponentHandler;
import com.bytechef.component.twilio.action.TwilioSendWhatsAppMessageAction;
import com.bytechef.definition.BaseProperty.BaseValueProperty;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Exercises the {@code twilio} agent channel, the first channel to route a reply property through the channel row
 * ({@code channelParameter}) and the first to pin one to a constant ({@code fixedParameter}).
 * <p>
 * The trigger's output is schema-less — it returns Twilio's form-encoded webhook body verbatim — so
 * {@code ComponentDsl} validates the request paths against nothing at all. The capitalised field names are therefore
 * pinned here, sourced from Twilio's own documentation of the parameters it posts to an incoming-message webhook
 * (<a href="https://www.twilio.com/docs/messaging/guides/webhook-request">Twilio Messaging webhook request</a>:
 * {@code From}, {@code To} and {@code Body} are sent capitalised, form-encoded). A lowercase path would construct and
 * deploy just as happily, and read nothing at run time.
 *
 * @author Ivica Cardic
 */
class TwilioNewWhatsappMessageTriggerAgentChannelTest {

    private static final TriggerDefinition TRIGGER_DEFINITION = TwilioNewWhatsappMessageTrigger.TRIGGER_DEFINITION;

    private static final ActionDefinition REPLY_ACTION_DEFINITION = TwilioSendWhatsAppMessageAction.ACTION_DEFINITION;

    /**
     * Twilio posts {@code From} as the sender of the inbound message, which is both the conversation the agent is
     * talking in and the address the reply goes back to.
     */
    @Test
    void testRequestPathsPinTwiliosCapitalisedWebhookFieldNames() {
        AgentRequestDefinition agentRequestDefinition = getAgentRequestDefinition();

        assertEquals(
            "From", agentRequestDefinition.getConversationIdPath(),
            "Twilio posts the inbound sender as the capitalised form field 'From'; the trigger declares no output "
                + "schema, so nothing but this test stands between a mistyped path and a silently empty conversation");
        assertEquals(
            "Body", agentRequestDefinition.getMessagePath(),
            "Twilio posts the inbound message text as the capitalised form field 'Body'");
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
            "twilio binds no attachmentsPath, so the generator wires [] as it does today");

        assertTrue(
            TRIGGER_DEFINITION.getOutputDefinition()
                .flatMap(OutputDefinition::getOutputResponse)
                .isEmpty(),
            "the trigger returns Twilio's raw webhook body and declares no output schema, so there is no attachments "
                + "field it could have bound");
    }

    /**
     * The channel row's {@code number} is what the reply is sent AS, and a channel-row key is only accepted when the
     * paired trigger declares a property of that name. The property is added optional on purpose: it is new on a
     * trigger that already exists in saved workflows, and making it required would break them.
     */
    @Test
    void testTriggerDeclaresTheChannelRowNumberAsAnOptionalProperty() {
        Property numberProperty = getTriggerProperty("number");

        assertFalse(
            numberProperty.getRequired(),
            "the number property is additive on an existing trigger, so requiring it would invalidate saved workflows");
        assertEquals(Property.Type.STRING, numberProperty.getType());
    }

    @Test
    void testReplyAddressesTheInboundSenderAndCarriesTheAgentText() {
        AgentReplyDefinition agentReplyDefinition = getAgentReplyDefinition();

        assertEquals(
            Optional.of("To"), agentReplyDefinition.getConversationIdProperty(),
            "the reply is addressed to the recipient channel address, which the envelope fills from the inbound From");
        assertEquals("Body", agentReplyDefinition.getMessageProperty());
        assertTrue(
            agentReplyDefinition.getAttachmentsProperty()
                .isEmpty(),
            "sendWhatsAppMessage declares no attachments property, matching the unbound request half");
    }

    /**
     * The sender is taken from the channel row rather than from the inbound {@code To}, which is the approved behaviour
     * change this channel makes: a row that configures a number replies as that number, so two rows of the same type no
     * longer have to share one sender.
     */
    @Test
    void testReplySenderComesFromTheChannelRowNumber() {
        AgentReplyDefinition agentReplyDefinition = getAgentReplyDefinition();

        assertEquals(
            Map.of("number", "From"), agentReplyDefinition.getChannelParameters(),
            "the channel row's number fills the action's From, replacing the inbound To that is mapped today");
    }

    /**
     * {@code useTemplate} defaults to true, and {@code Body} is hidden behind {@code useTemplate == false}. Pinning it
     * false is the whole reason a free-text agent reply is expressible at all, so the display condition that makes it
     * load-bearing is asserted alongside the pinned value.
     */
    @Test
    void testReplyPinsUseTemplateFalseSoTheAgentTextIsSentAsFreeText() {
        AgentReplyDefinition agentReplyDefinition = getAgentReplyDefinition();

        assertEquals(Map.of("useTemplate", false), agentReplyDefinition.getFixedParameters());

        BaseValueProperty<?> useTemplateProperty = assertInstanceOf(
            BaseValueProperty.class, getReplyActionProperty("useTemplate"));

        assertEquals(
            Optional.of(true), useTemplateProperty.getDefaultValue(),
            "useTemplate defaults to true, so leaving it unpinned would send the agent's reply as a template lookup");

        Property bodyProperty = getReplyActionProperty("Body");

        assertEquals(
            Optional.of("useTemplate == false"), bodyProperty.getDisplayCondition(),
            "Body is reachable only when useTemplate is false, which is what the fixed parameter guarantees");
    }

    @Test
    void testChannelPairsTheTriggerAndTheReplyActionUnderTheStoredKey() {
        AgentChannelDefinition agentChannelDefinition = getAgentChannelDefinition();

        assertEquals("twilio", agentChannelDefinition.getName(), "the stored channel key must not drift");
        assertEquals(Optional.of("Twilio (WhatsApp)"), agentChannelDefinition.getTitle());
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
            "the trigger needs no pinned parameters: the number it listens on is per-row configuration, not a "
                + "constant of the channel");
    }

    private static AgentChannelDefinition getAgentChannelDefinition() {
        TwilioComponentHandler twilioComponentHandler = new TwilioComponentHandler();

        ComponentDefinition componentDefinition = twilioComponentHandler.getDefinition();

        List<AgentChannelDefinition> agentChannelDefinitions = componentDefinition.getAgentChannels();

        assertEquals(1, agentChannelDefinitions.size(), "the twilio component declares exactly one agent channel");

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

    private static Property getReplyActionProperty(String name) {
        return findProperty(REPLY_ACTION_DEFINITION.getProperties(), name);
    }

    private static Property getTriggerProperty(String name) {
        return findProperty(TRIGGER_DEFINITION.getProperties(), name);
    }

    private static Property findProperty(List<? extends Property> properties, String name) {
        return properties.stream()
            .filter(property -> name.equals(property.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("property '%s' must be declared".formatted(name)));
    }
}
