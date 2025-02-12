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

package com.bytechef.component.quickbooks.action;

import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.ACCOUNT;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.ASSET_ACCOUNT_REF;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.EXPENSE_ACCOUNT_REF;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.INCOME_ACCOUNT_REF;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.INV_START_DATE;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.NAME;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.QTY_ON_HAND;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.TYPE;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.quickbooks.constant.ItemType;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
class QuickbooksCreateItemActionTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Object mockedObject = mock(Object.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            NAME, NAME, TYPE, ItemType.INVENTORY.name(), QTY_ON_HAND, 123, EXPENSE_ACCOUNT_REF, "expense",
            ACCOUNT, Map.of(
                INCOME_ACCOUNT_REF, "income", ASSET_ACCOUNT_REF, "asset",
                INV_START_DATE, LocalDate.of(2020, 1, 1))));
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @Test
    void testPerform() {
        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedObject);

        Object result = QuickbooksCreateItemAction.perform(mockedParameters, mockedParameters, mockedActionContext);

        assertEquals(mockedObject, result);

        Http.Body body = bodyArgumentCaptor.getValue();

        Map<String, Object> expectedBody = Map.of(
            NAME, NAME, QTY_ON_HAND, 123.0, INCOME_ACCOUNT_REF, Map.of(VALUE, "income"),
            ASSET_ACCOUNT_REF, Map.of(VALUE, "asset"), INV_START_DATE, "2020-01-01", EXPENSE_ACCOUNT_REF,
            Map.of(VALUE, "expense"));

        assertEquals(expectedBody, body.getContent());
    }
}
