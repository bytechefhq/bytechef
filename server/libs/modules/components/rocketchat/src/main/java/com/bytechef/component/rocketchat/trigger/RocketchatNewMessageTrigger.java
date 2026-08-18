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

import static com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import static com.bytechef.component.definition.ComponentDsl.agentRequest;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.rocketchat.constant.RocketchatConstants.CHANNEL_ID;
import static com.bytechef.component.rocketchat.constant.RocketchatConstants.ID;
import static com.bytechef.component.rocketchat.constant.RocketchatConstants.NAME;
import static com.bytechef.component.rocketchat.constant.RocketchatConstants.TEXT;
import static com.bytechef.component.rocketchat.constant.RocketchatConstants.USERNAME;

import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;

/**
 * The request half of the {@code rocketchat} agent channel.
 * <p>
 * The webhook body is handed through untouched and the trigger declares no output schema, so the descriptor's paths
 * name the fields of Rocket.Chat's own outgoing-webhook payload and cannot be checked against anything at build time —
 * the documented gap of the agent channel design. {@code RocketchatNewMessageTriggerAgentChannelTest} pins them
 * instead.
 * <p>
 * No {@code attachments} path is bound: the trigger declares no output to bind one from, the outgoing-webhook payload
 * documents no such field, and an absent path is how a channel states that it carries none — leaving the generator to
 * wire the literal {@code []} it wires today.
 *
 * @author Marija Horvat
 */
public class RocketchatNewMessageTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newMessage")
        .title("New Message")
        .description(
            "Trigger off whenever a new message is posted to any public channel, private group or direct messages.")
        .help("", "https://docs.bytechef.io/reference/components/rocketchat_v1#new-message")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .output()
        .agentRequest(
            agentRequest()
                .conversationId(CHANNEL_ID)
                .message(TEXT))
        .webhookDisable(RocketchatNewMessageTrigger::webhookDisable)
        .webhookEnable(RocketchatNewMessageTrigger::webhookEnable)
        .webhookRequest(RocketchatNewMessageTrigger::webhookRequest);

    private RocketchatNewMessageTrigger() {
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext context) {

        Map<String, ?> body = context.http(http -> http.post("/integrations.create"))
            .body(
                Body.of(
                    "type", "webhook-outgoing",
                    USERNAME, "rocket.cat",
                    "channel", "all_public_channels, all_private_groups, all_direct_messages",
                    "event", "sendMessage",
                    "urls", List.of(webhookUrl),
                    "enabled", true,
                    NAME, "Message Sent Trigger",
                    "scriptEnabled", false))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get("integration") instanceof Map<?, ?> integration) {
            return new WebhookEnableOutput(Map.of(ID, (String) integration.get("_id")), null);
        }

        return null;
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        context.http(http -> http.post("/integrations.remove"))
            .body(
                Body.of(
                    "integrationId", outputParameters.getRequiredString(ID),
                    "type", "webhook-outgoing"))
            .execute();
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters output, TriggerContext context) {

        return body.getContent();
    }
}
