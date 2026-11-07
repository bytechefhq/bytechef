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

package com.bytechef.platform.component.definition.voice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VoiceAgentToolsetTest {

    @Test
    void testEmptyToolsetHasNoDefinitions() {
        assertThat(VoiceAgentToolset.EMPTY.definitions()).isEmpty();
    }

    @Test
    void testEmptyToolsetAnswersUnknownToolWithoutThrowing() {
        assertThat(VoiceAgentToolset.EMPTY.call("lookupOrder", "{}"))
            .isEqualTo(VoiceAgentToolset.unknownTool("lookupOrder"));
    }

    @Test
    void testVoiceAgentClusterElementTypeIsSingleAndRequired() {
        assertThat(VoiceAgentFunction.VOICE_AGENT.key()).isEqualTo("voiceAgent");
        assertThat(VoiceAgentFunction.VOICE_AGENT.multipleElements()).isFalse();
        assertThat(VoiceAgentFunction.VOICE_AGENT.required()).isTrue();
    }
}
