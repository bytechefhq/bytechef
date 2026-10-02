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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class SecretPatternCatalogTest {

    private static final Map<String, Double> EXPECTED_SCORES = Map.ofEntries(
        Map.entry("PEM_PRIVATE_KEY", 0.9),
        Map.entry("AWS_ACCESS_KEY", 0.9),
        Map.entry("AWS_SECRET_KEY", 0.6),
        Map.entry("GITHUB_PAT", 0.9),
        Map.entry("GITHUB_FINE_GRAINED_PAT", 0.9),
        Map.entry("SLACK_TOKEN", 0.9),
        Map.entry("STRIPE_SECRET_KEY", 0.9),
        Map.entry("STRIPE_PUBLISHABLE_KEY", 0.9),
        Map.entry("GOOGLE_API_KEY", 0.9),
        Map.entry("OPENAI_KEY", 0.9),
        Map.entry("JWT", 0.9));

    @Test
    void testEveryPatternCarriesAScoreInRange() {
        for (SecretPatternCatalog.SecretPattern secretPattern : SecretPatternCatalog.ALL) {
            assertThat(secretPattern.score())
                .as("score for %s", secretPattern.type())
                .isBetween(0.0, 1.0);
        }
    }

    @Test
    void testEveryScoreIsOneOfTheThreeBandValues() {
        for (SecretPatternCatalog.SecretPattern secretPattern : SecretPatternCatalog.ALL) {
            assertThat(secretPattern.score())
                .as("score for %s", secretPattern.type())
                .isIn(0.2, 0.6, 0.9);
        }
    }

    @Test
    void testEveryCatalogScoreMatchesTheProvenanceTable() {
        assertThat(SecretPatternCatalog.ALL)
            .hasSize(EXPECTED_SCORES.size());

        for (SecretPatternCatalog.SecretPattern secretPattern : SecretPatternCatalog.ALL) {
            Double expected = EXPECTED_SCORES.get(secretPattern.type());

            assertThat(expected)
                .as("no expected score recorded for catalog type %s", secretPattern.type())
                .isNotNull();
            assertThat(secretPattern.score())
                .as("score for %s", secretPattern.type())
                .isEqualTo(expected);
        }
    }

    @Test
    void testCompactConstructorRejectsScoreOutsideZeroToOne() {
        Pattern pattern = Pattern.compile("x");

        assertThatThrownBy(() -> new SecretPatternCatalog.SecretPattern("X", pattern, 1.1))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SecretPatternCatalog.SecretPattern("X", pattern, -0.1))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SecretPatternCatalog.SecretPattern("X", pattern, Double.NaN))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
