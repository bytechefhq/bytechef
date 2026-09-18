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

package com.bytechef.component.neon.action;

import static com.bytechef.component.neon.constant.NeonConstants.ROW_DATA;
import static com.bytechef.component.neon.constant.NeonConstants.TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockContextSetupExtension.class)
class NeonCreateRowActionTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = forClass(Http.Body.class);
    private final Map<String, Object> rowData = Map.of("title", "Hello world");
    private final Object mockedObject = mock(Object.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(TABLE, "posts", ROW_DATA, rowData));

    @Test
    void testPerform(
        Context mockedContext, Http.Response mockedResponse, Http.Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.header(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(List.of(mockedObject));

        Object result = NeonCreateRowAction.perform(mockedParameters, null, mockedContext);

        assertEquals(mockedObject, result);
        assertEquals("/posts", stringArgumentCaptor.getAllValues()
            .get(0));
        assertEquals(
            List.of("Prefer", "return=representation"),
            stringArgumentCaptor.getAllValues()
                .subList(1, 3));
        assertEquals(Http.Body.of(rowData), bodyArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(Http.ResponseType.Type.JSON, configuration.getResponseType()
            .getType());
    }
}
