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
import static com.bytechef.component.chat.constant.ChatConstants.CONVERSATION_ID;
import static com.bytechef.component.chat.constant.ChatConstants.MESSAGE;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
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
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
public class ChatNewRequestTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newChatRequest")
        .title("New Chat Request")
        .description("A new chat request comes from the chat interface.")
        .type(TriggerType.STATIC_WEBHOOK)
        .workflowSyncExecution(true)
        .workflowSyncExecution(true)
        .properties(
            integer("mode")
                .options(
                    option("Hosted Chat", 1, "Use ByteChef's hosted chat interface"),
                    option("Embedded Chat", 2, "This option requires you to create your own chat interface"))
                .defaultValue(1)
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(CONVERSATION_ID)
                            .required(true),
                        string(MESSAGE),
                        array(ATTACHMENTS)
                            .items(fileEntry()))))
        .webhookRequest(ChatNewRequestTrigger::getWebhookResult);

    protected static Map<String, ?> getWebhookResult(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters webhookEnableOutput, TriggerContext context) {

        Assert.notNull(body.getContent(), "Body content is required.");

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
                    if (list.getFirst() instanceof FileEntry) {
                        return list;
                    } else {
                        return list.getFirst();
                    }
                } else {
                    return entry.getValue();
                }
            }));
    }
}
