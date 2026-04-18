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

class SecretKeyDetectorFileExtensionTest {

    @Test
    void tokensInsideAllowedFileExtensionBlockAreIgnored() {
        String content = "```py\nMY_AWS_KEY = \"AKIAIOSFODNN7EXAMPLE\"\n```\n"
            + "\nAnd prose AKIAIOSFODNN7ANOTHER too.";

        List<SecretMatch> matches = SecretKeyDetector.detect(
            content, Permissiveness.BALANCED, List.of(), List.of("py"));

        assertThat(matches)
            .extracting(SecretMatch::value)
            .contains("AKIAIOSFODNN7ANOTHER")
            .doesNotContain("AKIAIOSFODNN7EXAMPLE");
    }

    @Test
    void tokensInsideNonAllowedExtensionBlockAreStillScanned() {
        String content = "```rust\nMY_AWS_KEY = \"AKIAIOSFODNN7EXAMPLE\"\n```";

        List<SecretMatch> matches = SecretKeyDetector.detect(
            content, Permissiveness.BALANCED, List.of(), List.of("py"));

        assertThat(matches)
            .extracting(SecretMatch::value)
            .contains("AKIAIOSFODNN7EXAMPLE");
    }

    @Test
    void emptyExtensionListScansEverything() {
        String content = "```py\nAKIAIOSFODNN7EXAMPLE\n```";

        List<SecretMatch> matches = SecretKeyDetector.detect(
            content, Permissiveness.BALANCED, List.of(), List.of());

        assertThat(matches)
            .extracting(SecretMatch::value)
            .contains("AKIAIOSFODNN7EXAMPLE");
    }

    @Test
    void multipleExtensionsAllRespected() {
        String content = "```py\nAKIAIOSFODNN7EXAMPLE\n```\n"
            + "```js\nghp_abcdefghijklmnopqrstuvwxyz0123456789\n```";

        List<SecretMatch> matches = SecretKeyDetector.detect(
            content, Permissiveness.BALANCED, List.of(), List.of("py", "js"));

        assertThat(matches)
            .extracting(SecretMatch::value)
            .doesNotContain("AKIAIOSFODNN7EXAMPLE", "ghp_abcdefghijklmnopqrstuvwxyz0123456789");
    }
}
