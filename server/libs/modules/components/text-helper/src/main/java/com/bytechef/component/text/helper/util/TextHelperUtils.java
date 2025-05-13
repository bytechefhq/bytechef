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

package com.bytechef.component.text.helper.util;

import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.definition.Option;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Monika Kušter
 */
public class TextHelperUtils {

    private TextHelperUtils() {
    }

    public static List<Option<String>> getCurrencyOptions() {
        return Currency.getAvailableCurrencies()
            .stream()
            .sorted(Comparator.comparing(Currency::getCurrencyCode))
            .map(currency -> option(currency.getCurrencyCode(), currency.getCurrencyCode()))
            .collect(Collectors.toList());
    }
}
