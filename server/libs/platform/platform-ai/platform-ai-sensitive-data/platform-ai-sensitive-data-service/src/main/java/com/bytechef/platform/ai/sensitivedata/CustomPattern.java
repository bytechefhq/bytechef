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

package com.bytechef.platform.ai.sensitivedata;

import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public record CustomPattern(
    String type, Pattern pattern, SensitiveKind kind, double score,
    PiiPatternCatalog.@Nullable ContextRule contextRule) {

    public CustomPattern {
        if (!Double.isFinite(score) || score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException("score must be between 0.0 and 1.0, got: " + score);
        }

        if (contextRule != null && contextRule.score() <= score) {
            throw new IllegalArgumentException(
                "a context rule must RAISE confidence: " + type + " scores " + score + " and its rule offers " +
                    contextRule.score());
        }
    }
}
