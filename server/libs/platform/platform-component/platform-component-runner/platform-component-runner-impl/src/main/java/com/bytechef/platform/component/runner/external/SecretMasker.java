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

package com.bytechef.platform.component.runner.external;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Replaces known secret values in captured text. Best effort: a transformed value is not recognised.
 *
 * @author Ivica Cardic
 */
public final class SecretMasker {

    public static final SecretMasker NONE = new SecretMasker(List.of());

    static final String MASK = "******";
    static final int MINIMUM_LENGTH = 6;

    private static final Pattern LINE_BREAK = Pattern.compile("\\r?\\n|\\r");

    private final List<String> values;

    private SecretMasker(List<String> values) {
        this.values = values;
    }

    public static SecretMasker of(Collection<String> values) {
        List<String> candidates = new ArrayList<>();

        for (String value : values) {
            if (value == null) {
                continue;
            }

            candidates.add(value);

            if (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
                for (String line : LINE_BREAK.split(value)) {
                    candidates.add(line.trim());
                }
            }
        }

        List<String> eligibleValues = candidates.stream()
            .filter(Objects::nonNull)
            .filter(value -> value.length() >= MINIMUM_LENGTH)
            .distinct()
            .sorted(Comparator.comparingInt(String::length)
                .reversed())
            .toList();

        return eligibleValues.isEmpty() ? NONE : new SecretMasker(eligibleValues);
    }

    public String mask(String text) {
        if (values.isEmpty() || text.isEmpty()) {
            return text;
        }

        String masked = text;

        for (String value : values) {
            masked = masked.replace(value, MASK);
        }

        return masked;
    }
}
