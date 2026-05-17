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

import com.bytechef.component.ai.agent.guardrails.util.SecretKeyDetectorUtils.Permissiveness;
import com.bytechef.component.ai.agent.guardrails.util.SecretKeyDetectorUtils.SecretMatch;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class SecretKeyDetectorUtilsTokenAllowlistTest {

    @Test
    void bareUrlIsNotFlaggedAsHighEntropyToken() {
        List<SecretMatch> matches = SecretKeyDetectorUtils.detect(
            "see https://internal.example.com/docs/some-guide-here", Permissiveness.BALANCED);

        assertThat(matches)
            .extracting(SecretMatch::type)
            .doesNotContain("HIGH_ENTROPY_TOKEN");
    }

    @Test
    void urlContainingKnownPrefixIsStillFlagged() {
        List<SecretMatch> matches = SecretKeyDetectorUtils.detect(
            "callback https://example.com/AKIAIOSFODNN7EXAMPLE/path", Permissiveness.BALANCED);

        assertThat(matches)
            .extracting(SecretMatch::value)
            .anyMatch(value -> value.contains("AKIAIOSFODNN7EXAMPLE"));
    }

    @Test
    void tokenEndingInAllowedFileExtensionIsSkipped() {
        // A file-path-shaped token with modest entropy; should be skipped at BALANCED level.
        List<SecretMatch> matches = SecretKeyDetectorUtils.detect(
            "import some_long_config_file_identifier.py", Permissiveness.BALANCED);

        assertThat(matches)
            .extracting(SecretMatch::type)
            .doesNotContain("HIGH_ENTROPY_TOKEN");
    }
}
