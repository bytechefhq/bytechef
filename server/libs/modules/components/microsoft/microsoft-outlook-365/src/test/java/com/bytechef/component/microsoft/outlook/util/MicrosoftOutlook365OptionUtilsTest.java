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

import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ID;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ODATA_NEXT_LINK;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Option;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class MicrosoftOutlook365OptionUtilsTest {

    private final List<Option<String>> expectedOptions = List.of(option("abc", "abc"), option("cde", "cde"));
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final ArgumentCaptor<Object[]> queryArgumentCaptor = ArgumentCaptor.forClass(Object[].class);

    @Test
    void testGetCategoryOptions() {
        List<Map<String, String>> mails = List.of(Map.of("displayName", "abc"));

        Map<String, Object> body = Map.of(VALUE, mails, ODATA_NEXT_LINK, "link");

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(body);

        try (MockedStatic<MicrosoftOutlook365Utils> microsoftOutlook365UtilsMockedStatic =
            mockStatic(MicrosoftOutlook365Utils.class)) {

            microsoftOutlook365UtilsMockedStatic
                .when(() -> MicrosoftOutlook365Utils.getItemsFromNextPage(mockedContext, "link"))
                .thenReturn(List.of(Map.of("displayName", "cde")));

            List<Option<String>> categoryOptions = MicrosoftOutlook365OptionUtils.getCategoryOptions(
                null, null, Map.of(), anyString(), mockedContext);

            assertEquals(expectedOptions, categoryOptions);
        }
    }

    @Test
    void testGetMessageIdOptions() {
        List<Map<String, String>> mails = List.of(Map.of(ID, "abc"));

        Map<String, Object> body = Map.of(VALUE, mails, ODATA_NEXT_LINK, "link");

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(body);

        try (MockedStatic<MicrosoftOutlook365Utils> microsoftOutlook365UtilsMockedStatic =
            mockStatic(MicrosoftOutlook365Utils.class)) {

            microsoftOutlook365UtilsMockedStatic
                .when(() -> MicrosoftOutlook365Utils.getItemsFromNextPage(mockedContext, "link"))
                .thenReturn(List.of(Map.of(ID, "cde")));

            List<Option<String>> messageIdOptions = MicrosoftOutlook365OptionUtils.getMessageIdOptions(
                null, null, Map.of(), anyString(), mockedContext);

            assertEquals(expectedOptions, messageIdOptions);

            Object[] query = queryArgumentCaptor.getValue();

            assertEquals(List.of("$top", 100), Arrays.asList(query));
        }
    }
}
