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

package com.bytechef.platform.ai.a2a;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.platform.ai.a2a.A2AAgentResult.Completed;
import com.bytechef.platform.ai.a2a.A2AAgentResult.Failed;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class A2AAgentResultTest {

    @Test
    void testFailedReplacesAMissingErrorMessage() {
        assertThat(A2AAgentResult.ofFailed(null))
            .isEqualTo(new Failed("The agent failed without an error message", null));
    }

    @Test
    void testCompletedReplacesMissingTextWithAnEmptyString() {
        assertThat(new Completed(null, null)).isEqualTo(new Completed("", null));
    }

    @Test
    void testPendingResultsRequireADurableTaskReference() {
        assertThatThrownBy(() -> A2AAgentResult.ofInputRequired("approve", null))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> A2AAgentResult.ofWorking("running", null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testAgentRequestToStringDoesNotExposeTheServerSecretKey() {
        A2AAgentRequest request = new A2AAgentRequest("server-secret-key", "private text", "context-1", null);

        assertThat(request.toString()).doesNotContain("server-secret-key")
            .doesNotContain("private text")
            .contains("context-1");
    }
}
