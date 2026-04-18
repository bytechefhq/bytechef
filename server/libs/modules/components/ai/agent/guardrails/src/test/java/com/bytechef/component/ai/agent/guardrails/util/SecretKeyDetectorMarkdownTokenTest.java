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

class SecretKeyDetectorMarkdownTokenTest {

    @Test
    void markdownEmphasisAroundPrefixedSecretStillFlagged() {
        String content = "config: **sk-abc123defghij0987654321xyz** and done";

        List<SecretMatch> matches = SecretKeyDetector.detect(content, Permissiveness.BALANCED);

        assertThat(matches)
            .extracting(SecretMatch::type)
            .contains("PREFIXED_SECRET");
    }

    @Test
    void hashDecoratedPrefixedSecretIsStillFlagged() {
        String content = "env: # sk-abc123defghij0987654321xyz # end";

        List<SecretMatch> matches = SecretKeyDetector.detect(content, Permissiveness.BALANCED);

        assertThat(matches)
            .extracting(SecretMatch::type)
            .contains("PREFIXED_SECRET");
    }

    @Test
    void strictModeBypassesUrlShapeAllowlist() {
        // URL-shaped token with high entropy: BALANCED shape-allowlists it, STRICT does not.
        String content = "ping https://a.b.c.d/x4f9Q7pLk8mRnTsW1yZ2cDeFgHi3JkLmNp";

        List<SecretMatch> balanced = SecretKeyDetector.detect(content, Permissiveness.BALANCED);
        List<SecretMatch> strict = SecretKeyDetector.detect(content, Permissiveness.STRICT);

        assertThat(balanced)
            .extracting(SecretMatch::type)
            .doesNotContain("HIGH_ENTROPY_TOKEN");
        assertThat(strict)
            .extracting(SecretMatch::type)
            .contains("HIGH_ENTROPY_TOKEN");
    }
}
