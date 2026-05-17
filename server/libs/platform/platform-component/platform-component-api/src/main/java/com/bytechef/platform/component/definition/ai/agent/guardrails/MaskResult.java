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

package com.bytechef.platform.component.definition.ai.agent.guardrails;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Result of a {@link PreflightMasking#mask} call: {@link Entities} (per-type entity map merged across preflight
 * guardrails), {@link Masked} (text already rewritten), or {@link Unchanged}. One-way — no unmask round-trip.
 *
 * @author Ivica Cardic
 */
public sealed interface MaskResult permits MaskResult.Entities, MaskResult.Masked, MaskResult.Unchanged {

    Unchanged UNCHANGED = new Unchanged();

    static MaskResult entities(Map<String, List<String>> entities) {
        if (entities == null || entities.isEmpty()) {
            return UNCHANGED;
        }

        Map<String, List<String>> filtered = new LinkedHashMap<>();

        entities.forEach((type, values) -> {
            if (type == null || values == null || values.isEmpty()) {
                return;
            }

            List<String> nonEmpty = new ArrayList<>(values.size());

            for (String value : values) {
                if (value != null && !value.isEmpty()) {
                    nonEmpty.add(value);
                }
            }

            if (!nonEmpty.isEmpty()) {
                filtered.put(type, nonEmpty);
            }
        });

        if (filtered.isEmpty()) {
            return UNCHANGED;
        }

        return new Entities(filtered);
    }

    static MaskResult masked(String text, String originalText) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(originalText, "originalText");

        if (text.equals(originalText)) {
            return UNCHANGED;
        }

        return new Masked(text);
    }

    static MaskResult unchanged() {
        return UNCHANGED;
    }

    record Entities(Map<String, List<String>> entities) implements MaskResult {
        public Entities {
            Objects.requireNonNull(entities, "entities");

            if (entities.isEmpty()) {
                throw new IllegalArgumentException("entities must not be empty");
            }

            Map<String, List<String>> copy = new LinkedHashMap<>(entities.size());

            entities.forEach((type, values) -> {
                Objects.requireNonNull(type, "type");
                Objects.requireNonNull(values, "values");

                copy.put(type, List.copyOf(values));
            });

            entities = Map.copyOf(copy);
        }
    }

    record Masked(String text) implements MaskResult {
        public Masked {
            Objects.requireNonNull(text, "text");
        }
    }

    record Unchanged() implements MaskResult {
    }
}
