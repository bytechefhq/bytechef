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

package com.bytechef.component.quickbooks.util;

import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.quickbooks.constant.Entity.ACCOUNT;
import static com.bytechef.component.quickbooks.constant.ItemType.INVENTORY;
import static com.bytechef.component.quickbooks.constant.ItemType.SERVICE;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.ASSET_ACCOUNT_REF;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.DISPLAY_NAME;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.ID;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.INCOME_ACCOUNT_REF;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.INV_START_DATE;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.NAME;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.TYPE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableDateProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.quickbooks.constant.Entity;
import com.bytechef.component.quickbooks.constant.ItemType;
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

    public static List<? extends ValueProperty<?>> getPropertiesForItem(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        ActionContext actionContext) {

        ItemType type = inputParameters.getRequired(TYPE, ItemType.class);

        ModifiableStringProperty incomeAccount = string(INCOME_ACCOUNT_REF)
            .label("Income Account")
            .options(getOptions(ACCOUNT, "Income"))
            .required(type == INVENTORY || type == SERVICE);

        ModifiableStringProperty assetAccount = string(ASSET_ACCOUNT_REF)
            .label("Asset Account")
            .options(getOptions(ACCOUNT, "Asset"))
            .required(type == INVENTORY);

        ModifiableDateProperty inventoryStartDate = date(INV_START_DATE)
            .label("Inventory Start Date")
            .description("Date of opening balance for the inventory transaction.")
            .required(type == INVENTORY);

        return List.of(incomeAccount, assetAccount, inventoryStartDate);
    }

    public static OptionsFunction<String> getOptions(Entity entity, String accountType) {
        return (inputParameters, connectionParameters, arrayIndex, searchText, actionContext) -> {

            String encodeQuery = URLEncoder.encode("SELECT * FROM " + entity.getName(), StandardCharsets.UTF_8);

            Map<String, Object> body = actionContext
                .http(http -> http.get("/query"))
                .queryParameter("query", encodeQuery)
                .configuration(Http.responseType(Http.ResponseType.XML))
                .execute()
                .getBody(new TypeReference<>() {});

            List<Option<String>> options = new ArrayList<>();

            if (body.get("QueryResponse") instanceof Map<?, ?> map &&
                map.get(entity.getName()) instanceof List<?> list) {

                for (Object o : list) {
                    if (o instanceof Map<?, ?> entityMap) {
                        String entityAccountType = (String) entityMap.get("AccountType");
                        if (!entity.equals(ACCOUNT) || entityAccountType.equals(accountType)) {
                            String displayValue = getDisplayValue(entity, entityMap);

                            options.add(option(displayValue, (String) entityMap.get(ID)));
                        }
                    }
                }
            }

            return options;
        };
    }

    private static String getDisplayValue(Entity entity, Map<?, ?> entityMap) {
        return switch (entity) {
            case ACCOUNT, ITEM -> (String) entityMap.get(NAME);
            case CUSTOMER -> (String) entityMap.get(DISPLAY_NAME);
            case INVOICE -> (String) entityMap.get("DocNumber");
            case PAYMENT -> (String) entityMap.get(ID);
        };
    }
}
