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

import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.slack.constant.SlackConstants.CHALLENGE;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class SlackAnyEventTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("anyEvent")
        .title("Any Event")
        .description("Triggers when any user subscribed event happens.")
        .help("", "https://docs.bytechef.io/reference/components/slack_v1#any-event")
        .type(TriggerType.STATIC_WEBHOOK)
        .output()
        .webhookRequest(SlackAnyEventTrigger::webhookRequest)
        .webhookValidateOnEnable(SlackAnyEventTrigger::webhookValidateOnEnable)
        .help("", "https://docs.bytechef.io/reference/components/slack_v1#any-event");

    private SlackAnyEventTrigger() {
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters output, TriggerContext context) {

        Map<String, Object> content = body.getContent(new TypeReference<>() {});

        return content.get("event");
    }

    public static WebhookValidateResponse webhookValidateOnEnable(
        Parameters inputParameters, HttpHeaders headers, HttpParameters parameters, WebhookBody body,
        WebhookMethod method, TriggerContext context) {

        Map<String, Object> content = body.getContent(new TypeReference<>() {});

        return new WebhookValidateResponse(
            content.get(CHALLENGE), Map.of("Content-type", List.of("text/plain")), 200);
    }
}
