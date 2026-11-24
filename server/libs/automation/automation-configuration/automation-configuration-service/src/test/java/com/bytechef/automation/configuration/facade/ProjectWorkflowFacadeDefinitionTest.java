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

package com.bytechef.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.automation.configuration.exception.ProjectErrorType;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ProjectWorkflowFacadeDefinitionTest {

    @Test
    void testToJsonDefinitionReturnsAJsonDefinitionUnchanged() {
        String definition = "{\"label\":\"Workflow\",\"tasks\":[]}";

        assertThat(ProjectWorkflowFacadeImpl.toJsonDefinition(definition)).isEqualTo(definition);
    }

    @Test
    void testToJsonDefinitionConvertsAYamlDefinitionToJson() {
        String definition = """
            label: Workflow
            triggers:
              - name: trigger_1
                type: manual/v1/manual
            tasks:
              - name: logger_1
                type: logger/v1/info
                parameters:
                  text: Hello
            """;

        String jsonDefinition = ProjectWorkflowFacadeImpl.toJsonDefinition(definition);

        Map<String, Object> workflowMap = JsonUtils.readMap(jsonDefinition, Object.class);

        assertThat(workflowMap).containsEntry("label", "Workflow");
        assertThat((List<?>) workflowMap.get("triggers")).hasSize(1);
        assertThat((List<?>) workflowMap.get("tasks")).hasSize(1);
    }

    @Test
    void testToJsonDefinitionRejectsAnInvalidJsonDefinition() {
        assertInvalidWorkflowDefinition("{\"label\": \"Workflow\",");
    }

    @Test
    void testToJsonDefinitionRejectsAnInvalidYamlDefinition() {
        assertInvalidWorkflowDefinition("label: [Workflow");
    }

    @Test
    void testToJsonDefinitionRejectsADefinitionThatIsNotAnObject() {
        assertInvalidWorkflowDefinition("just some text");
    }

    @Test
    void testToJsonDefinitionRejectsAnEmptyDefinition() {
        assertInvalidWorkflowDefinition("  ");
    }

    private static void assertInvalidWorkflowDefinition(String definition) {
        assertThatThrownBy(() -> ProjectWorkflowFacadeImpl.toJsonDefinition(definition))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(ProjectErrorType.INVALID_WORKFLOW_DEFINITION.getErrorKey()));
    }
}
