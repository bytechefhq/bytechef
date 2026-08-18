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

package com.bytechef.component.chat.trigger;

import static com.bytechef.component.chat.constant.ChatConstants.ATTACHMENTS;
import static com.bytechef.component.chat.constant.ChatConstants.MODE;
import static com.bytechef.component.chat.constant.ChatConstants.MODE_EMBEDDED_CHAT;
import static com.bytechef.component.chat.constant.ChatConstants.MODE_HOSTED_CHAT;
import static com.bytechef.component.definition.ComponentDsl.agentChannelRequest;
import static com.bytechef.component.definition.ComponentDsl.agentRequest;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.trigger;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Ivica Cardic
 */
public class ChatNewRequestTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newChatRequest")
        .title("New Chat Request")
        .description("A new chat request comes from the chat interface.")
        .type(TriggerType.STATIC_WEBHOOK)
        .workflowSyncExecution(true)
        .properties(
            integer(MODE)
                .options(
                    option("Hosted Chat", MODE_HOSTED_CHAT, "Use ByteChef's hosted chat interface"),
                    option("Embedded Chat", MODE_EMBEDDED_CHAT,
                        "This option requires you to create your own chat interface"))
                .defaultValue(MODE_HOSTED_CHAT)
                .required(true))
        // The contract itself, from the one place it is spelled: this trigger's output IS the agent channel request,
        // so re-declaring the same three fields here would be a second spelling drifting from the first -- which it
        // already had, silently losing the property descriptions the contract carries.
        .output(outputSchema(agentChannelRequest()))
        .agentRequest(agentRequest().attachments(ATTACHMENTS))
        .webhookRequest(ChatNewRequestTrigger::getWebhookResult);

    protected static Map<String, ?> getWebhookResult(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters webhookEnableOutput, TriggerContext context) {

        if (body == null || body.getContent() == null) {
            throw new IllegalArgumentException("Invalid webhook request.");
        }

        Map<String, Object> content = checkMap((Map<?, ?>) body.getContent());

        if (!content.containsKey("attachments")) {
            content.put("attachments", List.of());
        }

        return content;
    }

    private static Map<String, Object> checkMap(Map<?, ?> map) {
        return map.entrySet()
            .stream()
            .collect(Collectors.toMap(entry -> (String) entry.getKey(), entry -> {
                if (entry.getValue() instanceof List<?> list) {
                    if (list.isEmpty()) {
                        return list;
                    }

                    if (list.getFirst() instanceof FileEntry) {
                        return list;
                    }

                    return list.getFirst();
                }

                return entry.getValue();
            }));
    }
}
