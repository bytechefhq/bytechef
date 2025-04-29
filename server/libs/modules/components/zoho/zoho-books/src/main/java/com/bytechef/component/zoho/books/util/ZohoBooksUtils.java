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

import static com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.zoho.books.constant.ZohoBooksConstants.CONTACT_NAME;
import static com.bytechef.component.zoho.books.constant.ZohoBooksConstants.CONTACT_TYPE;
import static com.bytechef.component.zoho.books.constant.ZohoBooksConstants.CUSTOMER_SUB_TYPE;
import static com.bytechef.component.zoho.books.constant.ZohoBooksConstants.INVOICE_NUMBER;
import static com.bytechef.component.zoho.books.constant.ZohoBooksConstants.SALES_ORDER_NUMBER;
import static com.bytechef.component.zoho.books.constant.ZohoBooksConstants.USE_CUSTOM_INVOICE_NUMBER;
import static com.bytechef.component.zoho.books.constant.ZohoBooksConstants.USE_CUSTOM_SALES_ORDER_NUMBER;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class ZohoBooksUtils {

    private ZohoBooksUtils() {
    }

    public static List<Property.ValueProperty<?>> createPropertiesForCustomerType(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        ActionContext actionContext) {

        String contentType = inputParameters.getRequiredString(CONTACT_TYPE);

        if (contentType.equals("customer")) {
            ModifiableStringProperty customerType = string(CUSTOMER_SUB_TYPE)
                .label("Customer Sub Type")
                .description("Type of the customer.")
                .options(option("BUSINESS", "business"), option("INDIVIDUAL", "individual"))
                .required(true);

            return List.of(customerType);
        }
        return List.of();
    }

    public static List<Property.ValueProperty<?>> createPropertiesForInvoiceNumber(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        ActionContext actionContext) {

        boolean customInvoiceNumber = inputParameters.getRequiredBoolean(USE_CUSTOM_INVOICE_NUMBER);

        if (customInvoiceNumber) {
            ModifiableStringProperty invoiceNumber = string(INVOICE_NUMBER)
                .label("Invoice Number")
                .description("Number of invoice.")
                .required(true);

            return List.of(invoiceNumber);
        }
        return List.of();
    }

    public static List<Property.ValueProperty<?>> createPropertiesForSalesOrderNumber(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        ActionContext actionContext) {

        boolean customSalesOrderNumber = inputParameters.getRequiredBoolean(USE_CUSTOM_SALES_ORDER_NUMBER);

        if (customSalesOrderNumber) {
            ModifiableStringProperty salesOrderNumber = string(SALES_ORDER_NUMBER)
                .label("Sales Order Number")
                .description("Number of sales order.")
                .required(true);

            return List.of(salesOrderNumber);
        }
        return List.of();
    }

    public static List<Option<String>> getCurrencyOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();

        Map<String, ?> body = context
            .http(http -> http.get("/settings/currencies"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get("currencies") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    options.add(option((String) map.get("currency_name"), (String) map.get("currency_id")));
                }
            }
        }
        return options;
    }

    public static List<Option<String>> getCustomersOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();

        Map<String, ?> body = context
            .http(http -> http.get("/contacts"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get("contacts") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map && map.get(CONTACT_TYPE)
                    .equals("customer")) {
                    options.add(option((String) map.get(CONTACT_NAME), (String) map.get("contact_id")));
                }
            }
        }
        return options;
    }

    public static List<Option<String>> getItemsOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();

        Map<String, ?> body = context
            .http(http -> http.get("/items"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get("items") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    options.add(option((String) map.get("name"), (String) map.get("item_id")));
                }
            }
        }
        return options;
    }
}
