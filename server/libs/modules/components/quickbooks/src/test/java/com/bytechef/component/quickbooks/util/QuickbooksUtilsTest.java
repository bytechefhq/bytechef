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

package com.bytechef.component.quickbooks.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.DISPLAY_NAME;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.ID;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.NAME;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.TYPE;
import static com.bytechef.component.quickbooks.util.QuickbooksUtils.getPropertiesForItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.quickbooks.constant.Entity;
import com.bytechef.component.quickbooks.constant.ItemType;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
class QuickbooksUtilsTest {

    private final List<Option<String>> expectedOptions = List.of(option("abc", "123"));
    private final Context.Http.Executor mockedExecutor = mock(Context.Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Context.Http.Response mockedResponse = mock(Context.Http.Response.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testGetPropertiesForItemWithInventoryType() {
        verifyProperties(ItemType.INVENTORY, true, true, true);
    }

    @Test
    void testGetPropertiesForItemWithServiceType() {
        verifyProperties(ItemType.SERVICE, true, false, false);
    }

    @Test
    void testGetPropertiesForItemWithOtherType() {
        verifyProperties(ItemType.NON_INVENTORY, false, false, false);
    }

    private void verifyProperties(
        ItemType type, boolean incomeRequired, boolean assetRequired, boolean inventoryDateRequired) {

        when(mockedParameters.getRequired(TYPE, ItemType.class)).thenReturn(type);

        List<? extends Property.ValueProperty<?>> properties =
            getPropertiesForItem(mockedParameters, mockedParameters, Map.of(), mockedActionContext);

        assertEquals(3, properties.size());
        assertEquals(incomeRequired, properties.get(0)
            .getRequired()
            .orElse(false));
        assertEquals(assetRequired, properties.get(1)
            .getRequired()
            .orElse(false));
        assertEquals(inventoryDateRequired, properties.get(2)
            .getRequired()
            .orElse(false));
    }

    private void setupHttpMock(Map<String, Object> responseBody) {
        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseBody);
    }

    private void verifyQuery(Entity expectedEntity) {
        assertEquals(
            List.of("query", URLEncoder.encode("SELECT * FROM " + expectedEntity.getName(), StandardCharsets.UTF_8)),
            stringArgumentCaptor.getAllValues());
    }

    @Test
    void testGetOptionsForCustomer() throws Exception {
        setupHttpMock(
            Map.of(
                "QueryResponse", Map.of(Entity.CUSTOMER.getName(), List.of(Map.of(DISPLAY_NAME, "abc", ID, "123")))));

        List<? extends Option<String>> result = QuickbooksUtils.getOptions(Entity.CUSTOMER, null)
            .apply(mockedParameters, mockedParameters, null, "", mockedActionContext);

        assertEquals(expectedOptions, result);
        verifyQuery(Entity.CUSTOMER);
    }

    @Test
    void testGetOptionsForInvoice() throws Exception {
        setupHttpMock(
            Map.of("QueryResponse", Map.of(Entity.INVOICE.getName(), List.of(Map.of("DocNumber", "abc", ID, "123")))));

        List<? extends Option<String>> result = QuickbooksUtils.getOptions(Entity.INVOICE, null)
            .apply(mockedParameters, mockedParameters, null, "", mockedActionContext);

        assertEquals(expectedOptions, result);
        verifyQuery(Entity.INVOICE);
    }

    @Test
    void testGetOptionsForPayment() throws Exception {
        setupHttpMock(Map.of("QueryResponse", Map.of(Entity.PAYMENT.getName(), List.of(Map.of(ID, "123")))));

        List<? extends Option<String>> result = QuickbooksUtils.getOptions(Entity.PAYMENT, null)
            .apply(mockedParameters, mockedParameters, null, "", mockedActionContext);

        assertEquals(List.of(option("123", "123")), result);
        verifyQuery(Entity.PAYMENT);
    }

    @Test
    void testGetOptionsForItem() throws Exception {
        setupHttpMock(Map.of("QueryResponse", Map.of(Entity.ITEM.getName(), List.of(Map.of(NAME, "abc", ID, "123")))));

        List<? extends Option<String>> result = QuickbooksUtils.getOptions(Entity.ITEM, null)
            .apply(mockedParameters, mockedParameters, null, "", mockedActionContext);

        assertEquals(expectedOptions, result);
        verifyQuery(Entity.ITEM);
    }

    @Test
    void testGetOptionsForIncomeAccount() throws Exception {
        setupHttpMock(
            Map.of(
                "QueryResponse",
                Map.of(Entity.ACCOUNT.getName(), List.of(Map.of(NAME, "abc", ID, "123", "AccountType", "Income")))));

        List<? extends Option<String>> result = QuickbooksUtils.getOptions(Entity.ACCOUNT, "Income")
            .apply(mockedParameters, mockedParameters, null, "", mockedActionContext);

        assertEquals(expectedOptions, result);
        verifyQuery(Entity.ACCOUNT);
    }
}
