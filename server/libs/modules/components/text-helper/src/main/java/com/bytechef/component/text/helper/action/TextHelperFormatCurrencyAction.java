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

package com.bytechef.component.text.helper.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.AMOUNT;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.CURRENCY;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.DECIMAL_DIGITS;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.DECIMAL_SEPARATOR;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.THOUSANDS_SEPARATOR;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.text.helper.util.TextHelperUtils;
import java.util.Currency;

/**
 * @author Monika Kušter
 */
public class TextHelperFormatCurrencyAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("formatCurrency")
        .title("Format Currency")
        .description("Format currency to the specified denomination.")
        .properties(
            string(CURRENCY)
                .label("Currency")
                .description("The type of currency you wish to use.")
                .options(TextHelperUtils.getCurrencyOptions())
                .required(true),
            number(AMOUNT)
                .label("Amount")
                .description("The amount to be formatted.")
                .required(true),
            integer(DECIMAL_DIGITS)
                .label("Decimal Digits")
                .description("Digits you would like after your decimal.")
                .required(true),
            string(DECIMAL_SEPARATOR)
                .label("Decimal Separator")
                .description("The character you would like to use as a decimal separator.")
                .required(true),
            string(THOUSANDS_SEPARATOR)
                .label("Thousands Separator")
                .description("The character you would like to use as a thousands separator.")
                .required(true))
        .output(outputSchema(string().description("Formatted currency.")))
        .help("", "https://docs.bytechef.io/reference/components/text-helper_v1#format-currency")
        .perform(TextHelperFormatCurrencyAction::perform);

    private TextHelperFormatCurrencyAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String formattedAmount = formatAmount(inputParameters);
        String currency = getCurrencySymbol(inputParameters.getRequiredString(CURRENCY));

        return formattedAmount + " " + currency;
    }

    private static String formatAmount(Parameters inputParameters) {
        int decimalDigits = inputParameters.getRequiredInteger(DECIMAL_DIGITS);
        String format = "%,." + decimalDigits + "f";
        double amount = inputParameters.getRequiredDouble(AMOUNT);

        return String.format(format, amount)
            .replace(",", inputParameters.getRequiredString(THOUSANDS_SEPARATOR))
            .replace(".", inputParameters.getRequiredString(DECIMAL_SEPARATOR));
    }

    private static String getCurrencySymbol(String currencyCode) {
        return Currency.getAvailableCurrencies()
            .stream()
            .filter(currency -> currency.getCurrencyCode()
                .equals(currencyCode))
            .findFirst()
            .map(Currency::getSymbol)
            .orElse("");
    }
}
