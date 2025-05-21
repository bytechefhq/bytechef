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

import static com.bytechef.component.attio.constant.AttioConstants.COMPANIES;
import static com.bytechef.component.attio.constant.AttioConstants.DATA;
import static com.bytechef.component.attio.constant.AttioConstants.DEALS;
import static com.bytechef.component.attio.constant.AttioConstants.ID;
import static com.bytechef.component.attio.constant.AttioConstants.PEOPLE;
import static com.bytechef.component.attio.constant.AttioConstants.USERS;
import static com.bytechef.component.attio.constant.AttioConstants.WORKSPACES;
import static com.bytechef.component.definition.ComponentDsl.option;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Nikolina Spehar
 */
class AttioUtilsTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Body.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of());
    private final Response mockedResponse = mock(Response.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final WebhookBody mockedWebhookBody = mock(WebhookBody.class);

    @Test
    void testGetCompanyIdOptions() throws Exception {
        Map<String, List<Object>> body = Map.of("data", List.of(
            Map.of("id", Map.of("option_id", "test_id"), "title", "test_title")));

        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(body);

        List<? extends Option<String>> options = AttioUtils.getCompanyIdOptions("attribute")
            .apply(mockedParameters, mockedParameters, Map.of(), "", mockedActionContext);

        List<Option<String>> expected = List.of(option("test_title", "test_id"));

        assertEquals(expected, options);
    }

    @Test
    void testGetContent() {
        Map<String, Object> content = Map.of("events", Map.of());

        when(mockedWebhookBody.getContent(any(TypeReference.class)))
            .thenReturn(content);

        assertEquals(Map.of(), AttioUtils.getContent(mockedWebhookBody));
    }

    @Test
    void testGetDealStageIdOptions() {
        Map<String, List<Object>> mockedBody = Map.of(
            "data", List.of(Map.of("id", Map.of("status_id", "test_id"), "title", "test_title")));

        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedBody);

        List<Option<String>> result = AttioUtils.getDealStageIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedActionContext);

        List<Option<String>> expected = List.of(option("test_title", "test_id"));

        assertEquals(expected, result);
    }

    @Test
    void testGetTargetActorIdOptions() {
        Map<String, List<Object>> mockedBody = Map.of(
            DATA, List.of(
                Map.of(
                    "id", Map.of("record_id", "record1"),
                    "values", Map.of("primary_email_address", List.of(Map.of("email_address", "email1")))),
                Map.of(
                    "id", Map.of("record_id", "record2"),
                    "values", Map.of("primary_email_address", List.of(Map.of("email_address", "email2"))))));

        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedBody);

        List<Option<String>> targetObjectOptions = AttioUtils.getTargetActorIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedActionContext);

        List<Option<String>> expectedOptions = List.of(option("email1", "record1"), option("email2", "record2"));

        assertEquals(expectedOptions, targetObjectOptions);
    }

    @Test
    void testGetTargetObjectOptions() {
        Map<String, List<Object>> mockedBody = Map.of(DATA, List.of(
            Map.of("singular_noun", "test1", "api_slug", "1"),
            Map.of("singular_noun", "test2", "api_slug", "2")));

        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedBody);

        List<Option<String>> targetObjectOptions = AttioUtils.getTargetObjectOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedActionContext);

        List<Option<String>> expectedOptions = List.of(
            option("test1", "1"),
            option("test2", "2"));

        assertEquals(expectedOptions, targetObjectOptions);
    }

    @Test
    void testGetTargetRecordIdOptions() throws Exception {
        Map<String, List<Object>> mockedBody = Map.of(
            "data", List.of(
                Map.of(
                    "id", Map.of("record_id", "test_id"),
                    "values", Map.of("name", List.of(Map.of("value", "test_value"))))));

        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedBody);

        List<? extends Option<String>> result = AttioUtils.getTargetRecordIdOptions("targetObject")
            .apply(mockedParameters, mockedParameters, Map.of(), "", mockedActionContext);

        List<Option<String>> expected = List.of(option("test_value", "test_id"));

        assertEquals(expected, result);
    }

    @Test
    void testGetWorkSpaceMemberIdOptions() {
        Map<String, List<Object>> mockedBody = Map.of(
            "data", List.of(
                Map.of("id", Map.of("workspace_member_id", "test_id"),
                    "first_name", "first", "last_name", "last")));

        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedBody);

        List<Option<String>> result = AttioUtils.getWorkSpaceMemberIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedActionContext);

        List<Option<String>> expected = List.of(option("first last", "test_id"));

        assertEquals(expected, result);
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
            .thenReturn(Map.of(DATA, Map.of(ID, Map.of("webhook_id", "123"))));

        String testEvent = "testEvent";
        String testWebhookUrl = "testWebhookUrl";

        String id = AttioUtils.subscribeWebhook(testEvent, mockedTriggerContext, testWebhookUrl);

        assertEquals("123", id);

        Body body = bodyArgumentCaptor.getValue();

        Map<String, Object> expectedBody = Map.of(
            DATA, Map.of(
                "target_url", testWebhookUrl,
                "subscriptions", List.of(Map.of("event_type", testEvent, "filter", Map.of("$and", List.of())))));

        assertEquals(expectedBody, body.getContent());
    }

    @Test
    void testUnsubscribeWebhook() {
        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);

        AttioUtils.unsubscribeWebhook(mockedTriggerContext, "123");

        verify(mockedTriggerContext, times(1)).http(any());
        verify(mockedExecutor, times(1)).execute();
    }

    @Test
    void testGetRecordValuesForPerson() {
        Map<String, Object> mockRecordMap = Map.of("first_name", "name", "last_name", "lastName",
            "email_address", "email",
            "description", "description",
            "company", "company",
            "job_title", "job_title",
            "associated_deals", List.of("associated_deal"),
            "associated_users", List.of("associated_user"));

        Map<String, Object> result = AttioUtils.getRecordValues(mockRecordMap, PEOPLE);

        Map<String, Object> expected = Map.of(
            "name", List.of(Map.of("first_name", "name", "last_name", "lastName", "full_name", "name lastName")),
            "email_addresses", List.of("email"),
            "description", List.of(Map.of("value", "description")),
            "company", List.of(Map.of("target_object", COMPANIES, "target_record_id", "company")),
            "job_title", List.of(Map.of("value", "job_title")),
            "associated_deals", List.of(Map.of("target_object", DEALS, "target_record_id", "associated_deal")),
            "associated_users", List.of(Map.of("target_object", USERS, "target_record_id", "associated_user")));

        assertEquals(expected, result);
    }

    @Test
    void testGetRecordValuesForCompany() {
        Map<String, Object> mockRecordMap = new HashMap<>();

        mockRecordMap.put("domains", "domain");
        mockRecordMap.put("name", "name");
        mockRecordMap.put("description", "description");
        mockRecordMap.put("facebook", "facebook");
        mockRecordMap.put("instagram", "instagram");
        mockRecordMap.put("linkedin", "linkedin");
        mockRecordMap.put("employee_range", "employee_range");
        mockRecordMap.put("associated_deals", List.of("associated_deal"));
        mockRecordMap.put("associated_workspaces", List.of("associated_workspace"));
        mockRecordMap.put("categories", List.of("category"));
        mockRecordMap.put("estimated_arr_usd", "estimated_arr_usd");
        mockRecordMap.put("foundation_date", LocalDate.of(2025, 5, 16));

        Map<String, Object> result = AttioUtils.getRecordValues(mockRecordMap, COMPANIES);

        Map<String, Object> expected = new HashMap<>();

        expected.put("domains", List.of(Map.of("domain", "domain")));
        expected.put("name", List.of(Map.of("value", "name")));
        expected.put("description", List.of(Map.of("value", "description")));
        expected.put("facebook", List.of(Map.of("value", "facebook")));
        expected.put("instagram", List.of(Map.of("value", "instagram")));
        expected.put("linkedin", List.of(Map.of("value", "linkedin")));
        expected.put("employee_range", List.of(Map.of("option", "employee_range")));
        expected.put("associated_deals",
            List.of(Map.of("target_object", DEALS, "target_record_id", "associated_deal")));
        expected.put("associated_workspaces",
            List.of(Map.of("target_object", WORKSPACES, "target_record_id", "associated_workspace")));
        expected.put("categories", List.of(Map.of("option", "category")));
        expected.put("estimated_arr_usd", List.of(Map.of("option", "estimated_arr_usd")));
        expected.put("foundation_date", List.of(Map.of("value", LocalDate.of(2025, 5, 16)
            .toString())));

        assertEquals(expected, result);
    }

    @Test
    void testGetRecordValuesForUser() {
        Map<String, Object> mockRecordMap = new HashMap<>();

        mockRecordMap.put("person", "person");
        mockRecordMap.put("email_address", "email_address");
        mockRecordMap.put("user_id", "user_id");
        mockRecordMap.put("workspace", List.of("workspace"));

        Map<String, Object> result = AttioUtils.getRecordValues(mockRecordMap, USERS);

        Map<String, Object> expected = new HashMap<>();

        expected.put("person", Map.of("target_object", PEOPLE, "target_record_id", "person"));
        expected.put("primary_email_address", "email_address");
        expected.put("user_id", "user_id");
        expected.put("workspace", List.of(Map.of("target_object", WORKSPACES, "target_record_id", "workspace")));

        assertEquals(expected, result);
    }

    @Test
    void testGetRecordValuesForDeal() {
        Map<String, Object> mockRecordMap = new HashMap<>();

        mockRecordMap.put("name", "name");
        mockRecordMap.put("stage", "stage");
        mockRecordMap.put("owner", "owner");
        mockRecordMap.put("associated_company", "associated_company");
        mockRecordMap.put("value", 32);
        mockRecordMap.put("associated_people", List.of("associated_people"));

        Map<String, Object> result = AttioUtils.getRecordValues(mockRecordMap, DEALS);

        Map<String, Object> expected = new HashMap<>();

        expected.put("name", "name");
        expected.put("stage", "stage");
        expected.put("value", List.of(Map.of("currency_value", 32)));
        expected.put("associated_company", List.of(Map.of("target_object", COMPANIES,
            "target_record_id", "associated_company")));
        expected.put("owner", List.of(Map.of("referenced_actor_id", "owner",
            "referenced_actor_type", "workspace-member")));
        expected.put("associated_people",
            List.of(Map.of("target_object", PEOPLE, "target_record_id", "associated_people")));

        assertEquals(expected, result);
    }

    @Test
    void testGetRecordValuesForWorkspace() {
        Map<String, Object> mockRecordMap = new HashMap<>();

        mockRecordMap.put("workspace_id", "workspace_id");
        mockRecordMap.put("name", "name");
        mockRecordMap.put("users", List.of("users"));
        mockRecordMap.put("company", "company");
        mockRecordMap.put("avatar_url", "avatar_url");

        Map<String, Object> result = AttioUtils.getRecordValues(mockRecordMap, WORKSPACES);

        Map<String, Object> expected = new HashMap<>();

        expected.put("name", "name");
        expected.put("workspace_id", "workspace_id");
        expected.put("avatar_url", "avatar_url");
        expected.put("company", Map.of("target_object", COMPANIES, "target_record_id", "company"));
        expected.put("users", List.of(Map.of("target_object", USERS, "target_record_id", "users")));

        assertEquals(expected, result);
    }
}
