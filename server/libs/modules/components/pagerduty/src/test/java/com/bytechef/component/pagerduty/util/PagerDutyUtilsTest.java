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

package com.bytechef.component.pagerduty.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.ASSIGNEE;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.ASSIGNMENTS;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.BODY;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.DETAILS;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.ESCALATION_POLICY;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.ID;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.INCIDENT_KEY;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.INCIDENT_TYPE;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.NAME;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.PRIORITY;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.SERVICE;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.TITLE;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.TYPE;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.URGENCY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class PagerDutyUtilsTest {

    private static final Context mockedContext = mock(Context.class);
    private static final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private static final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            TITLE, "title", SERVICE, "service", PRIORITY, "priority", URGENCY, "urgency",
            DETAILS, "details", ASSIGNMENTS, List.of("assignee1", "assignee2"), INCIDENT_KEY, "incidentKey",
            INCIDENT_TYPE, "incidentType", ESCALATION_POLICY, "escalationPolicy"));
    private static final Http.Response mockedResponse = mock(Http.Response.class);

    @BeforeEach
    void beforeEach() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
    }

    @Test
    void testGetEscalationPolicyIdOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("escalation_policies", List.of(
                Map.of(NAME, "policy1", ID, "1"),
                Map.of(NAME, "policy2", ID, "2"))));

        List<Option<String>> options = PagerDutyUtils.getEscalationPolicyIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        List<Option<String>> expectedOptions = List.of(
            option("policy1", "1"),
            option("policy2", "2"));

        assertEquals(expectedOptions, options);
    }

    @Test
    void testGetIncidentIdOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("incidents", List.of(
                Map.of(TITLE, "incident1", ID, "1"),
                Map.of(TITLE, "incident2", ID, "2"))));

        List<Option<String>> options = PagerDutyUtils.getIncidentIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        List<Option<String>> expectedOptions = List.of(
            option("incident1", "1"),
            option("incident2", "2"));

        assertEquals(expectedOptions, options);
    }

    @Test
    void testGetIncidentTypeOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("incident_types", List.of(
                Map.of("display_name", "displayName1", NAME, "incidentType1"),
                Map.of("display_name", "displayName2", NAME, "incidentType2"))));

        List<Option<String>> options = PagerDutyUtils.getIncidentTypeOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        List<Option<String>> expectedOptions = List.of(
            option("displayName1", "incidentType1"),
            option("displayName2", "incidentType2"));

        assertEquals(expectedOptions, options);
    }

    @Test
    void testGetPriorityOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("priorities", List.of(
                Map.of(NAME, "priority1", ID, "1"),
                Map.of(NAME, "priority2", ID, "2"))));

        List<Option<String>> options = PagerDutyUtils.getPriorityOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        List<Option<String>> expectedOptions = List.of(
            option("priority1", "1"),
            option("priority2", "2"));

        assertEquals(expectedOptions, options);
    }

    @Test
    void testGetRequestBody() {
        Map<String, Object> body = PagerDutyUtils.getRequestBody(mockedParameters);

        Map<String, Object> expectedBody = getExpectedBody();

        assertEquals(expectedBody, body);
    }

    @Test
    void testGetServiceIdOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("services", List.of(
                Map.of(NAME, "service1", ID, "1"),
                Map.of(NAME, "service2", ID, "2"))));

        List<Option<String>> options = PagerDutyUtils.getServiceIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        List<Option<String>> expectedOptions = List.of(
            option("service1", "1"),
            option("service2", "2"));

        assertEquals(expectedOptions, options);
    }

    @Test
    void testGetUserIdOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("users", List.of(
                Map.of(NAME, "user1", ID, "1"),
                Map.of(NAME, "user2", ID, "2"))));

        List<Option<String>> options = PagerDutyUtils.getUserIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        List<Option<String>> expectedOptions = List.of(
            option("user1", "1"),
            option("user2", "2"));

        assertEquals(expectedOptions, options);
    }

    private Map<String, Object> getExpectedBody() {
        Map<String, Object> expectedBodyContent = new HashMap<>();

        expectedBodyContent.put(TYPE, "incident");
        expectedBodyContent.put(TITLE, "title");
        expectedBodyContent.put(BODY, Map.of(TYPE, "incident_body", DETAILS, "details"));
        expectedBodyContent.put(ESCALATION_POLICY, Map.of(ID, "escalationPolicy", TYPE, "escalation_policy_reference"));
        expectedBodyContent.put(PRIORITY, Map.of(ID, "priority", TYPE, "priority_reference"));
        expectedBodyContent.put(SERVICE, Map.of(ID, "service", TYPE, "service_reference"));
        expectedBodyContent.put(INCIDENT_KEY, "incidentKey");
        expectedBodyContent.put(URGENCY, "urgency");
        expectedBodyContent.put(INCIDENT_TYPE, Map.of("name", "incidentType"));
        expectedBodyContent.put(
            ASSIGNMENTS,
            List.of(
                Map.of(ASSIGNEE, Map.of(ID, "assignee1", TYPE, "user_reference")),
                Map.of(ASSIGNEE, Map.of(ID, "assignee2", TYPE, "user_reference"))));

        return expectedBodyContent;
    }
}
