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

package com.bytechef.automation.assetfile.domain;

/**
 * Persisted as an {@code int} ordinal in the {@code asset_file.source} column.
 *
 * <p>
 * <strong>Append-only contract.</strong> Reordering, renaming, or removing values silently re-attributes every
 * historical row. {@code AssetFileSourceOrdinalStabilityTest} pins each ordinal — when adding a new value, append it at
 * the end and update the test. To remove or reorder, first migrate the column to a stable string code.
 * </p>
 *
 * @author Ivica Cardic
 */
public enum AssetFileSource {
    USER_UPLOAD,
    AI_GENERATED;

    /**
     * Resolves a stored ordinal to its enum value. Named {@code fromOrdinal} rather than {@code valueOf} to avoid
     * shadowing {@link Enum#valueOf(Class, String)} and to make the call site self-explanatory.
     */
    public static AssetFileSource fromOrdinal(short ordinal) {
        AssetFileSource[] values = values();

        if (ordinal < 0 || ordinal >= values.length) {
            throw new IllegalArgumentException("Invalid AssetFileSource ordinal: " + ordinal);
        }

        return values[ordinal];
    }
}
