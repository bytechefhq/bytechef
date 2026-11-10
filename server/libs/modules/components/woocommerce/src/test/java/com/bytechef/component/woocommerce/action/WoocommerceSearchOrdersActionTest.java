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

package com.bytechef.component.woocommerce.action;

import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.AFTER;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.BEFORE;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.CUSTOMER;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.PER_PAGE;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.SEARCH;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.STATUS;
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
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockContextSetupExtension.class)
class WoocommerceSearchOrdersActionTest {

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    private final ArgumentCaptor<Map<String, List<String>>> queryParametersArgumentCaptor =
        (ArgumentCaptor) forClass(Map.class);
    private final List<Map<String, Object>> responseList = List.of(Map.of("id", 123, "status", "processing"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Parameters parameters = MockParametersFactory.create(
            Map.of(
                STATUS, "processing",
                AFTER, LocalDateTime.of(2026, 11, 1, 0, 0),
                BEFORE, LocalDateTime.of(2026, 11, 10, 23, 59, 30),
                CUSTOMER, 42,
                SEARCH, "john@example.com",
                PER_PAGE, 50));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryParametersArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseList);

        Object result = WoocommerceSearchOrdersAction.perform(parameters, null, mockedContext);

        assertEquals(responseList, result);

        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/orders", stringArgumentCaptor.getValue());
        assertEquals(
            Map.of(
                STATUS, List.of("processing"),
                AFTER, List.of("2026-11-01T00:00:00"),
                BEFORE, List.of("2026-11-10T23:59:30"),
                CUSTOMER, List.of("42"),
                SEARCH, List.of("john@example.com"),
                PER_PAGE, List.of("50")),
            queryParametersArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
    }

    @Test
    void testPerformWithoutFilters(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        Parameters parameters = MockParametersFactory.create(Map.of());

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryParametersArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseList);

        Object result = WoocommerceSearchOrdersAction.perform(parameters, null, mockedContext);

        assertEquals(responseList, result);
        assertEquals("/orders", stringArgumentCaptor.getValue());
        assertEquals(Map.of(), queryParametersArgumentCaptor.getValue());
    }
}
