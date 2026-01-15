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

package com.bytechef.component.heygen.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.heygen.constant.HeyGenConstants.ID;
import static com.bytechef.component.heygen.constant.HeyGenConstants.NAME;
import static com.bytechef.component.heygen.constant.HeyGenConstants.TEMPLATE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.TypeReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
class HeyGenUtilsTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Body.class);
    private final ArgumentCaptor<Object[]> queryArgumentCaptor = ArgumentCaptor.forClass(Object[].class);
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Object mockedObject = mock(Object.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Response mockedResponse = mock(Response.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final TriggerDefinition.WebhookBody mockedWebhookBody = mock(TriggerDefinition.WebhookBody.class);

    @Test
    void testGetFolderIdOptions() {

        Map<String, Object> data = new HashMap<>();
        data.put("folders", List.of(Map.of(ID, "1", NAME, "test")));
        data.put("token", null);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("data", data);

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseBody);

        List<Option<String>> result = HeyGenUtils.getFolderIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(List.of(option("test", "1")), result);

        Object[] query = queryArgumentCaptor.getValue();
        assertEquals(Arrays.asList("limit", 100, "token", null), Arrays.asList(query));
    }

    @Test
    void testGetLanguageOptions() {

        Map<String, Object> responseBody = Map.of("data", Map.of("languages", List.of("English")));

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseBody);

        List<Option<String>> result = HeyGenUtils.getLanguageOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(List.of(option("English", "English")), result);
    }

    @Test
    void testGetTemplateIdOptions() {

        Map<String, Object> responseBody =
            Map.of("data",
                Map.of("templates", List.of(Map.of(TEMPLATE_ID, "1", NAME, "test"))));

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseBody);

        List<Option<String>> result = HeyGenUtils.getTemplateIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(List.of(option("test", "1")), result);
    }

    @Test
    void testAddWebhook() {
        Map<String, Object> responseBody = Map.of("data", Map.of("endpoint_id", "1"));
        String webhookUrl = "testWebhookUrl";
        String eventType = "testEventType";

        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseBody);

        String result = HeyGenUtils.addWebhook(eventType, mockedTriggerContext, webhookUrl);

        assertEquals("1", result);

        Body body = bodyArgumentCaptor.getValue();
        assertEquals(Map.of("url", webhookUrl, "events", List.of(eventType)), body.getContent());
    }

    @Test
    void testGetContent() {
        Map<String, Object> content = Map.of("event_data", mockedObject);

        when(mockedWebhookBody.getContent(any(TypeReference.class)))
            .thenReturn(content);

        Object result = HeyGenUtils.getContent(mockedWebhookBody);

        assertEquals(mockedObject, result);
    }
}
