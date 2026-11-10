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

package com.bytechef.component.xero.action;

import static com.bytechef.component.xero.constant.XeroConstants.CONTACT_IDS;
import static com.bytechef.component.xero.constant.XeroConstants.DATE_FROM;
import static com.bytechef.component.xero.constant.XeroConstants.DATE_TO;
import static com.bytechef.component.xero.constant.XeroConstants.INVOICES;
import static com.bytechef.component.xero.constant.XeroConstants.INVOICE_ID;
import static com.bytechef.component.xero.constant.XeroConstants.MODIFIED_SINCE;
import static com.bytechef.component.xero.constant.XeroConstants.PAGE;
import static com.bytechef.component.xero.constant.XeroConstants.STATUSES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
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
class XeroListInvoicesActionTest {

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    private final ArgumentCaptor<Map<String, List<String>>> headersArgumentCaptor =
        (ArgumentCaptor) forClass(Map.class);
    private final List<Map<String, Object>> invoices = List.of(Map.of(INVOICE_ID, "invoice-1"));
    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    private final ArgumentCaptor<Map<String, List<String>>> queryParametersArgumentCaptor =
        (ArgumentCaptor) forClass(Map.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        ActionContext mockedContext, Response mockedResponse, Http mockedHttp, Executor mockedExecutor,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Map<String, Object> parameterMap = new HashMap<>();

        parameterMap.put(STATUSES, List.of("DRAFT", "AUTHORISED"));
        parameterMap.put(CONTACT_IDS, List.of("contact-1", "contact-2"));
        parameterMap.put(DATE_FROM, LocalDate.of(2024, 1, 5));
        parameterMap.put(DATE_TO, LocalDate.of(2024, 12, 31));
        parameterMap.put(MODIFIED_SINCE, LocalDateTime.of(2024, 3, 4, 5, 6, 0));
        parameterMap.put(PAGE, 3);

        Parameters parameters = MockParametersFactory.create(parameterMap);

        mockHttp(mockedResponse, mockedHttp, mockedExecutor);

        Object result = XeroListInvoicesAction.perform(parameters, null, mockedContext);

        assertEquals(invoices, result);

        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/Invoices", stringArgumentCaptor.getValue());
        assertEquals(
            Map.of(
                PAGE, List.of("3"),
                STATUSES, List.of("DRAFT,AUTHORISED"),
                CONTACT_IDS, List.of("contact-1,contact-2"),
                "where", List.of("Date >= DateTime(2024,01,05) && Date <= DateTime(2024,12,31)")),
            queryParametersArgumentCaptor.getValue());
        assertEquals(
            Map.of("If-Modified-Since", List.of("2024-03-04T05:06:00")), headersArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
    }

    @Test
    void testPerformWithoutFilters(
        ActionContext mockedContext, Response mockedResponse, Http mockedHttp, Executor mockedExecutor) {

        Parameters parameters = MockParametersFactory.create(Map.of());

        mockHttp(mockedResponse, mockedHttp, mockedExecutor);

        Object result = XeroListInvoicesAction.perform(parameters, null, mockedContext);

        assertEquals(invoices, result);
        assertEquals(Map.of(PAGE, List.of("1")), queryParametersArgumentCaptor.getValue());
        assertEquals(Map.of(), headersArgumentCaptor.getValue());
    }

    @Test
    void testPerformWithDateFromOnly(
        ActionContext mockedContext, Response mockedResponse, Http mockedHttp, Executor mockedExecutor) {

        Parameters parameters = MockParametersFactory.create(Map.of(DATE_FROM, LocalDate.of(2024, 1, 5)));

        mockHttp(mockedResponse, mockedHttp, mockedExecutor);

        XeroListInvoicesAction.perform(parameters, null, mockedContext);

        assertEquals(
            Map.of(PAGE, List.of("1"), "where", List.of("Date >= DateTime(2024,01,05)")),
            queryParametersArgumentCaptor.getValue());
    }

    private void mockHttp(Response mockedResponse, Http mockedHttp, Executor mockedExecutor) {
        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryParametersArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.headers(headersArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(INVOICES, invoices));
    }
}
