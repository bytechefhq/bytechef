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

package com.bytechef.component.agile.crm.action;

import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.EMAIL;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.FIRST_NAME;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.PROPERTIES;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.TAGS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.agile.crm.util.AgileCrmUtils;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Nikolina Spehar
 */
class AgileCrmCreateContactActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(FIRST_NAME, "test", EMAIL, "test_email", TAGS, List.of("tag1", "tag2")));
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);
    private final Map<String, Object> responseMap = Map.of();

    @Test
    void testPerform() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        try (MockedStatic<AgileCrmUtils> agileCrmUtilsMockedStatic = mockStatic(AgileCrmUtils.class)) {
            agileCrmUtilsMockedStatic
                .when(() -> AgileCrmUtils.getPropertiesList(parametersArgumentCaptor.capture()))
                .thenReturn(List.of());

            Map<String, Object> result = AgileCrmCreateContactAction.perform(
                mockedParameters, mockedParameters, mockedContext);

            assertEquals(responseMap, result);

            Body body = bodyArgumentCaptor.getValue();

            Map<String, Object> expectedBody = Map.of(TAGS, List.of("tag1", "tag2"), PROPERTIES, List.of());

            assertEquals(expectedBody, body.getContent());
            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
        }
    }
}
