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

package com.bytechef.platform.notification.handler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Replaces the resolved secrets of a notification in the text a test send returns, so a receiver that echoes a header
 * value, the signing secret or the Slack URL does not show it to the admin.
 *
 * @author Ivica Cardic
 */
final class TestSendRedaction {

    static final String MASK = "********";

    private static final int MIN_SECRET_LENGTH = 4;

    private final List<String> secrets;

    private TestSendRedaction(List<String> secrets) {
        this.secrets = secrets;
    }

    /**
     * Collects every value whose trimmed length is at least 4, both as given and trimmed; shorter values would replace
     * ordinary text.
     */
    static TestSendRedaction of(List<@Nullable String> values) {
        Set<String> secrets = new LinkedHashSet<>();

        for (String value : values) {
            if (value == null) {
                continue;
            }

            String strippedValue = value.strip();

            if (strippedValue.length() >= MIN_SECRET_LENGTH) {
                secrets.add(value);
                secrets.add(strippedValue);
            }
        }

        List<String> sortedSecrets = new ArrayList<>(secrets);

        sortedSecrets.sort(Comparator.comparingInt(String::length)
            .reversed());

        return new TestSendRedaction(sortedSecrets);
    }

    @Nullable
    String redact(@Nullable String text) {
        if (text == null) {
            return null;
        }

        String redactedText = text;

        for (String secret : secrets) {
            redactedText = redactedText.replace(secret, MASK);
        }

        return redactedText;
    }
}
