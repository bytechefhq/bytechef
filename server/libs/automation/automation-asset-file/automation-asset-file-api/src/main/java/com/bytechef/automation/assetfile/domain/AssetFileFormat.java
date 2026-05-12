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
 * Persisted as a {@code short} ordinal in the {@code asset_file.format} column. Distinct from {@code mime_type}: the
 * format describes the artifact's <em>logical role</em> (a CSV the AI emitted as a chart staging file is
 * {@code format=CHART, mime_type=text/csv}) which the viewer routes on, while {@code mime_type} is the raw payload
 * encoding for download.
 *
 * <p>
 * <strong>Append-only contract.</strong> Reordering, renaming, or removing values silently re-attributes every
 * historical row. {@code AssetFileFormatOrdinalStabilityTest} pins each ordinal — when adding a new value, append it at
 * the end and update the test. To remove or reorder, first migrate the column to a stable string code.
 * </p>
 *
 * @author Ivica Cardic
 */
public enum AssetFileFormat {
    MARKDOWN,
    CODE,
    CSV,
    JSON,
    DOCX,
    PPTX,
    CHART,
    IMAGE,
    HTML;

    /**
     * Resolves a stored ordinal to its enum value. Named {@code fromOrdinal} rather than {@code valueOf} to avoid
     * shadowing {@link Enum#valueOf(Class, String)} and to make the call site self-explanatory.
     */
    public static AssetFileFormat fromOrdinal(short ordinal) {
        AssetFileFormat[] values = values();

        if (ordinal < 0 || ordinal >= values.length) {
            throw new IllegalArgumentException("Invalid AssetFileFormat ordinal: " + ordinal);
        }

        return values[ordinal];
    }
}
