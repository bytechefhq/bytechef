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

package com.bytechef.component.ai.agent.guardrails.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.ai.agent.guardrails.util.SecretKeyDetector.Permissiveness;
import com.bytechef.component.ai.agent.guardrails.util.SecretKeyDetector.SecretMatch;
import java.util.List;
import org.junit.jupiter.api.Test;

class SecretKeyDetectorPrefixTest {

    @Test
    void prefixedTokenIsFlaggedEvenAtPermissiveLevel() {
        List<SecretMatch> matches = SecretKeyDetector.detect("key=AKIAIOSFODNN7EXAMPLE", Permissiveness.PERMISSIVE);

        assertThat(matches)
            .extracting(SecretMatch::type)
            .contains("PREFIXED_SECRET");
    }

    @Test
    void shortOpenAiKeyIsFlaggedByPrefixEvenIfEntropyBelowThreshold() {
        List<SecretMatch> matches = SecretKeyDetector.detect("config sk-abcdef", Permissiveness.BALANCED);

        assertThat(matches)
            .extracting(SecretMatch::type)
            .contains("PREFIXED_SECRET");
    }

    @Test
    void nonPrefixedShortTokenIsNotFlagged() {
        List<SecretMatch> matches = SecretKeyDetector.detect("value=abcdef", Permissiveness.BALANCED);

        assertThat(matches)
            .extracting(SecretMatch::type)
            .doesNotContain("PREFIXED_SECRET");
    }

    @Test
    void githubPatTriggersBothNamedAndPrefixedMatches() {
        String ghp = "ghp_" + "A".repeat(36);

        List<SecretMatch> matches = SecretKeyDetector.detect("token " + ghp, Permissiveness.BALANCED);

        assertThat(matches)
            .extracting(SecretMatch::type)
            .contains("PREFIXED_SECRET");
    }
}
