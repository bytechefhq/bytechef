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

package com.bytechef.component.one.simple.api.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.CURRENCY_OPTIONS;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.FROM_CURRENCY;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.FROM_VALUE;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.TO_CURRENCY;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

/**
 * @author Luka Ljubić
 * @author Monika Kušter
 */
public class OneSimpleAPICurrencyConverterAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("currencyConverter")
        .title("Currency Converter")
        .description("Convert currency from one to another.")
        .properties(
            string(FROM_CURRENCY)
                .label("From Currency")
                .description("Currency from which you want to convert.")
                .options(CURRENCY_OPTIONS)
                .required(true),
            string(TO_CURRENCY)
                .label("To Currency")
                .description("Currency to which you want to convert.")
                .options(CURRENCY_OPTIONS)
                .required(true),
            number(FROM_VALUE)
                .label("Value")
                .description("Value to convert.")
                .defaultValue(1)
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(FROM_CURRENCY),
                        string(FROM_VALUE),
                        string(TO_CURRENCY),
                        number("to_value"),
                        string("to_exchange_rate"))))
        .help("", "https://docs.bytechef.io/reference/components/one-simple-api_v1#currency-converter")
        .perform(OneSimpleAPICurrencyConverterAction::perform);

    private OneSimpleAPICurrencyConverterAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(http -> http.get("/exchange_rate"))
            .queryParameters(
                FROM_CURRENCY, inputParameters.getRequiredString(FROM_CURRENCY),
                TO_CURRENCY, inputParameters.getRequiredString(TO_CURRENCY),
                FROM_VALUE, inputParameters.getRequiredDouble(FROM_VALUE))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
