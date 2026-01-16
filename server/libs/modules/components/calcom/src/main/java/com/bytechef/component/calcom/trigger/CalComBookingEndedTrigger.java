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

package com.bytechef.component.calcom.trigger;

import static com.bytechef.component.calcom.constant.CalComConstants.PERSON_RECORD;
import static com.bytechef.component.calcom.constant.CalComConstants.WEBHOOK_ID;
import static com.bytechef.component.calcom.util.CalComUtils.getContent;
import static com.bytechef.component.calcom.util.CalComUtils.subscribeWebhook;
import static com.bytechef.component.calcom.util.CalComUtils.unsubscribeWebhook;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class CalComBookingEndedTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("bookingEnded")
        .title("Booking Ended")
        .description("Triggers when a booking ends.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .output(
            outputSchema(
                object()
                    .properties(
                        string("type")
                            .description("Type of the meeting that ended."),
                        string("title")
                            .description("Title of the meeting that ended."),
                        string("startTime")
                            .description("Start time of the booking."),
                        string("endTime")
                            .description("End time of the booking."),
                        array("attendees")
                            .description("Attendees of the booking.")
                            .items(
                                PERSON_RECORD),
                        object("organizer")
                            .description("Organizer of the booking.")
                            .properties(
                                PERSON_RECORD))))
        .webhookEnable(CalComBookingEndedTrigger::webhookEnable)
        .webhookDisable(CalComBookingEndedTrigger::webhookDisable)
        .webhookRequest(CalComBookingEndedTrigger::webhookRequest);

    private CalComBookingEndedTrigger() {
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext context) {

        return new WebhookEnableOutput(
            Map.of(WEBHOOK_ID, subscribeWebhook("MEETING_ENDED", context, webhookUrl)), null);
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        unsubscribeWebhook(context, outputParameters.getString(WEBHOOK_ID));
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters output, TriggerContext context) {

        return getContent(body);
    }
}
