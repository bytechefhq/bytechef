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

import com.bytechef.test.assertion.OrdinalStabilityAssertions;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Pins the ordinal of every {@link AssetFileFormat} value to a fixed integer. The {@code asset_file.format} column
 * stores the ordinal — a reorder, rename, or removal silently re-attributes every historical row.
 *
 * <p>
 * <strong>This test fails on any reorder, rename, or removal.</strong> When you legitimately need to add a new value,
 * <em>append it at the end</em> and add a new entry to the expected map below — that's the only sanctioned change. To
 * remove or reorder, first migrate the column to a stable string code.
 * </p>
 *
 * @author Ivica Cardic
 */
class AssetFileFormatOrdinalStabilityTest {

    @Test
    void testAssetFileFormatOrdinalsAreStable() {
        Map<String, Integer> expected = new LinkedHashMap<>();

        expected.put("MARKDOWN", 0);
        expected.put("CODE", 1);
        expected.put("CSV", 2);
        expected.put("JSON", 3);
        expected.put("DOCX", 4);
        expected.put("PPTX", 5);
        expected.put("CHART", 6);
        expected.put("IMAGE", 7);
        expected.put("HTML", 8);

        OrdinalStabilityAssertions.assertOrdinalsMatch(
            AssetFileFormat.values(), expected, AssetFileFormat.class.getSimpleName());
    }
}
