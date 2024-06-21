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

package com.bytechef.component.one.simple.api.util;

import static com.bytechef.component.definition.ComponentDSL.option;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Luka Ljubić
 */
public class OneSimpleAPIUtils {

    private OneSimpleAPIUtils() {
    }

    public static List<Option<String>> getCurrencyOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, ActionContext context) {

        List<Option<String>> options = new ArrayList<>();

        options.add(option("Euro", "EUR"));
        options.add(option("Australian Dollar", "AUD"));
        options.add(option("Bosnia and Herzegovina Mark", "BAM"));
        options.add(option("Brazilian Real", "BRL"));
        options.add(option("Canadian Dollar", "CAD"));
        options.add(option("Swiss Franc", "CHF"));
        options.add(option("Czech Koruna", "CZK"));
        options.add(option("Danish Krone", "DKK"));
        options.add(option("Hong Kong Dollar", "HKD"));
        options.add(option("Japanese Yen", "JPY"));
        options.add(option("Mexican Peso", "MXN"));
        options.add(option("Serbian Dinar", "RSD"));
        options.add(option("Russian Ruble", "RUB"));
        options.add(option("United States Dollar", "USD"));

        return options;
    }
}
