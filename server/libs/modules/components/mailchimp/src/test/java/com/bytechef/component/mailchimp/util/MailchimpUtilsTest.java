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

package com.bytechef.component.mailchimp.util;

import static com.bytechef.component.definition.Authorization.AUTHORIZATION;
import static com.bytechef.component.definition.ComponentDsl.option;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.TypeReference;
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
class MailchimpUtilsTest {

    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<Object[]> objectsArgumentCaptor = forClass(Object[].class);

    @Test
    void testGetListIdOptions(
        Http mockedHttp, Executor mockedExecutor, Context mockedContext, Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("lists", List.of(Map.of("name", "abc", "id", "123")), "total_items", 2))
            .thenReturn(Map.of("lists", List.of(Map.of("name", "cde", "id", "345")), "total_items", 2));

        List<Option<String>> result = MailchimpUtils.getListIdOptions(null, null, Map.of(), "", mockedContext);

        assertEquals(List.of(option("abc", "123"), option("cde", "345")), result);

        for (ContextFunction<Http, Executor> httpFunction : httpFunctionArgumentCaptor.getAllValues()) {
            assertNotNull(httpFunction);
        }

        assertEquals(List.of("/lists", "/lists"), stringArgumentCaptor.getAllValues());

        for (ConfigurationBuilder configurationBuilder : configurationBuilderArgumentCaptor.getAllValues()) {
            Configuration configuration = configurationBuilder.build();

            assertEquals(ResponseType.JSON, configuration.getResponseType());
        }

        Object[] queryParameters = {
            "fields", "lists.id,lists.name,total_items",
            "count", "1000",
            "offset", 0
        };

        Object[] queryParameters2 = {
            "fields", "lists.id,lists.name,total_items",
            "count", "1000",
            "offset", 1
        };

        List<Object[]> allValues = objectsArgumentCaptor.getAllValues();

        assertEquals(2, allValues.size());
        assertArrayEquals(queryParameters, allValues.get(0));
        assertArrayEquals(queryParameters2, allValues.get(1));
    }

    @Test
    void testGetMailChimpServer(
        Http mockedHttp, Executor mockedExecutor, Context mockedContext, Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.header(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("dc", "us19"));

        assertEquals("us19", MailchimpUtils.getMailChimpServer("accessToken", mockedContext));
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals(
            List.of("https://login.mailchimp.com/oauth2/metadata", AUTHORIZATION, "OAuth accessToken"),
            stringArgumentCaptor.getAllValues());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
    }
}
