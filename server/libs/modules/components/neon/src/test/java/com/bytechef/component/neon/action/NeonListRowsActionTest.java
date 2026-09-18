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

import static com.bytechef.component.neon.constant.NeonConstants.FILTERS;
import static com.bytechef.component.neon.constant.NeonConstants.LIMIT;
import static com.bytechef.component.neon.constant.NeonConstants.OFFSET;
import static com.bytechef.component.neon.constant.NeonConstants.ORDER;
import static com.bytechef.component.neon.constant.NeonConstants.SELECT;
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
class NeonListRowsActionTest {

    private final Object mockedObject = mock(Object.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<Map<String, List<String>>> queryParametersArgumentCaptor = forClass(Map.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            TABLE, "posts",
            SELECT, "id,title",
            FILTERS, List.of("status=eq.published"),
            ORDER, "created_at.desc",
            LIMIT, 10,
            OFFSET, 5));

    @Test
    void testPerform(
        Context mockedContext, Http.Response mockedResponse, Http.Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryParametersArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(mockedObject);

        Object result = NeonListRowsAction.perform(mockedParameters, null, mockedContext);

        assertEquals(mockedObject, result);
        assertEquals("/posts", stringArgumentCaptor.getValue());
        assertEquals(
            Map.of(
                "status", List.of("eq.published"),
                SELECT, List.of("id,title"),
                ORDER, List.of("created_at.desc"),
                LIMIT, List.of("10"),
                OFFSET, List.of("5")),
            queryParametersArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(Http.ResponseType.Type.JSON, configuration.getResponseType()
            .getType());
    }
}
