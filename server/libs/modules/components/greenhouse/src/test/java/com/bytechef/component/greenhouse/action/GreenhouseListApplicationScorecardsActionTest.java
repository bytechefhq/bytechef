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

package com.bytechef.component.greenhouse.action;

import static com.bytechef.component.greenhouse.constant.GreenhouseConstants.APPLICATION_ID;
import static com.bytechef.component.greenhouse.constant.GreenhouseConstants.STATUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockContextSetupExtension.class)
class GreenhouseListApplicationScorecardsActionTest {

    private final List<Map<String, Object>> scorecards = List.of(Map.of("id", 211231, "application_id", 23558552));
    private final ArgumentCaptor<String> pathArgumentCaptor = forClass(String.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<Map<String, List<String>>> queryParametersArgumentCaptor = forClass(Map.class);

    @Test
    void testPerform(ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {
        Map<String, Object> inputParameters = new HashMap<>();

        inputParameters.put(APPLICATION_ID, "23558552");
        inputParameters.put(STATUS, "complete");

        Parameters mockedParameters = MockParametersFactory.create(inputParameters);

        mockHttp(mockedResponse, mockedExecutor, mockedHttp);

        List<Map<String, Object>> result = GreenhouseListApplicationScorecardsAction.perform(
            mockedParameters, null, mockedContext);

        assertEquals(scorecards, result);
        assertEquals("/scorecards", pathArgumentCaptor.getValue());
        assertEquals(
            Map.of("application_ids", List.of("23558552"), STATUS, List.of("complete"), "per_page", List.of("500")),
            queryParametersArgumentCaptor.getValue());
    }

    @Test
    void testPerformWithoutStatus(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        Parameters mockedParameters = MockParametersFactory.create(Map.of(APPLICATION_ID, "23558552"));

        mockHttp(mockedResponse, mockedExecutor, mockedHttp);

        List<Map<String, Object>> result = GreenhouseListApplicationScorecardsAction.perform(
            mockedParameters, null, mockedContext);

        assertEquals(scorecards, result);
        assertEquals(
            Map.of("application_ids", List.of("23558552"), "per_page", List.of("500")),
            queryParametersArgumentCaptor.getValue());
    }

    private void mockHttp(Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {
        when(mockedHttp.get(pathArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryParametersArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(scorecards);
    }
}
