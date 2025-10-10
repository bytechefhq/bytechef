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

package com.bytechef.component.binance.action;

import static com.bytechef.component.binance.constant.BinanceConstants.SYMBOL;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.binance.util.BinanceUtils;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

/**
 * @author Marija Horvat
 */
public class BinanceFetchPairPriceAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("fetchPairPrice")
        .title("Fetch Pair Price")
        .description("Fetch the price of a crypto pair from Binance.")
        .properties(
            string(SYMBOL)
                .label("Symbol")
                .description("The symbol of the crypto pair.")
                .options((OptionsFunction<String>) BinanceUtils::getSymbolsOptions)
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(SYMBOL)
                            .description("The symbol of the crypto pair."),
                        string("price")
                            .description("The price of the crypto pair."))))
        .perform(BinanceFetchPairPriceAction::perform);

    private BinanceFetchPairPriceAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(http -> http.get("https://api.binance.com/api/v3/ticker/price"))
            .queryParameter(SYMBOL, inputParameters.getRequiredString(SYMBOL))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
