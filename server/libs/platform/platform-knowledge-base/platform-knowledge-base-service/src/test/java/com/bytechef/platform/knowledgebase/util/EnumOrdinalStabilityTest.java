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

package com.bytechef.platform.knowledgebase.util;

import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSourceStatus;
import com.bytechef.platform.knowledgebase.domain.TombstoneStrategy;
import com.bytechef.test.assertion.OrdinalStabilityAssertions;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class EnumOrdinalStabilityTest {

    @Test
    void testKnowledgeBaseSourceStatusOrdinalsAreStable() {
        Map<String, Integer> expected = new LinkedHashMap<>();

        expected.put("BUILDING_PREVIEW", 0);
        expected.put("PREVIEW", 1);
        expected.put("READY", 2);
        expected.put("FAILED", 3);
        expected.put("DISABLED", 4);

        OrdinalStabilityAssertions.assertOrdinalsMatch(
            KnowledgeBaseSourceStatus.values(), expected, KnowledgeBaseSourceStatus.class.getSimpleName());
    }

    @Test
    void testTombstoneStrategyOrdinalsAreStable() {
        Map<String, Integer> expected = new LinkedHashMap<>();

        expected.put("PERIODIC_FULL_REPLACE", 0);
        expected.put("UPSTREAM_CHANGE_FEED", 1);
        expected.put("NONE", 2);

        OrdinalStabilityAssertions.assertOrdinalsMatch(
            TombstoneStrategy.values(), expected, TombstoneStrategy.class.getSimpleName());
    }
}
