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

package com.bytechef.zoho.commons;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.zoho.commons.ZohoConstants.CONTACT_NAME;
import static com.bytechef.component.zoho.commons.ZohoConstants.CONTACT_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import com.bytechef.component.zoho.commons.ZohoUtils;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
@ExtendWith(MockContextSetupExtension.class)
class ZohoUtilsTest {

    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final List<Option<String>> expectedOptions = List.of(
        option("List 1", "list1"), option("List 2", "list2"));
    private final Parameters mockedParameters = mock(Parameters.class);

    @BeforeEach
    void beforeEach(Executor mockedExecutor, Http mockedHttp) {
        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
    }

    @Test
    void testGetCurrencyOptions(
        Context mockedContext, Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Map<String, Object> mockedCurrencyMap = Map.of(
            "currencies", List.of(
                Map.of("currency_name", "List 1", "currency_id", "list1"),
                Map.of("currency_name", "List 2", "currency_id", "list2")));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedCurrencyMap);

        List<Option<String>> result = ZohoUtils.getCurrencyOptions(
            null, null, null, null, mockedContext);

        assertEquals(result, expectedOptions);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(Http.ResponseType.JSON, configuration.getResponseType());
        assertEquals("/settings/currencies", stringArgumentCaptor.getValue());
    }

    @Test
    void testGetCustomersOptions(
        Context mockedContext, Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Map<String, Object> mockedCustomersMap = Map.of(
            "contacts", List.of(
                Map.of(CONTACT_NAME, "List 1", "contact_id", "list1", CONTACT_TYPE, "customer"),
                Map.of(CONTACT_NAME, "List 2", "contact_id", "list2", CONTACT_TYPE, "customer")));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedCustomersMap);

        List<Option<String>> result = ZohoUtils.getCustomersOptions(
            null, null, null, null, mockedContext);

        assertEquals(result, expectedOptions);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(Http.ResponseType.JSON, configuration.getResponseType());
        assertEquals("/contacts", stringArgumentCaptor.getValue());
    }

    @Test
    void testGetItemsOptions(
        Context mockedContext, Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Map<String, Object> mockedItemsMap = Map.of(
            "items", List.of(
                Map.of("name", "List 1", "item_id", "list1"),
                Map.of("name", "List 2", "item_id", "list2")));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedItemsMap);

        List<Option<String>> result = ZohoUtils.getItemsOptions(
            null, null, null, null, mockedContext);

        assertEquals(result, expectedOptions);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(Http.ResponseType.JSON, configuration.getResponseType());
        assertEquals("/items", stringArgumentCaptor.getValue());
    }
}
