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

package com.bytechef.component.mailerlite.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.mailerlite.constant.MailerLiteConstants.DATA;
import static com.bytechef.component.mailerlite.constant.MailerLiteConstants.EVENTS;
import static com.bytechef.component.mailerlite.constant.MailerLiteConstants.GROUP;
import static com.bytechef.component.mailerlite.constant.MailerLiteConstants.ID;
import static com.bytechef.component.mailerlite.constant.MailerLiteConstants.TRIGGER_NAME;
import static com.bytechef.component.mailerlite.constant.MailerLiteConstants.URL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Nikolina Spehar
 */
class MailerLiteUtilsTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Body.class);
    private final Context mockedContext = mock(Context.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of());
    private final Response mockedResponse = mock(Response.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final TriggerDefinition.WebhookBody mockedWebhookBody = mock(TriggerDefinition.WebhookBody.class);
    private final Map<String, List<Map<String, Object>>> responseMap = Map.of(
        "data", List.of(
            Map.of("email", "test1@gmail.com",
                "id", "test_id1",
                "name", "group1"),
            Map.of("email", "test2@gmail.com",
                "id", "test_id2",
                "name", "group2")));

    @Test
    void testGetGroupIdOptions() {
        List<Option<String>> result = httpCall(GROUP);

        List<Option<String>> expected = List.of(
            option("group1", "test_id1"),
            option("group2", "test_id2"));

        assertEquals(expected, result);
    }

    @Test
    void testGetSubscriberIdOptions() {
        List<Option<String>> result = httpCall("subscribers");

        List<Option<String>> expected = List.of(
            option("test1@gmail.com", "test_id1"),
            option("test2@gmail.com", "test_id2"));

        assertEquals(expected, result);
    }

    private List<Option<String>> httpCall(String option) {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        List<Option<String>> result;

        if (option.equals(GROUP)) {
            result = MailerLiteUtils.getGroupIdOptions(
                mockedParameters, mockedParameters, Map.of(), "", mockedContext);
        } else {
            result = MailerLiteUtils.getSubscriberIdOptions(
                mockedParameters, mockedParameters, Map.of(), "", mockedContext);
        }

        return result;
    }

    @Test
    void testGetContent() {
        Map<String, String> content = Map.of();

        when(mockedWebhookBody.getContent(any(TypeReference.class)))
            .thenReturn(content);

        assertEquals(content, MailerLiteUtils.getContent(mockedWebhookBody));
    }

    @Test
    void testSubscribeWebhook() {
        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(DATA, Map.of(ID, "123")));

        String triggerTestName = "triggerTest";
        String testEvent = "testEvent";
        String testWebhookUrl = "testWebhookUrl";

        String id = MailerLiteUtils.subscribeWebhook(
            triggerTestName, testEvent, testWebhookUrl, mockedTriggerContext);

        assertEquals("123", id);

        Body body = bodyArgumentCaptor.getValue();

        Map<String, Object> expectedBody = Map.of(
            TRIGGER_NAME, triggerTestName,
            EVENTS, List.of(testEvent),
            URL, testWebhookUrl);

        assertEquals(expectedBody, body.getContent());
    }

    @Test
    void testUnsubscribeWebhook() {
        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);

        MailerLiteUtils.unsubscribeWebhook("123", mockedTriggerContext);

        verify(mockedTriggerContext, times(1)).http(any());
        verify(mockedExecutor, times(1)).execute();
    }
}
