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

package com.bytechef.hermes.component.definition;

import com.bytechef.commons.util.MapUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

public class AbstractParameters {

    private final Map<String, List<String>> parameters;

    public AbstractParameters(Map<String, String[]> parameters) {
        this.parameters = MapUtils.toMap(parameters, Map.Entry::getKey, entry -> Arrays.asList(entry.getValue()));
    }

    public List<String> allValues(String name) {
        return parameters.values()
            .stream()
            .flatMap(List::stream)
            .toList();
    }

    public Optional<String> firstValue(String name) {
        Optional<String> optional = Optional.empty();

        if (parameters.containsKey(name)) {
            List<String> values = parameters.get(name);

            if (values != null && !values.isEmpty()) {
                optional = Optional.of(values.get(0));
            }
        }

        return optional;
    }

    public OptionalLong firstValueAsLong(String name) {
        return firstValue(name).stream()
            .mapToLong(Long::valueOf)
            .findFirst();
    }

    public Map<String, List<String>> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }
}
