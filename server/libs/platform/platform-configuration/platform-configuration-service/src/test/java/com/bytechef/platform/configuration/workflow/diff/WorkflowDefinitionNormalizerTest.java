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

package com.bytechef.platform.configuration.workflow.diff;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowDefinitionNormalizerTest {

    @Test
    void testNormalizeSortsKeysAtEveryLevel() {
        Workflow workflow = new Workflow(
            "{\"tasks\":[{\"type\":\"a/v1/b\",\"name\":\"t1\",\"parameters\":{\"z\":1,\"a\":2}}],\"label\":\"L\"}",
            Workflow.Format.JSON);

        String json = WorkflowDefinitionNormalizer.toJson(WorkflowDefinitionNormalizer.normalize(workflow));

        assertThat(json.indexOf("\"label\"")).isLessThan(json.indexOf("\"tasks\""));
        assertThat(json.indexOf("\"a\"")).isLessThan(json.indexOf("\"z\""));
        assertThat(json.indexOf("\"name\"")).isLessThan(json.indexOf("\"parameters\""));
    }

    @Test
    void testNormalizeKeepsListOrder() {
        Workflow workflow = new Workflow(
            "{\"tasks\":[{\"name\":\"t2\",\"type\":\"a/v1/b\"},{\"name\":\"t1\",\"type\":\"a/v1/b\"}]}",
            Workflow.Format.JSON);

        List<?> tasks = (List<?>) WorkflowDefinitionNormalizer.normalize(workflow)
            .get("tasks");

        assertThat(((Map<?, ?>) tasks.get(0)).get("name")).isEqualTo("t2");
        assertThat(((Map<?, ?>) tasks.get(1)).get("name")).isEqualTo("t1");
    }

    @Test
    void testNormalizeStripsTaskNumber() {
        Workflow workflow = new Workflow(
            "{\"tasks\":[{\"name\":\"t1\",\"type\":\"a/v1/b\"}]}", Workflow.Format.JSON);

        String json = WorkflowDefinitionNormalizer.toJson(WorkflowDefinitionNormalizer.normalize(workflow));

        assertThat(json).doesNotContain("taskNumber");
    }

    @Test
    void testNormalizeParsesYaml() {
        Workflow workflow = new Workflow("label: L\ntasks: []\n", Workflow.Format.YAML);

        assertThat(WorkflowDefinitionNormalizer.normalize(workflow)).containsEntry("label", "L");
    }

    @Test
    void testToJsonPrintsPlainMaskForMaskedValues() {
        Workflow workflow = new Workflow(
            "{\"tasks\":[{\"name\":\"t1\",\"type\":\"a/v1/b\",\"parameters\":{\"password\":\"x\"}}]}",
            Workflow.Format.JSON);

        String json = WorkflowDefinitionNormalizer.toJson(WorkflowDefinitionNormalizer.normalize(workflow));

        assertThat(json).contains("\"" + SensitiveValueMasker.MASK + "\"")
            .doesNotContain(SensitiveValueMasker.MASK + ":");
    }
}
