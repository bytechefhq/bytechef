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
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class SecretKeyDetectorCustomRegexTest {

    @Test
    void testCustomRegexAdditionsAreIncludedInDetection() {
        List<Pattern> extra = List.of(Pattern.compile("\\bMY-INTERNAL-\\d{6}\\b"));

        List<SecretMatch> matches = SecretKeyDetector.detect(
            "leaked: MY-INTERNAL-987654", Permissiveness.PERMISSIVE, extra);

        assertThat(matches).extracting(SecretMatch::type)
            .contains("CUSTOM");
        assertThat(matches).extracting(SecretMatch::value)
            .contains("MY-INTERNAL-987654");
    }

    @Test
    void testCustomRegexNullDoesNotBreakDetection() {
        List<SecretMatch> matches = SecretKeyDetector.detect("AKIA0123456789ABCDEF", Permissiveness.PERMISSIVE, null);

        assertThat(matches).extracting(SecretMatch::type)
            .contains("AWS_ACCESS_KEY");
    }
}
