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

package com.bytechef.component.quickbooks.action;

import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.CUSTOMER;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.MAX_RESULTS;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.TXN_DATE_FROM;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.TXN_DATE_TO;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.UNPAID_ONLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
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
class QuickbooksSearchInvoicesActionTest {

    private final List<Map<String, Object>> invoices = List.of(Map.of("Id", "1", "DocNumber", "1001"));
    private final ArgumentCaptor<String> headerNameArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<String> headerValueArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<String> pathArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<String> queryNameArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<String> queryValueArgumentCaptor = forClass(String.class);

    @Test
    void testPerformWithoutFilters(
        ActionContext mockedActionContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        setupHttpMock(mockedResponse, mockedExecutor, mockedHttp, Map.of("QueryResponse", Map.of("Invoice", invoices)));

        Object result = QuickbooksSearchInvoicesAction.perform(
            MockParametersFactory.create(Map.of()), null, mockedActionContext);

        assertEquals(invoices, result);
        assertEquals("/query", pathArgumentCaptor.getValue());
        assertEquals("query", queryNameArgumentCaptor.getValue());
        assertEquals(
            "SELECT * FROM Invoice ORDERBY TxnDate DESC MAXRESULTS 100", queryValueArgumentCaptor.getValue());
        assertEquals("Accept", headerNameArgumentCaptor.getValue());
        assertEquals("application/json", headerValueArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
    }

    @Test
    void testPerformWithTransactionDates(
        ActionContext mockedActionContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        assertQuery(
            mockedActionContext, mockedResponse, mockedExecutor, mockedHttp,
            Map.of(TXN_DATE_FROM, LocalDate.of(2026, 1, 1), TXN_DATE_TO, LocalDate.of(2026, 1, 31)),
            "SELECT * FROM Invoice WHERE TxnDate >= '2026-01-01' AND TxnDate <= '2026-01-31' " +
                "ORDERBY TxnDate DESC MAXRESULTS 100");
    }

    @Test
    void testPerformWithTransactionDateFrom(
        ActionContext mockedActionContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        assertQuery(
            mockedActionContext, mockedResponse, mockedExecutor, mockedHttp,
            Map.of(TXN_DATE_FROM, LocalDate.of(2026, 1, 1)),
            "SELECT * FROM Invoice WHERE TxnDate >= '2026-01-01' ORDERBY TxnDate DESC MAXRESULTS 100");
    }

    @Test
    void testPerformWithTransactionDateTo(
        ActionContext mockedActionContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        assertQuery(
            mockedActionContext, mockedResponse, mockedExecutor, mockedHttp,
            Map.of(TXN_DATE_TO, LocalDate.of(2026, 1, 31)),
            "SELECT * FROM Invoice WHERE TxnDate <= '2026-01-31' ORDERBY TxnDate DESC MAXRESULTS 100");
    }

    @Test
    void testPerformWithCustomer(
        ActionContext mockedActionContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        assertQuery(
            mockedActionContext, mockedResponse, mockedExecutor, mockedHttp, Map.of(CUSTOMER, "58"),
            "SELECT * FROM Invoice WHERE CustomerRef = '58' ORDERBY TxnDate DESC MAXRESULTS 100");
    }

    @Test
    void testPerformWithUnpaidOnly(
        ActionContext mockedActionContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        assertQuery(
            mockedActionContext, mockedResponse, mockedExecutor, mockedHttp, Map.of(UNPAID_ONLY, true),
            "SELECT * FROM Invoice WHERE Balance > '0' ORDERBY TxnDate DESC MAXRESULTS 100");
    }

    @Test
    void testPerformWithUnpaidOnlyFalse(
        ActionContext mockedActionContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        assertQuery(
            mockedActionContext, mockedResponse, mockedExecutor, mockedHttp, Map.of(UNPAID_ONLY, false),
            "SELECT * FROM Invoice ORDERBY TxnDate DESC MAXRESULTS 100");
    }

    @Test
    void testPerformWithAllFilters(
        ActionContext mockedActionContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        Map<String, Object> parameters = new HashMap<>();

        parameters.put(TXN_DATE_FROM, LocalDate.of(2026, 1, 1));
        parameters.put(TXN_DATE_TO, LocalDate.of(2026, 1, 31));
        parameters.put(CUSTOMER, "58");
        parameters.put(UNPAID_ONLY, true);
        parameters.put(MAX_RESULTS, 25);

        assertQuery(
            mockedActionContext, mockedResponse, mockedExecutor, mockedHttp, parameters,
            "SELECT * FROM Invoice WHERE TxnDate >= '2026-01-01' AND TxnDate <= '2026-01-31' " +
                "AND CustomerRef = '58' AND Balance > '0' ORDERBY TxnDate DESC MAXRESULTS 25");
    }

    @Test
    void testPerformEscapesQuotesInCustomer(
        ActionContext mockedActionContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        assertQuery(
            mockedActionContext, mockedResponse, mockedExecutor, mockedHttp, Map.of(CUSTOMER, "58' OR Id > '0"),
            "SELECT * FROM Invoice WHERE CustomerRef = '58\\' OR Id > \\'0' ORDERBY TxnDate DESC MAXRESULTS 100");
    }

    @Test
    void testPerformEscapesBackslashesInCustomer(
        ActionContext mockedActionContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        assertQuery(
            mockedActionContext, mockedResponse, mockedExecutor, mockedHttp, Map.of(CUSTOMER, "58\\"),
            "SELECT * FROM Invoice WHERE CustomerRef = '58\\\\' ORDERBY TxnDate DESC MAXRESULTS 100");
    }

    @Test
    void testPerformClampsMaxResults(
        ActionContext mockedActionContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        assertQuery(
            mockedActionContext, mockedResponse, mockedExecutor, mockedHttp, Map.of(MAX_RESULTS, 5000),
            "SELECT * FROM Invoice ORDERBY TxnDate DESC MAXRESULTS 1000");
    }

    @Test
    void testPerformWithoutInvoicesInResponse(
        ActionContext mockedActionContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        setupHttpMock(mockedResponse, mockedExecutor, mockedHttp, Map.of("QueryResponse", Map.of()));

        Object result = QuickbooksSearchInvoicesAction.perform(
            MockParametersFactory.create(Map.of()), null, mockedActionContext);

        assertEquals(List.of(), result);
    }

    private void assertQuery(
        ActionContext mockedActionContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        Map<String, Object> parameterMap, String expectedQuery) {

        setupHttpMock(mockedResponse, mockedExecutor, mockedHttp, Map.of("QueryResponse", Map.of("Invoice", invoices)));

        Parameters parameters = MockParametersFactory.create(parameterMap);

        Object result = QuickbooksSearchInvoicesAction.perform(parameters, null, mockedActionContext);

        assertEquals(invoices, result);
        assertEquals(expectedQuery, queryValueArgumentCaptor.getValue());
    }

    private void setupHttpMock(
        Response mockedResponse, Executor mockedExecutor, Http mockedHttp, Map<String, Object> responseBody) {

        when(mockedHttp.get(pathArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(queryNameArgumentCaptor.capture(), queryValueArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.header(headerNameArgumentCaptor.capture(), headerValueArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseBody);
    }
}
