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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Package-private helpers for {@link Violation#info()} sanitization. Keeps the internal-key list and the stripping
 * logic off the public SDK surface — these are plumbing concerns between the advisor and the violation record, not
 * something external guardrail implementers should see or depend on.
 *
 * <p>
 * Previously lived as a {@code public Set<String> INTERNAL_INFO_KEYS} on {@link Violation}. That exposed the list as
 * part of the SDK contract (a binary-compat trap — any future addition to the set would silently change what survives
 * on the wire for downstream readers who had snapshotted the constant).
 */
final class ViolationInfo {

    /**
     * Keys that describe internal preflight artefacts (e.g. {@code maskEntities}) and must never appear on a
     * {@link Violation}'s {@link Violation#info()} map. These keys are stripped at record-construction time so the
     * invariant "{@code info()} is safe to serialize" is enforced by the type rather than left to each caller.
     *
     * <p>
     * Add new keys here when a guardrail adds a new internal-only diagnostic. Package-private on purpose — extending
     * the list is a ByteChef-internal decision, not an SDK extension point.
     */
    static final Set<String> INTERNAL_INFO_KEYS = Set.of("maskEntities");

    private ViolationInfo() {
    }

    /**
     * Strip {@link #INTERNAL_INFO_KEYS} from a caller-supplied map and return an immutable copy. Used by the record
     * compact constructors so no Violation can hold a map that includes internal keys.
     */
    static Map<String, Object> sanitize(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }

        boolean containsInternal = false;

        for (String key : INTERNAL_INFO_KEYS) {
            if (source.containsKey(key)) {
                containsInternal = true;

                break;
            }
        }

        if (!containsInternal) {
            return Map.copyOf(source);
        }

        Map<String, Object> filtered = new LinkedHashMap<>(source);

        INTERNAL_INFO_KEYS.forEach(filtered::remove);

        return Map.copyOf(filtered);
    }
}
