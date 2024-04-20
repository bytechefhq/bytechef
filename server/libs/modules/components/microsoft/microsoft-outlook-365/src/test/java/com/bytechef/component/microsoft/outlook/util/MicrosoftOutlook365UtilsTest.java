/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.microsoft.outlook.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Domiter
 */
class MicrosoftOutlook365UtilsTest {

    private final Context.Http.Executor mockedExecutor = mock(Context.Http.Executor.class);
    private final Context.Http.Response mockedResponse = mock(Context.Http.Response.class);
    private final ActionContext mockedContext = mock(ActionContext.class);

    @Test
    void testGetCategoryOptions() {
        Map<String, String> displayName = new LinkedHashMap<>();

        displayName.put("displayName", "displayName1");

        Map<String, Object> responeseMap = new LinkedHashMap<>();

        responeseMap.put("value", new ArrayList(Arrays.asList(displayName)));

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(Context.TypeReference.class)))
            .thenReturn(responeseMap);

        List<Option<String>> categoryOptions = MicrosoftOutlook365Utils.getCategoryOptions(
            null, null, Map.of(), anyString(), mockedContext);

        assertEquals(1, categoryOptions.size());

        Option<String> option = categoryOptions.getFirst();

        assertEquals("displayName1", option.getValue());
        assertEquals("displayName1", option.getLabel());
    }

    @Test
    void testGetMessageIdOptions() {
        Map<String, String> displayName = new LinkedHashMap<>();

        displayName.put("id", "123");

        Map<String, Object> responeseMap = new LinkedHashMap<>();

        responeseMap.put("value", new ArrayList(Arrays.asList(displayName)));

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(Context.TypeReference.class)))
            .thenReturn(responeseMap);

        List<Option<String>> messageIdOptions = MicrosoftOutlook365Utils.getMessageIdOptions(
            null, null, Map.of(), anyString(), mockedContext);

        assertEquals(1, messageIdOptions.size());

        Option<String> option = messageIdOptions.getFirst();

        assertEquals("123", option.getValue());
        assertEquals("123", option.getLabel());
    }
}
