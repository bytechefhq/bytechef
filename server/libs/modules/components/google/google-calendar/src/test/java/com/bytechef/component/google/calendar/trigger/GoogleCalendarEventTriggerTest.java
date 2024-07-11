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

import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CALENDAR_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.RESOURCE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.google.commons.GoogleServices;
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
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Domiter
 */
class GoogleCalendarEventTriggerTest {

    private final ArgumentCaptor<String> calendarIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<Channel> channelArgumentCaptor = ArgumentCaptor.forClass(Channel.class);
    private final Calendar mockedCalendar = mock(Calendar.class);
    private final Channel mockedChannel = mock(Channel.class);
    private final Channels mockedChannels = mock(Channels.class);
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final DynamicWebhookEnableOutput mockedDynamicWebhookEnableOutput = mock(DynamicWebhookEnableOutput.class);
    private final Events mockedEvents = mock(Events.class);
    private final com.google.api.services.calendar.model.Events mockedEvents2 = mock(
        com.google.api.services.calendar.model.Events.class);
    protected MockedStatic<GoogleServices> mockedGoogleServices;
    private final HttpHeaders mockedHttpHeaders = mock(HttpHeaders.class);
    private final HttpParameters mockedHttpParameters = mock(HttpParameters.class);
    private final List mockedList = mock(List.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Stop mockedStop = mock(Stop.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final Watch mockedWatch = mock(Watch.class);
    private final WebhookBody mockedWebhookBody = mock(WebhookBody.class);
    private final WebhookMethod mockedWebhookMethod = mock(WebhookMethod.class);
    private final ArgumentCaptor<String> orderByArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private static final String workflowExecutionId = "testWorkflowExecutionId";

    @BeforeEach
    public void beforeEach() {
        mockedGoogleServices = mockStatic(GoogleServices.class);

        mockedGoogleServices.when(() -> GoogleServices.getCalendar(mockedParameters))
            .thenReturn(mockedCalendar);
    }

    @AfterEach
    public void afterEach() {
        mockedGoogleServices.close();
    }

    @Test
    void testDynamicWebhookEnable() throws IOException {
        String webhookUrl = "testWebhookUrl";

        when(mockedParameters.getRequiredString(CALENDAR_ID))
            .thenReturn("calendar_id");

        when(mockedCalendar.events())
            .thenReturn(mockedEvents);
        when(mockedEvents.watch(calendarIdArgumentCaptor.capture(), channelArgumentCaptor.capture()))
            .thenReturn(mockedWatch);
        when(mockedWatch.execute())
            .thenReturn(mockedChannel);
        when(mockedChannel.getId())
            .thenReturn("1234");
        when(mockedChannel.getResourceId())
            .thenReturn("resourceId");

        DynamicWebhookEnableOutput dynamicWebhookEnableOutput = GoogleCalendarEventTrigger.dynamicWebhookEnable(
            mockedParameters, mockedParameters, webhookUrl, workflowExecutionId, mockedContext);

        Map<String, ?> parameters = dynamicWebhookEnableOutput.parameters();
        LocalDateTime webhookExpirationDate = dynamicWebhookEnableOutput.webhookExpirationDate();

        Map<String, String> expectedParameters = Map.of(ID, "1234", RESOURCE_ID, "resourceId");

        assertEquals(expectedParameters, parameters);
        assertNull(webhookExpirationDate);

        Channel channelArgumentCaptorValue = channelArgumentCaptor.getValue();

        assertEquals(channelArgumentCaptorValue.getAddress(), webhookUrl);
        assertEquals(true, channelArgumentCaptorValue.getPayload());
        assertEquals("web_hook", channelArgumentCaptorValue.getType());

        assertEquals("calendar_id", calendarIdArgumentCaptor.getValue());
    }

    @Test
    void testDynamicWebhookDisable() throws IOException {
        when(mockedParameters.getRequiredString(ID))
            .thenReturn("123");
        when(mockedParameters.getRequiredString(RESOURCE_ID))
            .thenReturn("abc");

        when(mockedCalendar.channels())
            .thenReturn(mockedChannels);
        when(mockedChannels.stop(channelArgumentCaptor.capture()))
            .thenReturn(mockedStop);

        GoogleCalendarEventTrigger.dynamicWebhookDisable(
            mockedParameters, mockedParameters, mockedParameters, workflowExecutionId, mockedContext);

        Channel channelArgumentCaptorValue = channelArgumentCaptor.getValue();

        assertEquals("123", channelArgumentCaptorValue.getId());
        assertEquals("abc", channelArgumentCaptorValue.getResourceId());
    }

    @Test
    void testDynamicWebhookRequest() throws IOException {
        Event event = new Event();
        java.util.List<Event> events = java.util.List.of(event);

        when(mockedParameters.getRequiredString(CALENDAR_ID))
            .thenReturn("calendar_id");

        when(mockedCalendar.events())
            .thenReturn(mockedEvents);
        when(mockedEvents.list(calendarIdArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.setOrderBy(orderByArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.execute())
            .thenReturn(mockedEvents2);
        when(mockedEvents2.getItems())
            .thenReturn(events);

        Event result = GoogleCalendarEventTrigger.dynamicWebhookRequest(
            mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
            mockedWebhookMethod, mockedDynamicWebhookEnableOutput, mockedTriggerContext);

        assertEquals(event, result);

        assertEquals("calendar_id", calendarIdArgumentCaptor.getValue());
        assertEquals("updated", orderByArgumentCaptor.getValue());
    }
}
