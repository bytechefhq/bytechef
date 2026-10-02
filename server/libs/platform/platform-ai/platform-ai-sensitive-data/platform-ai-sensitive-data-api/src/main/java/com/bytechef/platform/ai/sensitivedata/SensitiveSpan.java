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

import java.util.Objects;
import java.util.regex.Pattern;

public record SensitiveSpan(SensitiveKind kind, String category, int start, int end, double confidence) {

    private static final Pattern CATEGORY_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]*");

    public SensitiveSpan {
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(category, "category must not be null");

        if (!CATEGORY_PATTERN.matcher(category)
            .matches()) {

            throw new IllegalArgumentException("category must match [A-Z][A-Z0-9_]*, got: " + category);
        }

        if (start < 0) {
            throw new IllegalArgumentException("start must be >= 0");
        }

        if (end <= start) {
            throw new IllegalArgumentException("end must be > start");
        }

        if (!Double.isFinite(confidence) || confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence must be between 0.0 and 1.0");
        }
    }

    public static SensitiveSpan of(SensitiveKind kind, String category, int start, int end) {
        return new SensitiveSpan(kind, category, start, end, 1.0);
    }

    public String placeholder() {
        return "[REDACTED_" + category + "]";
    }

    public int length() {
        return end - start;
    }

    public SensitiveSpan withOffset(int offset) {
        if (offset == 0) {
            return this;
        }

        return new SensitiveSpan(kind, category, start + offset, end + offset, confidence);
    }

    public boolean overlaps(SensitiveSpan other) {
        return start < other.end && end > other.start;
    }

    @Override
    public String toString() {
        return "SensitiveSpan[" + kind + "/" + category + "[" + start + "," + end + ") confidence=" + confidence + "]";
    }
}
