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

package com.bytechef.automation.ai.a2a.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.commons.data.jdbc.wrapper.MapWrapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Ivica Cardic
 */
class A2aProjectWorkflowTest {

    @Test
    void testUnsetSkillMetadataReadsAsAbsent() {
        A2aProjectWorkflow a2aProjectWorkflow = new A2aProjectWorkflow(1L, 2L);

        assertThat(a2aProjectWorkflow.getSkillName()).isNull();
        assertThat(a2aProjectWorkflow.getSkillDescription()).isNull();
    }

    @Test
    void testBlankSkillMetadataClearsTheOverride() {
        A2aProjectWorkflow a2aProjectWorkflow = new A2aProjectWorkflow(1L, 2L);

        a2aProjectWorkflow.setSkillName("Summarize");
        a2aProjectWorkflow.setSkillDescription("Summarizes text");

        a2aProjectWorkflow.setSkillName(" ");
        a2aProjectWorkflow.setSkillDescription("");

        assertThat(a2aProjectWorkflow.getSkillName()).isNull();
        assertThat(a2aProjectWorkflow.getSkillDescription()).isNull();
        assertThat(a2aProjectWorkflow.getParameters()).isEmpty();
    }

    @Test
    void testSkillSettersKeepUnrelatedParameters() {
        A2aProjectWorkflow a2aProjectWorkflow = new A2aProjectWorkflow(1L, 2L);

        setStoredParameters(a2aProjectWorkflow, Map.of("other", "value"));

        a2aProjectWorkflow.setSkillName("Summarize");

        Map<String, ?> parameters = a2aProjectWorkflow.getParameters();

        assertThat(parameters.get("other")).isEqualTo("value");
        assertThat(parameters.get("skillName")).isEqualTo("Summarize");
    }

    @Test
    void testStoredSkillMetadataIsReadBack() {
        A2aProjectWorkflow a2aProjectWorkflow = new A2aProjectWorkflow(1L, 2L);

        setStoredParameters(
            a2aProjectWorkflow, Map.of("skillDescription", "Translates text", "skillName", "Translate"));

        assertThat(a2aProjectWorkflow.getSkillName()).isEqualTo("Translate");
        assertThat(a2aProjectWorkflow.getSkillDescription()).isEqualTo("Translates text");
    }

    private static void setStoredParameters(A2aProjectWorkflow a2aProjectWorkflow, Map<String, ?> parameters) {
        ReflectionTestUtils.setField(a2aProjectWorkflow, "parameters", new MapWrapper(parameters));
    }
}
