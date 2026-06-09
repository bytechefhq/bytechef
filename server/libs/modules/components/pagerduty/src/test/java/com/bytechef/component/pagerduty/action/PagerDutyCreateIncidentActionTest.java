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

package com.bytechef.component.pagerduty.action;

import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.ASSIGNMENTS;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.DETAILS;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.ESCALATION_POLICY;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.FROM;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.INCIDENT;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.INCIDENT_KEY;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.INCIDENT_TYPE;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.PRIORITY;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.SERVICE;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.TITLE;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.URGENCY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.pagerduty.util.PagerDutyUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Nikolina Spehar
 */
@ExtendWith(MockContextSetupExtension.class)
class PagerDutyCreateIncidentActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Body.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            FROM, "from", TITLE, "title", SERVICE, "service", PRIORITY, "priority", URGENCY, "urgency",
            DETAILS, "details", ASSIGNMENTS, List.of("assignee1", "assignee2"), INCIDENT_KEY, "incidentKey",
            INCIDENT_TYPE, "incidentType", ESCALATION_POLICY, "escalationPolicy"));
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);
    private final Map<String, Object> responseMap = Map.of();
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.header(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        try (MockedStatic<PagerDutyUtils> pagerDutyUtilsMockedStatic = mockStatic(PagerDutyUtils.class)) {
            pagerDutyUtilsMockedStatic
                .when(() -> PagerDutyUtils.getRequestBody(parametersArgumentCaptor.capture()))
                .thenReturn(Map.of());

            Map<String, Object> result = PagerDutyCreateIncidentAction.perform(
                mockedParameters, mockedParameters, mockedContext);

            assertEquals(responseMap, result);
            assertEquals(List.of("/incidents", FROM, "from"), stringArgumentCaptor.getAllValues());
            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
            assertNotNull(httpFunctionArgumentCaptor.getValue());

            ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
            Configuration configuration = configurationBuilder.build();

            assertEquals(ResponseType.JSON, configuration.getResponseType());

            Map<String, Object> expectedBody = Map.of(INCIDENT, Map.of());

            assertEquals(Body.of(expectedBody, Http.BodyContentType.JSON), bodyArgumentCaptor.getValue());
        }
    }
}
