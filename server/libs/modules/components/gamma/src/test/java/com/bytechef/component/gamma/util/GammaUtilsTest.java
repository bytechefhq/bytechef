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

package com.bytechef.component.gamma.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.gamma.constant.GammaConstants.ID;
import static com.bytechef.component.gamma.constant.GammaConstants.NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.TypeReference;
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
class GammaUtilsTest {

    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testGetThemeOptionsFollowsTheCursor(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        ArgumentCaptor<Object[]> queryParametersArgumentCaptor = forClass(Object[].class);
        Map<String, Object> lastPage = new HashMap<>();

        lastPage.put("data", List.of(Map.of(NAME, "name2", ID, "id2")));
        lastPage.put("hasMore", false);
        lastPage.put("nextCursor", null);

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryParametersArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of("data", List.of(Map.of(NAME, "name1", ID, "id1")), "hasMore", true, "nextCursor", "next"))
            .thenReturn(lastPage);

        List<Option<String>> result = GammaUtils.getThemeOptions(null, null, Map.of(), "", mockedContext);

        assertEquals(List.of(option("name1", "id1"), option("name2", "id2")), result);
        assertEquals(List.of("/themes", "/themes"), stringArgumentCaptor.getAllValues());

        List<Object[]> queryParameters = queryParametersArgumentCaptor.getAllValues();

        assertEquals(List.of("limit", "50"), List.of(queryParameters.get(0)));
        assertEquals(List.of("limit", "50", "after", "next"), List.of(queryParameters.get(1)));

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        assertEquals(ResponseType.JSON, configurationBuilder.build()
            .getResponseType());
    }
}
