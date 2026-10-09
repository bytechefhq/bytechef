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

package com.bytechef.component.ai.agent.chat.memory.aws;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.SessionChatMemoryComponentDefinition;
import com.bytechef.test.jsonasssert.JsonFileAssert;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AwsChatMemoryV2ComponentHandlerTest {

    @Test
    void testGetComponentDefinition() {
        JsonFileAssert.assertEquals(
            "definition/aws-chat-memory_v2.json", new AwsChatMemoryV2ComponentHandler().getDefinition());
    }

    @Test
    void testIsVersionTwoOfAwsChatMemoryWithAnOptionalSummarizerModel() {
        ComponentDefinition componentDefinition = new AwsChatMemoryV2ComponentHandler().getDefinition();

        assertThat(componentDefinition.getName()).isEqualTo("awsChatMemory");
        assertThat(componentDefinition.getVersion()).isEqualTo(2);
        assertThat(((SessionChatMemoryComponentDefinition) componentDefinition).getClusterElementTypes())
            .containsExactly(SessionChatMemoryComponentDefinition.SUMMARIZER_MODEL);
    }

    @Test
    void testSharesTheConnectionOfVersionOne() {
        ComponentDefinition versionOneComponentDefinition = new AwsChatMemoryComponentHandler().getDefinition();
        ComponentDefinition versionTwoComponentDefinition = new AwsChatMemoryV2ComponentHandler().getDefinition();

        assertThat(versionTwoComponentDefinition.getConnection()
            .orElseThrow()).isSameAs(
                versionOneComponentDefinition.getConnection()
                    .orElseThrow());
    }
}
