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

package com.bytechef.component.viber.trigger;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.viber.constant.ViberConstants.CONVERSATION_STARTED;
import static com.bytechef.component.viber.constant.ViberConstants.EVENTS;
import static com.bytechef.component.viber.constant.ViberConstants.EVENT_OUTPUT_PROPERTIES;
import static com.bytechef.component.viber.constant.ViberConstants.MESSAGE;
import static com.bytechef.component.viber.constant.ViberConstants.SUBSCRIBED;
import static com.bytechef.component.viber.constant.ViberConstants.UNSUBSCRIBED;
import static com.bytechef.component.viber.constant.ViberConstants.WELCOME_MESSAGE;
import static com.bytechef.component.viber.constant.ViberConstants.WELCOME_SENDER_NAME;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.viber.util.ViberEventUtils;
import com.bytechef.component.viber.util.ViberWebhookUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class ViberNewEventTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newEvent")
        .title("New Event")
        .description("Triggers when one of the selected Viber events happens.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties(
            array(EVENTS)
                .label("Events")
                .description("The Viber events that trigger the workflow.")
                .items(
                    string().options(
                        option("New Message", MESSAGE),
                        option("Conversation Started", CONVERSATION_STARTED),
                        option("Subscribed", SUBSCRIBED),
                        option("Unsubscribed", UNSUBSCRIBED)))
                .defaultValue(MESSAGE)
                .required(true),
            string(WELCOME_MESSAGE)
                .label("Welcome Message")
                .description(
                    "Text sent automatically when a user opens the chat with your bot. It is not sent when testing " +
                        "in the editor.")
                .controlType(ControlType.TEXT_AREA)
                .maxLength(7000)
                .displayCondition("contains(events, '" + CONVERSATION_STARTED + "')")
                .required(false),
            string(WELCOME_SENDER_NAME)
                .label("Welcome Sender Name")
                .description("Name shown as the sender of the welcome message.")
                .maxLength(28)
                .displayCondition("contains(events, '" + CONVERSATION_STARTED + "')")
                .required(false))
        .output(outputSchema(object().properties(EVENT_OUTPUT_PROPERTIES)))
        .webhookValidateWithConnection(ViberWebhookUtils::validate)
        .webhookValidateOnEnable(ViberWebhookUtils::validateOnEnable)
        .webhookEnable(ViberNewEventTrigger::webhookEnable)
        .webhookDisable(ViberNewEventTrigger::webhookDisable)
        .webhookRequest(ViberNewEventTrigger::webhookRequest)
        .help("", "https://docs.bytechef.io/reference/components/viber_v1#new-event");

    private ViberNewEventTrigger() {
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext context) {

        List<String> selectedEvents = inputParameters.getList(EVENTS, String.class, List.of());

        List<String> eventTypes = selectedEvents.contains(CONVERSATION_STARTED)
            ? List.of(CONVERSATION_STARTED) : List.of();

        return ViberWebhookUtils.enableWebhook(webhookUrl, eventTypes, context);
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        ViberWebhookUtils.disableWebhook(outputParameters.getString("webhookUrl"), context);
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters output, TriggerContext context) {

        Map<String, Object> event = body.getContent(new TypeReference<Map<String, Object>>() {});

        return ViberEventUtils.toOutput(
            event, new HashSet<>(inputParameters.getList(EVENTS, String.class, List.of())), context);
    }
}
