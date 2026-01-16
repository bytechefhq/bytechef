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

package com.bytechef.component.google.calendar.trigger;

import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CALENDAR_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.RESOURCE_ID;
import static com.bytechef.component.google.calendar.trigger.GoogleCalendarEventTrigger.webhookRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils.CustomEvent;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.bytechef.google.commons.GoogleUtils;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.Calendar.Channels;
import com.google.api.services.calendar.Calendar.Channels.Stop;
import com.google.api.services.calendar.Calendar.Events;
import com.google.api.services.calendar.Calendar.Events.List;
import com.google.api.services.calendar.Calendar.Events.Watch;
import com.google.api.services.calendar.model.Channel;
import com.google.api.services.calendar.model.Event;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleCalendarEventTriggerTest {

    private final ArgumentCaptor<Boolean> booleanArgumentCaptor = ArgumentCaptor.forClass(Boolean.class);
    private final ArgumentCaptor<Calendar> calendarArgumentCaptor = ArgumentCaptor.forClass(Calendar.class);
    private final ArgumentCaptor<Channel> channelArgumentCaptor = ArgumentCaptor.forClass(Channel.class);
    private final ArgumentCaptor<DateTime> dateTimeArgumentCaptor = ArgumentCaptor.forClass(DateTime.class);
    private final ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
    private final ArgumentCaptor<LocalDateTime> localDateTimeArgumentCaptor =
        ArgumentCaptor.forClass(LocalDateTime.class);
    private final Calendar mockedCalendar = mock(Calendar.class);
    private final Events mockedCalendarEvents = mock(Events.class);
    private final Channels mockedChannels = mock(Channels.class);
    private final CustomEvent mockedCustomEvent = mock(CustomEvent.class);
    private final HttpHeaders mockedHttpHeaders = mock(HttpHeaders.class);
    private final HttpParameters mockedHttpParameters = mock(HttpParameters.class);
    private final List mockedList = mock(List.class);
    private Parameters mockedParameters;
    private final Stop mockedStop = mock(Stop.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final Watch mockedWatch = mock(Watch.class);
    private final WebhookBody mockedWebhookBody = mock(WebhookBody.class);
    private final Parameters mockedWebhookEnableOutput = mock(Parameters.class);
    private final WebhookMethod mockedWebhookMethod = mock(WebhookMethod.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private static final String workflowExecutionId = "testWorkflowExecutionId";

    @Test
    void testWebhookEnable() throws IOException {
        String webhookUrl = "testWebhookUrl";
        UUID uuid = UUID.randomUUID();

        mockedParameters = MockParametersFactory.create(Map.of(CALENDAR_ID, "calendar_id"));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<UUID> uuidMockedStatic = mockStatic(UUID.class)) {

            uuidMockedStatic.when(UUID::randomUUID)
                .thenReturn(uuid);

            googleServicesMockedStatic
                .when(() -> GoogleServices.getCalendar(parametersArgumentCaptor.capture()))
                .thenReturn(mockedCalendar);
            when(mockedCalendar.events())
                .thenReturn(mockedCalendarEvents);
            when(mockedCalendarEvents.watch(stringArgumentCaptor.capture(), channelArgumentCaptor.capture()))
                .thenReturn(mockedWatch);
            when(mockedWatch.execute())
                .thenReturn(
                    new Channel()
                        .setId("1234")
                        .setResourceId("resourceId"));

            WebhookEnableOutput result = GoogleCalendarEventTrigger.webhookEnable(
                mockedParameters, mockedParameters, webhookUrl, workflowExecutionId, mockedTriggerContext);

            WebhookEnableOutput expectedWebhookEnableOutput = new WebhookEnableOutput(
                Map.of(ID, "1234", RESOURCE_ID, "resourceId"), null);

            assertEquals(expectedWebhookEnableOutput, result);

            Channel channel = new Channel()
                .setAddress(webhookUrl)
                .setId(String.valueOf(uuid))
                .setPayload(true)
                .setType("web_hook");

            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
            assertEquals(channel, channelArgumentCaptor.getValue());
            assertEquals("calendar_id", stringArgumentCaptor.getValue());
        }
    }

    @Test
    void testWebhookDisable() throws IOException {
        mockedParameters = MockParametersFactory.create(Map.of(ID, "123", RESOURCE_ID, "abc"));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getCalendar(parametersArgumentCaptor.capture()))
                .thenReturn(mockedCalendar);
            when(mockedCalendar.channels())
                .thenReturn(mockedChannels);
            when(mockedChannels.stop(channelArgumentCaptor.capture()))
                .thenReturn(mockedStop);

            GoogleCalendarEventTrigger.webhookDisable(
                mockedParameters, mockedParameters, mockedParameters, workflowExecutionId, mockedTriggerContext);

            Channel expectedChannel = new Channel()
                .setId("123")
                .setResourceId("abc");

            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
            assertEquals(expectedChannel, channelArgumentCaptor.getValue());
        }
    }

    @Test
    void testWebhookRequest() throws IOException {
        LocalDateTime localDateTime = LocalDateTime.of(2000, 1, 1, 1, 1, 1);
        String timezone = "Europe/Zagreb";

        Event event = new Event();
        java.util.List<Event> events = java.util.List.of(event);

        mockedParameters = MockParametersFactory.create(Map.of(CALENDAR_ID, "calendar_id"));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleCalendarUtils> googleCalendarUtilsMockedStatic = mockStatic(GoogleCalendarUtils.class);
            MockedStatic<GoogleUtils> googleUtilsMockedStatic = mockStatic(GoogleUtils.class);
            MockedStatic<LocalDateTime> localDateTimeMockedStatic = mockStatic(LocalDateTime.class)) {
            googleCalendarUtilsMockedStatic
                .when(() -> GoogleCalendarUtils.createCustomEvent(
                    eventArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(mockedCustomEvent);
            googleUtilsMockedStatic
                .when(() -> GoogleUtils.getCalendarTimezone(calendarArgumentCaptor.capture()))
                .thenReturn(timezone);
            googleServicesMockedStatic
                .when(() -> GoogleCalendarUtils.convertLocalDateTimeToDateInTimezone(
                    localDateTimeArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(java.sql.Timestamp.valueOf(localDateTime));
            googleServicesMockedStatic
                .when(() -> GoogleServices.getCalendar(parametersArgumentCaptor.capture()))
                .thenReturn(mockedCalendar);
            localDateTimeMockedStatic.when(() -> LocalDateTime.now(any(ZoneId.class)))
                .thenReturn(localDateTime);

            when(mockedTriggerContext.data(any()))
                .thenReturn(Optional.empty());
            when(mockedCalendar.events())
                .thenReturn(mockedCalendarEvents);
            when(mockedCalendarEvents.list(stringArgumentCaptor.capture()))
                .thenReturn(mockedList);
            when(mockedList.setOrderBy(stringArgumentCaptor.capture()))
                .thenReturn(mockedList);
            when(mockedList.setShowDeleted(booleanArgumentCaptor.capture()))
                .thenReturn(mockedList);
            when(mockedList.setUpdatedMin(dateTimeArgumentCaptor.capture()))
                .thenReturn(mockedList);
            when(mockedList.execute())
                .thenReturn(new com.google.api.services.calendar.model.Events().setItems(events));

            CustomEvent result = webhookRequest(
                mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
                mockedWebhookMethod, mockedWebhookEnableOutput, mockedTriggerContext);

            assertEquals(mockedCustomEvent, result);

            assertEquals(event, eventArgumentCaptor.getValue());
            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
            assertEquals(
                java.util.List.of("calendar_id", "updated", timezone, timezone), stringArgumentCaptor.getAllValues());
            assertEquals(true, booleanArgumentCaptor.getValue());
            assertEquals(new DateTime(java.sql.Timestamp.valueOf(localDateTime)), dateTimeArgumentCaptor.getValue());
            assertEquals(localDateTime, localDateTimeArgumentCaptor.getValue());
        }
    }
}
