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
 * Result of a single {@link PreflightMasking#mask(String, GuardrailContext) mask} call. Closed set of exactly three
 * outcomes, so the advisor can exhaustively switch on the result and no dispatch flag is needed:
 * <ul>
 * <li>{@link Entities} — the guardrail emits a per-type entity map; the advisor merges it across every preflight
 * guardrail and applies a single length-sorted global replacement pass.</li>
 * <li>{@link Masked} — the guardrail has already rewritten the text in place; the advisor uses the returned text as the
 * next stage's input without further processing.</li>
 * <li>{@link Unchanged} — no masking occurred; the advisor forwards the previous intermediate text unchanged.</li>
 * </ul>
 *
 * @author Ivica Cardic
 */
public sealed interface MaskResult permits MaskResult.Entities, MaskResult.Masked, MaskResult.Unchanged {

    Unchanged UNCHANGED = new Unchanged();

    /**
     * Per-type entity map — the advisor merges these across preflight guardrails, sorts longest-first, and applies the
     * merged mask globally so overlapping matches do not split each other.
     */
    record Entities(Map<String, List<String>> entities) implements MaskResult {
        public Entities {
            Objects.requireNonNull(entities, "entities must not be null");

            Map<String, List<String>> copy = new LinkedHashMap<>(entities.size());

            entities.forEach((type, values) -> {
                Objects.requireNonNull(type, "entity type must not be null");
                Objects.requireNonNull(values, "entity values must not be null");

                copy.put(type, List.copyOf(values));
            });

            entities = Map.copyOf(copy);
        }
    }

    /** The guardrail has rewritten the text; the advisor uses {@code text()} as the next stage's input. */
    record Masked(String text) implements MaskResult {
        public Masked {
            Objects.requireNonNull(text, "masked text must not be null");
        }
    }

    /** Singleton "no-op" outcome. */
    record Unchanged() implements MaskResult {
    }

    /**
     * Factory for the entity-map outcome. Returns {@link #unchanged()} on a null or empty input map so callers do not
     * need to short-circuit — the advisor treats the result uniformly.
     */
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

    /**
     * Factory for the in-place masked outcome. Returns {@link #unchanged()} when {@code text} equals
     * {@code originalText} so apply-style guardrails do not need to short-circuit. {@code text} must not be null.
     */
    static MaskResult masked(String text, String originalText) {
        Objects.requireNonNull(text, "masked text must not be null");

        if (text.equals(originalText)) {
            return UNCHANGED;
        }

        return new Masked(text);
    }

    /** Singleton "no-op" outcome. */
    static MaskResult unchanged() {
        return UNCHANGED;
    }
}
