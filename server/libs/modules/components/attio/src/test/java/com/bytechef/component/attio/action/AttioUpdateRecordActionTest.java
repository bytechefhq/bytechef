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

package com.bytechef.component.attio.action;

import static com.bytechef.component.attio.constant.AttioConstants.DATA;
import static com.bytechef.component.attio.constant.AttioConstants.RECORD_ID;
import static com.bytechef.component.attio.constant.AttioConstants.RECORD_TYPE;
import static com.bytechef.component.attio.constant.AttioConstants.VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.attio.util.AttioUtils;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Nikolina Spehar
 */
class AttioUpdateRecordActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Body.class);
    private final ArgumentCaptor<Map<String, Object>> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final Map<String, Object> responseMap = Map.of();
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(VALUE, Map.of(), RECORD_TYPE, "test", RECORD_ID, "testId"));

    @Test
    void perform() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        try (MockedStatic<AttioUtils> mockedAttioUtils = Mockito.mockStatic(AttioUtils.class)) {
            mockedAttioUtils
                .when(() -> AttioUtils.getRecordValues(mapArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(responseMap);

            Map<String, Object> result = AttioUpdateRecordAction.perform(
                mockedParameters, mockedParameters, mockedContext);

            assertEquals(responseMap, mapArgumentCaptor.getValue());
            assertEquals("test", stringArgumentCaptor.getValue());

            assertEquals(Map.of(), result);

            Body body = bodyArgumentCaptor.getValue();

            Map<String, Map<String, Object>> expectedBody = Map.of(DATA, Map.of("values", Map.of()));

            assertEquals(expectedBody, body.getContent());
        }

    }
}
