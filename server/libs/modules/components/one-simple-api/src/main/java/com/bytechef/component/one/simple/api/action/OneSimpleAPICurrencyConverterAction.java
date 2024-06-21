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

package com.bytechef.component.one.simple.api.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.ACCESS_TOKEN;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.BASE_URL;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.CURRENCY_CONVERTER;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.FROM_CURRENCY;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.FROM_VALUE;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.TO_CURRENCY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.one.simple.api.util.OneSimpleAPIUtils;

/**
 * @author Luka Ljubić
 */
public class OneSimpleAPICurrencyConverterAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CURRENCY_CONVERTER)
        .title("Currency Converter")
        .description("Convert your currency into any other")
        .properties(
            string(FROM_CURRENCY)
                .options((ActionOptionsFunction<String>) OneSimpleAPIUtils::getCurrencyOptions)
                .label("From Currency")
                .description("Select a currency from which you want to convert")
                .required(true),
            string(TO_CURRENCY)
                .options((ActionOptionsFunction<String>) OneSimpleAPIUtils::getCurrencyOptions)
                .label("To Currency")
                .description("Select a currency to which you want to convert")
                .required(true),
            number(FROM_VALUE)
                .defaultValue(0)
                .label("Value")
                .description("Input the number for conversion")
                .required(true))
        .outputSchema(
            object()
                .properties(
                    string(FROM_CURRENCY),
                    string(TO_CURRENCY),
                    string(TO_CURRENCY),
                    string("to_value")))
        .perform(OneSimpleAPICurrencyConverterAction::perform);

    private OneSimpleAPICurrencyConverterAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return context.http(http -> http.get(BASE_URL + "/exchange_rate"))
            .body(
                Body.of(
                    ACCESS_TOKEN, connectionParameters.getRequiredString(ACCESS_TOKEN),
                    FROM_CURRENCY, inputParameters.getRequiredString(FROM_CURRENCY),
                    TO_CURRENCY, inputParameters.getRequiredString(TO_CURRENCY),
                    FROM_VALUE, inputParameters.getRequiredString(FROM_VALUE),
                    "output", "json"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
