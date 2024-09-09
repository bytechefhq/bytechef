/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.google.calendar.trigger;

import static com.bytechef.component.definition.ComponentDSL.ModifiableTriggerDefinition;
import static com.bytechef.component.definition.ComponentDSL.outputSchema;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.ComponentDSL.trigger;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CALENDAR_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.RESOURCE_ID;

import com.bytechef.component.definition.OptionsDataSource.TriggerOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Channel;
import com.google.api.services.calendar.model.Event;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Monika Domiter
 */
public class GoogleCalendarEventTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newOrUpdatedEvent")
        .title("New or Updated Event")
        .description("Triggers when an event is added or updated")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties(
            string(CALENDAR_ID)
                .label("Calendar identifier")
                .options(
                    (TriggerOptionsFunction<String>) (
                        inputParameters, connectionParameters, arrayIndex, searchText, context) -> GoogleCalendarUtils
                            .getCalendarIdOptions(inputParameters, connectionParameters, null, null, context))
                .required(true))
        .output(outputSchema(EVENT_PROPERTY))
        .webhookEnable(GoogleCalendarEventTrigger::webhookEnable)
        .webhookDisable(GoogleCalendarEventTrigger::webhookDisable)
        .webhookRequest(GoogleCalendarEventTrigger::webhookRequest);

    private GoogleCalendarEventTrigger() {
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext context) {

        Channel channelConfig = new Channel()
            .setAddress(webhookUrl)
            .setId(String.valueOf(UUID.randomUUID()))
            .setPayload(true)
            .setType("web_hook");

        Channel channel;
        Calendar calendar = GoogleServices.getCalendar(connectionParameters);
        String calendarId = inputParameters.getRequiredString(CALENDAR_ID);

        try {
            channel = calendar.events()
                .watch(calendarId, channelConfig)
                .execute();
        } catch (IOException e) {
            throw new ProviderException(e);
        }

        return new WebhookEnableOutput(Map.of(ID, channel.getId(), RESOURCE_ID, channel.getResourceId()), null);
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        Calendar calendar = GoogleServices.getCalendar(connectionParameters);

        Channel channel = new Channel()
            .setId(outputParameters.getRequiredString(ID))
            .setResourceId(outputParameters.getRequiredString(RESOURCE_ID));

        try {
            calendar.channels()
                .stop(channel)
                .execute();
        } catch (IOException e) {
            throw new ProviderException(e);
        }
    }

    protected static Event webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, WebhookEnableOutput output, TriggerContext context)
        throws IOException {

        String calendarId = inputParameters.getRequiredString(CALENDAR_ID);
        Calendar calendar = GoogleServices.getCalendar(connectionParameters);

        List<Event> events = calendar.events()
            .list(calendarId)
            .setOrderBy("updated")
            .execute()
            .getItems();

        return events.getLast();
    }
}
