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

import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.DUE_DATE_FROM;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.DUE_DATE_TO;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.MAX_RESULTS;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.OPEN_BALANCE_ONLY;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.VENDOR_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
class QuickbooksSearchBillsActionTest {

    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor =
        ArgumentCaptor.forClass(ContextFunction.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http mockedHttp = mock(Http.class);
    private final Object mockedObject = mock(Object.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final ArgumentCaptor<String> queryArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPerformWithAllFilters() throws Exception {
        Parameters parameters = MockParametersFactory.create(
            Map.of(
                VENDOR_ID, "56", DUE_DATE_FROM, "2026-01-01", DUE_DATE_TO, "2026-01-31", OPEN_BALANCE_ONLY, true,
                MAX_RESULTS, 25));

        when(mockedActionContext.http(httpFunctionArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(anyString(), queryArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedObject);

        Object result = QuickbooksSearchBillsAction.perform(parameters, parameters, mockedActionContext);

        assertEquals(mockedObject, result);
        assertEquals(
            "SELECT * FROM Bill WHERE VendorRef = '56' AND DueDate >= '2026-01-01' AND DueDate <= '2026-01-31' " +
                "AND Balance > '0' MAXRESULTS 25",
            queryArgumentCaptor.getValue());

        ContextFunction<Http, Http.Executor> httpFunction = httpFunctionArgumentCaptor.getValue();

        httpFunction.apply(mockedHttp);

        verify(mockedHttp).get("/query");
    }

    @Test
    void testBuildQueryWithoutFilters() {
        Parameters parameters = MockParametersFactory.create(Map.of());

        assertEquals("SELECT * FROM Bill MAXRESULTS 100", QuickbooksSearchBillsAction.buildQuery(parameters));
    }

    @Test
    void testBuildQueryEscapesSingleQuotes() {
        Parameters parameters = MockParametersFactory.create(Map.of(VENDOR_ID, "5' OR '1'='1"));

        assertEquals(
            "SELECT * FROM Bill WHERE VendorRef = '5\\' OR \\'1\\'=\\'1' MAXRESULTS 100",
            QuickbooksSearchBillsAction.buildQuery(parameters));
    }

    @Test
    void testBuildQueryEscapesTrailingBackslash() {
        Parameters parameters = MockParametersFactory.create(Map.of(VENDOR_ID, "56\\"));

        assertEquals(
            "SELECT * FROM Bill WHERE VendorRef = '56\\\\' MAXRESULTS 100",
            QuickbooksSearchBillsAction.buildQuery(parameters));
    }

    @Test
    void testBuildQueryEscapesBackslashBeforeSingleQuote() {
        Parameters parameters = MockParametersFactory.create(Map.of(VENDOR_ID, "56\\' OR '1'='1"));

        assertEquals(
            "SELECT * FROM Bill WHERE VendorRef = '56\\\\\\' OR \\'1\\'=\\'1' MAXRESULTS 100",
            QuickbooksSearchBillsAction.buildQuery(parameters));
    }
}
