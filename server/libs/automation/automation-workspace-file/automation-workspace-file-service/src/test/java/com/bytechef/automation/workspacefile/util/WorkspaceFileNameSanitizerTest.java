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

package com.bytechef.automation.workspacefile.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class WorkspaceFileNameSanitizerTest {

    @Test
    void testStripsPathSeparators() {
        assertThat(WorkspaceFileNameSanitizer.sanitize("../etc/passwd")).isEqualTo("..etcpasswd");
    }

    @Test
    void testStripsControlChars() {
        assertThat(WorkspaceFileNameSanitizer.sanitize("ok\tname.md")).isEqualTo("okname.md");
    }

    @Test
    void testClampsToTwoHundredFiftyFive() {
        String input = "a".repeat(300) + ".md";

        assertThat(WorkspaceFileNameSanitizer.sanitize(input)).hasSize(255);
    }

    @Test
    void testReturnsUntitledWhenEmptyAfterSanitize() {
        assertThat(WorkspaceFileNameSanitizer.sanitize("/")).isEqualTo("untitled");
    }

    @Test
    void testReturnsUntitledWhenNull() {
        assertThat(WorkspaceFileNameSanitizer.sanitize(null)).isEqualTo("untitled");
    }
}
