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
import static com.bytechef.component.definition.ComponentDsl.agentChannelRequest;
import static com.bytechef.component.definition.ComponentDsl.agentRequest;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.slack.constant.SlackConstants.BOT_ID;
import static com.bytechef.component.slack.constant.SlackConstants.BOT_MESSAGE;
import static com.bytechef.component.slack.constant.SlackConstants.CHANNEL;
import static com.bytechef.component.slack.constant.SlackConstants.EVENT;
import static com.bytechef.component.slack.constant.SlackConstants.SUBTYPE;
import static com.bytechef.component.slack.constant.SlackConstants.TEXT;
import static com.bytechef.component.slack.constant.SlackConstants.TYPE;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TypeReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Brings a user's Slack message into a workflow. Unlike {@link SlackAnyEventTrigger}, whose output is the raw Slack
 * event, this trigger emits the agent channel request contract as its own output, so its request descriptor is the
 * identity one. That descriptor is metadata only the agent workflow generator reads: the trigger is registered like any
 * other and is equally usable in an ordinary hand-built workflow.
 * <p>
 * Slack redelivers a bot's own reply as another workspace event, which is why an event carrying {@code bot_id} or whose
 * {@code subtype} is {@code bot_message} returns an EMPTY collection rather than {@code null}: a non-batch collection
 * output is iterated one job per element, so zero elements create zero jobs, while {@code null} takes a different
 * branch that creates one job with empty inputs. Declining is therefore behaviour, not naming, which is why this cannot
 * be a descriptor on {@code anyEvent}.
 * <p>
 * An event subscription delivers every event type the app subscribed to, so the same empty collection is returned for
 * an event whose {@code type} is not {@code message} and for a message event carrying no text. The text check is the
 * robust half: it also covers the {@code message_changed} subtype, whose text lives under a nested {@code message}
 * object, and any future subtype that carries no prompt. A message trigger with no message has nothing to hand on. This
 * is a deliberate departure from {@link SlackAnyEventTrigger}, which fires on every event by design.
 *
 * @author Ivica Cardic
 */
public class SlackNewMessageTrigger {

    private static final String MESSAGE_EVENT_TYPE = "message";

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newMessage")
        .title("New Message")
        .description("Triggers when a user posts a message to a subscribed Slack channel. Events that are not " +
            "messages and messages carrying no text are ignored, as are messages the bot itself posted, so an " +
            "automation never responds to its own reply.")
        .type(TriggerType.STATIC_WEBHOOK)
        .output(outputSchema(agentChannelRequest()))
        .agentRequest(agentRequest().attachments(ATTACHMENTS))
        .webhookRequest(SlackNewMessageTrigger::webhookRequest)
        .webhookValidateOnEnable(SlackAnyEventTrigger::webhookValidateOnEnable)
        .help("", "https://docs.bytechef.io/reference/components/slack_v1#new-message");

    private SlackNewMessageTrigger() {
    }

    protected static List<Map<String, Object>> webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters output, TriggerContext context) {

        Map<String, Object> content = body.getContent(new TypeReference<>() {});

        if (!(content.get(EVENT) instanceof Map<?, ?> event)) {
            return List.of();
        }

        if (event.get(BOT_ID) != null || Objects.equals(event.get(SUBTYPE), BOT_MESSAGE)) {
            return List.of();
        }

        if (!Objects.equals(event.get(TYPE), MESSAGE_EVENT_TYPE)) {
            return List.of();
        }

        if (!(event.get(TEXT) instanceof String text) || text.isBlank()) {
            return List.of();
        }

        Map<String, Object> request = new HashMap<>();

        request.put(CONVERSATION_ID, event.get(CHANNEL));
        request.put(MESSAGE, text);
        request.put(ATTACHMENTS, List.of());

        return List.of(request);
    }
}
