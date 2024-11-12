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

import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.ACCOUNT;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.ASSET_ACCOUNT_REF;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.ID;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.INCOME_ACCOUNT_REF;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.INVENTORY;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.INV_START_DATE;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.NAME;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.SERVICE;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.TYPE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableDateProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.definition.TypeReference;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class QuickbooksUtils {

    private QuickbooksUtils() {
    }

    public static List<? extends ValueProperty<?>> addPropertiesForItem(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        ActionContext actionContext) {

        String type = inputParameters.getRequiredString(TYPE);

        ModifiableStringProperty incomeAccount = string(INCOME_ACCOUNT_REF)
            .label("Income Account")
            .options(QuickbooksUtils.getAccountOptions("Income"))
            .required(type.equals(INVENTORY) || type.equals(SERVICE));

        ModifiableStringProperty assetAccount = string(ASSET_ACCOUNT_REF)
            .label("Asset Account")
            .options(QuickbooksUtils.getAccountOptions("Asset"))
            .required(type.equals(INVENTORY));

        ModifiableDateProperty inventoryStartDate = date(INV_START_DATE)
            .label("Inventory Start Date")
            .description("Date of opening balance for the inventory transaction.")
            .required(type.equals(INVENTORY));

        return List.of(incomeAccount, assetAccount, inventoryStartDate);
    }

    public static ActionOptionsFunction<String> getAccountOptions(String accountType) {
        return (inputParameters, connectionParameters, arrayIndex, searchText, actionContext) -> {

            String encodeQuery = URLEncoder.encode("SELECT * FROM Account", StandardCharsets.UTF_8);

            Map<String, Object> body = actionContext
                .http(http -> http.get("/query"))
                .queryParameter("query", encodeQuery)
                .configuration(Http.responseType(Http.ResponseType.XML))
                .execute()
                .getBody(new TypeReference<>() {});

            List<Option<String>> options = new ArrayList<>();

            if (body.get("QueryResponse") instanceof Map<?, ?> map && map.get(ACCOUNT) instanceof List<?> list) {
                for (Object o : list) {
                    if (o instanceof Map<?, ?> entityMap) {
                        if (entityMap.get("AccountType")
                            .equals(accountType)) {

                            options.add(option((String) entityMap.get(NAME), (String) entityMap.get(ID)));
                        }
                    }
                }
            }

            return options;
        };
    }
}
