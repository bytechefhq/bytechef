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
import static com.bytechef.component.text.helper.constant.TextHelperConstants.DISPLAY_NAME;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.DOMAIN;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.EMAIL;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.LOCAL_PART;

import com.bytechef.component.definition.Option;
import com.bytechef.component.exception.ProviderException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Monika Kušter
 * @author Nikolina Špehar
 */
public class TextHelperUtils {

    private TextHelperUtils() {
    }

    public static int getPatternEndIndex(String pattern, String text, int matchNumber) {
        int index;
        int fromIndex = 0;

        for (int i = 0; i < matchNumber; i++) {
            index = text.indexOf(pattern, fromIndex);

            if (index == -1) {
                throw new ProviderException("No pattern was found in text or match number is too large.");
            }

            fromIndex = index + pattern.length();
        }

        return fromIndex;
    }

    public static List<String> extractByRegEx(String text, String regularExpression) {
        Pattern pattern = Pattern.compile(regularExpression);
        Matcher matcher = pattern.matcher(text);

        List<String> extractedStrings = new ArrayList<>();

        while (matcher.find()) {
            extractedStrings.add(matcher.group());
        }

        return extractedStrings;
    }

    public static List<Option<String>> getCurrencyOptions() {
        return Currency.getAvailableCurrencies()
            .stream()
            .sorted(Comparator.comparing(Currency::getCurrencyCode))
            .map(currency -> option(currency.getCurrencyCode(), currency.getCurrencyCode()))
            .collect(Collectors.toList());
    }

    public static Map<String, String> parseEmail(String emailInput) {
        Pattern pattern = Pattern.compile("\\s*(?:(.*?)\\s*<)?([\\w.+-]+)@([\\w.-]+)>?\\s*");
        Matcher matcher = pattern.matcher(emailInput);

        if (matcher.matches()) {
            String displayName = matcher.group(1) != null ? matcher.group(1) : "";
            String localPart = matcher.group(2);
            String domain = matcher.group(3);

            String email = localPart + "@" + domain;

            return Map.of(DISPLAY_NAME, displayName, LOCAL_PART, localPart, DOMAIN, domain, EMAIL, email);
        } else {
            throw new ProviderException("Invalid email format: " + emailInput);
        }
    }
}
