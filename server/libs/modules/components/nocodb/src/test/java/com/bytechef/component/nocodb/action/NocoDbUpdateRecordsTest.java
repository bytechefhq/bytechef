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

package com.bytechef.component.nocodb.action;

import static com.bytechef.component.nocodb.constant.NocoDbConstants.TABLE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.nocodb.util.NocoDbUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
@ExtendWith(MockContextSetupExtension.class)
class NocoDbUpdateRecordsTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(TABLE_ID, "123"));
    private final Object mockedObject = mock(Object.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = forClass(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        Context mockedContext, Executor mockedExecutor, Http mockedHttp, Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        try (MockedStatic<NocoDbUtils> nocoDbUtilsMockedStatic = mockStatic(NocoDbUtils.class)) {
            List<Map<String, String>> records = List.of(Map.of("key", "value"));
            nocoDbUtilsMockedStatic.when(
                () -> NocoDbUtils.transformRecordsForInsertion(parametersArgumentCaptor.capture()))
                .thenReturn(records);

            when(mockedHttp.patch(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.body(bodyArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedResponse.getBody())
                .thenReturn(mockedObject);

            Object result = NocoDbUpdateRecords.perform(mockedParameters, mockedParameters, mockedContext);

            assertEquals(mockedObject, result);
            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
            assertNotNull(httpFunctionArgumentCaptor.getValue());

            ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
            Configuration configuration = configurationBuilder.build();

            assertEquals(ResponseType.JSON, configuration.getResponseType());
            assertEquals("/api/v2/tables/123/records", stringArgumentCaptor.getValue());
            assertEquals(Body.of(records, BodyContentType.JSON), bodyArgumentCaptor.getValue());
        }
    }
}
