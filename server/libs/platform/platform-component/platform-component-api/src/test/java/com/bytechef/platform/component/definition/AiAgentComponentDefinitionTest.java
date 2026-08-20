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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AiAgentComponentDefinitionTest {

    /**
     * Pins the ITERATION ORDER, not just the contents. The generated {@code ai-agent_v1.json} definition snapshot
     * serialises this map verbatim, and {@code Map.of} — which this used to be — randomises its order per JVM run, so
     * every regeneration reshuffled these six keys and produced a spurious diff. {@code JsonFileAssert} compares JSON
     * objects order-insensitively and so cannot catch that; this test is the only thing that does.
     */
    @Test
    void testClusterElementClusterElementTypesIterationOrderIsDeterministic() {
        // CALLS_REAL_METHODS so the interface's own default method runs; the rest of ComponentDefinition is abstract
        // and irrelevant here.
        AiAgentComponentDefinition aiAgentComponentDefinition =
            mock(AiAgentComponentDefinition.class, CALLS_REAL_METHODS);

        Map<String, List<String>> clusterElementClusterElementTypes =
            aiAgentComponentDefinition.getClusterElementClusterElementTypes();

        assertThat(clusterElementClusterElementTypes.keySet()).containsExactly(
            "checkForViolations", "sanitizeText", "jailbreak", "nsfw", "topicalAlignment", "custom");
    }
}
