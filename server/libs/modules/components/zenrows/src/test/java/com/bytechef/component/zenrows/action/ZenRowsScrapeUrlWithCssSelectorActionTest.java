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

package com.bytechef.component.zenrows.action;

import static com.bytechef.component.zenrows.constant.ZenRowsConstants.CSS_EXTRACTOR;
import static com.bytechef.component.zenrows.constant.ZenRowsConstants.URL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ZenRowsScrapeUrlWithCssSelectorActionTest {

    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(URL, "mockUrl", CSS_EXTRACTOR, List.of(Map.of("key", "value"))));
    private final Http.Response mockedResponse = mock(Http.Response.class);
    public static final String mockedJson = "json";
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private static final String stringResponse = "scrapedUrl";

    @Test
    void testPerform() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(
            stringArgumentCaptor.capture(), stringArgumentCaptor.capture(),
            stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(stringResponse);

        when(mockedContext.json(any()))
            .thenReturn(mockedJson);

        String result = ZenRowsScrapeUrlWithCssSelectorAction.perform(
            mockedParameters, mockedParameters, mockedContext);

        assertEquals(stringResponse, result);

        assertEquals(List.of(URL, "mockUrl", CSS_EXTRACTOR, mockedJson), stringArgumentCaptor.getAllValues());
    }
}
