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

package com.bytechef.component.slack.trigger;

import static com.bytechef.component.definition.AgentChannelDefinition.ATTACHMENTS;
import static com.bytechef.component.definition.AgentChannelDefinition.CONVERSATION_ID;
import static com.bytechef.component.definition.AgentChannelDefinition.MESSAGE;
import static com.bytechef.component.slack.constant.SlackConstants.BOT_ID;
import static com.bytechef.component.slack.constant.SlackConstants.BOT_MESSAGE;
import static com.bytechef.component.slack.constant.SlackConstants.CHALLENGE;
import static com.bytechef.component.slack.constant.SlackConstants.CHANNEL;
import static com.bytechef.component.slack.constant.SlackConstants.EVENT;
import static com.bytechef.component.slack.constant.SlackConstants.SUBTYPE;
import static com.bytechef.component.slack.constant.SlackConstants.TEXT;
import static com.bytechef.component.slack.constant.SlackConstants.TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.AgentRequestDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.definition.BaseProperty;
import com.bytechef.definition.BaseProperty.BaseObjectProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class SlackNewMessageTriggerTest {

    private final HttpHeaders mockedHttpHeaders = mock(HttpHeaders.class);
    private final HttpParameters mockedHttpParameters = mock(HttpParameters.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final WebhookBody mockedWebhookBody = mock(WebhookBody.class);
    private final Parameters mockedWebhookEnableOutput = mock(Parameters.class);
    private final WebhookMethod mockedWebhookMethod = mock(WebhookMethod.class);

    @Test
    void testWebhookRequestMapsUserMessageToTheContract() {
        mockEvent(Map.of(TYPE, "message", CHANNEL, "C0123456789", TEXT, "What is the deploy status?"));

        Object result = webhookRequest();

        assertEquals(
            List.of(
                Map.of(
                    CONVERSATION_ID, "C0123456789", MESSAGE, "What is the deploy status?", ATTACHMENTS, List.of())),
            result,
            "a user message must map onto the agent channel request contract");
    }

    /**
     * Slack redelivers the bot's own reply as a new event. Declining has to be an EMPTY collection: a non-batch
     * collection output is iterated one job per element, so zero elements create zero jobs, while returning null takes
     * a different branch that creates one job with empty inputs.
     */
    @Test
    void testWebhookRequestDeclinesEventCarryingBotId() {
        mockEvent(Map.of(TYPE, "message", CHANNEL, "C0123456789", TEXT, "I am the bot's own reply", BOT_ID, "B042"));

        Object result = webhookRequest();

        assertEquals(List.of(), result, "an event carrying bot_id must decline to fire, and must not decline by null");
    }

    @Test
    void testWebhookRequestDeclinesBotMessageSubtype() {
        mockEvent(
            Map.of(
                TYPE, "message", CHANNEL, "C0123456789", TEXT, "I am the bot's own reply", SUBTYPE, BOT_MESSAGE));

        Object result = webhookRequest();

        assertEquals(
            List.of(), result, "an event whose subtype is bot_message must decline to fire, and must not decline "
                + "by null");
    }

    /**
     * The workspace event subscription delivers every event type the app is subscribed to, not only messages. A
     * reaction, a channel creation or a member join carries no text at all, so firing on one would run the whole
     * workflow on an empty message — and, on an agent channel, spend a billed LLM turn and post the answer into the
     * channel.
     */
    @Test
    void testWebhookRequestDeclinesNonMessageEventType() {
        mockEvent(Map.of(TYPE, "reaction_added", CHANNEL, "C0123456789"));

        Object result = webhookRequest();

        assertEquals(
            List.of(), result, "an event whose type is not message must decline to fire, and must not decline by null");
    }

    @Test
    void testWebhookRequestDeclinesMessageEventWithoutText() {
        Map<String, Object> event = new HashMap<>();

        event.put(TYPE, "message");
        event.put(CHANNEL, "C0123456789");
        event.put(TEXT, null);

        mockEvent(event);

        Object result = webhookRequest();

        assertEquals(
            List.of(), result, "a message event carrying no text must decline to fire, and must not decline by null");
    }

    /**
     * The text check is what covers the {@code message_changed} subtype, whose text lives under a nested
     * {@code message} object rather than at the top level.
     */
    @Test
    void testWebhookRequestDeclinesMessageEventWithBlankText() {
        mockEvent(Map.of(TYPE, "message", CHANNEL, "C0123456789", TEXT, "   "));

        Object result = webhookRequest();

        assertEquals(
            List.of(), result,
            "a message event whose text is blank must decline to fire, and must not decline by null");
    }

    /**
     * Without the URL-verification answer Slack cannot enable the event subscription at all, so the channel would be
     * dead on arrival.
     */
    @Test
    void testWebhookValidateOnEnableAnswersTheChallenge() {
        when(mockedWebhookBody.getContent(any(TypeReference.class)))
            .thenReturn(Map.of(CHALLENGE, "123456789"));

        WebhookValidateResponse webhookValidateResponse = SlackNewMessageTrigger.TRIGGER_DEFINITION
            .getWebhookValidateOnEnable()
            .orElseThrow()
            .apply(
                mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody, mockedWebhookMethod,
                mockedTriggerContext);

        assertEquals(200, webhookValidateResponse.status());
        assertEquals(Map.of("Content-type", List.of("text/plain")), webhookValidateResponse.headers());
        assertEquals("123456789", webhookValidateResponse.body());
    }

    /**
     * Asserting the whole set at once, rather than path by path, so a field the output declares cannot stay silently
     * unbound: an absent attachments path means "this channel carries no attachments", which would make the generator
     * wire [] instead of the trigger's own value.
     */
    @Test
    void testEveryDeclaredContractFieldIsBoundByTheRequestDescriptor() {
        AgentRequestDefinition agentRequestDefinition = SlackNewMessageTrigger.TRIGGER_DEFINITION
            .getAgentRequestDefinition()
            .orElseThrow();

        Set<String> boundPaths = Stream.concat(
            Stream.of(agentRequestDefinition.getConversationIdPath(), agentRequestDefinition.getMessagePath()),
            agentRequestDefinition.getAttachmentsPath()
                .stream())
            .collect(Collectors.toSet());

        BaseObjectProperty<?> objectProperty = assertInstanceOf(
            BaseObjectProperty.class, SlackNewMessageTrigger.TRIGGER_DEFINITION.getOutputDefinition()
                .orElseThrow()
                .getOutputSchema(),
            "the trigger's output must be a declared object schema");

        Set<String> declaredPropertyNames = objectProperty.getProperties()
            .stream()
            .map(BaseProperty::getName)
            .collect(Collectors.toSet());

        assertEquals(
            Set.of(CONVERSATION_ID, MESSAGE, ATTACHMENTS), declaredPropertyNames,
            "the trigger's output must be the agent channel request contract itself");
        assertEquals(
            declaredPropertyNames, boundPaths,
            "the request descriptor must bind exactly the fields the trigger's output declares");
    }

    private void mockEvent(Map<String, ?> event) {
        when(mockedWebhookBody.getContent(any(TypeReference.class)))
            .thenReturn(Map.of(EVENT, event));
    }

    private Object webhookRequest() {
        return SlackNewMessageTrigger.webhookRequest(
            mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
            mockedWebhookMethod, mockedWebhookEnableOutput, mockedTriggerContext);
    }
}
