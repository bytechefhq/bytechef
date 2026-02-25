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

package com.bytechef.component.microsoft.outlook.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CALENDAR;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ID;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.NAME;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ODATA_NEXT_LINK;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.SUBJECT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.VALUE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
@ExtendWith(MockContextSetupExtension.class)
class MicrosoftOutlook365OptionUtilsTest {

    private final List<Option<String>> expectedOptions = List.of(option("abc", "abc"), option("cde", "cde"));
    private final ArgumentCaptor<Object[]> queryArgumentCaptor = forClass(Object[].class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testGetCalendarIdOptions(
        ActionContext mockedActionContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Map<String, Object> body = Map.of(VALUE, List.of(Map.of(ID, "abc", NAME, "abc")), ODATA_NEXT_LINK, "link");

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(body, Map.of(VALUE, List.of(Map.of(ID, "cde", NAME, "cde"))));

        List<Option<String>> categoryOptions = MicrosoftOutlook365OptionUtils.getCalendarIdOptions(
            null, null, Map.of(), anyString(), mockedActionContext);

        assertEquals(expectedOptions, categoryOptions);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        List<ConfigurationBuilder> configurationBuilders = configurationBuilderArgumentCaptor.getAllValues();

        for (ConfigurationBuilder configurationBuilder : configurationBuilders) {
            Configuration configuration = configurationBuilder.build();

            assertEquals(ResponseType.JSON, configuration.getResponseType());
        }

        assertEquals(List.of("/me/calendars", "link"), stringArgumentCaptor.getAllValues());
    }

    @Test
    void testGetCategoryOptions(
        ActionContext mockedActionContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Map<String, Object> body = Map.of(VALUE, List.of(Map.of("displayName", "abc")), ODATA_NEXT_LINK, "link");

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(body, Map.of(VALUE, List.of(Map.of("displayName", "cde"))));

        List<Option<String>> categoryOptions = MicrosoftOutlook365OptionUtils.getCategoryOptions(
            null, null, Map.of(), anyString(), mockedActionContext);

        assertEquals(expectedOptions, categoryOptions);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        List<ConfigurationBuilder> configurationBuilders = configurationBuilderArgumentCaptor.getAllValues();

        for (ConfigurationBuilder configurationBuilder : configurationBuilders) {
            Configuration configuration = configurationBuilder.build();

            assertEquals(ResponseType.JSON, configuration.getResponseType());
        }

        assertEquals(List.of("/me/outlook/masterCategories", "link"), stringArgumentCaptor.getAllValues());
    }

    @Test
    void testGetEventIdOptions(
        ActionContext mockedActionContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Parameters mockedParameters = MockParametersFactory.create(Map.of(CALENDAR, "xy"));
        Map<String, Object> body = Map.of(VALUE, List.of(Map.of(ID, "abc", SUBJECT, "abc")), ODATA_NEXT_LINK, "link");

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(body, Map.of(VALUE, List.of(Map.of(ID, "cde", SUBJECT, "cde"))));

        List<Option<String>> categoryOptions = MicrosoftOutlook365OptionUtils.getEventIdOptions(
            mockedParameters, null, Map.of(), anyString(), mockedActionContext);

        assertEquals(expectedOptions, categoryOptions);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        List<ConfigurationBuilder> configurationBuilders = configurationBuilderArgumentCaptor.getAllValues();

        for (ConfigurationBuilder configurationBuilder : configurationBuilders) {
            Configuration configuration = configurationBuilder.build();

            assertEquals(ResponseType.JSON, configuration.getResponseType());
        }

        assertEquals(List.of("/me/calendars/xy/events", "link"), stringArgumentCaptor.getAllValues());
    }

    @Test
    void testGetMessageIdOptions(
        ActionContext mockedActionContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Map<String, Object> body = Map.of(VALUE, List.of(Map.of(SUBJECT, "abc", ID, "abc")), ODATA_NEXT_LINK, "link");

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(body, Map.of(VALUE, List.of(Map.of(SUBJECT, "cde", ID, "cde"))));

        List<Option<String>> messageIdOptions = MicrosoftOutlook365OptionUtils.getMessageIdOptions(
            null, null, Map.of(), anyString(), mockedActionContext);

        assertEquals(expectedOptions, messageIdOptions);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        List<ConfigurationBuilder> configurationBuilders = configurationBuilderArgumentCaptor.getAllValues();

        for (ConfigurationBuilder configurationBuilder : configurationBuilders) {
            Configuration configuration = configurationBuilder.build();

            assertEquals(ResponseType.JSON, configuration.getResponseType());
        }

        assertEquals(List.of("/me/messages", "link"), stringArgumentCaptor.getAllValues());

        Object[] expectedQuery = {
            "$top", 100
        };

        assertArrayEquals(expectedQuery, queryArgumentCaptor.getValue());
    }
}
