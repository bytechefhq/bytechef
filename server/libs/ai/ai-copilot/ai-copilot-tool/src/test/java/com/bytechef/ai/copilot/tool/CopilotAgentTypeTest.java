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

package com.bytechef.ai.copilot.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ai.copilot.util.Mode;
import com.bytechef.ai.copilot.util.Source;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class CopilotAgentTypeTest {

    @Test
    void testKeysAreUnique() {
        List<String> keys = Arrays.stream(CopilotAgentType.values())
            .map(CopilotAgentType::key)
            .toList();

        assertThat(keys).doesNotHaveDuplicates();
    }

    @Test
    void testChatAgentKeysAreDerivedFromTheSourceAndMode() {
        assertThat(CopilotAgentType.WORKFLOW_CODE_EDITOR_ASK.key())
            .isEqualTo(Source.WORKFLOW_CODE_EDITOR.agentId(Mode.ASK))
            .isEqualTo("workflow_code_editor_ask");
        assertThat(CopilotAgentType.WORKFLOW_EDITOR_EMBEDDED_BUILD.key()).isEqualTo("workflow_editor_embedded_build");
        assertThat(CopilotAgentType.CONVERTER_BUILD.key()).isEqualTo(Source.CONVERTER.agentId(Mode.ASK));
    }

    @Test
    void testNoCopilotAgentTypeIsAFallback() {
        assertThat(CopilotAgentType.values()).noneMatch(CopilotAgentType::isFallback);
    }
}
