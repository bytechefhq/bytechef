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
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.ESCALATION_POLICY;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.FROM;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.INCIDENT;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.INCIDENT_ID;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.INCIDENT_TYPE;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.PRIORITY;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.RESOLUTION;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.STATUS;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.TITLE;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.URGENCY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.pagerduty.util.PagerDutyUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Nikolina Spehar
 */
class PagerDutyUpdateIncidentActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Body.class);
    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            FROM, "from", INCIDENT_ID, "incidentId", INCIDENT_TYPE, "incidentType", STATUS, "status",
            ASSIGNMENTS, List.of("assignee1", "assignee2"), RESOLUTION, "resolution", TITLE, "title",
            PRIORITY, "priority", URGENCY, "urgency", ESCALATION_POLICY, "escalationPolicy"));
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);
    private final Map<String, Object> responseMap = Map.of();
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPerform() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.header(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        try (MockedStatic<PagerDutyUtils> pagerDutyUtilsMockedStatic = mockStatic(PagerDutyUtils.class)) {
            pagerDutyUtilsMockedStatic
                .when(() -> PagerDutyUtils.getRequestBody(parametersArgumentCaptor.capture()))
                .thenReturn(Map.of());

            Map<String, Object> result = PagerDutyUpdateIncidentAction.perform(
                mockedParameters, mockedParameters, mockedContext);

            assertEquals(responseMap, result);

            Body body = bodyArgumentCaptor.getValue();

            assertEquals(Map.of(INCIDENT, Map.of()), body.getContent());
            assertEquals(List.of(FROM, "from"), stringArgumentCaptor.getAllValues());
            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
        }
    }
}
