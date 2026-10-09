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

package com.bytechef.platform.component.definition;

import static com.bytechef.platform.component.definition.ai.agent.ModelFunction.MODEL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class SessionChatMemoryComponentDefinitionTest {

    @Test
    void testTheOnlyChildIsAnOptionalSummarizerModel() {
        SessionChatMemoryComponentDefinition sessionChatMemoryComponentDefinition =
            mock(SessionChatMemoryComponentDefinition.class, CALLS_REAL_METHODS);

        List<ClusterElementType> clusterElementTypes = sessionChatMemoryComponentDefinition.getClusterElementTypes();

        assertThat(clusterElementTypes).containsExactly(SessionChatMemoryComponentDefinition.SUMMARIZER_MODEL);
        assertThat(SessionChatMemoryComponentDefinition.SUMMARIZER_MODEL.required()).isFalse();
    }

    @Test
    void testTheSummarizerModelIsTheModelType() {
        ClusterElementType summarizerModel = SessionChatMemoryComponentDefinition.SUMMARIZER_MODEL;

        assertThat(summarizerModel.name()).isEqualTo(MODEL.name());
        assertThat(summarizerModel.key()).isEqualTo(MODEL.key());
        assertThat(summarizerModel.label()).isEqualTo(MODEL.label());
    }
}
