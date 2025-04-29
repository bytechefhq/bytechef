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

package com.bytechef.component.zoho.books.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.zoho.books.constant.ZohoBooksConstants.CONTACT_NAME;
import static com.bytechef.component.zoho.books.constant.ZohoBooksConstants.CONTACT_TYPE;
import static com.bytechef.component.zoho.books.constant.ZohoBooksConstants.CUSTOMER_SUB_TYPE;
import static com.bytechef.component.zoho.books.constant.ZohoBooksConstants.INVOICE_NUMBER;
import static com.bytechef.component.zoho.books.constant.ZohoBooksConstants.SALES_ORDER_NUMBER;
import static com.bytechef.component.zoho.books.constant.ZohoBooksConstants.USE_CUSTOM_INVOICE_NUMBER;
import static com.bytechef.component.zoho.books.constant.ZohoBooksConstants.USE_CUSTOM_SALES_ORDER_NUMBER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
class ZohoBooksUtilsTest {
    private final List<Option<String>> expectedOptions = List.of(
        option("List 1", "list1"), option("List 2", "list2"));
    private final Context mockedContext = mock(Context.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Response mockedResponse = mock(Response.class);
    private final ArgumentCaptor<Context.Http.Body> bodyArgumentCaptor =
        ArgumentCaptor.forClass(Context.Http.Body.class);

    @BeforeEach
    void beforeEach() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
    }

    @Test
    void testCreatePropertiesForCustomerType() {
        when(mockedParameters.getRequiredString(CONTACT_TYPE)).thenReturn("customer");

        List<Property.ValueProperty<?>> result = ZohoBooksUtils.createPropertiesForCustomerType(
            mockedParameters, mockedParameters, Map.of(), mockedActionContext);

        ModifiableStringProperty customer = (ModifiableStringProperty) result.getFirst();
        assertEquals(CUSTOMER_SUB_TYPE, customer.getName());
    }

    @Test
    void testCreatePropertiesForInvoiceNumber() {
        when(mockedParameters.getRequiredBoolean(USE_CUSTOM_INVOICE_NUMBER)).thenReturn(true);

        List<Property.ValueProperty<?>> result = ZohoBooksUtils.createPropertiesForInvoiceNumber(
            mockedParameters, mockedParameters, Map.of(), mockedActionContext);

        ModifiableStringProperty invoiceNumber = (ModifiableStringProperty) result.getFirst();
        assertEquals(INVOICE_NUMBER, invoiceNumber.getName());
    }

    @Test
    void testCreatePropertiesForSalesOrderNumber() {
        when(mockedParameters.getRequiredBoolean(USE_CUSTOM_SALES_ORDER_NUMBER)).thenReturn(true);

        List<Property.ValueProperty<?>> result = ZohoBooksUtils.createPropertiesForSalesOrderNumber(
            mockedParameters, mockedParameters, Map.of(), mockedActionContext);

        ModifiableStringProperty salesOrderNumber = (ModifiableStringProperty) result.getFirst();
        assertEquals(SALES_ORDER_NUMBER, salesOrderNumber.getName());
    }

    @Test
    void testGetCurrencyOptions() {
        Map<String, Object> mockedCurrencyMap = Map.of(
            "currencies", List.of(
                Map.of("currency_name", "List 1", "currency_id", "list1"),
                Map.of("currency_name", "List 2", "currency_id", "list2")));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedCurrencyMap);

        List<Option<String>> result = ZohoBooksUtils.getCurrencyOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertThat(result, Matchers.containsInAnyOrder(expectedOptions.toArray()));
    }

    @Test
    void testGetCustomersOptions() {
        Map<String, Object> mockedCustomersMap = Map.of(
            "contacts", List.of(
                Map.of(CONTACT_NAME, "List 1", "contact_id", "list1", CONTACT_TYPE, "customer"),
                Map.of(CONTACT_NAME, "List 2", "contact_id", "list2", CONTACT_TYPE, "customer")));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedCustomersMap);

        List<Option<String>> result = ZohoBooksUtils.getCustomersOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertThat(result, Matchers.containsInAnyOrder(expectedOptions.toArray()));
    }

    @Test
    void testGetItemsOptions() {
        Map<String, Object> mockedItemsMap = Map.of(
            "items", List.of(
                Map.of("name", "List 1", "item_id", "list1"),
                Map.of("name", "List 2", "item_id", "list2")));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedItemsMap);

        List<Option<String>> result = ZohoBooksUtils.getItemsOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertThat(result, Matchers.containsInAnyOrder(expectedOptions.toArray()));
    }
}
