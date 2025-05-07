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

package com.bytechef.component.attio.util;

import static com.bytechef.component.attio.constant.AttioConstants.DATA;
import static com.bytechef.component.attio.constant.AttioConstants.ID;
import static com.bytechef.component.attio.constant.AttioConstants.RECORD_TYPE;
import static com.bytechef.component.attio.constant.AttioConstants.USERS;
import static com.bytechef.component.attio.constant.AttioConstants.USER_RECORD;
import static com.bytechef.component.definition.ComponentDsl.option;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
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
class AttioUtilsTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Body.class);
    private final Context mockedContext = mock(Context.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of());
    private final Response mockedResponse = mock(Response.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final TriggerDefinition.WebhookBody mockedWebhookBody = mock(TriggerDefinition.WebhookBody.class);

    @Test
    void getCompanyArrIdOptions() {
        getCompanyIdOptions();
    }

    @Test
    void getCompanyCategoriesIdOptions() {
        getCompanyIdOptions();
    }

    @Test
    void getCompanyEmployeeRangeIdOptions() {
        getCompanyIdOptions();
    }

    @Test
    void getCompanyIdOptions() {
        Map<String, List<Object>> mockedBody = Map.of("data", List.of(
            Map.of("id", Map.of("option_id", "test_id"), "title", "test_title")));

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedBody);

        List<Option<String>> result = AttioUtils.getCompanyArrIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        List<Option<String>> expected = List.of(option("test_title", "test_id"));

        assertEquals(expected, result);
    }

    @Test
    void getWorkSpaceMemberIdOptions() {
        Map<String, List<Object>> mockedBody = Map.of("data", List.of(
            Map.of("id", Map.of("workspace_member_id", "test_id"),
                "first_name", "first",
                "last_name", "last")));

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedBody);

        List<Option<String>> result = AttioUtils.getWorkSpaceMemberIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        List<Option<String>> expected = List.of(option("first last", "test_id"));

        assertEquals(expected, result);
    }

    @Test
    void getTargetRecordCompanyOptions() {
        getTargetRecordIdOptions();
    }

    @Test
    void getTargetRecordDealOptions() {
        getTargetRecordIdOptions();
    }

    @Test
    void getTargetRecordPersonOptions() {
        getTargetRecordIdOptions();
    }

    @Test
    void getTargetRecordWorkspaceOptions() {
        getTargetRecordIdOptions();
    }

    @Test
    void getRecordIdOptions() {
        getTargetRecordIdOptions();
    }

    @Test
    void getTargetRecordIdOptions() {
        Map<String, List<Object>> mockedBody = Map.of("data", List.of(
            Map.of("id", Map.of("record_id", "test_id"),
                "values", Map.of("name", List.of(Map.of("value", "test_value"))))));

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedBody);

        List<Option<String>> result = AttioUtils.getTargetRecordDealOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        List<Option<String>> expected = List.of(option("test_value", "test_id"));

        assertEquals(expected, result);
    }

    @Test
    void getDealStageIdOptions() {
        Map<String, List<Object>> mockedBody = Map.of("data", List.of(
            Map.of("id", Map.of("status_id", "test_id"), "title", "test_title")));

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedBody);

        List<Option<String>> result = AttioUtils.getDealStageIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        List<Option<String>> expected = List.of(option("test_title", "test_id"));

        assertEquals(expected, result);
    }

    @Test
    void getRecordAttributes() {
        Parameters mockedParameters = MockParametersFactory.create(Map.of(RECORD_TYPE, USERS));

        List<ModifiableValueProperty<?, ?>> expected = List.of(USER_RECORD);

        List<ModifiableValueProperty<?, ?>> result = AttioUtils.getRecordAttributes(
            mockedParameters, mockedParameters, Map.of(), mockedContext);

        assertEquals(expected, result);
    }

    @Test
    void getTargetObjectOptions() {
        Map<String, List<Object>> mockedBody = Map.of(DATA, List.of(
            Map.of("singular_noun", "test1", "api_slug", "1"),
            Map.of("singular_noun", "test2", "api_slug", "2")));

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedBody);

        List<Option<String>> targetObjectOptions = AttioUtils.getTargetObjectOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        List<Option<String>> expectedOptions = List.of(
            option("test1", "1"),
            option("test2", "2"));

        assertEquals(expectedOptions, targetObjectOptions);
    }

    @Test
    void getTargetActorIdOptions() {
        Map<String, List<Object>> mockedBody = Map.of(DATA, List.of(
            Map.of("id", Map.of("record_id", "record1"),
                "values", Map.of("primary_email_address", List.of(Map.of("email_address", "email1")))),
            Map.of("id", Map.of("record_id", "record2"),
                "values", Map.of("primary_email_address", List.of(Map.of("email_address", "email2"))))));

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedBody);

        List<Option<String>> targetObjectOptions = AttioUtils.getTargetActorIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        List<Option<String>> expectedOptions = List.of(
            option("email1", "record1"),
            option("email2", "record2"));

        assertEquals(expectedOptions, targetObjectOptions);
    }

    @Test
    void subscribeWebhook() {
        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(DATA, Map.of(ID, Map.of("webhook_id", "123"))));

        String testEvent = "testEvent";
        String testWebhookUrl = "testWebhookUrl";

        String id = AttioUtils.subscribeWebhook(
            testEvent, mockedTriggerContext, testWebhookUrl);

        assertEquals("123", id);

        Body body = bodyArgumentCaptor.getValue();

        Map<String, Object> expectedBody = Map.of(DATA, Map.of(
            "target_url", testWebhookUrl,
            "subscriptions", List.of(
                Map.of("event_type", testEvent,
                    "filter", Map.of("$and", List.of())))));

        assertEquals(expectedBody, body.getContent());
    }

    @Test
    void unsubscribeWebhook() {
        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);

        AttioUtils.unsubscribeWebhook(mockedTriggerContext, "123");

        verify(mockedTriggerContext, times(1)).http(any());
        verify(mockedExecutor, times(1)).execute();
    }

    @Test
    void getContent() {
        Map<String, Object> content = Map.of("events", Map.of());

        when(mockedWebhookBody.getContent(any(TypeReference.class)))
            .thenReturn(content);

        assertEquals(Map.of(), AttioUtils.getContent(mockedWebhookBody));
    }
}
