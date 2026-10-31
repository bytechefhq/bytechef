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
import com.bytechef.platform.configuration.dto.WorkflowChangeType;
import com.bytechef.platform.configuration.dto.WorkflowDefinitionDiff;
import com.bytechef.platform.configuration.service.WorkflowDefinitionDiffService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowDefinitionDiffServiceTest {

    private final WorkflowDefinitionDiffService diffService = new WorkflowDefinitionDiffServiceImpl();

    @Test
    void testDiffReportsAddedWhenOldIsNull() {
        WorkflowDefinitionDiff diff = diffService.diff(null, json("{\"label\":\"L\",\"tasks\":[]}"));

        assertThat(diff.changeType()).isEqualTo(WorkflowChangeType.ADDED);
        assertThat(diff.oldDefinition()).isNull();
        assertThat(diff.newDefinition()).contains("\"label\" : \"L\"");
    }

    @Test
    void testDiffReportsRemovedWhenNewIsNull() {
        WorkflowDefinitionDiff diff = diffService.diff(json("{\"label\":\"L\",\"tasks\":[]}"), null);

        assertThat(diff.changeType()).isEqualTo(WorkflowChangeType.REMOVED);
        assertThat(diff.newDefinition()).isNull();
        assertThat(diff.oldDefinition()).isNotNull();
    }

    @Test
    void testDiffReportsUnchangedForReorderedKeys() {
        WorkflowDefinitionDiff diff = diffService.diff(
            json("{\"label\":\"L\",\"tasks\":[]}"), json("{\n  \"tasks\": [],\n  \"label\": \"L\"\n}"));

        assertThat(diff.changeType()).isEqualTo(WorkflowChangeType.UNCHANGED);
        assertThat(diff.changes()).isEmpty();
        assertThat(diff.oldDefinition()).isEqualTo(diff.newDefinition());
    }

    @Test
    void testDiffReportsUnchangedForSameContentInDifferentFormats() {
        WorkflowDefinitionDiff diff = diffService.diff(
            json("{\"label\":\"L\",\"tasks\":[]}"), new Workflow("label: L\ntasks: []\n", Workflow.Format.YAML));

        assertThat(diff.changeType()).isEqualTo(WorkflowChangeType.UNCHANGED);
    }

    @Test
    void testDiffMasksBothDefinitions() {
        WorkflowDefinitionDiff diff = diffService.diff(
            json("{\"tasks\":[{\"name\":\"t\",\"type\":\"a/v1/b\",\"parameters\":{\"password\":\"x\"}}]}"),
            json("{\"tasks\":[{\"name\":\"t\",\"type\":\"a/v1/b\",\"parameters\":{\"password\":\"y\"}}]}"));

        assertThat(diff.changeType()).isEqualTo(WorkflowChangeType.MODIFIED);
        assertThat(diff.oldDefinition()).contains(SensitiveValueMasker.MASK)
            .doesNotContain("\"x\"");
        assertThat(diff.newDefinition()).contains(SensitiveValueMasker.MASK)
            .doesNotContain("\"y\"");
    }

    private static Workflow json(String definition) {
        return new Workflow(definition, Workflow.Format.JSON);
    }
}
