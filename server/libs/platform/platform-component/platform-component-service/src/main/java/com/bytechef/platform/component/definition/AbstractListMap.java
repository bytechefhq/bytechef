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

package com.bytechef.platform.component.definition;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * @author Ivica Cardic
 */
public class AbstractListMap extends HashMap<String, List<String>> {

    public AbstractListMap(Map<String, List<String>> parameters) {
        super(parameters);
    }

    public List<String> allValues(String name) {
        return values()
            .stream()
            .flatMap(List::stream)
            .toList();
    }

    public Optional<String> firstValue(String name) {
        Optional<String> optional = Optional.empty();

        if (containsKey(name)) {
            List<String> values = get(name);

            if (values != null && !values.isEmpty()) {
                optional = Optional.of(values.getFirst());
            }
        }

        return optional;
    }

    public OptionalLong firstValueAsLong(String name) {
        return firstValue(name).stream()
            .mapToLong(Long::valueOf)
            .findFirst();
    }

    public Map<String, List<String>> toMap() {
        return Collections.unmodifiableMap(this);
    }
}
